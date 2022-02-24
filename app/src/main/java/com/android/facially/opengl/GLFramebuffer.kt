package com.android.facially.opengl

import android.opengl.GLES20
import android.opengl.GLES30.*
import android.opengl.GLES31
import android.util.Log
import com.android.facially.TAG

class GLFramebuffer constructor(width: Int, height: Int) {
    var framebuffer = 0
    var texture = 0

    var temp = IntArray(1)

    init {
        GLES20.glGenFramebuffers(1, temp, 0).apply { framebuffer = temp[0] }
        GLES20.glGenTextures(1, temp, 0).apply { texture = temp[0] }
        glBindTexture(GL_TEXTURE_2D, texture)
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA8, width, height)
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0)
    }

    fun bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer)
    }

    fun unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, GL_NONE)
    }

}