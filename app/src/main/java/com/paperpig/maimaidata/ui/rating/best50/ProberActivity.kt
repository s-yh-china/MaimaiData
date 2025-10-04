package com.paperpig.maimaidata.ui.rating.best50

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.afollestad.materialdialogs.MaterialDialog
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityProberBinding
import com.paperpig.maimaidata.db.entity.SongWithRecordEntity
import com.paperpig.maimaidata.model.GameSongObjectWithRating
import com.paperpig.maimaidata.repository.SongWithRecordRepository
import com.paperpig.maimaidata.utils.CreateBest50
import com.paperpig.maimaidata.utils.PermissionHelper
import com.paperpig.maimaidata.utils.ProgressCallback
import com.paperpig.maimaidata.widgets.AnimationHelper
import kotlinx.coroutines.launch

class ProberActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProberBinding

    private lateinit var proberVersionAdapter: ProberVersionAdapter
    private var oldRating = listOf<GameSongObjectWithRating>()
    private var newRating = listOf<GameSongObjectWithRating>()

    private lateinit var animationHelper: AnimationHelper

    private lateinit var permissionHelper: PermissionHelper

    val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        permissionHelper.onRequestPermissionsResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProberBinding.inflate(layoutInflater)
        setContentView(binding.root)

        animationHelper = AnimationHelper(layoutInflater)
        binding.proberContainerLayout.addView(animationHelper.loadLayout(), 0)
        animationHelper.startAnimation()

        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        supportActionBar?.title = getString(R.string.best50)
        binding.oldVersionRdoBtn.text = String.format(getString(R.string.old_version_35), 0)
        binding.newVersionRdoBtn.text = String.format(getString(R.string.new_version_15), 0)

        permissionHelper = PermissionHelper.with(this)

        binding.refreshLayout.apply {
            isEnabled = false
            isRefreshing = true
            setColorSchemeResources(R.color.colorPrimary)
        }

        SongWithRecordRepository.getInstance().getAllSongWithRecord().observe(this) {
            setData(it)
        }
    }

    private fun setData(list: List<SongWithRecordEntity>) {
        binding.refreshLayout.isRefreshing = false

        binding.proberVp.apply {
            proberVersionAdapter = ProberVersionAdapter()
            adapter = proberVersionAdapter
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    if (position == 0) {
                        binding.oldVersionRdoBtn.isChecked = true
                        binding.oldVersionIndicator.visibility = View.VISIBLE
                        binding.newVersionIndicator.visibility = View.GONE
                    } else {
                        binding.newVersionRdoBtn.isChecked = true
                        binding.oldVersionIndicator.visibility = View.GONE
                        binding.newVersionIndicator.visibility = View.VISIBLE
                    }
                }
            })
        }

        binding.versionGroup.setOnCheckedChangeListener { _, i ->
            when (i) {
                R.id.old_version_rdo_btn -> {
                    binding.proberVp.currentItem = 0
                    binding.oldVersionIndicator.visibility = View.VISIBLE
                    binding.newVersionIndicator.visibility = View.GONE
                }

                R.id.new_version_rdo_btn -> {
                    binding.proberVp.currentItem = 1
                    binding.oldVersionIndicator.visibility = View.GONE
                    binding.newVersionIndicator.visibility = View.VISIBLE
                }
            }
        }

        proberVersionAdapter.setData(list)
        oldRating = proberVersionAdapter.b35Adapter.recordList.map { it.first }
        newRating = proberVersionAdapter.b15Adapter.recordList.map { it.first }

        binding.oldVersionRdoBtn.text = String.format(
            getString(R.string.old_version_35),
            oldRating.sumOf { it.rating }
        )
        binding.newVersionRdoBtn.text = String.format(
            getString(R.string.new_version_15),
            newRating.sumOf { it.rating }
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.share_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()

            R.id.menu_share -> permissionHelper
                .registerLauncher(requestPermissionLauncher)
                .checkStoragePermission(object : PermissionHelper.PermissionCallback {
                    override fun onAllGranted() {
                        createImage()
                    }

                    override fun onDenied(deniedPermissions: List<String>) {
                        Toast.makeText(this@ProberActivity, getString(R.string.storage_permission_denied), Toast.LENGTH_SHORT).show()
                    }
                })
        }

        return true
    }

    private fun createImage() {
        if (oldRating.isEmpty() && newRating.isEmpty()) {
            Toast.makeText(this, getString(R.string.best50_empty), Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = MaterialDialog.Builder(this@ProberActivity)
            .title(getString(R.string.best50_create_dialog, ""))
            .content(getString(R.string.wait_dialog))
            .progress(true, 0)
            .cancelable(false)
            .build()
        progressDialog.show()

        val callback = object : ProgressCallback {
            override fun onProgressUpdate(progress: String, message: String) {
                progressDialog.setTitle(getString(R.string.best50_create_dialog, progress))
                progressDialog.setContent(message)
            }

            override fun onComplete() {
                progressDialog.dismiss()
            }
        }

        lifecycleScope.launch {
            CreateBest50(callback).createSongInfo(this@ProberActivity, oldRating, newRating)
        }
    }

    override fun onResume() {
        super.onResume()
        animationHelper.resumeAnimation()
    }

    override fun onPause() {
        super.onPause()
        animationHelper.pauseAnimation()
    }

    override fun onDestroy() {
        super.onDestroy()
        animationHelper.stopAnimation()
    }
}