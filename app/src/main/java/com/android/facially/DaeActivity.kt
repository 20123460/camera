package com.android.facially

import android.content.Context
import android.content.Intent
import android.opengl.GLES20.*
import com.android.facially.activity.PreviewActivity
import com.android.facially.opengl.GLFramebuffer
import com.android.facially.render.OesRender
import com.android.facially.render.RGBARender

class DaeActivity : PreviewActivity() {
    companion object {
        init {
            System.loadLibrary("facially_jni")
        }

        fun launch(context: Context) {
            context.startActivity(Intent(context, DaeActivity::class.java))
        }
    }


    private val mCameraMatrix = FloatArray(16)

    private var rgbaRender: RGBARender? = null

    val mDaeCore by lazy { DaeCore() }

    override fun onDraw(
        oes: Int,
        rotation: Int,
        displayRotation: Int,
        width: Int,
        height: Int,
        surfaceWidth: Int,
        surfaceHeight: Int
    ) {
        var uvWidth = width
        var uvHeight = height
        if (rotation == 90 || rotation == 270) {
            uvWidth = height
            uvHeight = width
        }
        System.arraycopy(cameraMatrix, 0, mCameraMatrix, 0, 16)
        if (oesRender == null)
            oesRender = OesRender(this)
        if (framebuffer == null)
            framebuffer = GLFramebuffer(uvWidth, uvHeight)
        if (rgbaRender == null) {
            rgbaRender = RGBARender(this)
        }
        framebuffer?.bind()
        oesRender?.onDraw(oes, rotation, 0, mCameraMatrix, width, height, uvWidth, uvHeight, false)
//        test(uvWidth,uvHeight)
        framebuffer?.unbind()





        rgbaRender?.onDraw(
            framebuffer!!.texture,
            0,
            displayRotation,
            mIdentityMatrix,
            uvWidth,
            uvHeight,
            surfaceWidth,
            surfaceHeight,
            true
        )
    }
}