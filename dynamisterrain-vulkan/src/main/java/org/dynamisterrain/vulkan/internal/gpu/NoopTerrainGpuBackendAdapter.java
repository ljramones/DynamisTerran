package org.dynamisterrain.vulkan.internal.gpu;

import org.dynamisterrain.vulkan.GpuMemoryOps;

/**
 * Internal default adapter stub used during seam introduction.
 */
public final class NoopTerrainGpuBackendAdapter implements TerrainGpuBackendAdapter {
    @Override
    public long createSampler(final GpuMemoryOps memoryOps) {
        return memoryOps.createSampler();
    }
}
