package com.android.facially.activity

import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLES30.*
import android.opengl.GLES31
import android.os.Bundle
import android.util.Log
import com.android.facially.OesRender
import com.android.facially.RGBARender
import com.android.facially.TAG
import com.android.facially.opengl.*
import com.android.facially.readAssert
import java.nio.ByteBuffer
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
    private var vao: GL3DVao? = null
    private var texture = 0

    private val landmarkMatrix = FloatArray(16)

    private var imageData: ByteBuffer? = null
    private var imageWidth = 0
    private var imageHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        thread {
            mesh = loadFbx(filesDir.absolutePath, assets)
            assets.open("mask/" + mesh!!.path).apply {
                val bitmap = BitmapFactory.decodeStream(this)
                imageData = ByteBuffer.allocateDirect(bitmap.byteCount)
                imageWidth = bitmap.width
                imageHeight = bitmap.height
                bitmap.copyPixelsToBuffer(imageData)
                close()
            }
        }
    }

    private var locTexMatrix = 0
    private var locMvpMatrix = 0

    override fun onSurfaceCreated() {
        super.onSurfaceCreated()

        glEnable(GL_DEPTH_TEST)

        locTexMatrix = program.getUniformLocation("texTransform")
        locMvpMatrix = program.getUniformLocation("mvpTransform")
        val locTexture = program.getUniformLocation("input_texture")
        program.use()
        GLES31.glUniform1i(locTexture, 0)
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


        // draw mask
        if (mesh != null && imageData != null) {

            if (texture == 0) {
                val temp = IntArray(1)
                GLES20.glGenTextures(1, temp, 0)
                texture = temp[0]
                glBindTexture(GL_TEXTURE_2D, texture)
                glTexImage2D(
                    GL_TEXTURE_2D, 0, GL_RGBA8, imageWidth, imageHeight, 0, GL_RGBA,
                    GL_UNSIGNED_BYTE, imageData
                )
//                glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA8, imageWidth, imageHeight)
//                glTexSubImage2D(
//                    GL_TEXTURE_2D,
//                    0,
//                    0,
//                    0,
//                    imageWidth,
//                    imageHeight,
//                    GL_RGBA,
//                    GL_UNSIGNED_BYTE,
//                    imageData
//                )

                // todo test
                GLES20.glGenFramebuffers(1, temp, 0)
                val testbuffer = temp[0]
                glBindFramebuffer(GL_FRAMEBUFFER, testbuffer)
                glFramebufferTexture2D(
                    GL_FRAMEBUFFER,
                    GL_COLOR_ATTACHMENT0,
                    GL_TEXTURE_2D,
                    texture,
                    0
                )
                val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
                if (status != GL_FRAMEBUFFER_COMPLETE) {
                    Log.e(TAG, "test: -------- glCheckFramebufferStatus")
                }
                glDrawBuffers(1, intArrayOf(GL_COLOR_ATTACHMENT0), 0)
                val buffer = ByteBuffer.allocateDirect(imageWidth * imageHeight * 4);
                buffer.position(0)
                glReadPixels(0, 0, imageWidth, imageHeight, GL_RGBA, GL_UNSIGNED_BYTE, buffer)
                buffer.position(0)
                val bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(buffer)
                Log.e(TAG, "test: -------- check bitmap")
            }
            if (vao == null) {
                val vertexes = floatArrayOf(
                    1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
                    1.0f, -1.0f, 0.0f, 1.0f, 0.0f,
                    -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
                    -1.0f, 1.0f, 0.0f, 0.0f, 1.0f
                )
                val indices = intArrayOf(
                    0, 1, 2,
                    1, 2, 3
                )
                vao = GL3DVao(mesh!!.vertexes, mesh!!.indices)
//                vao = GL3DVao(vertexes, indices)
            }

            program.use()
            glViewport(0, 0, width, height)

            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_2D, texture)

            vao?.bind()
            glDrawElements(GL_TRIANGLES, mesh!!.indices.size, GL_UNSIGNED_INT, 0)
//            glDrawArrays(GL_POINTS, 0, mesh!!.vertexes.size)
//            glDrawArrays(GL_POINTS, 0, 4)
            vao?.unbind()

            val err = GLES30.glGetError()
            if (err != GLES30.GL_NO_ERROR) {
                Log.e(TAG, "FbxActivity onDraw: -------------- $err")
            }

//            test(width, height)
        }

        framebuffer?.unbind()


        // draw on screen
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