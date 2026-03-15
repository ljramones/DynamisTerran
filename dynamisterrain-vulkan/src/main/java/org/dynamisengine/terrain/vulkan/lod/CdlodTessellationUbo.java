package org.dynamisengine.terrain.vulkan.lod;

public record CdlodTessellationUbo(
    float worldScale,
    float heightScale,
    int patchSize,
    int textureWidth,
    int textureHeight,
    float terrainWorldSizeX,
    float terrainWorldSizeZ
) {
}
