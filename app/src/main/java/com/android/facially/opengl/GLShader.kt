package com.android.facially.opengl

import android.opengl.GLES30.*
import android.util.Log
import com.android.facially.util.TAG

class GLShader constructor(type: Int, shaderSource: String) {
    val shader = glCreateShader(type)
    private val status = IntArray(1)

    init {
        glShaderSource(shader, shaderSource)
        glCompileShader(shader)
        glGetShaderiv(shader, GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e(TAG, "error:" + glGetShaderInfoLog(shader))
            glDeleteShader(shader)
        }
    }

    fun release() {
        glDeleteShader(shader)
    }
}