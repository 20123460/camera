package com.android.facially

import android.content.Context
import android.widget.Toast

const val TAG = "facially"

fun Context.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Context.readAssert(path: String): String {
    val stream = assets.open(path)
    val nBytes = stream.available()
    val bytes = ByteArray(nBytes)
    stream.read(bytes)
    stream.close()
    return String(bytes)
}

//                // todo test
//                GLES20.glGenFramebuffers(1, temp, 0)
//                val testbuffer = temp[0]
//                glBindFramebuffer(GL_FRAMEBUFFER, testbuffer)
//                glFramebufferTexture2D(
//                    GL_FRAMEBUFFER,
//                    GL_COLOR_ATTACHMENT0,
//                    GL_TEXTURE_2D,
//                    texture,
//                    0
//                )
//                val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
//                if (status != GL_FRAMEBUFFER_COMPLETE) {
//                    Log.e(TAG, "test: -------- glCheckFramebufferStatus")
//                }
//                glDrawBuffers(1, intArrayOf(GL_COLOR_ATTACHMENT0), 0)
//                val buffer = ByteBuffer.allocateDirect(imageWidth * imageHeight * 4);
//                buffer.position(0)
//                glReadPixels(0, 0, imageWidth, imageHeight, GL_RGBA, GL_UNSIGNED_BYTE, buffer)
//                buffer.position(0)
//                val bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
//                bitmap.copyPixelsFromBuffer(buffer)
//                Log.e(TAG, "test: -------- check bitmap")
//                val err = GLES30.glGetError()
//                if (err != GLES30.GL_NO_ERROR) {
//                    Log.e(TAG, "FbxActivity onDraw: -------------- $err")
//                }