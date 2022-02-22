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