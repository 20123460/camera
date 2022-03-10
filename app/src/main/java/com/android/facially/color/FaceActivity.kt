package com.android.facially.color

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import com.android.facially.BitmapUtil
import com.android.facially.R
import com.android.facially.TAG
import com.android.facially.readAssertImage
import java.nio.ByteBuffer

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face)
        val image = readAssertImage("test.jpg")
        ivImage.setImageBitmap(image)
        val width = image.width
        val height = image.height
        val time = detect(BitmapUtil.bitmapToBgr(image),width,height)
        Log.e(TAG, "onCreate: ------- cost : ${time}", )
    }

    external fun detect(image: ByteArray,width:Int,height:Int):Long

}