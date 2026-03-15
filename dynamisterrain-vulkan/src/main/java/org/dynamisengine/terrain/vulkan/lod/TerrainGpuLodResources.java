package org.dynamisengine.terrain.vulkan.lod;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import org.dynamisengine.terrain.core.lod.CdlodFrameResult;
import org.dynamisengine.terrain.core.lod.CdlodPatch;
import org.dynamisengine.terrain.vulkan.GpuBuffer;
import org.dynamisengine.terrain.vulkan.GpuMemoryOps;

public final class TerrainGpuLodResources {
    private final GpuMemoryOps memoryOps;
    private final int maxPatches;
    private final int maxVertices;

    private GpuBuffer patchListBuffer;
    private GpuBuffer visiblePatchBuffer;
    private GpuBuffer morphFactorBuffer;
    private GpuBuffer terrainVertexBuffer;
    private GpuBuffer indirectDrawBuffer;
    private GpuBuffer indirectDispatchBuffer;

    private List<CdlodPatch> patchList = List.of();
    private List<CdlodPatch> visiblePatches = List.of();
    private float[] morphFactors = new float[0];
    private int visibleCount = 0;

    private TerrainGpuLodResources(final GpuMemoryOps memoryOps, final int maxPatches, final int maxVertices) {
        this.memoryOps = memoryOps;
        this.maxPatches = maxPatches;
        this.maxVertices = maxVertices;
    }

    public static TerrainGpuLodResources allocate(
        final long device,
        final GpuMemoryOps memoryOps,
        final int maxPatches,
        final int maxVertices
    ) {
        final TerrainGpuLodResources res = new TerrainGpuLodResources(memoryOps, maxPatches, maxVertices);
        res.patchListBuffer = memoryOps.createBuffer(Math.max(1, maxPatches * 48));
        res.visiblePatchBuffer = memoryOps.createBuffer(Math.max(1, maxPatches * 48));
        res.morphFactorBuffer = memoryOps.createBuffer(Math.max(1, maxPatches * 4));
        res.terrainVertexBuffer = memoryOps.createBuffer(Math.max(1, maxVertices * 48));
        res.indirectDrawBuffer = memoryOps.createBuffer(Math.max(1, maxPatches * 16));
        res.indirectDispatchBuffer = memoryOps.createBuffer(12);
        return res;
    }

    public void uploadPatchList(final CdlodFrameResult result, final long commandBuffer) {
        this.patchList = List.copyOf(result.visiblePatches());
        this.visibleCount = 0;
        this.visiblePatches = List.of();
        this.morphFactors = new float[0];

        final ByteBuffer bb = ByteBuffer.allocate(this.patchList.size() * 36).order(ByteOrder.LITTLE_ENDIAN);
        for (CdlodPatch patch : this.patchList) {
            bb.putFloat(patch.worldX());
            bb.putFloat(patch.worldZ());
            bb.putFloat(Math.max(patch.worldWidth(), patch.worldDepth()));
            bb.putInt(patch.lodLevel());
            bb.putFloat(Math.max(1.0f, patch.worldWidth() * (1 << patch.lodLevel())));
            bb.putFloat(0f);
            bb.putFloat(0f);
            bb.putFloat(0f);
        }
        this.patchListBuffer.upload(bb.array());
    }

    void setSelectionOutput(final List<CdlodPatch> visible, final float[] morph, final int dispatchX) {
        this.visiblePatches = List.copyOf(visible);
        this.visibleCount = visible.size();
        this.morphFactors = morph.clone();

        final ByteBuffer vis = ByteBuffer.allocate(Math.max(1, visible.size() * 36)).order(ByteOrder.LITTLE_ENDIAN);
        for (CdlodPatch patch : visible) {
            vis.putFloat(patch.worldX());
            vis.putFloat(patch.worldZ());
            vis.putFloat(Math.max(patch.worldWidth(), patch.worldDepth()));
            vis.putInt(patch.lodLevel());
            vis.putFloat(Math.max(1.0f, patch.worldWidth() * (1 << patch.lodLevel())));
            vis.putFloat(0f);
            vis.putFloat(0f);
            vis.putFloat(0f);
        }
        this.visiblePatchBuffer.upload(vis.array());

        final ByteBuffer mf = ByteBuffer.allocate(Math.max(1, morph.length * 4)).order(ByteOrder.LITTLE_ENDIAN);
        for (float m : morph) {
            mf.putFloat(m);
        }
        this.morphFactorBuffer.upload(mf.array());

        final ByteBuffer dispatch = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
        dispatch.putInt(dispatchX);
        dispatch.putInt(1);
        dispatch.putInt(1);
        this.indirectDispatchBuffer.upload(dispatch.array());
    }

    void setTessellationOutput(final byte[] vertices, final byte[] draws) {
        this.terrainVertexBuffer.upload(vertices);
        this.indirectDrawBuffer.upload(draws);
    }

    public List<CdlodPatch> patchList() {
        return this.patchList;
    }

    public List<CdlodPatch> visiblePatches() {
        return this.visiblePatches;
    }

    public float[] morphFactors() {
        return this.morphFactors;
    }

    public int visibleCount() {
        return this.visibleCount;
    }

    public byte[] terrainVertexBytes() {
        return this.terrainVertexBuffer.data();
    }

    public byte[] indirectDrawBytes() {
        return this.indirectDrawBuffer.data();
    }

    void overwriteIndirectDrawBytes(final byte[] draws) {
        this.indirectDrawBuffer.upload(draws);
    }

    public byte[] indirectDispatchBytes() {
        return this.indirectDispatchBuffer.data();
    }

    public GpuBuffer visiblePatchBuffer() {
        return this.visiblePatchBuffer;
    }

    public GpuBuffer morphFactorBuffer() {
        return this.morphFactorBuffer;
    }

    public GpuBuffer terrainVertexBuffer() {
        return this.terrainVertexBuffer;
    }

    public GpuBuffer indirectDrawBuffer() {
        return this.indirectDrawBuffer;
    }

    public int maxPatches() {
        return this.maxPatches;
    }

    public int maxVertices() {
        return this.maxVertices;
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
        this.patchList = new ArrayList<>();
        this.visiblePatches = new ArrayList<>();
        this.morphFactors = new float[0];
    }
}
