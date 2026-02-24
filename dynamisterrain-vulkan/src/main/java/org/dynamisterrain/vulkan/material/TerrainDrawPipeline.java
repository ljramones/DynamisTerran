package org.dynamisterrain.vulkan.material;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.dynamisterrain.vulkan.lod.TerrainGpuLodResources;

public final class TerrainDrawPipeline {
    private final long pipelineHandle;
    private final long renderPass;
    private final long descriptorSetLayout;

    private int lastRecordedDrawCount;

    private TerrainDrawPipeline(final long pipelineHandle, final long renderPass, final long descriptorSetLayout) {
        this.pipelineHandle = pipelineHandle;
        this.renderPass = renderPass;
        this.descriptorSetLayout = descriptorSetLayout;
    }

    public static TerrainDrawPipeline create(final long device, final long renderPass, final long descriptorSetLayout) {
        final long handle = Math.max(1L, device ^ renderPass ^ descriptorSetLayout ^ 0x5445525241494E4CL);
        return new TerrainDrawPipeline(handle, renderPass, descriptorSetLayout);
    }

    public void record(
        final long commandBuffer,
        final TerrainGpuLodResources lodResources,
        final TerrainMaterialDescriptorSets descriptorSets,
        final int frameIndex
    ) {
        if (this.pipelineHandle == 0L || lodResources == null || descriptorSets == null) {
            this.lastRecordedDrawCount = 0;
            return;
        }
        if (descriptorSets.descriptorSet(0, frameIndex) == 0L) {
            this.lastRecordedDrawCount = 0;
            return;
        }

        int count = 0;
        final byte[] draws = lodResources.indirectDrawBytes();
        final ByteBuffer bb = ByteBuffer.wrap(draws).order(ByteOrder.LITTLE_ENDIAN);
        for (int offset = 0; offset + 16 <= draws.length; offset += 16) {
            final int vertexCount = bb.getInt(offset);
            final int instanceCount = bb.getInt(offset + 4);
            if (vertexCount > 0 && instanceCount > 0) {
                count++;
            }
        }
        this.lastRecordedDrawCount = count;
    }

    public int lastRecordedDrawCount() {
        return this.lastRecordedDrawCount;
    }

    public long pipelineHandle() {
        return this.pipelineHandle;
    }

    public void destroy() {
        this.lastRecordedDrawCount = 0;
    }
}
