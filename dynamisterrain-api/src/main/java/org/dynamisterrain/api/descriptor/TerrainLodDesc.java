package org.dynamisterrain.api.descriptor;

public record TerrainLodDesc(
    int lodLevels,
    TessellationMode tessellationMode,
    float screenSpaceError,
    int patchSize,
    float morphStart,
    float morphEnd
) {
}
