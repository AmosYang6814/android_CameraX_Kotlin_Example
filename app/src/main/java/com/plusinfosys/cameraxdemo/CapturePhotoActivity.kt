package com.plusinfosys.cameraxdemo

import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar.*
import java.io.File
import java.security.Permission
import java.util.jar.Manifest

class CapturePhotoActivity : AppCompatActivity() {

    private var lensFacing = CameraX.LensFacing.BACK
    private val TAG = "CapturePhotoActivity"
    val folderPath = "/test"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imgBack.setOnClickListener { finish() }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            val permissioncamera = checkSelfPermission(android.Manifest.permission.CAMERA);
            if (permissioncamera != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 1001);
                return
            } else {
                initializeCamera()
            }
        } else {
            initializeCamera()
        }

        // Every time the provided texture view changes, recompute layout
        texture.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }
    }

    fun initializeCamera() {
        texture.post { startCamera() }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 1001) {
            var result = 0
            if (grantResults.isNotEmpty()) {
                for (item in grantResults) {
                    result += item
                }
            }
            if (result == PackageManager.PERMISSION_GRANTED) {
                initializeCamera()
            } else {
                Toast.makeText(this, "Permission require to access camera.", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun startCamera() {
        btn_flash.visibility = View.VISIBLE

        val metrics = DisplayMetrics().also { texture.display.getRealMetrics(it) }
        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)

        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)
            setTargetResolution(screenSize)
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(windowManager.defaultDisplay.rotation)
            setTargetRotation(texture.display.rotation)
        }.build()

        val preview = Preview(previewConfig)
        preview.setOnPreviewOutputUpdateListener {

            val parent = texture.parent as ViewGroup
            parent.removeView(texture)
            texture.surfaceTexture = it.surfaceTexture
            parent.addView(texture, 0)
            updateTransform()
        }


        // Create configuration object for the image capture use case
        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                setLensFacing(lensFacing)
                setTargetAspectRatio(screenAspectRatio)
                setTargetRotation(texture.display.rotation)
                setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
            }.build()

        // Build the image capture use case and attach button click listener
        val imageCapture = ImageCapture(imageCaptureConfig)
        btn_take_picture.setOnClickListener {

            val file = File(
                getExternalFilesDir(null)!!.getAbsolutePath() +
                        "${folderPath}${System.currentTimeMillis()}.jpg"
            )

            imageCapture.takePicture(file,
                object : ImageCapture.OnImageSavedListener {
                    override fun onError(
                        error: ImageCapture.UseCaseError,
                        message: String, exc: Throwable?
                    ) {
                        val msg = "Photo capture failed: $message"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()

                    }

                    override fun onImageSaved(file: File) {
                        val msg = "Photo capture successfully: ${file.absolutePath}"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    }
                })

        }

        btn_flash.setOnClickListener {
            val flashMode = imageCapture?.flashMode
            if (flashMode == FlashMode.ON) {
                btn_flash.setText("Flash ON")
                imageCapture?.flashMode = FlashMode.OFF
            } else {
                btn_flash.setText("Flash OFF")

                imageCapture?.flashMode = FlashMode.ON
            }
        }

        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    private fun updateTransform() {
        val matrix = Matrix()
        val centerX = texture.width / 2f
        val centerY = texture.height / 2f

        val rotationDegrees = when (texture.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)
        texture.setTransform(matrix)
    }
}
