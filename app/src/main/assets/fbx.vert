#version 300 es

layout(location = 0) in vec3 vertex;
layout(location = 1) in vec2 frag;

uniform mat4 mvpTransform;
uniform mat4 texTransform;

out vec2 frag_coord;

void main() {
    gl_Position = mvpTransform * vec4(vertex,1.0);
    gl_PointSize = 10.0;
    frag_coord = frag;
}