#version 330 core

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aColor;
layout(location = 2) in vec3 aNormal;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

out vec3 Normal;
out vec3 FragPos;

void main()
{
    FragPos = vec3(model*vec4(aPos, 1.0));

    Normal = inverse(transpose(mat3(model))) * aNormal;

    gl_Position = projection * view * vec4(FragPos, 1.0);
}