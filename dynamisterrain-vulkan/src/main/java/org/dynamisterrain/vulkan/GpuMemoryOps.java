package org.dynamisterrain.vulkan;

public interface GpuMemoryOps {
    GpuImage2D createImage2D(int width, int height, int bytesPerPixel);

    GpuBuffer createBuffer(int sizeBytes);

    long createSampler();

    void destroyImage(GpuImage2D image);

    void destroyBuffer(GpuBuffer buffer);

    void destroySampler(long sampler);
}
