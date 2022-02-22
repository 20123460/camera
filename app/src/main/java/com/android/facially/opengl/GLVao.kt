package com.android.facially.opengl

import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.GLES30.*
import java.nio.FloatBuffer

class GLVao {
    private val vertexes = floatArrayOf(
        -1.0f, -1.0f,  // Lower-left
        1.0f, -1.0f,    // Lower-right
        -1.0f, 1.0f,  // Upper-left
        1.0f, 1.0f    // Upper-right
    )
    private val temp = IntArray(1)
    var buffer = 0
    var vao = 0

    init {
        glGenBuffers(1, temp, 0).apply { buffer = temp[0] }
        glBindBuffer(GL_ARRAY_BUFFER, buffer)
        glBufferData(GL_ARRAY_BUFFER, 8*4, FloatBuffer.wrap(vertexes), GL_STATIC_DRAW)

        glGenVertexArrays(1, temp, 0).apply { vao = temp[0] }
        glBindVertexArray(vao)
        glEnableVertexAttribArray(0)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2*4, 0)
        glBindVertexArray(GL_NONE)

        glBindBuffer(GL_ARRAY_BUFFER, GL_NONE)
    }

    fun bind() {
        glBindVertexArray(vao)
    }

    fun unbind() {
        glBindVertexArray(GL_NONE)
    }

    fun release() {
        GLES20.glDeleteBuffers(1, intArrayOf(buffer), 0)
        glDeleteVertexArrays(1, intArrayOf(vao), 0)
    }
}