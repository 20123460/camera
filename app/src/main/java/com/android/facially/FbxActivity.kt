package com.android.facially

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class FbxActivity : AppCompatActivity() {

    companion object{
        fun launch(context: Context){
            context.startActivity(Intent(context,FbxActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fbx)
    }
}