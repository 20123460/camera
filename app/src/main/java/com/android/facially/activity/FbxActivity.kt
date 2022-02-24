package com.android.facially.activity

import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.opengl.GLES11Ext
import android.opengl.GLES30.*
import android.opengl.GLES31
import android.os.Bundle
import com.android.facially.OesRender
import com.android.facially.RGBARender
import com.android.facially.opengl.GLFramebuffer
import com.android.facially.opengl.GLProgram
import com.android.facially.opengl.GLShader
import com.android.facially.readAssert
import kotlin.concurrent.thread


class Mesh constructor(val vertexes: FloatArray, val indices: IntArray, val path: String) {

}

class FbxActivity : PreviewActivity() {

    companion object {
        init {
            System.loadLibrary("facially_jni")
        }

        fun launch(context: Context) {
            context.startActivity(Intent(context, FbxActivity::class.java))
        }
    }

    private var mesh: Mesh? = null
    private val vertex by lazy { GLShader(GLES31.GL_VERTEX_SHADER, readAssert("fbx.vert")) }
    private val fragment by lazy { GLShader(GLES31.GL_FRAGMENT_SHADER, readAssert("fbx.frag")) }
    private val program by lazy { GLProgram(vertex, fragment) }
    private var rgbaRender: RGBARender? = null
    private val landmarkMatrix = FloatArray(16)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        thread {
            mesh = loadFbx(filesDir.absolutePath, assets)
        }
    }

    override fun onSurfaceCreated() {
        super.onSurfaceCreated()

    }

    override fun onSurfaceDestroy() {
        super.onSurfaceDestroy()
        rgbaRender?.onDestroy()
    }

    override fun onDraw(
        oes: Int,
        rotation: Int,
        displayRotation: Int,
        width: Int,
        height: Int,
        surfaceWidth: Int,
        surfaceHeight: Int
    ) {
        System.arraycopy(cameraMatrix, 0, landmarkMatrix, 0, 16)
        if (rgbaRender == null) {
            rgbaRender = RGBARender(this)
        }
        if (framebuffer == null) {
            framebuffer = GLFramebuffer(width, height)
        }
        framebuffer?.bind()

        if (oesRender == null)
            oesRender = OesRender(this)
        oesRender?.onDraw(oes, 0, 0, cameraMatrix, width, height, width, height, false)
//        test(width, height)


        framebuffer?.unbind()

        rgbaRender?.onDraw(
            framebuffer!!.texture,
            rotation,
            displayRotation,
            landmarkMatrix,
            width,
            height,
            surfaceWidth,
            surfaceHeight,
            true
        )
    }


    private external fun loadFbx(path: String, assert: AssetManager): Mesh
}