package org.dynamisterrain.vulkan.internal.gpu;

import org.dynamisterrain.vulkan.TerrainGpuContext;

/**
 * Internal default adapter stub used during A1 seam introduction.
 */
public final class NoopTerrainGpuBackendAdapter implements TerrainGpuBackendAdapter {
    @Override
    public void bindContext(final TerrainGpuContext context) {
        // A1 stub: no runtime behavior changes.
    }
}
