package com.android.facially.util

import android.graphics.RectF
import android.opengl.Matrix

private val temp = FloatArray(32)
private val result = FloatArray(32)
private val previewSize = RectF()
private val previewCropSize = RectF()

fun calculateMvp(
    rotation: Int,
    width: Int,
    height: Int,
    surfaceWidth: Int,
    surfaceHeight: Int,
    returnMatrix: FloatArray
) {
    val flip = rotation == 90 || rotation == 270
    val textureWidth = if (flip) height else width
    val textureHeight = if (flip) width else height
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
        previewCropSize.height() / 2f,
        // 由于设备归一化坐标系是左手系
//        previewCropSize.height() / 2f,
//        -previewCropSize.height() / 2f,
        0f, 1f
    )
    Matrix.multiplyMM(returnMatrix, 0, result, 16, result, 0)
}

