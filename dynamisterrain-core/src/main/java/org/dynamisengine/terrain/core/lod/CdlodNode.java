package org.dynamisengine.terrain.core.lod;

public record CdlodNode(
    int lodLevel,
    float worldX,
    float worldZ,
    int patchSize,
    int childCount
) {
}
