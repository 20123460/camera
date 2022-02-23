package com.android.facially

import android.Manifest
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.opengl.GLES20.*
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
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
import com.android.facially.egl.EglCore
import com.android.facially.egl.EglCore.FLAG_TRY_GLES3
import com.android.facially.egl.WindowSurface
import java.util.concurrent.FutureTask

private fun Handler.sync(runnable: Runnable) {
    val future = FutureTask { runnable.run() }
    post(future)
    future.get()
}


abstract class CameraActivity : AppCompatActivity() {

    protected val surfaceView: SurfaceView by lazy { findViewById(R.id.surface_view) }
    protected var surfaceWidth = 0
    protected var surfaceHeight = 0

    // camera
    private val cameraExecutor by lazy { ContextCompat.getMainExecutor(this) }
    private var cameraSurfaceTexture: SurfaceTexture? = null
    private var cameraSurface: Surface? = null
    private val cameraTextures = IntArray(1)

    protected val cameraMatrix = FloatArray(16)
    protected var cameraWidth = 0
    protected var cameraHeight = 0
    protected var cameraRotation = 0

    private val cameraThread = HandlerThread("CameraThread").apply { start() }
    private var cameraHandler = Handler(cameraThread.looper)

    private var egl: EglCore? = null
    private var eglSurface: WindowSurface? = null


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

    protected var displayRotation = 0
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
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

    open fun onSurfaceCreated() {}
    open fun onSurfaceDestroy() {}
    open fun onAnalysis(proxy: ImageProxy) {}
    abstract fun onDraw(oes: Int, width: Int, height: Int)

    private fun createCameraSurface() {
        cameraHandler.sync {
            glGenTextures(1, cameraTextures, 0)
            cameraSurfaceTexture = SurfaceTexture(cameraTextures[0])
            cameraSurfaceTexture?.setDefaultBufferSize(cameraWidth, cameraHeight)
            cameraSurfaceTexture?.setOnFrameAvailableListener({
                if (surfaceView.isAttachedToWindow) {
                    it.updateTexImage()
                    it.getTransformMatrix(cameraMatrix)
                    onDraw(cameraTextures[0], cameraWidth, cameraHeight)
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
}