package com.paperpig.maimaidata.ui.checklist

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MediatorLiveData
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityVersionCheckBinding
import com.paperpig.maimaidata.db.entity.RecordEntity
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity
import com.paperpig.maimaidata.model.DifficultyType
import com.paperpig.maimaidata.model.Version
import com.paperpig.maimaidata.repository.RecordRepository
import com.paperpig.maimaidata.repository.SongWithChartRepository
import com.paperpig.maimaidata.utils.SpUtil

class VersionCheckActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVersionCheckBinding
    private var searchVersionString = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityVersionCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        supportActionBar?.title = getString(R.string.version_query)

        initView()
        getData()
    }

    private fun getData() {
        var songs: List<SongWithChartsEntity>? = null
        var records: List<RecordEntity>? = null
        val allSongs = SongWithChartRepository.getInstance().getAllSongWithCharts()
        val allRecords = RecordRepository.getInstance().getRecordsByDifficulty(DifficultyType.REMASTER)
        MediatorLiveData<Pair<List<SongWithChartsEntity>, List<RecordEntity>>>().apply {
            addSource(allSongs) { newSongs ->
                songs = newSongs
                if (songs != null && records != null) {
                    value = Pair(songs!!, records!!)
                }
            }
            addSource(allRecords) { newRecords ->
                records = newRecords
                if (songs != null && records != null) {
                    value = Pair(songs, records)
                }
            }
            observe(this@VersionCheckActivity) { (songs, records) ->
                (binding.versionCheckRecycler.adapter as VersionCheckAdapter).apply {
                    setData(songs, records)
                    updateData(searchVersionString)
                }
            }
        }
    }

    private fun initView() {
        val lastSelectedPosition = SpUtil.getLastQueryVersion()
        val versionList = getVersionList()
        searchVersionString = versionList[lastSelectedPosition].versionName

        //设置spinner适配器
        binding.versionSpn.apply {
            adapter = VersionArrayAdapter(
                this@VersionCheckActivity,
                R.layout.item_spinner_version,
                versionList
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(lastSelectedPosition, true)
            onItemSelectedListener =
                object : OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        SpUtil.saveLastQueryVersion(position)
                        searchVersionString = (parent?.getItemAtPosition(position) as Version).versionName
                        (binding.versionCheckRecycler.adapter as VersionCheckAdapter).updateData(searchVersionString)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                    }
                }
        }

        // 设置recyclerView适配器
        binding.versionCheckRecycler.apply {
            adapter = VersionCheckAdapter(context)

            layoutManager = FlexboxLayoutManager(context).apply {
                flexDirection = FlexDirection.ROW
                justifyContent = JustifyContent.FLEX_START // 设置主轴上的对齐方式为起始位置
            }
        }

        binding.switchBtn.setOnClickListener {
            (binding.versionCheckRecycler.adapter as VersionCheckAdapter).updateDisplay()
        }
    }

    private fun getVersionList(): MutableList<Version> {
        return mutableListOf(
            Version("maimai", R.drawable.maimai),
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
            Version("舞萌DX", R.drawable.maimaidx_cn),
            Version("舞萌DX 2021", R.drawable.maimaidx_2021),
            Version("舞萌DX 2022", R.drawable.maimaidx_2022),
            Version("舞萌DX 2023", R.drawable.maimaidx_2023),
            Version("舞萌DX 2024", R.drawable.maimaidx_2024),
            Version("舞萌DX 2025", R.drawable.maimaidx_2025),
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home ->
                finish()
        }
        return true
    }
}