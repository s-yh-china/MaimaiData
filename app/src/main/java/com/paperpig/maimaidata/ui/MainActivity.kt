package com.paperpig.maimaidata.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.paperpig.maimaidata.MaimaiDataApplication
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityMainBinding
import com.paperpig.maimaidata.repository.ChartRepository
import com.paperpig.maimaidata.ui.rating.RatingFragment
import com.paperpig.maimaidata.ui.songlist.SongListFragment
import com.paperpig.maimaidata.utils.UpdateManager
import io.reactivex.disposables.Disposable

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var ratingFragment: RatingFragment
    private lateinit var songListFragment: SongListFragment
    private val updateManager: UpdateManager = UpdateManager(this)
    private var updateDisposable: Disposable? = null
    private var isUpdateChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarLayout.toolbar)

        updateManager.checkChartStatusUpdate(this)
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

    private fun checkUpdate() {
        updateDisposable = updateManager.checkAppUpdate {
            updateManager.checkDataUpdate(this) {
                isUpdateChecked = true
            }
        }
    }

    private fun queryMaxNotes() {
        ChartRepository.getInstance().getMaxNotes().observe(this) {
            MaimaiDataApplication.instance.maxNotesStats = it
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