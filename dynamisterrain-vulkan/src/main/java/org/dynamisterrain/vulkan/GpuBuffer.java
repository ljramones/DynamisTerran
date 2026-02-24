package org.dynamisterrain.vulkan;

import java.util.Arrays;

public final class GpuBuffer {
    private final long handle;
    private byte[] data;

    public GpuBuffer(final long handle, final int sizeBytes) {
        this.handle = handle;
        this.data = new byte[Math.max(1, sizeBytes)];
    }

    public long handle() {
        return this.handle;
    }

    public byte[] data() {
        return this.data;
    }

    public void upload(final byte[] src) {
        this.data = Arrays.copyOf(src, src.length);
    }
}
