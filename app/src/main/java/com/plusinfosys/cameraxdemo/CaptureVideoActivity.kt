package com.plusinfosys.cameraxdemo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Matrix
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_video_capture.*
import kotlinx.android.synthetic.main.toolbar.*
import java.io.File
import java.security.Permission
import java.util.jar.Manifest

@SuppressLint("RestrictedApi")

class CaptureVideoActivity : AppCompatActivity() {

    private var lensFacing = CameraX.LensFacing.BACK
    private val TAG = "CaptureVideoActivity"
    val folderPath = "/test"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_capture)

        imgBack.setOnClickListener { finish() }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            val permissioncamera = checkSelfPermission(android.Manifest.permission.CAMERA);
            val permissionRecordAudio = checkSelfPermission(android.Manifest.permission.RECORD_AUDIO);

            if (permissioncamera != PackageManager.PERMISSION_GRANTED || permissionRecordAudio != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO), 1001);
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

        val videoCaptureConfig = VideoCaptureConfig.Builder()
            .apply { setTargetRotation(texture.display.rotation) }
            .build()

        val videoCapture = VideoCapture(videoCaptureConfig)

        btn_start_recording.setOnClickListener {
            val file = File(getExternalFilesDir(null)!!.getAbsolutePath() + "${folderPath}${System.currentTimeMillis()}.mp4")

            btn_stop_recording.visibility = View.VISIBLE;
            btn_start_recording.visibility = View.GONE;

            videoCapture.startRecording(file, object : VideoCapture.OnVideoSavedListener {
                override fun onVideoSaved(file: File?) {
                    Log.i(TAG, "Video File : $file")
                    Toast.makeText(baseContext, file?.absolutePath, Toast.LENGTH_SHORT).show()
                }

                override fun onError(useCaseError: VideoCapture.UseCaseError?, message: String?, cause: Throwable?) {
                    Log.i(TAG, "Video Error: $message")
                }
            })
        }

        btn_stop_recording.setOnClickListener {
            videoCapture.stopRecording()
            Log.i(TAG, "Video File stopped")
            btn_stop_recording.visibility = View.GONE;
            btn_start_recording.visibility = View.VISIBLE;
        }

        CameraX.bindToLifecycle(this, preview, videoCapture)
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
