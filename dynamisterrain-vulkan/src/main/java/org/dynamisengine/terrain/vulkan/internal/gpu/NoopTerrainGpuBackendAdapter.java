package org.dynamisengine.terrain.vulkan.internal.gpu;

import org.dynamisengine.terrain.vulkan.GpuMemoryOps;

/**
 * Internal default adapter stub used during seam introduction.
 */
public final class NoopTerrainGpuBackendAdapter implements TerrainGpuBackendAdapter {
    @Override
    public long createSampler(final GpuMemoryOps memoryOps) {
        return memoryOps.createSampler();
    }
}
