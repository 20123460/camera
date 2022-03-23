package com.android.facially.activity

import android.Manifest
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.opengl.GLES20.*
import android.opengl.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.alibaba.android.mnnkit.actor.FaceDetector
import com.alibaba.android.mnnkit.entity.FaceDetectConfig
import com.alibaba.android.mnnkit.entity.MNNCVImageFormat
import com.alibaba.android.mnnkit.entity.MNNFlipType
import com.alibaba.android.mnnkit.intf.InstanceCreatedListener
import com.android.facially.egl.EglCore
import com.android.facially.egl.EglCore.FLAG_TRY_GLES3
import com.android.facially.egl.WindowSurface
import com.android.facially.util.toast
import java.lang.Error
import java.util.concurrent.FutureTask
import com.android.facially.R
import com.android.facially.util.TAG

private fun Handler.sync(runnable: Runnable) {
    val future = FutureTask { runnable.run() }
    post(future)
    future.get()
}

abstract class CameraActivity : AppCompatActivity() {

    private val surfaceView: SurfaceView by lazy { findViewById(R.id.surface_view) }
    private var surfaceWidth = 0
    private var surfaceHeight = 0

    // camera
    private val cameraExecutor by lazy { ContextCompat.getMainExecutor(this) }
    private var cameraSurfaceTexture: SurfaceTexture? = null
    private var cameraSurface: Surface? = null
    private val cameraTextures = IntArray(1)

    private var cameraWidth = 0
    private var cameraHeight = 0
    private var cameraRotation = 0

    private val cameraThread = HandlerThread("CameraThread").apply { start() }
    private var cameraHandler = Handler(cameraThread.looper)

    private var egl: EglCore? = null
    private var eglSurface: WindowSurface? = null

    //face
    protected val landmarks = FloatArray(212)
    protected var roll = 0f // 旋转
    protected var pitch = 0f // 点头
    protected var yaw = 0f//  摇头

    // protected
    protected val cameraMatrix = FloatArray(16)
    protected val mIdentityMatrix = FloatArray(16)
    private var displayRotation = 0

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // permission

    private val request = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            openCamera()
        } else {
            toast("没有相机权限")
            finish()
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // display rotation

    private fun calRotation() {
        window.decorView.post {
            window.decorView.let { view ->
                displayRotation = when (view.display.rotation) {
                    Surface.ROTATION_0 -> 0
                    Surface.ROTATION_90 -> 90
                    Surface.ROTATION_180 -> 180
                    Surface.ROTATION_270 -> 270
                    else -> 0
                }
            }
        }
    }

    private val displayManager by lazy {
        getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int): Unit {
            calRotation()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        displayManager.unregisterDisplayListener(displayListener)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private var mFaceDetector: FaceDetector? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        Matrix.setIdentityM(mIdentityMatrix, 0)
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
        displayManager.registerDisplayListener(displayListener, Handler(mainLooper))
        calRotation()
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                cameraHandler.sync {
                    egl = EglCore(null, FLAG_TRY_GLES3)
                    eglSurface = WindowSurface(egl, holder.surface, false)
                    eglSurface?.makeCurrent()
                    onSurfaceCreated()
                }
                request.launch(Manifest.permission.CAMERA)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                surfaceWidth = width
                surfaceHeight = height
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraHandler.sync {
                    onSurfaceDestroy()
                }
            }
        })
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    open fun onSurfaceCreated() {}
    open fun onSurfaceDestroy() {}
    abstract fun onDraw(
        oes: Int,
        rotation: Int,
        displayRotation: Int,
        width: Int,
        height: Int,
        surfaceWidth: Int,
        surfaceHeight: Int
    )
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun createCameraSurface() {
        cameraHandler.sync {
            glGenTextures(1, cameraTextures, 0)
            cameraSurfaceTexture = SurfaceTexture(cameraTextures[0])
            cameraSurfaceTexture?.setDefaultBufferSize(cameraWidth, cameraHeight)
            cameraSurfaceTexture?.setOnFrameAvailableListener({
                if (surfaceView.isAttachedToWindow) {
                    it.updateTexImage()
                    it.getTransformMatrix(cameraMatrix)
                    onDraw(
                        cameraTextures[0],
                        cameraRotation,
                        displayRotation,
                        cameraWidth,
                        cameraHeight,
                        surfaceWidth,
                        surfaceHeight
                    )
                    eglSurface?.swapBuffers()
                }
            }, cameraHandler)
            cameraSurface = Surface(cameraSurfaceTexture)
        }
    }

    private fun releaseCameraSurface() {
        cameraHandler.sync {
            cameraSurface?.release()
            cameraSurfaceTexture?.release()
            glDeleteTextures(1, cameraTextures, 0)
            eglSurface?.release()
            egl?.release()
        }
    }

    private fun openCamera() {
        ProcessCameraProvider.getInstance(this).apply {
            addListener({
                val provider = get()
                val preview = Preview.Builder()
                    .setTargetResolution(Size(720, 1280))
                    .build()
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(720, 1280))
                    .build()
                analysis.setAnalyzer(cameraExecutor, {
                    onAnalysis(it)
                })
                provider.bindToLifecycle(
                    this@CameraActivity,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview, analysis
                )
                preview.setSurfaceProvider {
                    cameraWidth = it.resolution.width
                    cameraHeight = it.resolution.height
                    createCameraSurface()
                    it.setTransformationInfoListener(cameraExecutor, {
                        cameraRotation = it.rotationDegrees
                    })
                    it.provideSurface(
                        cameraSurface!!,
                        cameraExecutor, {
                            if (it.resultCode == RESULT_SURFACE_USED_SUCCESSFULLY) {
                                releaseCameraSurface()
                                cameraThread.quitSafely()
                            }
                        })
                }
            }, cameraExecutor)
        }
    }

    private var bytes: ByteArray? = null
    private fun onAnalysis(proxy: ImageProxy) {
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
}