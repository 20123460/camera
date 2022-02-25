#version 300 es
layout(location = 0) in vec4 in_position;

//uniform mat4 texTransform;
//uniform mat4 mvpTransform;

void main() {

    // 世界顶点坐标系 右手
    // ( -1.0,1.0 )         ( 1.0,1.0 )
    //       |--------------|
    //       |              |
    //       |              |
    //       |              |
    //       |--------------|
    // ( -1.0,-1.0 )        ( 1.0,-1.0 )


    // 归一化坐标系 左手
    // ( -1.0,-1.0 )         ( 1.0,-1.0 )
    //       |--------------|
    //       |              |
    //       |              |
    //       |              |
    //       |--------------|
    // ( -1.0,1.0 )        ( 1.0,1.0 )



    vec4 temp = vec4(in_position.x,  in_position.y, 0.0, 1.0);
    temp  =  vec4(in_position.xy * 2.0, temp.zw);
    temp  =  vec4(temp.x-1.0, temp.y - 1.0, temp.zw);

    gl_Position = temp;

    //    gl_Position = vec4(in_position.xy, 0.0, 1.0);
    gl_PointSize = 10.0;

}