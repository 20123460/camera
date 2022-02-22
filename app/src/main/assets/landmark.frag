#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;
in vec2 frag_coord;
layout(location = 0) out vec4 outColor;
uniform samplerExternalOES input_texture;

void main() {
    outColor = texture(input_texture, frag_coord);
}