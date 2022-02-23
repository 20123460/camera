#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;
in vec2 frag_coord;
layout(location = 0) out vec4 outColor;
uniform samplerExternalOES input_texture;
uniform vec2 landmarks[106];

void main() {
    bool have = false;
    for (int i =0;i<106;i++){
        if (distance(landmarks[i], frag_coord) < 0.005){
            have = true;
        }
    }
    if (have){
        outColor = vec4(1.0, 0.0, 0.0, 1.0);
    } else {
        outColor = texture(input_texture, frag_coord);
    }
}