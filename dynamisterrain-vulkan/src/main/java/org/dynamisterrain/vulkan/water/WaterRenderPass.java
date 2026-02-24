package org.dynamisterrain.vulkan.water;

import org.dynamisterrain.core.lod.Matrix4f;
import org.dynamisterrain.vulkan.GpuMemoryOps;

public final class WaterRenderPass {
    private final long pipelineHandle;
    private int recordedCalls;

    private WaterRenderPass(final long pipelineHandle) {
        this.pipelineHandle = pipelineHandle;
    }

    public static WaterRenderPass create(final long device, final long renderPass, final GpuMemoryOps memoryOps) {
        final long handle = Math.max(1L, device ^ renderPass ^ 0x5741544552504153L);
        return new WaterRenderPass(handle);
    }

    public void record(
        final long commandBuffer,
        final WaterDescriptorSets descriptorSets,
        final Matrix4f invViewProj,
        final Matrix4f viewProj,
        final int frameIndex
    ) {
        if (descriptorSets == null || descriptorSets.descriptorSet(0, frameIndex) == 0L) {
            return;
        }
        this.recordedCalls++;
    }

    public long pipelineHandle() {
        return this.pipelineHandle;
    }

    public int recordedCalls() {
        return this.recordedCalls;
    }

    public void destroy() {
        this.recordedCalls = 0;
    }
}
