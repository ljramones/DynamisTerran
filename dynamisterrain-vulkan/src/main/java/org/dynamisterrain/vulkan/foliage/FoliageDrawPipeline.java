package org.dynamisterrain.vulkan.foliage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class FoliageDrawPipeline {
    private final long pipelineHandle;
    private int lastRecordedDrawCount;

    private FoliageDrawPipeline(final long pipelineHandle) {
        this.pipelineHandle = pipelineHandle;
    }

    public static FoliageDrawPipeline create(final long device, final long renderPass, final long descriptorSetLayout) {
        final long handle = Math.max(1L, device ^ renderPass ^ descriptorSetLayout ^ 0x464F4C49414745L);
        return new FoliageDrawPipeline(handle);
    }

    public void record(
        final long commandBuffer,
        final FoliageInstanceBuffer instanceBuf,
        final FoliageDescriptorSets descriptorSets,
        final int layerCount,
        final int frameIndex
    ) {
        if (this.pipelineHandle == 0L || descriptorSets == null || instanceBuf == null) {
            this.lastRecordedDrawCount = 0;
            return;
        }

        int draws = 0;
        final ByteBuffer bb = ByteBuffer.wrap(instanceBuf.indirectDrawBuffer().data()).order(ByteOrder.LITTLE_ENDIAN);
        final int max = Math.min(layerCount, instanceBuf.indirectDrawBuffer().data().length / 16);
        for (int i = 0; i < max; i++) {
            final int offset = i * 16;
            final int vertexCount = bb.getInt(offset);
            final int instanceCount = bb.getInt(offset + 4);
            if (vertexCount > 0 && instanceCount > 0) {
                draws++;
            }
        }
        this.lastRecordedDrawCount = draws;
    }

    public long pipelineHandle() {
        return this.pipelineHandle;
    }

    public int lastRecordedDrawCount() {
        return this.lastRecordedDrawCount;
    }

    public void destroy() {
        this.lastRecordedDrawCount = 0;
    }
}
