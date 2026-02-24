package org.dynamisterrain.api.gpu;

public record TerrainGpuResources(
    long heightmapImage,
    long normalMap,
    long horizonMap,
    long flowMap,
    long splatmap0,
    long splatmap1,
    long foliageIndirectBuffer,
    long sampler
) {
    public static final TerrainGpuResources NULL = new TerrainGpuResources(
        0L,
        0L,
        0L,
        0L,
        0L,
        0L,
        0L,
        0L
    );
}
