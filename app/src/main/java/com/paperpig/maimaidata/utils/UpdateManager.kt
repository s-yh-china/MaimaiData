package com.paperpig.maimaidata.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener3
import com.paperpig.maimaidata.BuildConfig
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.network.MaimaiDataRequests
import com.paperpig.maimaidata.repository.ChartStatsRepository
import com.paperpig.maimaidata.repository.SongWithRecordRepository
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream

class UpdateManager(val context: Context) {
    fun checkAppUpdate(ignoreSkip: Boolean = false, onCompleted: () -> Unit): Disposable {
        return MaimaiDataRequests.fetchUpdateInfo().subscribe({ updateInfo ->
            if (updateInfo.version > BuildConfig.VERSION_NAME && updateInfo.url.isNotBlank()) {
                if (!ignoreSkip && SpUtil.getSkipVersion() == updateInfo.version) {
                    onCompleted()
                    return@subscribe
                }

                MaterialDialog.Builder(context)
                    .title(context.getString(R.string.maimai_data_update_title, updateInfo.version))
                    .content((updateInfo.info ?: context.getString(R.string.maimai_data_update_default_content)).replace("\\n", "\n"))
                    .positiveText(R.string.maimai_data_update_download)
                    .negativeText(R.string.common_cancel)
                    .neutralText(R.string.maimai_data_version_skip)
                    .autoDismiss(false)
                    .onPositive { d, _ ->
                        d.dismiss()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                            startActivity(context, Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, ("package:${context.packageName}").toUri()), null)
                            return@onPositive
                        } else {
                            startDownload(context, updateInfo.url, "new_version.apk") { task ->
                                val intent = Intent(Intent.ACTION_VIEW)
                                val fileProviderUri = FileProvider.getUriForFile(
                                    context, "${context.applicationContext.packageName}.fileprovider", task.file!!
                                )
                                intent.setDataAndType(fileProviderUri, "application/vnd.android.package-archive")
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                startActivity(context, intent, null)
                            }
                        }
                    }
                    .onNegative { d, _ ->
                        d.dismiss()
                        onCompleted()
                    }
                    .onNeutral { d, _ ->
                        SpUtil.saveSkipVersion(updateInfo.version)
                        Toast.makeText(context, "已跳过 ${updateInfo.version} 版本", Toast.LENGTH_LONG).show()
                        d.dismiss()
                        onCompleted()
                    }
                    .cancelable(false)
                    .show()
            } else {
                onCompleted()
            }
        }, {
            Toast.makeText(context, context.getString(R.string.maimai_data_check_error), Toast.LENGTH_LONG).show()
            it.printStackTrace()
            onCompleted()
        })
    }

    fun checkDataUpdate(lifecycleOwner: LifecycleOwner, onCompleted: () -> Unit): Disposable {
        return MaimaiDataRequests.getDataVersion().subscribe({ it ->
            if (SpUtil.getDataVersion() < it.version) {
                MaterialDialog.Builder(context)
                    .title(context.getString(R.string.maimai_data_data_update_title))
                    .content(String.format(context.getString(R.string.maimai_data_data_update_info), SpUtil.getDataVersion(), it.version))
                    .positiveText(R.string.maimai_data_update_download)
                    .negativeText(R.string.common_cancel)
                    .onPositive { _, _ ->
                        startDownload(context, it.songListUrl, "song_list_data.json") { task ->
                            lifecycleOwner.lifecycleScope.launch {
                                val data = getSongListData(context)
                                if (SongWithRecordRepository.getInstance().updateDatabase(data)) {
                                    SpUtil.setDataVersion(it.version)
                                    checkChartStatusUpdate(lifecycleOwner, force = true)
                                }
                            }
                        }
                    }
                    .onNegative { d, _ -> d.dismiss() }
                    .onAny { _, _ -> onCompleted() }
                    .cancelable(false)
                    .show()
            } else {
                onCompleted()
            }
        }, { it ->
            Toast.makeText(context, context.getString(R.string.maimai_data_check_error), Toast.LENGTH_LONG).show()
            it.printStackTrace()
            onCompleted()
        })
    }

    fun checkChartStatusUpdate(lifecycleOwner: LifecycleOwner, force: Boolean = false): Disposable? {
        if (SpUtil.getDataVersion() != "0") {
            val currentTime = System.currentTimeMillis()
            if ((currentTime - SpUtil.getLastUpdateChartStats()) >= 5 * 24 * 60 * 60 * 1000L || force) {
                return MaimaiDataRequests.getChartStats(SpUtil.getDataVersion()).subscribe({
                    lifecycleOwner.lifecycleScope.launch {
                        val allSongs = withContext(Dispatchers.IO) { SongWithRecordRepository.getInstance().getAllSong() }
                        val convertChatStats = JsonConvertToDb.convertChatStats(it, allSongs)
                        if (ChartStatsRepository.getInstance().replaceAllChartStats(convertChatStats)) {
                            SpUtil.saveLastUpdateChartStats(it.time * 1000L)
                        }
                    }
                }, {
                    it.printStackTrace()
                    Toast.makeText(context, context.getString(R.string.maimai_data_check_error), Toast.LENGTH_LONG).show()
                })
            }
        }
        return null
    }

    private fun startDownload(context: Context, url: String, filename: String, onCompleted: (DownloadTask) -> Unit) {
        val updateDialog = MaterialDialog.Builder(context)
            .title(context.getString(R.string.maimai_data_download_title))
            .content(context.getString(R.string.maimai_data_start_download))
            .cancelable(false)
            .show()
        val downloadTask = DownloadTask.Builder(url, context.filesDir.path, filename)
            .setConnectionCount(1)
            .setMinIntervalMillisCallbackProcess(16)
            .setPassIfAlreadyCompleted(false)
            .build()

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
                Toast.makeText(context, context.getString(R.string.maimai_data_download_error), Toast.LENGTH_SHORT).show()
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
}