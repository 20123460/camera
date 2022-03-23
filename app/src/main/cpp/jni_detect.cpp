#include <jni.h>

//
// Created by 20123460 on 2022/3/10.
//
#include "../../../libs/libfacedetection/include/facedetection/facedetectcnn.h"
#include <android/log.h>

typedef struct {
    float x1;
    float y1;
    float x2;
    float y2;
    float score;
} f_box;


extern "C"
JNIEXPORT jlong JNICALL
Java_com_android_facially_color_FaceActivity_detect(JNIEnv *env, jobject thiz, jbyteArray image,
                                                    jint width, jint height) {
    jbyte *data = env->GetByteArrayElements(image, 0);
    unsigned char buffer[0x2000];

    auto start = std::chrono::system_clock::now();
    int *pResults = facedetect_cnn(buffer,reinterpret_cast<unsigned char *>(data), width, height,width*3);
    auto end = std::chrono::system_clock::now();
    long time = (end-start).count()/1000;

    for (int i = 0; i < (pResults ? *pResults : 0); i++) {
        f_box box;
        short *p = ((short *) (pResults + 1)) + 142 * i;
        box.score = p[0];
        box.x1 = p[1];
        box.y1 = p[2];
        int w = p[3];
        int h = p[4];
        box.x2 = box.x1 + float(w);
        box.y2 = box.y1 + float(h);
    }
    return time;
}