package org.dynamisterrain.api.gpu;

public record TerrainGpuResources(
    long heightmapImage,
    long normalMap,
    long horizonMap,
    long flowMap,
    long splatmap0,
    long splatmap1,
    long foliageIndirectBuffer,
    @Deprecated(since = "0.1.0")
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

    /**
     * Preferred typed sampler reference path.
     */
    public TerrainGpuSamplerRef samplerRef() {
        return new TerrainGpuSamplerRef(this.sampler);
    }
}
