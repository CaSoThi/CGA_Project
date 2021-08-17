#version 330 core

//input from vertex shader
in struct VertexData
{
    vec3 position;
     vec2 texture;
     vec3 normale;

} vertexData;

//Material
uniform sampler2D diff;
uniform sampler2D emit;
uniform sampler2D specular;
uniform float shininess;


out vec4 color;

void main() {
    vec3 n = normalize(vertexData.normale);
    vec3 v = normalize(vertexData.position);

    vec3 diffCol = texture(diff, vertexData.texture).rgb;
    vec3 planetCol = texture(emit, vertexData.texture).rgb;
    vec3 specularCol = texture(specular, vertexData.texture).rgb;

    //emissive Term
    //farbe definiert in scene
    vec3 result = planetCol;
    color = vec4(result, 1.0);
}