#version 300 es
precision mediump float;
layout(location = 0) out vec4 outColor;
in vec2 frag_coord;
uniform sampler2D input_texture;

void main() {
    outColor = texture(input_texture, frag_coord);
//    outColor = vec4(1.0,0.0,0.0,1.0);
}