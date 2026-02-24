package org.dynamisterrain.vulkan.lod;

public final class TerrainLodBarriers {
    private TerrainLodBarriers() {
    }

    public static void selectionToTessellation(final long commandBuffer, final TerrainGpuLodResources res) {
        // No-op in scaffold backend; explicit barriers are applied in real Vulkan backend.
    }

    public static void tessellationToDraw(final long commandBuffer, final TerrainGpuLodResources res) {
        // No-op in scaffold backend; explicit barriers are applied in real Vulkan backend.
    }
}
