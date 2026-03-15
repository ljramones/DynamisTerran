package org.dynamisengine.terrain.vulkan.horizon;

import org.dynamisengine.terrain.core.heightmap.HeightmapData;
import org.dynamisengine.terrain.vulkan.GpuMemoryOps;
import org.dynamisengine.terrain.vulkan.TerrainGpuContext;

public final class HorizonMapBaker {
    private static final float HALF_PI = 1.5707964f;
    private static final float[] DIR_X = new float[] {0f, 1f, 1f, 1f, 0f, -1f, -1f, -1f};
    private static final float[] DIR_Z = new float[] {-1f, -1f, 0f, 1f, 1f, 1f, 0f, -1f};
    private static final float[] DIAG_SCALE = new float[] {1f, 1.41421356f, 1f, 1.41421356f, 1f, 1.41421356f, 1f, 1.41421356f};

    private final long device;
    private final GpuMemoryOps memoryOps;

    private HorizonMapBaker(final long device, final GpuMemoryOps memoryOps) {
        this.device = device;
        this.memoryOps = memoryOps;
    }

    public static HorizonMapBaker create(final long device, final GpuMemoryOps memoryOps) {
        return new HorizonMapBaker(device, memoryOps);
    }

    public void bake(final long commandBuffer, final TerrainGpuContext ctx, final HorizonBakeConfig config) {
        final HeightmapData hm = ctx.heightmapData();
        if (hm == null) {
            throw new IllegalStateException("Heightmap must be uploaded before horizon bake");
        }
        final int width = hm.width();
        final int height = hm.height();
        final byte[] out = new byte[width * height * 4];

        final HorizonBakeUBO ubo = new HorizonBakeUBO(
            config.worldScale(),
            config.heightScale(),
            1.0f / Math.max(1, config.searchRadius()),
            Math.max(1, config.searchRadius()),
            width,
            height,
            0f,
            0f
        );

        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                final float h0 = hm.pixelAt(x, z);
                final float[] angles = new float[8];
                for (int d = 0; d < 8; d++) {
                    float maxAngle = 0f;
                    for (int step = 1; step <= ubo.searchRadius(); step++) {
                        final int sx = clamp(Math.round(x + DIR_X[d] * step), 0, width - 1);
                        final int sz = clamp(Math.round(z + DIR_Z[d] * step), 0, height - 1);
                        final float sh = hm.pixelAt(sx, sz);
                        final float dist = step * ubo.worldScale() * DIAG_SCALE[d];
                        final float rise = Math.max(sh - h0, 0f);
                        final float angle = (float) Math.atan(rise / Math.max(dist, 0.0001f));
                        if (angle > maxAngle) {
                            maxAngle = angle;
                        }
                    }
                    angles[d] = maxAngle;
                }

                final int idx = (z * width + x) * 4;
                out[idx] = pack(angles[0], angles[1]);
                out[idx + 1] = pack(angles[2], angles[3]);
                out[idx + 2] = pack(angles[4], angles[5]);
                out[idx + 3] = pack(angles[6], angles[7]);
            }
        }

        ctx.horizonMapTexture().upload(out);
    }

    public void destroy() {
        // No-op for scaffold baker.
    }

    private static byte pack(final float a0, final float a1) {
        final int hi = clamp(Math.round((a0 / HALF_PI) * 15f), 0, 15);
        final int lo = clamp(Math.round((a1 / HALF_PI) * 15f), 0, 15);
        return (byte) ((hi << 4) | lo);
    }

    public static float unpackDirectionAngle(final byte packedChannel, final boolean highNibble) {
        final int v = packedChannel & 0xFF;
        final int nibble = highNibble ? ((v >> 4) & 0xF) : (v & 0xF);
        return (nibble / 15.0f) * HALF_PI;
    }

    private static int clamp(final int v, final int min, final int max) {
        return Math.max(min, Math.min(max, v));
    }
}
