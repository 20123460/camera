package com.android.facially.render

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES31
import android.opengl.Matrix
import com.android.facially.util.calculateMvp
import com.android.facially.opengl.GLProgram
import com.android.facially.opengl.GLShader
import com.android.facially.opengl.GLVao
import com.android.facially.util.readAssert

class OesRender constructor(context: Context) {
    private val vertex by lazy {
        GLShader(
            GLES31.GL_VERTEX_SHADER,
            context.readAssert("general.vert")
        )
    }
    private val vertexScreen by lazy {
        GLShader(
            GLES31.GL_VERTEX_SHADER,
            context.readAssert("general_screen.vert")
        )
    }
    private val fragment by lazy {
        GLShader(
            GLES31.GL_FRAGMENT_SHADER,
            context.readAssert("general.frag")
        )
    }
    private var program: GLProgram? = null

    private val vertexes = floatArrayOf(
        -1.0f, -1.0f,  // Lower-left
        1.0f, -1.0f,    // Lower-right
        -1.0f, 1.0f,  // Upper-left
        1.0f, 1.0f    // Upper-right
    )
    private val vao by lazy { GLVao(vertexes) }

    private var locTexMatrix = 0
    private var locMvpMatrix = 0
    private val tempMatrix = FloatArray(16)

    fun init() {
        locTexMatrix = program!!.getUniformLocation("texTransform")
        locMvpMatrix = program!!.getUniformLocation("mvpTransform")
        program!!.use()
        val loc = program!!.getUniformLocation("input_texture")
        GLES31.glUniform1i(loc, 0)
    }

    fun onDestroy() {
        vertex.release()
        fragment.release()
        program?.release()
        vao.release()
    }

    fun onDraw(
        id: Int,
        rotation: Int,
        displayRotation: Int,
        cameraMatrix: FloatArray,
        width: Int,
        height: Int,
        surfaceWidth: Int,
        surfaceHeight: Int,
        screen: Boolean
    ) {
        if (program == null) {
//            program = GLProgram(if (screen) vertexScreen else vertex, fragment)
            program = GLProgram( vertex, fragment)
            init()
        }
        program?.use()
        GLES31.glViewport(0, 0, surfaceWidth, surfaceHeight)
        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0)
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, id)

        if (screen) {
            System.arraycopy(cameraMatrix,0,tempMatrix,0,16)
            // tex matrix
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
            Matrix.rotateM(tempMatrix, 0, -displayRotation + 0f, 0f, 0f, 1f)
            GLES31.glUniformMatrix4fv(locTexMatrix, 1, false, tempMatrix, 0)
            // mvp
            calculateMvp(rotation, width, height, surfaceWidth, surfaceHeight, tempMatrix)
            GLES31.glUniformMatrix4fv(locMvpMatrix, 1, false, tempMatrix, 0)
        } else {
            GLES31.glUniformMatrix4fv(locTexMatrix, 1, false, cameraMatrix, 0)
            calculateMvp(rotation, width, height, surfaceWidth, surfaceHeight, tempMatrix)
            GLES31.glUniformMatrix4fv(locMvpMatrix, 1, false, tempMatrix, 0)
        }

        vao.bind()
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4)
        vao.unbind()
    }
}