#version 450

const vec2 QUAD[4] = vec2[](
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0)
);

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

layout(push_constant) uniform PC {
    mat4 viewProj;
};

layout(location=0) out vec2 outUV;
layout(location=1) out vec2 outScreenUV;
layout(location=2) out vec3 outWorldPos;

void main() {
    vec2 q    = QUAD[gl_VertexIndex];
    vec3 wpos = vec3(q.x * terrainSizeX, waterElevation, q.y * terrainSizeZ);

    vec4 clip   = viewProj * vec4(wpos, 1.0);
    outUV       = q * normalTiling;
    outScreenUV = (clip.xy / clip.w) * 0.5 + 0.5;
    outWorldPos = wpos;
    gl_Position = clip;
}
