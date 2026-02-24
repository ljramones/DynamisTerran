package org.dynamisterrain.core.heightmap;

import java.util.Arrays;
import org.dynamisterrain.api.descriptor.HeightmapFormat;

public final class HeightmapData {
    private final float[] pixels;
    private final int width;
    private final int height;
    private final HeightmapFormat format;

    private HeightmapData(final float[] pixels, final int width, final int height, final HeightmapFormat format) {
        if (pixels == null) {
            throw new IllegalArgumentException("pixels cannot be null");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be positive");
        }
        if (pixels.length != width * height) {
            throw new IllegalArgumentException("pixels length does not match dimensions");
        }
        this.pixels = pixels;
        this.width = width;
        this.height = height;
        this.format = format;
    }

    public static HeightmapData ofR16(final short[] raw, final int w, final int h, final float heightScale) {
        if (raw == null) {
            throw new IllegalArgumentException("raw cannot be null");
        }
        if (raw.length != w * h) {
            throw new IllegalArgumentException("raw length does not match dimensions");
        }
        final float[] pixels = new float[raw.length];
        final float scale = Math.max(heightScale, 0.0f);
        for (int i = 0; i < raw.length; i++) {
            final int unsigned = raw[i] & 0xFFFF;
            pixels[i] = (unsigned / 65535.0f) * scale;
        }
        return new HeightmapData(pixels, w, h, HeightmapFormat.R16);
    }

    public static HeightmapData ofR32F(final float[] raw, final int w, final int h) {
        if (raw == null) {
            throw new IllegalArgumentException("raw cannot be null");
        }
        if (raw.length != w * h) {
            throw new IllegalArgumentException("raw length does not match dimensions");
        }
        return new HeightmapData(Arrays.copyOf(raw, raw.length), w, h, HeightmapFormat.R32F);
    }

    public static HeightmapData empty(final int w, final int h) {
        return new HeightmapData(new float[w * h], w, h, HeightmapFormat.R32F);
    }

    public float pixelAt(final int x, final int z) {
        return this.pixels[index(clampX(x), clampZ(z))];
    }

    public void setPixel(final int x, final int z, final float v) {
        this.pixels[index(clampX(x), clampZ(z))] = v;
    }

    public float[] pixels() {
        return this.pixels;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public HeightmapFormat format() {
        return this.format;
    }

    int clampX(final int x) {
        return Math.max(0, Math.min(this.width - 1, x));
    }

    int clampZ(final int z) {
        return Math.max(0, Math.min(this.height - 1, z));
    }

    int index(final int x, final int z) {
        return z * this.width + x;
    }
}
