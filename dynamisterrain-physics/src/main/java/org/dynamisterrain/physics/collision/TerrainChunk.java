package org.dynamisterrain.physics.collision;

public record TerrainChunk(
    int chunkX,
    int chunkZ,
    int chunkSizeTexels,
    float worldOriginX,
    float worldOriginZ,
    float worldSize,
    long collisionShapeHandle,
    long rigidBodyHandle
) {
}
