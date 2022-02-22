package com.android.facially

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val tvLandmark: TextView by lazy { findViewById(R.id.tv_landmark) }
    private val tvFbx: TextView by lazy { findViewById(R.id.tv_fbx) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvLandmark.setOnClickListener { LandmarkActivity.launch(this) }
        tvFbx.setOnClickListener { FbxActivity.launch(this) }
    }


}