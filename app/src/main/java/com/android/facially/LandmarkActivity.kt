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
import androidx.camera.core.ImageProxy
import androidx.lifecycle.Transformations.map
import com.alibaba.android.mnnkit.actor.FaceDetector
import com.alibaba.android.mnnkit.entity.MNNCVImageFormat
import com.android.facially.opengl.GLProgram
import com.android.facially.opengl.GLShader
import com.android.facially.opengl.GLVao
import javax.microedition.khronos.opengles.GL10Ext
import javax.microedition.khronos.opengles.GL11Ext
import com.alibaba.android.mnnkit.intf.InstanceCreatedListener
import java.lang.Error
import com.alibaba.android.mnnkit.entity.FaceDetectConfig
import com.alibaba.android.mnnkit.entity.MNNFlipType


open class LandmarkActivity : CameraActivity() {

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, LandmarkActivity::class.java))
        }
    }

    open fun drawLandmark(): Boolean {
        return true
    }

    private val vertex by lazy {
        GLShader(
            GL_VERTEX_SHADER,
            readAssert(if (drawLandmark()) "landmark.vert" else "general.vert")
        )
    }
    private val fragment by lazy {
        GLShader(
            GL_FRAGMENT_SHADER,
            readAssert(if (drawLandmark()) "landmark.frag" else "general.frag")
        )
    }
    private val program by lazy { GLProgram(vertex, fragment) }
    private val vao by lazy { GLVao() }
    private val landmarks = FloatArray(212)
    private var roll = 0f // 旋转
    private var pitch = 0f // 点头
    private var yaw = 0f//  摇头
    var locTexMatrix = 0
    var locMvpMatrix = 0
    var locLandmarks = 0

    private var mFaceDetector: FaceDetector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val createConfig = FaceDetector.FaceDetectorCreateConfig()
        createConfig.mode = FaceDetector.FaceDetectMode.MOBILE_DETECT_MODE_VIDEO
        FaceDetector.createInstanceAsync(
            this,
            createConfig,
            object : InstanceCreatedListener<FaceDetector> {
                override fun onSucceeded(faceDetector: FaceDetector) {
                    mFaceDetector = faceDetector
                }

                override fun onFailed(i: Int, error: Error) {
                    Log.e(TAG, "create face detetector failed: $error")
                }
            })
    }

    override fun onSurfaceDestroy() {
        vertex.release()
        fragment.release()
        program.release()
        vao.release()
    }


    override fun onSurfaceCreated() {
        locTexMatrix = program.getUniformLocation("texTransform")
        locMvpMatrix = program.getUniformLocation("mvpTransform")
        if (drawLandmark()) {
            locLandmarks = program.getUniformLocation("landmarks")
        }
        program.use()
        val loc = program.getUniformLocation("input_texture")
        glUniform1i(loc, 0)
    }

    private var bytes: ByteArray? = null

    override fun onAnalysis(proxy: ImageProxy) {
        val width = proxy.width
        val height = proxy.height
        if (bytes == null || bytes?.size != width * height) {
            bytes = ByteArray(width * height)
        }
        proxy.planes[0].buffer.get(bytes!!)
        val detectConfig =
            FaceDetectConfig.ACTIONTYPE_HEAD_YAW or FaceDetectConfig.ACTIONTYPE_HEAD_PITCH
        val report = mFaceDetector?.inference(
            bytes,
            width,
            height,
            MNNCVImageFormat.GRAY,
            detectConfig,
            cameraRotation,
            cameraRotation,
            MNNFlipType.FLIP_NONE
        )
        Log.e(TAG, "onAnalysis: -- ${report?.size ?: 0}")
        if (!report.isNullOrEmpty()) {
            val face = report[0]
            System.arraycopy(face.keyPoints, 0, landmarks, 0, 212)
            yaw = face.yaw
            pitch = face.pitch
            roll = face.roll
            for (i in 0 until 106) {
                landmarks[i * 2] /= width + 0f
                landmarks[i * 2 + 1] /= height + 0f
            }
        } else {
            for (i in 0 until 212) landmarks[i] = 0f
        }
        proxy.close()
    }

    override fun onDraw(oes: Int, width: Int, height: Int) {
        program.use()
        glViewport(0, 0, surfaceWidth, surfaceHeight)
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        glClear(GL_COLOR_BUFFER_BIT)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_EXTERNAL_OES, oes)

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
        Matrix.rotateM(cameraMatrix, 0, -displayRotation + 0f, 0f, 0f, 1f)
        glUniformMatrix4fv(locTexMatrix, 1, false, cameraMatrix, 0)
        // mvp
        calculateMvp()
        glUniformMatrix4fv(locMvpMatrix, 1, false, cameraMatrix, 0)
        // landmark
        if (drawLandmark()) {
            glUniform2fv(locLandmarks, 106, landmarks, 0)
        }

        vao.bind()
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
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