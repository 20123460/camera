package com.android.facially

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.graphics.RectF
import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.opengl.GLES20
import android.opengl.GLES31.*
import android.opengl.Matrix
import android.os.Bundle
import android.util.Log
import android.view.Surface
import androidx.lifecycle.Transformations.map
import com.android.facially.opengl.GLProgram
import com.android.facially.opengl.GLShader
import com.android.facially.opengl.GLVao
import javax.microedition.khronos.opengles.GL10Ext
import javax.microedition.khronos.opengles.GL11Ext


class LandmarkActivity : CameraActivity() {

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, LandmarkActivity::class.java))
        }
    }

    private val vertex by lazy { GLShader(GL_VERTEX_SHADER, readAssert("landmark.vert")) }
    private val fragment by lazy { GLShader(GL_FRAGMENT_SHADER, readAssert("landmark.frag")) }
    val program by lazy { GLProgram(vertex, fragment) }
    val vao by lazy { GLVao() }
    var locTexMatrix = 0
    var locMvpMatrix = 0


    override fun onSurfaceCreated() {
        locTexMatrix = program.getUniformLocation("texTransform")
        locMvpMatrix = program.getUniformLocation("mvpTransform")
        program.use()
        val loc = program.getUniformLocation("input_texture")
        glUniform1i(loc, 0)
    }

    override fun onDraw(oes: Int, width: Int, height: Int) {
        program.use()
        glViewport(0, 0, surfaceWidth, surfaceHeight)
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        glClear(GL_COLOR_BUFFER_BIT)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_EXTERNAL_OES, oes)


        when (displayRotation) {
            90 -> {
                Matrix.translateM(cameraMatrix, 0, 0f, 1f, 0f)
            }
            180 -> {
                Matrix.translateM(cameraMatrix, 0, 1f, 1f, 0f)
            }
            270 -> {
                Matrix.translateM(cameraMatrix, 0, 1f, 0f, 0f)
            }
        }
        Matrix.rotateM(cameraMatrix, 0, -displayRotation + 0f, 0f, 0f, 1f)
        glUniformMatrix4fv(locTexMatrix, 1, false, cameraMatrix, 0)
        calculateMvp()
        glUniformMatrix4fv(locMvpMatrix, 1, false, cameraMatrix, 0)

        vao.bind()
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        vao.unbind()

        if (glGetError() != GL_NO_ERROR) {
            Log.e(TAG, "onDraw: --------- error : ${glGetError()}")
        }
    }

    private val temp = FloatArray(32)
    private val result = FloatArray(32)
    private val previewSize = RectF()
    private val previewCropSize = RectF()


    private fun calculateMvp() {
        val flip = cameraRotation == 90 || cameraRotation == 270
        val textureWidth = if (flip) cameraHeight else cameraWidth
        val textureHeight = if (flip) cameraWidth else cameraHeight
        val halfWidth = textureWidth / 2f
        val halfHeight = textureHeight / 2f
        Matrix.setIdentityM(temp, 0)
        Matrix.translateM(temp, 0, halfWidth, halfHeight, 0f)
        Matrix.scaleM(temp, 0, halfWidth, halfHeight, 1f)
        Matrix.setLookAtM(
            temp,
            16,
            halfWidth,
            halfHeight,
            1f,
            halfWidth,
            halfHeight,
            0f,
            0f,
            1f,
            0f
        )
        Matrix.multiplyMM(result, 0, temp, 16, temp, 0)
        // p
        val centerCropMatrix = android.graphics.Matrix()
        previewSize.set(0f, 0f, textureWidth + 0f, textureHeight + 0f)
        previewCropSize.set(0f, 0f, surfaceWidth + 0f, surfaceHeight + 0f)
        centerCropMatrix.setRectToRect(
            previewCropSize,
            previewSize,
            android.graphics.Matrix.ScaleToFit.CENTER
        )
        centerCropMatrix.mapRect(previewCropSize)
        Matrix.orthoM(
            result,
            16,
            -previewCropSize.width() / 2f,
            previewCropSize.width() / 2f,
            -previewCropSize.height() / 2f,
            previewCropSize.height() / 2f, 0f, 1f
        )
        Matrix.multiplyMM(cameraMatrix, 0, result, 16, result, 0)
    }
}