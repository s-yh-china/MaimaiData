package com.paperpig.maimaidata.ui.checklist

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityVersionClearCheckBinding
import com.paperpig.maimaidata.model.DifficultyType
import com.paperpig.maimaidata.model.Version
import com.paperpig.maimaidata.repository.SongWithRecordRepository
import com.paperpig.maimaidata.utils.SpUtil

class VersionClearCheckActivity : AppCompatActivity() {

    private val difficultyList = mutableListOf(DifficultyType.BASIC, DifficultyType.ADVANCED, DifficultyType.EXPERT, DifficultyType.MASTER, DifficultyType.REMASTER)
    private val versionList = listOf(
        Version("maimai", R.drawable.maimai),
        Version("maimai PLUS", R.drawable.maimai_plus),
        Version("GreeN", R.drawable.maimai_green),
        Version("GreeN PLUS", R.drawable.maimai_green_plus),
        Version("ORANGE", R.drawable.maimai_orange),
        Version("ORANGE PLUS", R.drawable.maimai_orange),
        Version("PiNK", R.drawable.maimai_pink),
        Version("PiNK PLUS", R.drawable.maimai_pink_plus),
        Version("MURASAKi", R.drawable.maimai_murasaki),
        Version("MURASAKi PLUS", R.drawable.maimai_murasaki_plus),
        Version("MiLK", R.drawable.maimai_milk),
        Version("MiLK PLUS", R.drawable.maimai_milk_plus),
        Version("FiNALE", R.drawable.maimai_finale),
        Version("霸者", R.drawable.namep_clear),
    )

    private val songIdNotIn = listOf(44, 146, 185, 189, 190, 341, 419, 451, 455, 460, 524, 687, 688, 712, 731, 792, 853)

    private var currentVersion = ""
    private var currentDifficulty = DifficultyType.BASIC

    private lateinit var binding: ActivityVersionClearCheckBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityVersionClearCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        supportActionBar?.title = getString(R.string.version_clear_query)

        initView()
        getData()
    }

    private fun initView() {
        val lastSelectedPosition = SpUtil.getLastQueryVersionClear()
        currentVersion = versionList[lastSelectedPosition].versionName

        binding.versionSpn.apply {
            adapter = VersionArrayAdapter(this@VersionClearCheckActivity, R.layout.item_spinner_version, versionList).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(lastSelectedPosition, true)
            onItemSelectedListener =
                object : OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        SpUtil.saveLastQueryVersionClear(position)
                        currentVersion = (parent?.getItemAtPosition(position) as Version).versionName
                        (binding.versionClearCheckRecycler.adapter as VersionClearCheckAdapter).updateData(currentVersion, currentDifficulty)
                        if (currentVersion == "霸者") {
                            binding.difficultySpn.isVisible = false
                        } else {
                            binding.difficultySpn.isVisible = true
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                    }
                }
        }

        val lastSelectedDifficultyIndex = SpUtil.getLastQueryVersionClearDifficulty()
        currentDifficulty = difficultyList[lastSelectedDifficultyIndex]

        binding.difficultySpn.apply {
            adapter = ArrayAdapter(this@VersionClearCheckActivity, android.R.layout.simple_spinner_item, difficultyList.map { it.displayName }).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(lastSelectedDifficultyIndex, true)
            onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    SpUtil.saveLastQueryVersionClearDifficulty(position)
                    currentDifficulty = difficultyList[position]
                    (binding.versionClearCheckRecycler.adapter as VersionClearCheckAdapter).updateData(currentVersion, currentDifficulty)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }

        binding.versionClearCheckRecycler.apply {
            adapter = VersionClearCheckAdapter(context)

            layoutManager = FlexboxLayoutManager(context).apply {
                flexDirection = FlexDirection.ROW
                justifyContent = JustifyContent.FLEX_START
            }
        }
    }

    private fun getData() {
        SongWithRecordRepository.getInstance().getAllSongWithRecord().observe(this@VersionClearCheckActivity) {
            (binding.versionClearCheckRecycler.adapter as VersionClearCheckAdapter).apply {
                setData(it.filter { song -> "舞萌" !in song.songData.version && song.songData.id !in songIdNotIn })
                updateData(currentVersion, currentDifficulty)
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