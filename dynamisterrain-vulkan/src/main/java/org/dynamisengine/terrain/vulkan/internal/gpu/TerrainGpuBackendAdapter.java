package org.dynamisengine.terrain.vulkan.internal.gpu;

import org.dynamisengine.terrain.vulkan.GpuMemoryOps;

/**
 * Internal anti-corruption seam for terrain GPU/backend interactions.
 *
 * This is intentionally internal and non-SPI in A2.
 */
public interface TerrainGpuBackendAdapter {
    long createSampler(GpuMemoryOps memoryOps);
}
