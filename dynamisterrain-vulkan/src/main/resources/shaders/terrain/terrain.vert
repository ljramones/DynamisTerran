#version 450

struct TerrainVertex {
    vec3  position; float pad0;
    vec3  normal;   float pad1;
    vec2  uv;
    float morphFactor; float pad2;
};

layout(set=0, binding=0) readonly buffer TerrainVertexBuf {
    TerrainVertex verts[];
};

layout(set=3, binding=0) uniform TerrainMaterialUBO {
    float waterElevation;
    float wickingRange;
    int   splatmapMode;
    int   materialCount;
    float terrainSizeX;
    float terrainSizeZ;
    float heightScale;
    float worldScale;
    vec3  sunDirection;
    float pad0;
    float farPlane;
    float nearPlane;
    float pad1, pad2;
};

layout(push_constant) uniform PushConstants {
    mat4 viewProj;
};

layout(location=0) out vec3  outWorldPos;
layout(location=1) out vec3  outNormal;
layout(location=2) out vec2  outUV;
layout(location=3) out float outMorphFactor;
layout(location=4) out float outLinearDepth;

void main() {
    TerrainVertex v = verts[gl_VertexIndex];

    outWorldPos = v.position;
    outNormal = normalize(v.normal);
    outUV = v.uv;
    outMorphFactor = v.morphFactor;

    vec4 clip = viewProj * vec4(v.position, 1.0);
    outLinearDepth = (clip.z / clip.w - nearPlane) / (farPlane - nearPlane);
    gl_Position = clip;
}
