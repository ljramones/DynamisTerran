package org.dynamisterrain.core.lod;

public record CdlodPatch(
    int lodLevel,
    float worldX,
    float worldZ,
    float worldWidth,
    float worldDepth,
    float minHeight,
    float maxHeight,
    float centerX,
    float centerZ
) {
}
