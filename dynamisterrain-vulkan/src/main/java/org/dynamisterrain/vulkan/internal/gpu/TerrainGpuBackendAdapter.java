package org.dynamisterrain.vulkan.internal.gpu;

import org.dynamisterrain.vulkan.TerrainGpuContext;

/**
 * Internal anti-corruption seam for terrain GPU/backend interactions.
 *
 * This is intentionally internal and non-SPI in A1.
 */
public interface TerrainGpuBackendAdapter {
    void bindContext(TerrainGpuContext context);
}
