package com.android.facially

import com.android.facially.activity.FbxActivity
import com.android.facially.activity.LandmarkActivity
import com.android.facially.activity.PreviewActivity
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.facially.activity.LandmarkFActivity

class MainActivity : AppCompatActivity() {

    private val tvLandmark: TextView by lazy { findViewById(R.id.tv_landmark) }
    private val tvLandmarkF: TextView by lazy { findViewById(R.id.tv_landmark_f) }
    private val tvPreview: TextView by lazy { findViewById(R.id.tv_preview) }
    private val tvFbx: TextView by lazy { findViewById(R.id.tv_fbx) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvPreview.setOnClickListener { PreviewActivity.launch(this) }
        tvLandmark.setOnClickListener { LandmarkActivity.launch(this) }
        tvLandmarkF.setOnClickListener { LandmarkFActivity.launch(this) }
        tvFbx.setOnClickListener { FbxActivity.launch(this) }
    }
}