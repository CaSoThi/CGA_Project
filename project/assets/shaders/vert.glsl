#version 330 core

layout(location = 0) in vec3 position;
layout(location =  1) in vec2 tc;
layout(location = 2) in vec3 normale;

uniform mat4 model_matrix;
uniform mat4 view_matrix;
uniform mat4 proj_matrix;
uniform vec2 tcMultiplier;

out struct VertexData
{
    vec3 position;
    vec2 texture;
    vec3 normale;

} vertexData;



void main(){

     mat4 modelView = view_matrix * model_matrix;
     vec4 pos =  modelView * vec4(position, 1.0f);
     vec4 nor = inverse(transpose(modelView)) * vec4(normale, 0.0f);

     gl_Position = proj_matrix * pos;
     vertexData.position = -pos.xyz;
     vertexData.texture = tc * tcMultiplier;
     vertexData.normale = nor.xyz;


}
