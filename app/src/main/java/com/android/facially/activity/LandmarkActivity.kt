package com.android.facially.activity

import android.content.Context
import android.content.Intent
import android.opengl.GLES30.*
import android.opengl.GLES31
import android.opengl.Matrix
import com.android.facially.render.OesRender
import com.android.facially.util.calculateMvp
import com.android.facially.opengl.GLProgram
import com.android.facially.opengl.GLShader
import com.android.facially.opengl.GLVao
import com.android.facially.util.readAssert


open class LandmarkActivity : PreviewActivity() {

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, LandmarkActivity::class.java))
        }
    }

    private val vertex by lazy { GLShader(GL_VERTEX_SHADER, readAssert("landmark.vert")) }
    private val fragment by lazy { GLShader(GL_FRAGMENT_SHADER, readAssert("landmark.frag")) }
    private val landmarkProgram by lazy { GLProgram(vertex, fragment) }
    private val vao by lazy { GLVao(landmarks) }

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
        locTexMatrix = landmarkProgram.getUniformLocation("texTransform")
        locMvpMatrix = landmarkProgram.getUniformLocation("mvpTransform")
    }

    private val landmarkMatrix = FloatArray(16)


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
        oesRender?.onDraw(
            oes,
            rotation,
            displayRotation,
            cameraMatrix,
            width,
            height,
            surfaceWidth,
            surfaceHeight,
            true
        )

        vao.update(landmarks)
        landmarkProgram.use()

        glViewport(0, 0, surfaceWidth, surfaceHeight)

        // mvp
        calculateMvp(rotation, width, height, surfaceWidth, surfaceHeight, cameraMatrix)
        glUniformMatrix4fv(locMvpMatrix, 1, false, cameraMatrix, 0)

        // tex matrix
        when (displayRotation) {
            90 -> {
                Matrix.translateM(landmarkMatrix, 0, 0f, 1f, 0f)
            }
            180 -> {
                Matrix.translateM(landmarkMatrix, 0, 1f, 1f, 0f)
            }
            270 -> {
                Matrix.translateM(landmarkMatrix, 0, 1f, 0f, 0f)
            }
        }
        Matrix.rotateM(landmarkMatrix, 0, -displayRotation + 0f, 0f, 0f, 1f)
        GLES31.glUniformMatrix4fv(locTexMatrix, 1, false, landmarkMatrix, 0)

//            Matrix.setIdentityM(cameraMatrix, 0)
//            GLES31.glUniformMatrix4fv(locTexMatrix, 1, false, cameraMatrix, 0)
//            GLES31.glUniformMatrix4fv(locMvpMatrix, 1, false, cameraMatrix, 0)

        vao.bind()
        glDrawArrays(GL_POINTS, 0, 106)
        vao.unbind()
    }
}