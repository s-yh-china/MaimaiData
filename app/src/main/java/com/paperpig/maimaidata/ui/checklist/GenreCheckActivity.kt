package com.paperpig.maimaidata.ui.checklist

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MediatorLiveData
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityGenreCheckBinding
import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.db.entity.RecordEntity
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity
import com.paperpig.maimaidata.repository.RecordRepository
import com.paperpig.maimaidata.repository.SongWithChartRepository
import com.paperpig.maimaidata.utils.SpUtil

class GenreCheckActivity : AppCompatActivity() {
    private val difficultyList = mutableListOf("Basic", "Advanced", "Expert", "Master", "Re:Master")
    private val genreList = mutableListOf(
        "流行&动漫",
        "niconico & VOCALOID",
        "东方Project",
        "其他游戏",
        "舞萌",
        "音击&中二节奏",
    )

    private lateinit var binding: ActivityGenreCheckBinding
    private var searchGenreString = ""
    private var currentDifficultyIndex = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGenreCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        supportActionBar?.title = getString(R.string.genre_query)

        initView()
        getData()
    }

    private fun getData() {
        var songs: List<SongWithChartsEntity>? = null
        var records: List<RecordEntity>? = null
        val allSongs = SongWithChartRepository.getInstance(AppDataBase.getInstance().songWithChartDao()).getAllSongWithCharts()
        val allRecords = RecordRepository.getInstance(AppDataBase.getInstance().recordDao()).getAllRecord()
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
                    value = Pair(songs, records!!)
                }
            }
            observe(this@GenreCheckActivity) { (songs, records) ->
                (binding.genreCheckRecycler.adapter as GenreCheckAdapter).apply {
                    setData(songs, records)
                    updateData(searchGenreString, currentDifficultyIndex)
                }
            }
        }
    }

    private fun initView() {
        val lastSelectedPosition = SpUtil.getLastQueryGenre()
        searchGenreString = genreList[lastSelectedPosition]

        binding.genreSpn.apply {
            adapter = ArrayAdapter(this@GenreCheckActivity, android.R.layout.simple_spinner_item, genreList).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(lastSelectedPosition, true)
            onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        SpUtil.saveLastQueryGenre(position)
                        searchGenreString = parent?.getItemAtPosition(position) as String
                        (binding.genreCheckRecycler.adapter as GenreCheckAdapter).updateData(searchGenreString, currentDifficultyIndex)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                    }
                }
        }

        val lastSelectedDifficultyIndex = SpUtil.getLastQueryDifficulty()
        currentDifficultyIndex = lastSelectedDifficultyIndex

        binding.difficultySpn.apply {
            adapter = ArrayAdapter(this@GenreCheckActivity, android.R.layout.simple_spinner_item, difficultyList).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(currentDifficultyIndex, true)
            onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        currentDifficultyIndex = position
                        SpUtil.saveLastQueryDifficulty(position)
                        (binding.genreCheckRecycler.adapter as GenreCheckAdapter).updateData(searchGenreString, currentDifficultyIndex)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                    }
                }
        }


        binding.genreCheckRecycler.apply {
            adapter = GenreCheckAdapter(context)

            layoutManager = FlexboxLayoutManager(context).apply {
                flexDirection = FlexDirection.ROW
                justifyContent = JustifyContent.FLEX_START
            }
        }

        binding.switchBtn.setOnClickListener {
            (binding.genreCheckRecycler.adapter as GenreCheckAdapter).updateDisplay()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }
}