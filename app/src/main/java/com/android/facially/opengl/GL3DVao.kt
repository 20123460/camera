package com.android.facially.opengl

import android.opengl.GLES20
import android.opengl.GLES30.*
import java.nio.FloatBuffer
import java.nio.IntBuffer

class GL3DVao constructor(vertexes: FloatArray, indices: IntArray) {

    private val temp = IntArray(2)
    var vertexesBuffer = 0
    var indicesBuffer = 0
    var vao = 0

    init {
        glGenBuffers(2, temp, 0).apply {
            vertexesBuffer = temp[0]
            indicesBuffer = temp[1]
        }
        glBindBuffer(GL_ARRAY_BUFFER, vertexesBuffer)
        glBufferData(
            GL_ARRAY_BUFFER,
            vertexes.size * 4,
            FloatBuffer.wrap(vertexes),
            GL_DYNAMIC_DRAW
        )
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer)
        glBufferData(
            GL_ELEMENT_ARRAY_BUFFER,
            indices.size * 4,
            IntBuffer.wrap(indices),
            GL_STATIC_DRAW
        )

        glGenVertexArrays(1, temp, 0).apply { vao = temp[0] }
        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vertexesBuffer)
        glEnableVertexAttribArray(0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * 4, 0)
        glEnableVertexAttribArray(1)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * 4, 3*4)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,indicesBuffer)

        glBindVertexArray(GL_NONE)
        glBindBuffer(GL_ARRAY_BUFFER, GL_NONE)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, GL_NONE)
    }

//    fun update(data: FloatArray) {
//        glBindBuffer(GL_ARRAY_BUFFER, vertexesBuffer)
//        glBufferSubData(GL_ARRAY_BUFFER, 0, data.size * 4, FloatBuffer.wrap(data))
//        glBindBuffer(GL_ARRAY_BUFFER, GL_NONE)
//    }

    fun bind() {
        glBindVertexArray(vao)
    }

    fun unbind() {
        glBindVertexArray(GL_NONE)
    }

    fun release() {
        GLES20.glDeleteBuffers(2, intArrayOf(vertexesBuffer,indicesBuffer), 0)
        glDeleteVertexArrays(1, intArrayOf(vao), 0)
    }
}