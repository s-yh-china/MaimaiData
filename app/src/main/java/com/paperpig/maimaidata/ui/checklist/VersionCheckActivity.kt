package com.paperpig.maimaidata.ui.checklist

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityVersionCheckBinding
import com.paperpig.maimaidata.model.Version
import com.paperpig.maimaidata.repository.SongWithRecordRepository
import com.paperpig.maimaidata.utils.SpUtil
import com.paperpig.maimaidata.widgets.Settings

class VersionCheckActivity : AppCompatActivity() {

    private val skipSongList = listOf(
        44, 70, 146,
        185, 189, 190,
        341,
        // ORANGE empty
        419,
        451, 455, 460,
        524,
        // MURASAKi empty
        853,
        687, 688, 712,
        731,
        792,
        10146,
        11213,
        11253, 11267,
        11484, 11497 // 如果这俩能复活
    )

    private val versionList = listOf(
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

    private var currentVersion = ""

    private lateinit var binding: ActivityVersionCheckBinding

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

    private fun initView() {
        val lastSelectedPosition = SpUtil.getLastQueryVersion()
        currentVersion = versionList[lastSelectedPosition].versionName

        binding.versionSpn.apply {
            adapter = VersionArrayAdapter(this@VersionCheckActivity, R.layout.item_spinner_version, versionList).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(lastSelectedPosition, true)
            onItemSelectedListener =
                object : OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        SpUtil.saveLastQueryVersion(position)
                        currentVersion = (parent?.getItemAtPosition(position) as Version).versionName
                        (binding.versionCheckRecycler.adapter as VersionCheckAdapter).updateData(currentVersion)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                    }
                }
        }

        binding.versionCheckRecycler.apply {
            adapter = VersionCheckAdapter(context)

            layoutManager = FlexboxLayoutManager(context).apply {
                flexDirection = FlexDirection.ROW
                justifyContent = JustifyContent.FLEX_START
            }
        }

        binding.switchBtn.setOnClickListener {
            (binding.versionCheckRecycler.adapter as VersionCheckAdapter).updateDisplay()
        }
    }

    private fun getData() {
        SongWithRecordRepository.getInstance().getAllSongWithRecord().observe(this@VersionCheckActivity) {
            (binding.versionCheckRecycler.adapter as VersionCheckAdapter).apply {
                setData(
                    if (Settings.getVersionCheckSkipSong()) {
                        it.filter { song -> song.songData.id !in skipSongList }
                    } else {
                        it
                    }
                )
                updateData(currentVersion)
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