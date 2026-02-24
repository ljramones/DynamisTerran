#version 450

layout(location=0) in vec2 inUV;
layout(location=1) in vec2 inScreenUV;
layout(location=2) in vec3 inWorldPos;
layout(location=0) out vec4 outColor;

layout(set=0, binding=0) uniform sampler2D depthBuffer;
layout(set=0, binding=1) uniform sampler2D colorBuffer;
layout(set=0, binding=2) uniform sampler2D waterNormal;
layout(set=0, binding=3) uniform sampler3D aerialLut;

layout(set=1, binding=0) uniform WaterUBO {
    float waterElevation;
    float foamDepthThreshold;
    float normalScrollSpeed;
    float normalTiling;
    vec3  shallowColor;  float pad0;
    vec3  deepColor;     float pad1;
    float gameTime;
    float terrainSizeX;
    float terrainSizeZ;
    float pad2;
};

layout(set=1, binding=1) uniform WeatherUBO {
    float snowIntensity;
    float wetness;
    float rainIntensity;
    float windSpeed;
    vec3  windDirection; float pad;
};

void main() {
    float scroll = gameTime * normalScrollSpeed;
    vec2 uv1 = inUV + vec2( scroll,  scroll * 0.7);
    vec2 uv2 = inUV + vec2(-scroll * 0.8, scroll * 1.1);
    vec3 n1  = texture(waterNormal, uv1).rgb * 2.0 - 1.0;
    vec3 n2  = texture(waterNormal, uv2).rgb * 2.0 - 1.0;
    vec3 n   = normalize(n1 + n2);

    float sceneDepth = texture(depthBuffer, inScreenUV).r;
    float waterDepth = gl_FragCoord.z;
    float depthDiff  = max(sceneDepth - waterDepth, 0.0);
    float depthFactor= clamp(depthDiff / 10.0, 0.0, 1.0);
    vec3  waterColor = mix(shallowColor, deepColor, depthFactor);

    vec2 refrUV = inScreenUV + n.xz * 0.02 * (1.0 - depthFactor);
    vec3 refraction = texture(colorBuffer, refrUV).rgb;
    waterColor = mix(waterColor, refraction, 0.4 * (1.0 - depthFactor));

    float foamFactor = 1.0 - clamp(depthDiff / foamDepthThreshold, 0.0, 1.0);
    foamFactor = pow(foamFactor, 2.0);

    float rippleStrength = 1.0 + rainIntensity * 2.0;
    n = normalize(mix(vec3(0,1,0), n, rippleStrength));

    vec3 viewDir = normalize(vec3(0,1,0));
    float fresnel = pow(1.0 - max(dot(n, viewDir), 0.0), 4.0);

    vec3 finalColor = mix(waterColor, vec3(1.0), foamFactor);

    vec4 aerial = texture(aerialLut,
        vec3(inScreenUV, clamp(gl_FragCoord.z, 0.0, 1.0)));
    finalColor = finalColor * aerial.a + aerial.rgb;

    outColor = vec4(finalColor, mix(0.7, 0.95, depthFactor));
}
