#version 450

layout(location=0) in vec3  inWorldPos;
layout(location=1) in vec3  inNormal;
layout(location=2) in vec2  inUV;
layout(location=3) in float inMorphFactor;
layout(location=4) in float inLinearDepth;

layout(location=0) out vec4 outAlbedo;
layout(location=1) out vec4 outNormalRough;
layout(location=2) out vec4 outORM;

layout(set=0, binding=1) uniform sampler2D normalMapTex;
layout(set=0, binding=2) uniform sampler2D horizonMapTex;
layout(set=0, binding=3) uniform sampler2D flowMapTex;
layout(set=1, binding=0) uniform sampler2D splatmap0Tex;
layout(set=1, binding=1) uniform sampler2D splatmap1Tex;
layout(set=2, binding=0) uniform sampler3D aerialPerspectiveLut;

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

layout(set=3, binding=1) uniform WeatherUBO {
    float snowIntensity;
    float wetness;
    float rainIntensity;
    float windSpeed;
    vec3  windDirection;
    float pad;
};

layout(set=4, binding=0) uniform sampler2D materialTextures[];

float sampleHorizonAngle(vec2 uv, float sunAzimuth) {
    float dirF  = mod(sunAzimuth / (2.0 * 3.14159265), 1.0) * 8.0;
    int   dir0  = int(dirF) % 8;
    int   dir1  = (dir0 + 1) % 8;
    float t     = fract(dirF);

    vec4 packed = texture(horizonMapTex, uv);

    float channels[8];
    channels[0] = floor(packed.r * 255.0) / 16.0;
    channels[1] = mod(packed.r * 255.0, 16.0);
    channels[2] = floor(packed.g * 255.0) / 16.0;
    channels[3] = mod(packed.g * 255.0, 16.0);
    channels[4] = floor(packed.b * 255.0) / 16.0;
    channels[5] = mod(packed.b * 255.0, 16.0);
    channels[6] = floor(packed.a * 255.0) / 16.0;
    channels[7] = mod(packed.a * 255.0, 16.0);

    float angle0 = channels[dir0] / 15.0 * (3.14159265 * 0.5);
    float angle1 = channels[dir1] / 15.0 * (3.14159265 * 0.5);
    return mix(angle0, angle1, t);
}

vec4 blendMaterials(vec2 uv, int layerCount) {
    vec4 weights0 = texture(splatmap0Tex, uv);

    vec3 albedo = vec3(0.0);
    albedo += weights0.r * texture(materialTextures[0], uv * 4.0).rgb;
    albedo += weights0.g * texture(materialTextures[3], uv * 4.0).rgb;
    albedo += weights0.b * texture(materialTextures[6], uv * 4.0).rgb;
    albedo += weights0.a * texture(materialTextures[9], uv * 4.0).rgb;

    return vec4(albedo, 1.0);
}

float wickFactor(float worldY) {
    return 1.0 - smoothstep(0.0, wickingRange, worldY - waterElevation);
}

vec3 applyAerialPerspective(vec3 color, vec2 screenUV, float linearDepth) {
    vec4 aerial = texture(aerialPerspectiveLut,
        vec3(screenUV, clamp(linearDepth, 0.0, 1.0)));
    return color * aerial.a + aerial.rgb;
}

void main() {
    vec2 uv = inUV;

    vec4 albedo = blendMaterials(uv, splatmapMode);

    float upFacing = max(0.0, dot(inNormal, vec3(0,1,0)));
    albedo.rgb = mix(albedo.rgb, vec3(0.95, 0.97, 1.0),
                     snowIntensity * upFacing);

    float wetDarken = 1.0 - wetness * 0.35;
    albedo.rgb *= wetDarken;

    float wick = wickFactor(inWorldPos.y) * wetness;
    albedo.rgb *= (1.0 - wick * 0.4);

    float sunAzimuth = atan(sunDirection.x, sunDirection.z) + 3.14159265;
    float horizonAngle = sampleHorizonAngle(uv, sunAzimuth);
    float sunAltitude = asin(clamp(sunDirection.y, -1.0, 1.0));
    float shadowFactor = sunAltitude > horizonAngle ? 1.0 : 0.15;

    float roughness = 0.85;
    roughness *= (1.0 - wetness * 0.5);

    vec2 screenUV = gl_FragCoord.xy /
        vec2(float(textureSize(aerialPerspectiveLut, 0).x),
             float(textureSize(aerialPerspectiveLut, 0).y));
    vec3 finalColor = applyAerialPerspective(
        albedo.rgb * shadowFactor, screenUV, inLinearDepth);

    outAlbedo = vec4(finalColor, 1.0);
    outNormalRough = vec4(inNormal * 0.5 + 0.5, roughness);
    outORM = vec4(shadowFactor, roughness, 0.0, 1.0);
}
