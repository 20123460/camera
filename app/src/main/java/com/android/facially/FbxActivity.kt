package com.android.facially

import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle


class FbxActivity : LandmarkActivity() {

    companion object {
        init {
            System.loadLibrary("facially_jni")
        }

        fun launch(context: Context) {
            context.startActivity(Intent(context, FbxActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadFbx(filesDir.absolutePath,assets)
    }

    override fun drawLandmark(): Boolean {
        return false
    }


    external fun loadFbx(path: String, assert: AssetManager)
}