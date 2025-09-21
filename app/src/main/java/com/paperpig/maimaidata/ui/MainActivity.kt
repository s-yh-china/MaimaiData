package com.paperpig.maimaidata.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener3
import com.paperpig.maimaidata.BuildConfig
import com.paperpig.maimaidata.MaimaiDataApplication
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityMainBinding
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.network.MaimaiDataRequests
import com.paperpig.maimaidata.repository.ChartRepository
import com.paperpig.maimaidata.repository.ChartStatsRepository
import com.paperpig.maimaidata.repository.SongWithChartRepository
import com.paperpig.maimaidata.ui.rating.RatingFragment
import com.paperpig.maimaidata.ui.songlist.SongListFragment
import com.paperpig.maimaidata.utils.JsonConvertToDb
import com.paperpig.maimaidata.utils.SpUtil
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream

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
            if (it.version > BuildConfig.VERSION_NAME && it.url.isNotBlank()) {
                MaterialDialog.Builder(this)
                    .title(this@MainActivity.getString(R.string.maimai_data_update_title, it.version))
                    .content((it.info ?: this@MainActivity.getString(R.string.maimai_data_update_default_content)).replace("\\n", "\n"))
                    .positiveText(R.string.maimai_data_update_download)
                    .negativeText(R.string.common_cancel)
                    .onPositive { _, which ->
                        if (DialogAction.POSITIVE == which) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
                                val uri = ("package:$packageName").toUri()
                                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, uri)
                                startActivity(intent)
                                return@onPositive
                            } else {
                                startDownload(it.url, "new_version.apk") { task ->
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    val fileProviderUri = FileProvider.getUriForFile(
                                        this, "${application.packageName}.fileprovider", task.file!!
                                    )
                                    intent.setDataAndType(fileProviderUri, "application/vnd.android.package-archive")
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    startActivity(intent)
                                }
                            }
                        }
                    }
                    .onNegative { d, _ -> d.dismiss() }
                    .autoDismiss(true).cancelable(true).show()
            } else {
                updateDisposable = MaimaiDataRequests.getDataVersion().subscribe({ it ->
                    isUpdateChecked = true
                    if (SpUtil.getDataVersion() < it.version) {
                        MaterialDialog.Builder(this)
                            .title(this@MainActivity.getString(R.string.maimai_data_data_update_title))
                            .content(String.format(this@MainActivity.getString(R.string.maimai_data_data_update_info), SpUtil.getDataVersion(), it.version))
                            .positiveText(R.string.maimai_data_update_download)
                            .negativeText(R.string.common_cancel)
                            .onPositive { _, which ->
                                if (DialogAction.POSITIVE == which) {
                                    startDownload(it.songListUrl, "song_list_data.json") { task ->
                                        lifecycleScope.launch {
                                            val data = getSongListData(this@MainActivity)
                                            if (SongWithChartRepository.getInstance().updateDatabase(data)) {
                                                SpUtil.setDataVersion(it.version)
                                                checkChartStatus(force = true)
                                            }
                                        }
                                    }
                                }
                            }
                            .onNegative { d, _ -> d.dismiss() }
                            .autoDismiss(true).cancelable(true).show()
                    }
                }, { it ->
                    Toast.makeText(this, "数据版本检查失败", Toast.LENGTH_LONG).show()
                    it.printStackTrace()
                })
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
        ChartRepository.getInstance().getMaxNotes().observe(this) {
            MaimaiDataApplication.instance.maxNotesStats = it
        }
    }

    private fun checkChartStatus(force: Boolean = false) {
        if (SpUtil.getDataVersion() != "0") {
            val currentTime = System.currentTimeMillis()
            if ((currentTime - SpUtil.getLastUpdateChartStats()) >= 5 * 24 * 60 * 60 * 1000L || force) {
                checkChartStatusDisposable = MaimaiDataRequests.getChartStats(SpUtil.getDataVersion()).subscribe({
                    lifecycleScope.launch {
                        val allSongs = withContext(Dispatchers.IO) { SongWithChartRepository.getInstance().getAllSong() }
                        val convertChatStats = JsonConvertToDb.convertChatStats(it, allSongs)
                        if (ChartStatsRepository.getInstance().replaceAllChartStats(convertChatStats)) {
                            SpUtil.saveLastUpdateChartStats(it.time * 1000L)
                        }
                    }
                }, {
                    it.printStackTrace()
                    Toast.makeText(this, "谱面状态数据下载失败", Toast.LENGTH_LONG).show()
                })
            }
        }
    }

    private fun startDownload(url: String, filename: String, onCompleted: (DownloadTask) -> Unit) {
        val updateDialog = MaterialDialog.Builder(this).title(getString(R.string.maimai_data_download_title)).content(getString(R.string.maimai_data_start_download)).cancelable(false).show()
        downloadTask = DownloadTask.Builder(url, filesDir.path, filename).setConnectionCount(1).setMinIntervalMillisCallbackProcess(16).setPassIfAlreadyCompleted(false).build()

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
                onCompleted(task)
            }

            override fun canceled(task: DownloadTask) {
                updateDialog.dismiss()
            }

            override fun error(task: DownloadTask, e: Exception) {
                e.message?.let { Log.e("Download", it) }
                Toast.makeText(this@MainActivity, getString(R.string.maimai_data_download_error), Toast.LENGTH_SHORT).show()
                updateDialog.dismiss()
            }

            override fun warn(task: DownloadTask) {
            }
        })
    }

    suspend fun getSongListData(context: Context?): List<SongData> {
        return withContext(Dispatchers.IO) {
            try {
                val fileInputStream: FileInputStream? = context?.openFileInput("song_list_data.json")
                val list = fileInputStream?.bufferedReader().use { it?.readText() }
                Gson().fromJson(list, object : TypeToken<List<SongData>>() {}.type)
            } catch (_: Exception) {
                emptyList()
            }
        }
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