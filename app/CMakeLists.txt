cmake_minimum_required(VERSION 3.4.1)

add_definitions(-DBUILD_SHARED_LIBS)
add_subdirectory(../third_party/libfacedetection face)

add_library(facially_jni
        SHARED
        src/main/cpp/jni.cpp
        src/main/cpp/jni_detect.cpp
        )
target_include_directories(facially_jni PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/libs/assimp/include)
target_include_directories(facially_jni PRIVATE ${CMAKE_BINARY_DIR})
#target_include_directories(facially_jni PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/libs/libfacedetection/include)
target_include_directories(facially_jni PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/../third_party/libfacedetection/src)
link_directories(${CMAKE_CURRENT_SOURCE_DIR}/libs/libfacedetection/${CMAKE_ANDROID_ARCH_ABI})
target_link_libraries(facially_jni
        android
        log
        ${CMAKE_CURRENT_SOURCE_DIR}/libs/assimp/${CMAKE_ANDROID_ARCH_ABI}/libassimp.so
        ${CMAKE_CURRENT_SOURCE_DIR}/libs/assimp/${CMAKE_ANDROID_ARCH_ABI}/libandroid_jniiosystem.a
#        ${CMAKE_CURRENT_SOURCE_DIR}/libs/libfacedetection/${CMAKE_ANDROID_ARCH_ABI}/libfacedetection.a
        facedetection
        )

