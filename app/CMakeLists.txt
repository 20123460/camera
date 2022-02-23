cmake_minimum_required(VERSION 3.4.1)

add_library(facially_jni
        SHARED
        src/main/cpp/jni.cpp
        )
target_include_directories(facially_jni PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/libs/assimp/include)
target_link_libraries(facially_jni
        android
        log
        ${CMAKE_CURRENT_SOURCE_DIR}/libs/assimp/${CMAKE_ANDROID_ARCH_ABI}/libassimp.so
        ${CMAKE_CURRENT_SOURCE_DIR}/libs/assimp/${CMAKE_ANDROID_ARCH_ABI}/libandroid_jniiosystem.a
        )

