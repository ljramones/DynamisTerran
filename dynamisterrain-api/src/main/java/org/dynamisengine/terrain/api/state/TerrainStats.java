package org.dynamisengine.terrain.api.state;

public record TerrainStats(
    int chunkCount,
    int visibleChunks,
    long triangleCount,
    long foliageInstanceCount,
    float gpuTimeMs
) {
}
