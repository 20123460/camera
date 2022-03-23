package com.android.facially.color

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import com.android.facially.util.BitmapUtil
import com.android.facially.R
import com.android.facially.util.TAG
import com.android.facially.util.readAssertImage
import kotlin.concurrent.thread
import kotlin.time.ExperimentalTime

class FaceActivity : AppCompatActivity() {
    companion object {
        init {
            System.loadLibrary("facially_jni")
        }

        fun launch(context: Context) {
            context.startActivity(Intent(context, FaceActivity::class.java))
        }
    }

    val ivImage: ImageView by lazy { findViewById(R.id.iv_image) }

    @OptIn(ExperimentalTime::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face)
        val image = readAssertImage("test.jpg")
        ivImage.setImageBitmap(image)
        val width = image.width
        val height = image.height
        thread {
            val bgr = BitmapUtil.bitmapToBgr(image)
            val start = System.currentTimeMillis()
            val time = detect(bgr, width, height)
            val end = System.currentTimeMillis()
            Log.e(TAG, "onCreate: ------- cost : ${time}")
            Log.e(TAG, "onCreate: ------- cost all : ${end - start}")
        }
    }

    external fun detect(image: ByteArray, width: Int, height: Int): Long

}