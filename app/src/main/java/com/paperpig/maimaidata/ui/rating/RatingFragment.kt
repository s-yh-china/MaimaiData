package com.paperpig.maimaidata.ui.rating

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.view.MenuProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.crawler.CrawlerCaller
import com.paperpig.maimaidata.crawler.CrawlerCaller.writeLog
import com.paperpig.maimaidata.crawler.WechatCrawlerListener
import com.paperpig.maimaidata.databinding.FragmentRatingBinding
import com.paperpig.maimaidata.model.Rating
import com.paperpig.maimaidata.model.SongRank
import com.paperpig.maimaidata.network.MaimaiDataRequests
import com.paperpig.maimaidata.network.server.HttpServerService
import com.paperpig.maimaidata.network.vpn.core.LocalVpnService
import com.paperpig.maimaidata.repository.RecordRepository
import com.paperpig.maimaidata.ui.BaseFragment
import com.paperpig.maimaidata.ui.about.SettingsActivity
import com.paperpig.maimaidata.ui.checklist.GenreCheckActivity
import com.paperpig.maimaidata.ui.checklist.LevelCheckActivity
import com.paperpig.maimaidata.ui.checklist.VersionCheckActivity
import com.paperpig.maimaidata.ui.checklist.VersionClearCheckActivity
import com.paperpig.maimaidata.ui.rating.best50.ProberActivity
import com.paperpig.maimaidata.utils.JsonConvertToDb
import com.paperpig.maimaidata.utils.SpUtil
import com.paperpig.maimaidata.utils.getInt
import com.paperpig.maimaidata.widgets.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random

class RatingFragment : BaseFragment<FragmentRatingBinding>(), WechatCrawlerListener, LocalVpnService.onStatusChangedListener {

    private lateinit var binding: FragmentRatingBinding

    private lateinit var resultAdapter: RatingResultAdapter

    private val proberUpdateDialog by lazy { ProberUpdateDialog(requireContext()) }

    private val httpServiceIntent by lazy { Intent(requireContext(), HttpServerService::class.java) }

    private val vpnActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            startProxyServices()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = RatingFragment()

        const val TAG = "RatingFragment"
    }

    override fun getViewBinding(container: ViewGroup?): FragmentRatingBinding {
        binding = FragmentRatingBinding.inflate(layoutInflater, container, false)
        return binding
    }

    override fun onResume() {
        super.onResume()
        binding.accountText.setText(Settings.getNickname())
        setProberUpdateButton(LocalVpnService.IsRunning)
    }

    @SuppressLint("CheckResult")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ratingResultRecycler.apply {
            resultAdapter = RatingResultAdapter()
            layoutManager = LinearLayoutManager(context)
            adapter = resultAdapter
        }

        binding.proberLevelCheckBtn.setOnClickListener {
            startActivity(Intent(context, LevelCheckActivity::class.java))
        }

        binding.proberVersionCheckBtn.setOnClickListener {
            startActivity(Intent(context, VersionCheckActivity::class.java))
        }
        binding.proberVersionCheckBtn.setOnLongClickListener {
            startActivity(Intent(context, VersionClearCheckActivity::class.java))
            true
        }

        binding.proberGenreCheckBtn.setOnClickListener {
            startActivity(Intent(context, GenreCheckActivity::class.java))
        }

        binding.proberBest50Btn.setOnClickListener {
            startActivity(Intent(context, ProberActivity::class.java))
        }

        binding.calculateBtn.setOnClickListener {
            hideKeyboard(view)
            onCalculate(binding.targetRatingEdit.text.toString())
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val songLevel = binding.inputSongLevel.text.toString()
                val songAchievement = binding.inputSongAchievement.text.toString()
                if (songAchievement.isNotEmpty() && songAchievement != "." && songLevel.isNotEmpty() && songLevel != ".") {
                    binding.outputSingleRating.text = SongRank.achievementToRating(
                        binding.inputSongLevel.text.toString().toDouble(), binding.inputSongAchievement.text.toString().toDouble()
                    ).toString()
                } else {
                    binding.outputSingleRating.text = "0"
                }
            }
        }

        binding.inputSongLevel.addTextChangedListener(textWatcher)
        binding.inputSongAchievement.addTextChangedListener(textWatcher)

        CrawlerCaller.setOnWechatCrawlerListener(this)
        LocalVpnService.addOnStatusChangedListener(this)

        binding.proberProxySimpleText.setOnClickListener {
            proberUpdateDialog.show()
        }

        binding.proberProxyUpdateBtn.setOnClickListener {
            if (LocalVpnService.IsRunning) {
                LocalVpnService.IsRunning = false
                stopHttpService()
            } else {
                if (!Settings.getProberUpdateUseAPI() || SpUtil.getUserId().isNullOrEmpty()) {
                    val intent: Intent? = LocalVpnService.prepare(context)
                    if (intent == null) {
                        startProxyServices()
                    } else {
                        vpnActivityResultLauncher.launch(intent)
                    }
                } else {
                    onStartAuth()
                    onMessageReceived("开始获取数据")
                    MaimaiDataRequests.getUserMusicData(SpUtil.getUserId()!!).subscribe(
                        { data ->
                            onMessageReceived("已获取数据，正在载入")
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    val records = JsonConvertToDb.convertUserRecordData(data, Settings.getUpdateDifficulty())
                                    RecordRepository.getInstance().replaceAllRecord(records)
                                    writeLog("maimai 数据更新完成，共加载了 ${records.size} 条数据")
                                }
                                onFinishUpdate()
                            }
                        },
                        { e ->
                            onError(e)
                            onMessageReceived("因未知原因数据获取失败")
                        }
                    )
                }
            }
        }
        setProberUpdateButton(false)

        binding.proberProxyUpdateHelpIv.setOnClickListener {
            showHelpDialog()
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.settings).isVisible = !isHidden
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.about_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.settings -> {
                        startActivity(Intent(requireContext(), SettingsActivity::class.java))
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun startProxyServices() {
        startVPNService()
        startHttpService()
        if (!Settings.getProberUpdateUseAPI()) {
            createLinkUrl()
        } else {
            Toast.makeText(requireContext(), "请在微信中打开登陆二维码", Toast.LENGTH_SHORT).show()
        }
        getWechatApi()
    }

    private fun createLinkUrl() {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val randomChar = (1..10)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")

        val link = "http://127.0.0.2:8284/$randomChar"

        val mClipData = ClipData.newPlainText("copyText", link)
        (requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(mClipData)

        Toast.makeText(requireContext(), "已复制链接，请在微信中粘贴并打开", Toast.LENGTH_SHORT).show()
    }

    private fun getWechatApi() {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            val cmp = ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI")
            intent.apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                component = cmp
            }
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
        }
    }

    private fun startVPNService() {
        requireContext().startService(Intent(requireContext(), LocalVpnService::class.java))
    }

    private fun startHttpService() {
        requireContext().startService(httpServiceIntent)
    }

    private fun stopHttpService() {
        requireContext().stopService(httpServiceIntent)
    }

    private fun onCalculate(targetString: String) {
        val targetRating = targetString.getInt()
        if (targetRating <= 0) {
            Toast.makeText(this.context, "请输入文本!", Toast.LENGTH_SHORT).show()
            return
        }
        val rating = targetRating / 50
        val minLevel = getReachableLevel(rating)

        val results = (150 downTo minLevel)
            .map { it to getReachableAchievement(it, rating) }
            .filter { (_, achievement) ->
                when (achievement) {
                    800000, 900000, 940000 -> true
                    in 970000..1010000 -> true
                    else -> false
                }
            }
            .groupBy { (_, achievement) -> achievement }
            .mapValues { (_, pairs) -> pairs.minByOrNull { (level, _) -> level }?.first }
            .mapNotNull { (achievement, minLevel) ->
                if (minLevel == null) return@mapNotNull null

                val ratingValue = SongRank.achievementToRating(minLevel, achievement)
                Rating(
                    innerLevel = minLevel / 10f,
                    achievement = String.format(Locale.getDefault(), "%.4f%%", achievement / 10000f),
                    rating = ratingValue,
                    total = ratingValue * 50
                )
            }

        resultAdapter.setData(results)
    }

    override fun onStatusChanged(status: String, isRunning: Boolean) {
        setProberUpdateButton(isRunning)
    }

    @SuppressLint("SetTextI18n")
    override fun onLogReceived(logString: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
        proberUpdateDialog.appendText("[$timestamp] $logString\n")
    }

    @SuppressLint("SetTextI18n")
    override fun onMessageReceived(logString: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
        proberUpdateDialog.appendText("[$timestamp] $logString\n")
        binding.proberProxySimpleText.text = "[$timestamp] $logString"
    }

    override fun onStartAuth() {
        binding.proberProxySimpleText.text = ""
        binding.proberProxyIndicator.isIndeterminate = true
        binding.proberProxyStatusGroup.visibility = View.VISIBLE
    }

    override fun onFinishUpdate() {
        binding.proberProxyIndicator.visibility = View.INVISIBLE
        stopHttpService()
        setProberUpdateButton(false)
    }

    @SuppressLint("SetTextI18n")
    override fun onError(e: Throwable) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
        proberUpdateDialog.appendText("[$timestamp] $e\n")
        binding.proberProxySimpleText.text = "[$timestamp] $e"
        binding.proberProxyIndicator.visibility = View.INVISIBLE
        stopHttpService()
    }

    override fun onDestroy() {
        super.onDestroy()
        CrawlerCaller.removeOnWechatCrawlerListener()
        LocalVpnService.removeOnStatusChangedListener(this)
    }

    private fun showHelpDialog() {
        if (isServerMaintenance()) {
            MaterialDialog.Builder(requireContext())
                .title(getString(R.string.prober_update_disable_help))
                .content(R.string.prober_update_disable_content)
                .build()
                .show()
            return
        }
        if (!Settings.getProberUpdateUseAPI()) {
            val helpStringBuilder = getString(R.string.prober_update_content_step1) + "\n" +
                getString(R.string.prober_update_content_step2) + "\n" +
                getString(R.string.prober_update_content_step3) + "\n" +
                getString(R.string.prober_update_content_step4) + "\n"
            MaterialDialog.Builder(requireContext())
                .title(getString(R.string.prober_update_help))
                .content(helpStringBuilder)
                .build()
                .show()
        } else {
            if (!SpUtil.getUserId().isNullOrEmpty()) {
                MaterialDialog.Builder(requireContext())
                    .title(getString(R.string.prober_update_help))
                    .content(R.string.prober_update_api_help_content_step1)
                    .build()
                    .show()
            } else {
                val helpStringBuilder = getString(R.string.prober_update_api_user_id_bind_content_step1) + "\n" +
                    getString(R.string.prober_update_api_user_id_bind_content_step2) + "\n" +
                    getString(R.string.prober_update_api_user_id_bind_content_step3) + "\n" +
                    getString(R.string.prober_update_api_user_id_bind_content_step4) + "\n"
                MaterialDialog.Builder(requireContext())
                    .title(getString(R.string.prober_update_api_user_id_bind_help))
                    .content(helpStringBuilder)
                    .build()
                    .show()
            }
        }
    }

    private fun setProberUpdateButton(isVpnRunning: Boolean) {
        if (isVpnRunning) {
            binding.proberProxyUpdateBtn.setText(R.string.update_prober_stop_proxy)
        } else {
            if (isServerMaintenance()) {
                binding.proberProxyUpdateBtn.isEnabled = false
                binding.proberProxyUpdateBtn.setText(R.string.update_prober_disable)
            } else {
                if (!Settings.getProberUpdateUseAPI() || !SpUtil.getUserId().isNullOrEmpty()) {
                    binding.proberProxyUpdateBtn.setText(R.string.update_prober)
                } else {
                    binding.proberProxyUpdateBtn.setText(R.string.update_prober_api_user_id_bind)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private val UTC_PLUS_8 = ZoneId.of("Asia/Shanghai")

@RequiresApi(Build.VERSION_CODES.O)
private val START_TIME = LocalTime.of(4, 0)

@RequiresApi(Build.VERSION_CODES.O)
private val END_TIME = LocalTime.of(7, 0)

fun isServerMaintenance(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        return LocalTime.now(UTC_PLUS_8).let { time -> time.isAfter(START_TIME) && time.isBefore(END_TIME) }
    } else {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+08:00"))
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return ((hour > 4) || (hour == 4)) && (hour < 7)
    }
}

private fun getReachableLevel(rating: Int): Int = (10..150).firstOrNull { rating < SongRank.achievementToRating(it, 1005000) } ?: 0

private fun getReachableAchievement(level: Int, rating: Int): Int {
    if (SongRank.achievementToRating(level, 1005000) < rating) {
        return 1010001
    }

    var maxAchi = 1010000
    var minAchi = 0
    var midAchi: Int

    repeat(21) {
        if (maxAchi - minAchi >= 2) {
            midAchi = (maxAchi + minAchi) / 2

            if (SongRank.achievementToRating(level, midAchi) < rating) {
                minAchi = midAchi
            } else {
                maxAchi = midAchi
            }
        }
    }
    return maxAchi
}
