package com.paperpig.maimaidata.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityPinchImageBinding
import com.paperpig.maimaidata.glide.GlideApp
import com.paperpig.maimaidata.utils.PermissionHelper
import com.paperpig.maimaidata.utils.PictureUtils
import com.paperpig.maimaidata.utils.setDebouncedClickListener
import kotlinx.coroutines.launch

class PinchImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPinchImageBinding

    private lateinit var permissionHelper: PermissionHelper

    private var imageDrawable: Drawable? = null
    private var isLocalImage = false
    private var imageStringUri: String? = null

    companion object {
        private const val IMAGE_URL = "image_url"
        private const val IMAGE_THUMBNAIL_URL = "thumbnail_url"

        private const val LOAD_MODE = "load_mode"
        private const val MODE_ONLINE = 1
        private const val MODE_LOCAL_URI = 2

        private const val SAVE_PATH_FOLDER = "save_folder"
        private const val SAVE_PATH_FILENAME = "save_filename"
        private const val CLICK_TO_EXIT = "click_to_exit"

        @JvmStatic
        fun actionStart(
            context: Context,
            mode: Int,
            imageUrl: String,
            thumbnailUrl: String? = null,
            saveFolder: String? = null,
            saveFilename: String? = null,
            clickToExit: Boolean = true,
            bundle: Bundle? = null
        ) {
            val intent = Intent(context, PinchImageActivity::class.java).apply {
                putExtra(LOAD_MODE, mode)
                putExtra(CLICK_TO_EXIT, clickToExit)
                putExtra(IMAGE_URL, imageUrl)
                putExtra(IMAGE_THUMBNAIL_URL, thumbnailUrl)
                putExtra(SAVE_PATH_FOLDER, saveFolder)
                putExtra(SAVE_PATH_FILENAME, saveFilename)
            }
            context.startActivity(intent, bundle)
        }
    }

    val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        permissionHelper.onRequestPermissionsResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPinchImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionHelper = PermissionHelper.Companion.with(this)

        val loadMode = intent.getIntExtra(LOAD_MODE, MODE_ONLINE)
        val clickToExit = intent.getBooleanExtra(CLICK_TO_EXIT, true)
        val saveFolder = intent.getStringExtra(SAVE_PATH_FOLDER)
        val saveFilename = intent.getStringExtra(SAVE_PATH_FILENAME)

        if (clickToExit) {
            binding.pinchImageView.setOnClickListener { finishAfterTransition() }
        } else {
            binding.pinchImageView.setOnClickListener(null)
        }

        imageStringUri = intent.getStringExtra(IMAGE_URL)
        when (loadMode) {
            MODE_ONLINE -> loadOnlineImage()
            MODE_LOCAL_URI -> loadLocalUri()
        }

        setupActionButtons(saveFolder, saveFilename)
    }

    private fun loadOnlineImage() {
        if (imageStringUri.isNullOrEmpty()) {
            Toast.makeText(this, "无效的URI", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val thumbnailUrl = intent.getStringExtra(IMAGE_THUMBNAIL_URL)

        isLocalImage = false

        GlideApp.with(this)
            .load(imageStringUri)
            .thumbnail(GlideApp.with(this).load(thumbnailUrl))
            .addListener(coverLoadListener())
            .into(binding.pinchImageView)
    }

    private fun loadLocalUri() {
        if (imageStringUri.isNullOrEmpty()) {
            Toast.makeText(this, "无效的URI", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        isLocalImage = true
        binding.imageButtonGroup.visibility = View.VISIBLE
        binding.pinchImageView.transitionName = null

        GlideApp.with(this)
            .load(imageStringUri)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .addListener(coverLoadListener())
            .into(binding.pinchImageView)
    }

    private fun setupActionButtons(saveFolder: String?, saveFilename: String?) {
        binding.saveImageBtn.setDebouncedClickListener {
            imageDrawable?.apply {
                permissionHelper.registerLauncher(requestPermissionLauncher)
                    .checkStoragePermission(object : PermissionHelper.PermissionCallback {
                        override fun onAllGranted() {
                            lifecycleScope.launch {
                                val bitmap = imageDrawable!!.toBitmap(
                                    imageDrawable!!.intrinsicWidth,
                                    imageDrawable!!.intrinsicHeight
                                )

                                val folder = saveFolder ?: PictureUtils.imagePath
                                val filename = if (isLocalImage && saveFilename != null) saveFilename else "image_default"

                                PictureUtils.savePicture(this@PinchImageActivity, bitmap, folder, filename)
                            }
                        }

                        override fun onDenied(deniedPermissions: List<String>) {
                            Toast.makeText(this@PinchImageActivity, getString(R.string.storage_permission_denied), Toast.LENGTH_SHORT).show()
                        }
                    })
            }
        }

        binding.shareImageBtn.setDebouncedClickListener {
            imageDrawable?.apply {
                lifecycleScope.launch {
                    val uriToShare: Uri? = if (isLocalImage) {
                        imageStringUri?.toUri()
                    } else {
                        PictureUtils.saveCacheBitmap(
                            this@PinchImageActivity,
                            imageDrawable!!.toBitmap(400, 400)
                        )?.let {
                            FileProvider.getUriForFile(this@PinchImageActivity, "${application.packageName}.fileprovider", it)
                        }
                    }

                    uriToShare?.apply {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, uriToShare)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        startActivity(
                            Intent.createChooser(shareIntent, getString(R.string.share_image))
                        )
                    } ?: run {
                        Toast.makeText(this@PinchImageActivity, getString(R.string.share_image_error), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun coverLoadListener(): RequestListener<Drawable> {
        return object : RequestListener<Drawable> {
            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable?>?, isFirstResource: Boolean): Boolean {
                Toast.makeText(this@PinchImageActivity, R.string.download_image_error, Toast.LENGTH_SHORT).show()
                return false
            }

            override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable?>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                // 加载完成显示按钮
                binding.imageButtonGroup.visibility = View.VISIBLE
                imageDrawable = resource
                return false
            }
        }
    }

    override fun finishAfterTransition() {
        binding.pinchImageView.reset()
        super.finishAfterTransition()
    }
}