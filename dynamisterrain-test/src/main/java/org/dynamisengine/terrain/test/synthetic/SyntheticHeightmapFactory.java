package org.dynamisengine.terrain.test.synthetic;

import org.dynamisengine.terrain.core.heightmap.HeightmapData;

public final class SyntheticHeightmapFactory {
    private SyntheticHeightmapFactory() {
    }

    public static HeightmapData flat(final int size, final float height) {
        final HeightmapData hm = HeightmapData.empty(size, size);
        for (int i = 0; i < hm.pixels().length; i++) {
            hm.pixels()[i] = height;
        }
        return hm;
    }

    public static HeightmapData hill(final int size, final float peak) {
        final HeightmapData hm = HeightmapData.empty(size, size);
        final float cx = size * 0.5f;
        final float cz = size * 0.5f;
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                final float dx = x - cx;
                final float dz = z - cz;
                final float d = (float) Math.sqrt(dx * dx + dz * dz);
                hm.setPixel(x, z, Math.max(0f, peak - d));
            }
        }
        return hm;
    }

    public static HeightmapData valley(final int size, final float depth) {
        final HeightmapData hm = HeightmapData.empty(size, size);
        final float cx = size * 0.5f;
        final float cz = size * 0.5f;
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                final float dx = x - cx;
                final float dz = z - cz;
                final float d = (float) Math.sqrt(dx * dx + dz * dz);
                hm.setPixel(x, z, d * 0.5f - depth);
            }
        }
        return hm;
    }

    public static HeightmapData vShape(final int size, final float depth) {
        final HeightmapData hm = HeightmapData.empty(size, size);
        final float mid = size * 0.5f;
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                hm.setPixel(x, z, Math.abs(x - mid) - depth);
            }
        }
        return hm;
    }

    public static HeightmapData checkerboard(final int size, final float low, final float high) {
        final HeightmapData hm = HeightmapData.empty(size, size);
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                hm.setPixel(x, z, ((x + z) & 1) == 0 ? low : high);
            }
        }
        return hm;
    }

    public static HeightmapData sinWave(final int size, final float amplitude, final float frequency) {
        final HeightmapData hm = HeightmapData.empty(size, size);
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                hm.setPixel(x, z, (float) (Math.sin(x * frequency) * Math.cos(z * frequency) * amplitude));
            }
        }
        return hm;
    }
}
