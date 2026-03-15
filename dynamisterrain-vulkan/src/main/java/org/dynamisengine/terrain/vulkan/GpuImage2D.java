package org.dynamisengine.terrain.vulkan;

import java.util.Arrays;

public final class GpuImage2D {
    private final long handle;
    private final int width;
    private final int height;
    private final int bytesPerPixel;
    private byte[] data;

    public GpuImage2D(final long handle, final int width, final int height, final int bytesPerPixel) {
        this.handle = handle;
        this.width = width;
        this.height = height;
        this.bytesPerPixel = bytesPerPixel;
        this.data = new byte[Math.max(1, width * height * bytesPerPixel)];
    }

    public long handle() {
        return this.handle;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public int bytesPerPixel() {
        return this.bytesPerPixel;
    }

    public byte[] data() {
        return this.data;
    }

    public void upload(final byte[] src) {
        this.data = Arrays.copyOf(src, src.length);
    }
}
