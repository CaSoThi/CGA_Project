#version 330 core

layout(location = 0) in vec3 position;
layout(location =  1) in vec2 tc;
layout(location = 2) in vec3 normale;

//uniforms
// translation object to world
uniform mat4 model_matrix;
uniform mat4 view_matrix;
uniform mat4 proj_matrix;
uniform vec2 tcMultiplier;

//Position von Lights entgegen nehmen
uniform vec3 byklePointLightPos;
uniform vec3 bykleSpotLightPos;
uniform vec3 bykleSpot2LightPos;


out struct VertexData
{
    vec3 position;
    vec2 texture;
    vec3 normale;
    vec3 toPointLight;
    vec3 toSpotLight;
    vec3 to2SpotLight;

} vertexData;



void main(){
    mat4 modelView = view_matrix * model_matrix;
    vec4 pos =  modelView * vec4(position, 1.0f);
    vec4 nor = inverse(transpose(modelView)) * vec4(normale, 0.0f);

    // Pointlight im Camera Space platzieren
    // Berechnung der toLight Vektors
    // Lichtrichtung
    vec4 lp = view_matrix * vec4(byklePointLightPos, 1.0);
    vertexData.toPointLight = (lp - pos).xyz;                          //von p zu lp (licht im camera space)

    vec4 lp2 = view_matrix * vec4(bykleSpotLightPos, 1.0);
    vertexData.toSpotLight = (lp2 - pos).xyz;

    vec4 lp3 = view_matrix * vec4(bykleSpot2LightPos, 1.0);
    vertexData.to2SpotLight = (lp3 - pos).xyz;


    gl_Position = proj_matrix * pos;
    vertexData.position = -pos.xyz;
    vertexData.texture = tc * tcMultiplier;
    vertexData.normale = nor.xyz;



}