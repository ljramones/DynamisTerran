#version 450

layout(location=0) in vec3 inPosition;
layout(location=1) in vec3 inNormal;
layout(location=2) in vec2 inUV;

struct FoliageInstance {
    vec3  worldPos;     float rotation;
    float scale;        int   layerIndex;
    float lodAlpha;     float pad1;
    vec3  bsCenter;     float bsRadius;
};
layout(set=0, binding=0) readonly buffer VisibleBuf { FoliageInstance visible[]; };

layout(set=1, binding=0) uniform FoliageWindUBO {
    vec3  windDirection;  float pad0;
    float windSpeed;
    float gameTime;
    float windStrength;
    float pad1;
};

layout(push_constant) uniform PC {
    mat4 viewProj;
};

layout(location=0) out vec3  outWorldPos;
layout(location=1) out vec3  outNormal;
layout(location=2) out vec2  outUV;
layout(location=3) out float outLodAlpha;
layout(location=4) out flat int outLayerIndex;

void main() {
    FoliageInstance inst = visible[gl_InstanceIndex];

    float c = cos(inst.rotation);
    float s = sin(inst.rotation);
    mat3 rotScale = mat3(
        vec3( c, 0,  s),
        vec3( 0, 1,  0),
        vec3(-s, 0,  c)
    ) * inst.scale;

    vec3 localPos = rotScale * inPosition;

    float windMask = max(0.0, inPosition.y / max(inst.scale, 0.001));
    float windSin  = sin(gameTime * windSpeed * 2.3
                       + inst.worldPos.x * 0.1
                       + inst.worldPos.z * 0.17);
    vec3 windOffset = windDirection * windSin * windStrength * windMask;

    vec3 worldPos = inst.worldPos + localPos + windOffset;

    outWorldPos   = worldPos;
    outNormal     = normalize(rotScale * inNormal);
    outUV         = inUV;
    outLodAlpha   = inst.lodAlpha;
    outLayerIndex = inst.layerIndex;

    gl_Position = viewProj * vec4(worldPos, 1.0);
}
