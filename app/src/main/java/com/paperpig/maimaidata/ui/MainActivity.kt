package com.paperpig.maimaidata.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener3
import com.paperpig.maimaidata.BuildConfig
import com.paperpig.maimaidata.MaimaiDataApplication
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityMainBinding
import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.model.AppUpdateModel
import com.paperpig.maimaidata.network.MaimaiDataRequests
import com.paperpig.maimaidata.repository.ChartRepository
import com.paperpig.maimaidata.repository.ChartStatsRepository
import com.paperpig.maimaidata.repository.SongDataRepository
import com.paperpig.maimaidata.repository.SongWithChartRepository
import com.paperpig.maimaidata.ui.rating.RatingFragment
import com.paperpig.maimaidata.ui.songlist.SongListFragment
import com.paperpig.maimaidata.utils.JsonConvertToDb
import com.paperpig.maimaidata.utils.SpUtil
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var ratingFragment: RatingFragment
    private lateinit var songListFragment: SongListFragment
    private var updateDisposable: Disposable? = null
    private var checkChartStatusDisposable: Disposable? = null
    private var isUpdateChecked = false
    private var downloadTask: DownloadTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarLayout.toolbar)

        checkChartStatus()

        queryMaxNotes()

        if (savedInstanceState != null) {
            supportActionBar?.title = savedInstanceState.getString("TOOLBAR_TITLE")

            supportFragmentManager.getFragment(savedInstanceState, SongListFragment.TAG)?.apply {
                songListFragment = this as SongListFragment
            }

            supportFragmentManager.getFragment(savedInstanceState, RatingFragment.TAG)?.apply {
                ratingFragment = this as RatingFragment
            }
        } else {
            showFragment(R.id.navDXSongList)
        }

        binding.mainBottomNaviView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navDXSongList -> {
                    showFragment(R.id.navDXSongList)
                    true
                }

                R.id.navDxTarget -> {
                    showFragment(R.id.navDxTarget)
                    true
                }

                else -> {
                    true
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("TOOLBAR_TITLE", supportActionBar?.title.toString())
        if (::songListFragment.isInitialized) supportFragmentManager.putFragment(
            outState, SongListFragment.TAG, songListFragment
        )
        if (::ratingFragment.isInitialized) supportFragmentManager.putFragment(
            outState, RatingFragment.TAG, ratingFragment
        )
    }

    override fun onResume() {
        super.onResume()
        if (!isUpdateChecked) {
            updateDisposable?.dispose()
            checkUpdate()
        }
    }

    /**
     * check update
     */
    private fun checkUpdate() {
        updateDisposable = MaimaiDataRequests.fetchUpdateInfo().subscribe({
            isUpdateChecked = true
            if (it.version > BuildConfig.VERSION_NAME && it.url.isNotBlank()) {
                MaterialDialog.Builder(this)
                    .title(this@MainActivity.getString(R.string.maimai_data_update_title, it.version))
                    .content((it.info ?: this@MainActivity.getString(R.string.maimai_data_update_default_content)).replace("\\n", "\n"))
                    .positiveText(R.string.maimai_data_update_download)
                    .negativeText(R.string.common_cancel)
                    .onPositive { _, which ->
                        if (DialogAction.POSITIVE == which) {
                            startActivity(Intent().apply {
                                action = Intent.ACTION_VIEW
                                data = it.url.toUri()
                            })
                        }
                    }
                    .onNegative { d, _ -> d.dismiss() }
                    .autoDismiss(true).cancelable(true).show()
            } else if (SpUtil.getDataVersion() < it.dataVersion) {
                MaterialDialog.Builder(this)
                    .title(this@MainActivity.getString(R.string.maimai_data_data_update_title))
                    .content(String.format(this@MainActivity.getString(R.string.maimai_data_data_update_info), SpUtil.getDataVersion(), it.dataVersion))
                    .positiveText(R.string.maimai_data_update_download)
                    .negativeText(R.string.common_cancel)
                    .onPositive { _, which ->
                        if (DialogAction.POSITIVE == which) {
                            startDataDownload(it)
                        }
                    }
                    .onNegative { d, _ -> d.dismiss() }
                    .autoDismiss(true).cancelable(true).show()
            }
        }, {
            Toast.makeText(this, "版本检查失败", Toast.LENGTH_LONG).show()
            it.printStackTrace()
        })
    }

    /**
     * 查询最大notes数量
     */
    private fun queryMaxNotes() {
        ChartRepository.getInstance(AppDataBase.getInstance().chartDao()).getMaxNotes().observe(this) {
            MaimaiDataApplication.instance.maxNotesStats = it
        }
    }

    /**
     * 检查水鱼谱面数据
     */
    private fun checkChartStatus() {
        // 每五天更新数据
        val lastUpdateTime = SpUtil.getLastUpdateChartStats()
        val currentTime = System.currentTimeMillis()
        if ((currentTime - lastUpdateTime) >= 5 * 24 * 60 * 60 * 1000L) {
            checkChartStatusDisposable = MaimaiDataRequests.getChartStatus().subscribe({ t ->
                lifecycleScope.launch {
                    val convertChatStats = JsonConvertToDb.convertChatStats(t)
                    val result = ChartStatsRepository.getInstance(
                        AppDataBase.getInstance().chartStatsDao()
                    ).replaceAllChartStats(convertChatStats)
                    if (result) {
                        SpUtil.saveLastUpdateChartStats(currentTime)
                    }
                }
            }, {
                it.printStackTrace()
                Toast.makeText(this, "谱面状态数据下载失败", Toast.LENGTH_LONG).show()
            })
        }
    }

    private fun startDataDownload(appUpdateModel: AppUpdateModel) {
        val updateDialog = MaterialDialog.Builder(this).title(getString(R.string.maimai_data_download_title)).content(getString(R.string.maimai_data_start_download)).cancelable(false).show()
        downloadTask = DownloadTask.Builder(appUpdateModel.dataUrl, filesDir.path, "songdata.json").setMinIntervalMillisCallbackProcess(16).setPassIfAlreadyCompleted(false).build()

        downloadTask!!.enqueue(object : DownloadListener3() {
            override fun retry(task: DownloadTask, cause: ResumeFailedCause) {
            }

            override fun connected(task: DownloadTask, blockCount: Int, currentOffset: Long, totalLength: Long) {
            }

            override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {
                updateDialog.setContent("$currentOffset/$totalLength")
            }

            override fun started(task: DownloadTask) {
                updateDialog.show()
            }

            override fun completed(task: DownloadTask) {
                updateDialog.dismiss()
                lifecycleScope.launch {
                    val data = SongDataRepository().getData(this@MainActivity)
                    if (SongWithChartRepository.getInstance(AppDataBase.getInstance().songWithChartDao()).updateDatabase(data)) {
                        SpUtil.setDataVersion(appUpdateModel.dataVersion)
                    }
                }
            }

            override fun canceled(task: DownloadTask) {
                updateDialog.dismiss()
            }

            override fun error(task: DownloadTask, e: Exception) {
                Toast.makeText(this@MainActivity, getString(R.string.maimai_data_download_error), Toast.LENGTH_SHORT).show()
                updateDialog.dismiss()
            }

            override fun warn(task: DownloadTask) {
            }
        })
    }

    private fun showFragment(int: Int) {
        invalidateMenu()
        val ft = supportFragmentManager.beginTransaction()
        hideAllFragment(ft)
        when (int) {
            R.id.navDxTarget -> {
                supportActionBar?.setTitle(R.string.dx_rating_correlation)
                if (!::ratingFragment.isInitialized) {
                    ratingFragment = RatingFragment.newInstance()
                    ft.add(R.id.fragment_content, ratingFragment, RatingFragment.TAG)
                } else {
                    ft.show(ratingFragment)
                }
            }

            R.id.navDXSongList -> {
                supportActionBar?.setTitle(R.string.dx_song_list)
                if (!::songListFragment.isInitialized) {
                    songListFragment = SongListFragment.newInstance()
                    ft.add(R.id.fragment_content, songListFragment, SongListFragment.TAG)
                } else {
                    ft.show(songListFragment)
                }

            }
        }
        ft.commit()
    }

    private fun hideAllFragment(ft: FragmentTransaction) {
        ft.apply {
            if (::ratingFragment.isInitialized) {
                hide(ratingFragment)
            }
            if (::songListFragment.isInitialized) {
                hide(songListFragment)
            }
        }
    }
}