package org.dynamisengine.terrain.api.gpu;

/**
 * Typed terrain sampler reference.
 */
public record TerrainGpuSamplerRef(long handle) {
    public static final TerrainGpuSamplerRef NULL = new TerrainGpuSamplerRef(0L);
}
