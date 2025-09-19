package com.paperpig.maimaidata.ui.songdetail

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivitySongDetailBinding
import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.db.entity.RecordEntity
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity
import com.paperpig.maimaidata.glide.GlideApp
import com.paperpig.maimaidata.model.SongType
import com.paperpig.maimaidata.network.MaimaiDataClient
import com.paperpig.maimaidata.repository.AliasRepository
import com.paperpig.maimaidata.repository.RecordRepository
import com.paperpig.maimaidata.utils.*
import com.paperpig.maimaidata.widgets.Settings

class SongDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySongDetailBinding

    private lateinit var data: SongWithChartsEntity

    companion object {
        const val EXTRA_DATA_KEY = "data"

        fun actionStart(context: Context, songWithChartsEntity: SongWithChartsEntity) {
            val intent = Intent(context, SongDetailActivity::class.java).apply {
                putExtra(EXTRA_DATA_KEY, songWithChartsEntity)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
        binding = ActivitySongDetailBinding.inflate(layoutInflater)

        with(binding) {
            setContentView(root)

            setSupportActionBar(toolbar)
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                setDisplayShowHomeEnabled(true)
            }

            data = intent.getParcelableExtra<SongWithChartsEntity>(EXTRA_DATA_KEY)!!
            val songData = data.songData

            // 设置背景颜色
            appbarLayout.setBackgroundColor(
                ContextCompat.getColor(
                    this@SongDetailActivity,
                    songData.bgColor
                )
            )
            tabLayout.apply {
                setSelectedTabIndicatorColor(
                    ContextCompat.getColor(
                        this@SongDetailActivity,
                        songData.bgColor
                    )
                )
                setTabTextColors(
                    Color.BLACK, ContextCompat.getColor(
                        this@SongDetailActivity,
                        songData.bgColor
                    )
                )
            }
            toolbarLayout.setContentScrimResource(songData.bgColor)
            GlideApp.with(this@SongDetailActivity)
                .load(MaimaiDataClient.IMAGE_BASE_URL + songData.imageUrl)
                .into(songJacket)
            songJacket.setBackgroundColor(
                ContextCompat.getColor(
                    this@SongDetailActivity,
                    songData.strokeColor
                )
            )

            // 显示歌曲信息
            songTitle.apply {
                text = songData.title

                setShrinkOnTouch()
                setCopyOnLongClick(songData.title)
            }

            songIdText.apply {
                text = songData.id.toString()

                setShrinkOnTouch()
                setCopyOnLongClick(songData.id.toString())
            }

            songArtist.text = songData.artist
            songBpm.text = songData.bpm.toString()
            songGenre.text = songData.genre
            GlideApp.with(this@SongDetailActivity).apply {
                when (songData.type) {
                    SongType.DX -> {
                        load(R.drawable.ic_deluxe).into(binding.songType)
                    }

                    SongType.SD -> {
                        load(R.drawable.ic_standard).into(binding.songType)
                    }

                    SongType.UTAGE -> {
                        // load(R.drawable.ic_utage).into(binding.songType) TODO find a utage icon
                    }
                }
            }
            setVersionImage(songAddVersion, songData.jpVersion)
            setCnVersionImage(songAddCnVersion, songData.version)

            val colorFilter: (Boolean) -> Int = { isFavor: Boolean ->
                if (isFavor) {
                    0
                } else {
                    Color.WHITE
                }
            }
            favButton.apply {
                setColorFilter(colorFilter.invoke(SpUtil.isFavorite(songData.id.toString())))
                setOnClickListener {
                    val isFavor = SpUtil.isFavorite(songData.id.toString())
                    SpUtil.setFavorite(songData.id.toString(), !isFavor)
                    setColorFilter(colorFilter.invoke(!isFavor))
                }
            }

            // 显示别名
            if (Settings.getEnableShowAlias()) {
                AliasRepository.getInstance(AppDataBase.getInstance().aliasDao())
                    .getAliasListBySongId(songData.id).observe(this@SongDetailActivity) {
                        // 对添加的别名进行flow约束
                        if (it.isNotEmpty()) {
                            val aliasViewIds = songAliasFlow.referencedIds.toMutableList()
                            it.forEachIndexed { _, item ->
                                val textView = TextView(this@SongDetailActivity).apply {
                                    text = item.alias
                                    id = View.generateViewId()
                                    aliasViewIds.add(id)
                                    val padding = 5.toDp().toInt()
                                    setPadding(padding, padding, padding, padding)
                                    setBackgroundResource(R.drawable.mmd_song_alias_info_bg)
                                    setTextColor(
                                        ContextCompat.getColor(
                                            this@SongDetailActivity,
                                            songData.bgColor
                                        )
                                    )
                                    layoutParams = ConstraintLayout.LayoutParams(
                                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                                    )

                                    setShrinkOnTouch()
                                    setCopyOnLongClick(item.alias)
                                }
                                constraintLayout.addView(textView)
                            }
                            songAliasFlow.referencedIds = aliasViewIds.toIntArray()
                        } else {
                            aliasLabel.visibility = View.GONE
                        }
                    }
            } else {
                aliasLabel.visibility = View.GONE
            }

            // 打开歌曲大图
            songJacket.setOnClickListener {
                val options: ActivityOptions = ActivityOptions
                    .makeSceneTransitionAnimation(
                        this@SongDetailActivity,
                        binding.songJacket,
                        "shared_image"
                    )
                PinchImageActivity.actionStart(
                    this@SongDetailActivity,
                    MaimaiDataClient.IMAGE_BASE_URL + songData.imageUrl,
                    songData.id.toString(),
                    options.toBundle()
                )
            }

            // 设置收藏
            favButton.apply {
                val colorFilter: (Boolean) -> Int = { isFavor: Boolean ->
                    if (isFavor) {
                        0
                    } else {
                        Color.WHITE
                    }
                }
                setColorFilter(colorFilter.invoke(SpUtil.isFavorite(songData.id.toString())))
                setOnClickListener {
                    val isFavor = SpUtil.isFavorite(songData.id.toString())
                    SpUtil.setFavorite(songData.id.toString(), !isFavor)
                    setColorFilter(colorFilter.invoke(!isFavor))
                }
            }

            // 获取成绩数据
            RecordRepository.getInstance(AppDataBase.getInstance().recordDao()).getRecordsBySongId(songData.id).observe(this@SongDetailActivity) { setupFragments(it) }
        }
    }

    private fun setupFragments(recordList: List<RecordEntity>) {
        val list = ArrayList<Fragment>()

        (1..data.charts.size).forEach { i ->
            val position = data.charts.size - i
            list.add(SongLevelFragment.newInstance(data, position, recordList.find {
                it.songId == data.songData.id && it.levelIndex == position
            }))
        }

        binding.viewPager.adapter = LevelDataFragmentAdapter(supportFragmentManager, -1, list)
        binding.tabLayout.setupWithViewPager(binding.viewPager)
    }

    class LevelDataFragmentAdapter(
        fragmentManager: FragmentManager,
        behavior: Int,
        private val list: List<Fragment>
    ) : FragmentPagerAdapter(fragmentManager, behavior) {

        override fun getItem(position: Int): Fragment {
            return list[position]
        }

        override fun getCount(): Int {
            return list.size
        }

        override fun getPageTitle(position: Int): CharSequence {
            return when (count) {
                1 -> arrayOf("宴·会·场")[position]
                2 -> arrayOf("1p", "2p")[position]
                4 -> arrayOf("MAS", "EXP", "ADV", "BAS")[position]
                else -> arrayOf("Re:MAS", "MAS", "EXP", "ADV", "BAS")[position]
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }

    private fun setVersionImage(view: ImageView, addVersion: String) {
        @DrawableRes val versionDrawable = with(addVersion) {
            when {
                equals("maimai") -> R.drawable.maimai
                equals("maimai PLUS") -> R.drawable.maimai_plus
                equals("GreeN") -> R.drawable.maimai_green
                equals("GreeN PLUS") -> R.drawable.maimai_green_plus
                equals("ORANGE") -> R.drawable.maimai_orange
                equals("ORANGE PLUS") -> R.drawable.maimai_orange_plus
                equals("PiNK") -> R.drawable.maimai_pink
                equals("PiNK PLUS") -> R.drawable.maimai_pink_plus
                equals("MURASAKi") -> R.drawable.maimai_murasaki
                equals("MURASAKi PLUS") -> R.drawable.maimai_murasaki_plus
                equals("MiLK") -> R.drawable.maimai_milk
                equals("MiLK PLUS") -> R.drawable.maimai_milk_plus
                equals("FiNALE") -> R.drawable.maimai_finale
                equals("maimaiでらっくす") -> R.drawable.maimaidx
                equals("maimaiでらっくす PLUS") -> R.drawable.maimaidx_plus
                equals("Splash") -> R.drawable.maimaidx_splash
                equals("Splash PLUS") -> R.drawable.maimaidx_splash_plus
                equals("UNiVERSE") -> R.drawable.maimaidx_universe
                equals("UNiVERSE PLUS") -> R.drawable.maimaidx_universe_plus
                equals("FESTiVAL") -> R.drawable.maimaidx_festival
                equals("FESTiVAL PLUS") -> R.drawable.maimaidx_festival_plus
                equals("BUDDiES") -> R.drawable.maimaidx_buddies
                equals("BUDDiES PLUS") -> R.drawable.maimaidx_buddies_plus
                else -> 0
            }
        }
        Glide.with(view.context)
            .load(versionDrawable)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(view)
    }

    private fun setCnVersionImage(view: ImageView, addVersion: String) {
        @DrawableRes val versionDrawable = with(addVersion) {
            when {
                equals("舞萌DX") -> R.drawable.maimaidx_cn
                equals("舞萌DX 2021") -> R.drawable.maimaidx_2021
                equals("舞萌DX 2022") -> R.drawable.maimaidx_2022
                equals("舞萌DX 2023") -> R.drawable.maimaidx_2023
                equals("舞萌DX 2024") -> R.drawable.maimaidx_2024
                equals("舞萌DX 2025") -> R.drawable.maimaidx_2025
                else -> 0
            }
        }
        Glide.with(view.context)
            .load(versionDrawable)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(view)
    }
}