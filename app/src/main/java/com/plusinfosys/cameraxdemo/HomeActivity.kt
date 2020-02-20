package com.plusinfosys.cameraxdemo

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.toolbar.*

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home);
        imgBack.visibility = View.GONE
        btn_take_picture.setOnClickListener {
            startActivity(Intent(this, CapturePhotoActivity::class.java))
        }

        btn_take_video.setOnClickListener {
            startActivity(Intent(this, CaptureVideoActivity::class.java))
        }
    }
}