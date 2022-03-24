package com.android.facially

class DaeCore {
    companion object {
        init {
            System.loadLibrary("facial_jni")
        }
    }

    external fun loadModel()
}