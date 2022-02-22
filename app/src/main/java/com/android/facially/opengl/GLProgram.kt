package com.android.facially.opengl

import android.opengl.GLES20
import android.opengl.GLES30.*
import android.util.Log
import com.android.facially.TAG

class GLProgram constructor(vert: GLShader, frag: GLShader) {
    private val program = glCreateProgram()

    private val status = IntArray(1)

    init {
        glAttachShader(program, vert.shader)
        glAttachShader(program, frag.shader)
        glLinkProgram(program)
        GLES20.glGetProgramiv(program, GL_LINK_STATUS, status, 0)
        if (status[0] < 0) {
            Log.e(TAG, "error: ${glGetProgramInfoLog(program)}")
            release()
        }
    }

    fun use() {
        glUseProgram(program)
    }

    fun getUniformLocation(name: String): Int {
        return glGetUniformLocation(program, name)
    }

    fun release() {
        glDeleteProgram(program)
    }
}