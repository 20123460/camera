#version 300 es
layout(location = 0) in vec4 in_position;

uniform mat4 texTransform;
uniform mat4 mvpTransform;

void main() {

//    vec4 test = vec4(in_position.x, 1.0-in_position.xy, in_position.zw);
    //    gl_Position =   texTransform * temp;


    vec4 temp  = texTransform * in_position;
    temp = vec4(1.0-temp.xy, temp.zw);
    gl_Position = mvpTransform * vec4(temp.xy*2.0-1.0, temp.zw);
    gl_PointSize = 10.0;
}