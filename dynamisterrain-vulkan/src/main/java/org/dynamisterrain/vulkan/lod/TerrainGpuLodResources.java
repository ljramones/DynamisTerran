package org.dynamisterrain.vulkan.lod;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.dynamisterrain.core.lod.CdlodFrameResult;
import org.dynamisterrain.core.lod.CdlodPatch;
import org.dynamisterrain.vulkan.GpuBuffer;
import org.dynamisterrain.vulkan.GpuMemoryOps;

public final class TerrainGpuLodResources {
    private final GpuMemoryOps memoryOps;

    private GpuBuffer patchListBuffer;
    private GpuBuffer visiblePatchBuffer;
    private GpuBuffer morphFactorBuffer;
    private GpuBuffer terrainVertexBuffer;
    private GpuBuffer indirectDrawBuffer;
    private GpuBuffer indirectDispatchBuffer;

    private TerrainGpuLodResources(final GpuMemoryOps memoryOps) {
        this.memoryOps = memoryOps;
    }

    public static TerrainGpuLodResources allocate(
        final long device,
        final GpuMemoryOps memoryOps,
        final int maxPatches,
        final int maxVertices
    ) {
        final TerrainGpuLodResources res = new TerrainGpuLodResources(memoryOps);
        res.patchListBuffer = memoryOps.createBuffer(Math.max(1, maxPatches * 48));
        res.visiblePatchBuffer = memoryOps.createBuffer(Math.max(1, maxPatches * 48));
        res.morphFactorBuffer = memoryOps.createBuffer(Math.max(1, maxPatches * 4));
        res.terrainVertexBuffer = memoryOps.createBuffer(Math.max(1, maxVertices * 32));
        res.indirectDrawBuffer = memoryOps.createBuffer(Math.max(1, maxPatches * 20));
        res.indirectDispatchBuffer = memoryOps.createBuffer(12);
        return res;
    }

    public void uploadPatchList(final CdlodFrameResult result, final long commandBuffer) {
        final ByteBuffer bb = ByteBuffer.allocate(result.patchCount() * 36).order(ByteOrder.LITTLE_ENDIAN);
        for (CdlodPatch patch : result.visiblePatches()) {
            bb.putInt(patch.lodLevel());
            bb.putFloat(patch.worldX());
            bb.putFloat(patch.worldZ());
            bb.putFloat(patch.worldWidth());
            bb.putFloat(patch.worldDepth());
            bb.putFloat(patch.minHeight());
            bb.putFloat(patch.maxHeight());
            bb.putFloat(patch.centerX());
            bb.putFloat(patch.centerZ());
        }
        this.patchListBuffer.upload(bb.array());
    }

    public void destroy() {
        if (this.patchListBuffer != null) {
            this.memoryOps.destroyBuffer(this.patchListBuffer);
        }
        if (this.visiblePatchBuffer != null) {
            this.memoryOps.destroyBuffer(this.visiblePatchBuffer);
        }
        if (this.morphFactorBuffer != null) {
            this.memoryOps.destroyBuffer(this.morphFactorBuffer);
        }
        if (this.terrainVertexBuffer != null) {
            this.memoryOps.destroyBuffer(this.terrainVertexBuffer);
        }
        if (this.indirectDrawBuffer != null) {
            this.memoryOps.destroyBuffer(this.indirectDrawBuffer);
        }
        if (this.indirectDispatchBuffer != null) {
            this.memoryOps.destroyBuffer(this.indirectDispatchBuffer);
        }
    }
}
