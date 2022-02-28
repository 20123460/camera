package com.android.facially.activity

import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.opengl.*
import android.opengl.GLES30.*
import android.os.Bundle
import android.util.Log
import com.android.facially.*
import com.android.facially.opengl.*
import java.nio.ByteBuffer
import kotlin.concurrent.thread


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
    private val maskMatrix = FloatArray(16)

    private var imageData: ByteBuffer? = null
    private var imageWidth = 0
    private var imageHeight = 0


    private val temp = FloatArray(32)
    private val result = FloatArray(32)
    private val modelMatrix = FloatArray(16)
    private val previewSize = RectF()
    private val previewCropSize = RectF()

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
        glEnable(GL_DEPTH_TEST)

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
                glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA8, imageWidth, imageHeight)
                imageData?.position(0)
                glTexSubImage2D(
                    GL_TEXTURE_2D,
                    0,
                    0,
                    0,
                    imageWidth,
                    imageHeight,
                    GL_RGBA,
                    GL_UNSIGNED_BYTE,
                    imageData
                )
            }

            if (vao == null) {
                vao = GL3DVao(mesh!!.vertexes, mesh!!.indices)
            }

            program.use()
            glViewport(0, 0, width, height)

            glBindTexture(GL_TEXTURE_2D, texture)
            glActiveTexture(GL_TEXTURE0)

            Matrix.setIdentityM(temp, 0)
            Matrix.setIdentityM(temp, 16)
//            Matrix.rotateM(temp, 0, 180f, 0f, 1f, 0f)
            Matrix.rotateM(temp, 0, 270f, 1f, 0f, 0f)
//            Matrix.translateM(temp, 0, -mesh!!.pos[0], -mesh!!.pos[1], -mesh!!.pos[2])

            Matrix.setLookAtM(temp, 16, 0f, 0f, 2f, 0f, 0f, 0f, 0f, 1f, 0f)

            Matrix.setIdentityM(result, 16)
            Matrix.multiplyMM(result, 0, temp, 16, temp, 0)
            Matrix.frustumM(
                temp,
                0,
                -1f,
                1f,
                -1f,
                1f,
                1f,
                3f
            )
//            Matrix.frustumM(
//                temp,
//                0,
//                -width / height.toFloat(),
//                width / height.toFloat(),
//                -1f,
//                1f,
//                1f,
//                3f
//            )

//            Matrix.perspectiveM(maskMatrix,0,90f,width/height.toFloat(),1f,50f)
//            Matrix.perspectiveM(temp,16,90f,width/height.toFloat(),0f,1f)
            Matrix.multiplyMM(maskMatrix, 0, temp, 0, result, 0)
            GLES20.glUniformMatrix4fv(locMvpMatrix, 1, false, maskMatrix, 0)
//            GLES20.glUniformMatrix4fv(locMvpMatrix, 1, false, modelMatrix, 0)

            vao?.bind()
            glDrawElements(GL_TRIANGLES, mesh!!.indices.size, GL_UNSIGNED_INT, 0)
            vao?.unbind()

            val err = GLES30.glGetError()
            if (err != GLES30.GL_NO_ERROR) {
                Log.e(TAG, "FbxActivity onDraw: -------------- $err")
            }
            test(width, height)
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