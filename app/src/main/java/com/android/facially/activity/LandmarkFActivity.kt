package com.android.facially.activity

import android.content.Context
import android.content.Intent
import android.opengl.GLES30
import android.util.Log
import com.android.facially.opengl.GLFramebuffer
import com.android.facially.opengl.GLProgram
import com.android.facially.opengl.GLShader
import com.android.facially.opengl.GLVao
import com.android.facially.render.OesRender
import com.android.facially.render.RGBARender
import com.android.facially.util.TAG
import com.android.facially.util.readAssert

class LandmarkFActivity : PreviewActivity() {
    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, LandmarkFActivity::class.java))
        }
    }

    private val vertex by lazy { GLShader(GLES30.GL_VERTEX_SHADER, readAssert("landmarkf.vert")) }
    private val fragment by lazy {
        GLShader(
            GLES30.GL_FRAGMENT_SHADER,
            readAssert("landmark.frag")
        )
    }
    private val landmarkProgram by lazy { GLProgram(vertex, fragment) }

//    private val vertexes = floatArrayOf(
//        -1f, -1f,  // Lower-left
//        1f, -1f,    // Lower-right
//        -1f, 1f,  // Upper-left
//        1f, 1f    // Upper-right
//    )
    private val vao by lazy { GLVao(landmarks) }
//    private val vao by lazy { GLVao(vertexes) }

    private var locTexMatrix = 0
    private var locMvpMatrix = 0

    override fun onSurfaceDestroy() {
        vertex.release()
        fragment.release()
        landmarkProgram.release()
        vao.release()
    }

    override fun onSurfaceCreated() {
        super.onSurfaceCreated()
//        locTexMatrix = landmarkProgram.getUniformLocation("texTransform")
//        locMvpMatrix = landmarkProgram.getUniformLocation("mvpTransform")
    }

    private val landmarkMatrix = FloatArray(16)
    private var rgbaRender: RGBARender? = null

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
        if (oesRender == null)
            oesRender = OesRender(this)
        if (framebuffer == null)
            framebuffer = GLFramebuffer(width, height)
        if (rgbaRender == null) {
            rgbaRender = RGBARender(this)
        }
        framebuffer?.bind()
        oesRender?.onDraw(oes, 0, 0, mIdentityMatrix, width, height, width, height, false)

        vao.update(landmarks)
        landmarkProgram.use()

        GLES30.glViewport(0, 0, width, height)

//        Matrix.setIdentityM(cameraMatrix, 0)
//        GLES31.glUniformMatrix4fv(locTexMatrix, 1, false, cameraMatrix, 0)
//        GLES31.glUniformMatrix4fv(locMvpMatrix, 1, false, cameraMatrix, 0)

        vao.bind()
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, 106)
//        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, 2)
//        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 100)
        vao.unbind()

//        test(width,height)
        framebuffer?.unbind()

        val err = GLES30.glGetError()
        if (err != GLES30.GL_NO_ERROR) {
            Log.e(TAG, "onDraw: -------------- $err")
        }

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
}