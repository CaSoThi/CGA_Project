#version 330 core

//input from vertex shader
in struct VertexData
{
    vec3 position;
     vec2 texture;
     vec3 normale;
     vec3 toPointLight;
     vec3 toSpotLight;
     vec3 to2SpotLight;

} vertexData;

//Material
uniform sampler2D diff;
uniform sampler2D emit;
uniform sampler2D specular;
uniform float shininess;

uniform vec3 PointLightCol;
uniform vec3 PointLightAttParam;

uniform vec3 SpotLightCol;
uniform vec3 SpotLightAttParam;
uniform vec2 SpotLightAngle;
uniform vec3 SpotLightDir;

uniform vec3 farbe;


//fragment shader output
out vec4 color;

// Übergabe der Werte für diffuse specular und shininess
vec3 shade(vec3 n, vec3 l, vec3 v, vec3 diff, vec3 spec, float shine){
    // Kreuzprodukt aus normale und lichvektor
      vec3 diffuse =  diff * max(0.0, dot(n,l));            // max mit 0 -> nur positive lichwerte
      vec3 reflectDir = reflect(-l, n);                     //reflect: erwartet vektor von lichtquelle zu fragment pos -> aktuell noch anders herum
      float cosb = max(dot(v, reflectDir), 0.0);

      vec3 specular = spec * pow(cosb, shine);

      return diffuse+specular;
}

//nimmt Parameter für Faktoren entgegen, vorher definiert
float attenuate(float len, vec3 attParam){
    return 1.0/(attParam.x + attParam.y * len + attParam.z * len * len);
}
//Berechnung der Lichtintensität
vec3 pointLightIntensity(vec3 lightcolour, float len){
    return lightcolour * attenuate(len, PointLightAttParam);
}

vec3 spotLightIntensity(vec3 spotlightcolour, float len, vec3 sp, vec3 spDir){
    float costheta = dot(sp, normalize(spDir));
    float cosphi = cos(SpotLightAngle.x);
    float cosgamma = cos(SpotLightAngle.y);

    float intensity = (costheta-cosgamma)/(cosphi-cosgamma);
    float cintensity = clamp(intensity, 0.0f, 1.0f);

    return spotlightcolour * cintensity * attenuate(len, SpotLightAttParam);
   // return spotlightcolour;
}

void main(){
    vec3 n = normalize(vertexData.normale);
    vec3 v = normalize(vertexData.position);
    float lpLength = length(vertexData.toPointLight);
    vec3 lp = vertexData.toPointLight/lpLength;
    float spLength = length(vertexData.toSpotLight);
    vec3 sp = vertexData.toSpotLight/spLength;
    vec3 diffCol = texture(diff, vertexData.texture).rgb;
    vec3 emitCol = texture(emit, vertexData.texture).rgb;
    vec3 specularCol = texture(specular, vertexData.texture).rgb;

    //emissive Term
    //farbe definiert in scene
    vec3 result = emitCol*farbe;

    //ambient Term
    result += shade(n, lp, v, diffCol, specularCol, shininess) * pointLightIntensity(PointLightCol, lpLength);
    result += shade(n, sp, v, diffCol, specularCol, shininess) * spotLightIntensity(SpotLightCol, spLength, sp, SpotLightDir);

    color = vec4(result, 1.0);
}