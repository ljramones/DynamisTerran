#version 450

layout(location=0) in vec3  inWorldPos;
layout(location=1) in vec3  inNormal;
layout(location=2) in vec2  inUV;
layout(location=3) in float inLodAlpha;
layout(location=4) in flat int inLayerIndex;

layout(location=0) out vec4 outAlbedo;
layout(location=1) out vec4 outNormalRough;
layout(location=2) out vec4 outORM;

layout(set=2, binding=0) uniform sampler2D foliageTextures[];

void main() {
    vec4 albedo = texture(foliageTextures[inLayerIndex], inUV);

    float threshold = mix(0.5, 0.1, inLodAlpha);
    if (albedo.a < threshold) discard;
    if (inLodAlpha < 0.1) discard;

    outAlbedo      = vec4(albedo.rgb, 1.0);
    outNormalRough = vec4(inNormal * 0.5 + 0.5, 0.8);
    outORM         = vec4(1.0, 0.8, 0.0, 1.0);
}
