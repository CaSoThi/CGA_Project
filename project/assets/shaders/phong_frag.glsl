#version 330 core
in vec3 Normal;
in vec3 FragPos;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightCol;
uniform vec3 objectCol;

out vec4 FragColor;

void main() {
    //ambient
    float ambientStrength = 0.1;
    vec3 ambient = ambientStrength * lightCol;

    //diffuse
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    float diffuseStrength = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diffuseStrength * lightCol;

    //specular
    float specularStrength = 0.5;
    vec3 viewDir = normalize(viewPos - FragPos);
    vec3 reflectDir = reflect(-lightDir, norm);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32);
    vec3 specular = specularStrength * spec * lightCol;


    //result
    vec3 result = (ambient + diffuse + specular) * objectCol;
    FragColor = vec4(result, 1.0);

}
