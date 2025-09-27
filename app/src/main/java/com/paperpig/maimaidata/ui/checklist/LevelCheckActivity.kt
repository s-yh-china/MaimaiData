package com.paperpig.maimaidata.ui.checklist

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityLevelCheckBinding
import com.paperpig.maimaidata.repository.SongWithRecordRepository
import com.paperpig.maimaidata.utils.SpUtil

class LevelCheckActivity : AppCompatActivity() {

    private var currentLevel = ""

    private lateinit var binding: ActivityLevelCheckBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLevelCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        supportActionBar?.title = getString(R.string.level_query)

        initView()
        getData()
    }

    private fun initView() {
        val levelArrays = resources.getStringArray(R.array.dxp_song_level).toMutableList().apply { removeAt(0) }
        binding.levelText.text = getString(R.string.search_level_string, levelArrays[0])

        fun refreshText(index: Int) {
            currentLevel = levelArrays.getOrNull(index) ?: "UNKNOWN"
            if (binding.levelSlider.value.toInt() == levelArrays.size - 1) {
                binding.btnRight.isVisible = false
                binding.btnLeft.isVisible = true
            } else if (binding.levelSlider.value.toInt() == 0) {
                binding.btnRight.isVisible = true
                binding.btnLeft.isVisible = false
            } else {
                binding.btnRight.isVisible = true
                binding.btnLeft.isVisible = true
            }
        }

        binding.levelSlider.apply {
            value = 0f
            addOnChangeListener { _, value, _ ->
                val index = value.toInt()
                refreshText(index)
                SpUtil.saveLastQueryLevel(binding.levelSlider.value)
                binding.levelText.text = context.getString(R.string.search_level_string, currentLevel)
                (binding.levelCheckRecycler.adapter as LevelCheckAdapter).updateData(currentLevel)
            }

            setLabelFormatter { value ->
                val index = value.toInt()
                getString(R.string.search_level_string, levelArrays.getOrNull(index) ?: "UNKNOWN")
            }
        }

        binding.levelCheckRecycler.apply {
            layoutManager = FlexboxLayoutManager(context).apply {
                flexDirection = FlexDirection.ROW
                justifyContent = JustifyContent.FLEX_START
            }
            adapter = LevelCheckAdapter(context)
        }

        binding.levelSlider.value = SpUtil.getLastQueryLevel()
        refreshText(binding.levelSlider.value.toInt())

        binding.switchBtn.setOnClickListener {
            (binding.levelCheckRecycler.adapter as LevelCheckAdapter).updateDisplay()
        }

        binding.btnLeft.setOnClickListener {
            if (binding.levelSlider.value.toInt() != 0) {
                binding.levelSlider.value -= 1f
                refreshText(binding.levelSlider.value.toInt())
            }
        }

        binding.btnRight.setOnClickListener {
            if (binding.levelSlider.value.toInt() != levelArrays.size - 1) {
                binding.levelSlider.value += 1f
                refreshText(binding.levelSlider.value.toInt())
            }
        }
    }

    private fun getData() {
        SongWithRecordRepository.getInstance().getAllSongWithRecord().observe(this@LevelCheckActivity) {
            (binding.levelCheckRecycler.adapter as LevelCheckAdapter).apply {
                setData(it)
                updateData(currentLevel)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }
}