package org.dynamisterrain.vulkan;

import java.util.concurrent.atomic.AtomicLong;

public final class InMemoryGpuMemoryOps implements GpuMemoryOps {
    private final AtomicLong ids = new AtomicLong(1L);

    @Override
    public GpuImage2D createImage2D(final int width, final int height, final int bytesPerPixel) {
        return new GpuImage2D(this.ids.getAndIncrement(), width, height, bytesPerPixel);
    }

    @Override
    public GpuBuffer createBuffer(final int sizeBytes) {
        return new GpuBuffer(this.ids.getAndIncrement(), sizeBytes);
    }

    @Override
    public long createSampler() {
        return this.ids.getAndIncrement();
    }

    @Override
    public void destroyImage(final GpuImage2D image) {
        // No-op in scaffold memory backend.
    }

    @Override
    public void destroyBuffer(final GpuBuffer buffer) {
        // No-op in scaffold memory backend.
    }

    @Override
    public void destroySampler(final long sampler) {
        // No-op in scaffold memory backend.
    }
}
