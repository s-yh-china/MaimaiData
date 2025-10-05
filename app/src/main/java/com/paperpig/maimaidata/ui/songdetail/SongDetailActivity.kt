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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivitySongDetailBinding
import com.paperpig.maimaidata.db.entity.SongWithRecordEntity
import com.paperpig.maimaidata.glide.GlideApp
import com.paperpig.maimaidata.model.DifficultyType
import com.paperpig.maimaidata.model.GameSongObject
import com.paperpig.maimaidata.model.SongType
import com.paperpig.maimaidata.network.MaimaiDataClient
import com.paperpig.maimaidata.repository.AliasRepository
import com.paperpig.maimaidata.repository.SongWithRecordRepository
import com.paperpig.maimaidata.ui.PinchImageActivity
import com.paperpig.maimaidata.utils.PictureUtils
import com.paperpig.maimaidata.utils.SongSortHelper
import com.paperpig.maimaidata.utils.SpUtil
import com.paperpig.maimaidata.utils.setCopyOnLongClick
import com.paperpig.maimaidata.utils.setShrinkOnTouch
import com.paperpig.maimaidata.utils.toDp
import com.paperpig.maimaidata.widgets.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val EXTRA_DATA_KEY = "data"
private const val EXTRA_INITIAL_DIFFICULTY_KEY = "initial_difficulty"

class SongDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySongDetailBinding
    private lateinit var data: SongWithRecordEntity

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

            data = intent.getParcelableExtra<SongWithRecordEntity>(EXTRA_DATA_KEY)!!
            val songData = data.songData

            // 设置背景颜色
            appbarLayout.setBackgroundColor(ContextCompat.getColor(this@SongDetailActivity, songData.bgColor))
            tabLayout.apply {
                setSelectedTabIndicatorColor(ContextCompat.getColor(this@SongDetailActivity, songData.bgColor))
                setTabTextColors(Color.BLACK, ContextCompat.getColor(this@SongDetailActivity, songData.bgColor))
            }
            toolbarLayout.setContentScrimResource(songData.bgColor)
            GlideApp.with(this@SongDetailActivity).load(MaimaiDataClient.IMAGE_BASE_URL + songData.imageUrl).into(songJacket)
            songJacket.setBackgroundColor(ContextCompat.getColor(this@SongDetailActivity, songData.strokeColor))

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
                if (songData.type != SongType.UTAGE) {
                    load(songData.type.icon).into(binding.songType)
                }
            }
            setVersionImage(songAddVersion, songData.jpVersion)
            setCnVersionImage(songAddCnVersion, songData.version)

            val colorFilter: (Boolean) -> Int = { if (it) 0 else Color.WHITE }
            favButton.apply {
                setColorFilter(colorFilter.invoke(SpUtil.isFavorite(songData.id.toString())))
                setOnClickListener {
                    val isFavor = SpUtil.isFavorite(songData.id.toString())
                    SpUtil.setFavorite(songData.id.toString(), !isFavor)
                    setColorFilter(colorFilter.invoke(!isFavor))
                }
            }

            if (Settings.getEnableShowAlias()) {
                AliasRepository.getInstance().getAliasListBySongId(songData.id).observe(this@SongDetailActivity) {
                    if (it.isNotEmpty()) {
                        val aliasViewIds = songAliasFlow.referencedIds.toMutableList()
                        it.forEach { item ->
                            val textView = TextView(this@SongDetailActivity).apply {
                                text = item.alias
                                id = View.generateViewId()
                                aliasViewIds.add(id)
                                val padding = 5.toDp().toInt()
                                setPadding(padding, padding, padding, padding)
                                setBackgroundResource(R.drawable.mmd_song_alias_info_bg)
                                setTextColor(ContextCompat.getColor(this@SongDetailActivity, songData.bgColor))
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

            songJacket.setOnClickListener {
                val options: ActivityOptions = ActivityOptions
                    .makeSceneTransitionAnimation(
                        this@SongDetailActivity,
                        binding.songJacket,
                        "shared_image"
                    )
                val largeImageId = songData.id.toString().padStart(5, '0')
                PinchImageActivity.actionStart(
                    context = this@SongDetailActivity,
                    mode = 1,
                    imageUrl = "${MaimaiDataClient.DIVING_FISH_COVER_URL}$largeImageId.png",
                    thumbnailUrl = "${MaimaiDataClient.IMAGE_BASE_URL}${songData.imageUrl}",
                    saveFilename = largeImageId,
                    saveFolder = PictureUtils.coverPath,
                    bundle = options.toBundle()
                )
            }

            favButton.apply {
                setColorFilter(colorFilter.invoke(SpUtil.isFavorite(songData.id.toString())))
                setOnClickListener {
                    val isFavor = SpUtil.isFavorite(songData.id.toString())
                    SpUtil.setFavorite(songData.id.toString(), !isFavor)
                    setColorFilter(colorFilter.invoke(!isFavor))
                }
            }

            searchButton.apply {
                if (data.songData.type == SongType.UTAGE) {
                    visibility = View.GONE
                }
                setOnClickListener {
                    this.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100)
                        .withEndAction { this.animate().scaleX(1f).scaleY(1f).setDuration(100).start() }
                        .start()

                    val progressDialog = MaterialDialog.Builder(this@SongDetailActivity)
                        .progress(true, 0)
                        .content(getString(R.string.wait_dialog))
                        .cancelable(false)
                        .build()
                    progressDialog.show()

                    val liveData = SongWithRecordRepository.getInstance().getAllSongWithRecord()
                    liveData.observe(this@SongDetailActivity, object : Observer<List<SongWithRecordEntity>> {
                        override fun onChanged(value: List<SongWithRecordEntity>) {
                            liveData.removeObserver(this)
                            lifecycleScope.launch(Dispatchers.Default) {
                                val result = SongSortHelper.getSongClosestLocation(data, value)
                                withContext(Dispatchers.Main) {
                                    progressDialog.dismiss()

                                    MaterialDialog.Builder(this@SongDetailActivity)
                                        .title(getString(R.string.song_find_dialog_title))
                                        .content(
                                            getString(
                                                R.string.song_find_dialog_content,
                                                result.group.displayName,
                                                result.sort.displayName,
                                                result.groupName,
                                                result.difficulty.displayName,
                                                if (result.isReversed) getString(R.string.song_find_reversed) else getString(R.string.song_find_fronted),
                                                result.index + 1
                                            )
                                        ).build().show()
                                }
                            }
                        }
                    })
                }
            }

            val initialDifficulty = intent.getSerializableExtra(EXTRA_INITIAL_DIFFICULTY_KEY)!! as DifficultyType
            setupFragments(initialDifficulty)
        }
    }

    private fun setupFragments(initialDifficulty: DifficultyType) {
        var initialPage: Int
        val sortedCharts = if (data.songData.type == SongType.UTAGE) {
            data.charts.sortedBy { it.difficultyType.difficultyIndex }
        } else {
            data.charts.sortedByDescending { it.difficultyType.difficultyIndex }
        }

        initialPage = sortedCharts.indexOfFirst { it.difficultyType == initialDifficulty }

        val list = sortedCharts.map { chart ->
            SongLevelFragment.newInstance(
                GameSongObject.formSongWithRecord(data, chart.difficultyType)!!
            )
        }

        binding.viewPager.adapter = LevelDataFragmentAdapter(supportFragmentManager, -1, list)
        binding.tabLayout.setupWithViewPager(binding.viewPager)

        if (initialPage != -1) {
            binding.viewPager.setCurrentItem(initialPage, false)
        }
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
        if (versionDrawable != 0) {
            Glide.with(view.context)
                .load(versionDrawable)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(view)
        } else {
            view.isVisible = false
        }
    }

    companion object {
        fun actionStart(context: Context, detailData: SongWithRecordEntity, initialDifficulty: DifficultyType = DifficultyType.UNKNOWN) {
            val intent = Intent(context, SongDetailActivity::class.java).apply {
                putExtra(EXTRA_DATA_KEY, detailData)
                putExtra(EXTRA_INITIAL_DIFFICULTY_KEY, initialDifficulty)
            }
            context.startActivity(intent)
        }
    }
}