#version 300 es
precision mediump float;
in vec2 frag_coord;
layout(location = 0) out vec4 outColor;
uniform sampler2D input_texture;

void main() {
    outColor = texture(input_texture, frag_coord);
}