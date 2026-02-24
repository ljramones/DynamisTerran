package org.dynamisterrain.core.heightmap;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import org.dynamisterrain.api.descriptor.BlendMode;
import org.dynamisterrain.api.descriptor.HeightmapFormat;
import org.dynamisterrain.api.event.DeformShape;
import org.dynamisterrain.api.event.HeightDeformEvent;

public final class HeightmapOps {
    private HeightmapOps() {
    }

    public static HeightmapData load(
        final Path path,
        final HeightmapFormat format,
        final int width,
        final int height,
        final float heightScale
    ) throws IOException {
        final byte[] bytes = Files.readAllBytes(path);
        final int texelCount = width * height;
        if (format == HeightmapFormat.R16) {
            final int expected = texelCount * 2;
            if (bytes.length != expected) {
                throw new IOException("Unexpected R16 byte length: " + bytes.length + " != " + expected);
            }
            final short[] raw = new short[texelCount];
            final ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < texelCount; i++) {
                raw[i] = buffer.getShort();
            }
            return HeightmapData.ofR16(raw, width, height, heightScale);
        }

        final int expected = texelCount * 4;
        if (bytes.length != expected) {
            throw new IOException("Unexpected R32F byte length: " + bytes.length + " != " + expected);
        }
        final float[] raw = new float[texelCount];
        final ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < texelCount; i++) {
            raw[i] = buffer.getFloat();
        }
        return HeightmapData.ofR32F(raw, width, height);
    }

    public static float heightAt(
        final HeightmapData hm,
        final float worldX,
        final float worldZ,
        final float worldScale,
        final float heightScale
    ) {
        final float texelX = worldX / worldScale;
        final float texelZ = worldZ / worldScale;

        final float clampedX = clamp(texelX, 0.0f, hm.width() - 1.0f);
        final float clampedZ = clamp(texelZ, 0.0f, hm.height() - 1.0f);

        final int x0 = (int) Math.floor(clampedX);
        final int z0 = (int) Math.floor(clampedZ);
        final int x1 = Math.min(x0 + 1, hm.width() - 1);
        final int z1 = Math.min(z0 + 1, hm.height() - 1);

        final float tx = clampedX - x0;
        final float tz = clampedZ - z0;

        final float h00 = hm.pixelAt(x0, z0);
        final float h10 = hm.pixelAt(x1, z0);
        final float h01 = hm.pixelAt(x0, z1);
        final float h11 = hm.pixelAt(x1, z1);

        final float hx0 = lerp(h00, h10, tx);
        final float hx1 = lerp(h01, h11, tx);
        return lerp(hx0, hx1, tz);
    }

    public static float[] generateNormals(final HeightmapData hm, final float worldScale, final float heightScale) {
        final int width = hm.width();
        final int height = hm.height();
        final float[] out = new float[width * height * 3];

        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                final float h00 = hm.pixelAt(x - 1, z - 1);
                final float h10 = hm.pixelAt(x, z - 1);
                final float h20 = hm.pixelAt(x + 1, z - 1);
                final float h01 = hm.pixelAt(x - 1, z);
                final float h21 = hm.pixelAt(x + 1, z);
                final float h02 = hm.pixelAt(x - 1, z + 1);
                final float h12 = hm.pixelAt(x, z + 1);
                final float h22 = hm.pixelAt(x + 1, z + 1);

                final float dX = ((h20 + 2.0f * h21 + h22) - (h00 + 2.0f * h01 + h02)) / (8.0f * worldScale);
                final float dZ = ((h02 + 2.0f * h12 + h22) - (h00 + 2.0f * h10 + h20)) / (8.0f * worldScale);

                float nx = -dX;
                float ny = 1.0f;
                float nz = -dZ;
                final float invLen = invSqrt(nx * nx + ny * ny + nz * nz);
                nx *= invLen;
                ny *= invLen;
                nz *= invLen;

                final int idx = (z * width + x) * 3;
                out[idx] = nx;
                out[idx + 1] = ny;
                out[idx + 2] = nz;
            }
        }

        return out;
    }

    public static void deform(final HeightmapData hm, final HeightDeformEvent event, final float worldScale) {
        final float radius = Math.max(event.radius(), 0.0f);
        if (radius <= 0.0f) {
            return;
        }

        final int minX = hm.clampX(worldToTexelX(event.centerX() - radius, worldScale));
        final int maxX = hm.clampX(worldToTexelX(event.centerX() + radius, worldScale));
        final int minZ = hm.clampZ(worldToTexelX(event.centerZ() - radius, worldScale));
        final int maxZ = hm.clampZ(worldToTexelX(event.centerZ() + radius, worldScale));

        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                final float wx = texelToWorldX(x, worldScale);
                final float wz = texelToWorldX(z, worldScale);
                final float dx = wx - event.centerX();
                final float dz = wz - event.centerZ();
                final float dist = (float) Math.sqrt(dx * dx + dz * dz);
                if (dist > radius) {
                    continue;
                }

                final float normalized = dist / radius;
                final float delta;
                if (event.shape() == DeformShape.CONE) {
                    delta = -event.depth() * (1.0f - normalized);
                } else if (event.shape() == DeformShape.EXPLOSION) {
                    final float half = 0.5f;
                    if (normalized < half) {
                        final float core = 1.0f - (normalized / half);
                        delta = -event.depth() * core;
                    } else {
                        final float rim = 1.0f - ((normalized - half) / half);
                        delta = event.depth() * 0.35f * Math.max(rim, 0.0f);
                    }
                } else {
                    final float gaussian = (float) Math.exp(-4.0f * normalized * normalized);
                    delta = -event.depth() * gaussian;
                }

                hm.setPixel(x, z, hm.pixelAt(x, z) + delta);
            }
        }
    }

    public static void applyStamp(
        final HeightmapData hm,
        final HeightmapData stamp,
        final BlendMode mode,
        final float strength,
        final int centerTexelX,
        final int centerTexelZ
    ) {
        final int halfW = stamp.width() / 2;
        final int halfH = stamp.height() / 2;
        final float blend = clamp(strength, 0.0f, 1.0f);

        for (int sz = 0; sz < stamp.height(); sz++) {
            for (int sx = 0; sx < stamp.width(); sx++) {
                final int tx = centerTexelX + (sx - halfW);
                final int tz = centerTexelZ + (sz - halfH);
                if (tx < 0 || tz < 0 || tx >= hm.width() || tz >= hm.height()) {
                    continue;
                }

                final float dst = hm.pixelAt(tx, tz);
                final float src = stamp.pixelAt(sx, sz);
                final float out;
                if (mode == BlendMode.SET) {
                    out = src * strength;
                } else if (mode == BlendMode.BLEND) {
                    out = lerp(dst, src, blend);
                } else {
                    out = dst + src * strength;
                }
                hm.setPixel(tx, tz, out);
            }
        }
    }

    public static float[] minMaxInRegion(final HeightmapData hm, int x0, int z0, int x1, int z1) {
        int minX = hm.clampX(Math.min(x0, x1));
        int maxX = hm.clampX(Math.max(x0, x1));
        int minZ = hm.clampZ(Math.min(z0, z1));
        int maxZ = hm.clampZ(Math.max(z0, z1));

        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                final float v = hm.pixelAt(x, z);
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
        }
        return new float[] {min, max};
    }

    public static float texelToWorldX(final int texelX, final float worldScale) {
        return texelX * worldScale;
    }

    public static int worldToTexelX(final float worldX, final float worldScale) {
        return (int) (worldX / worldScale);
    }

    private static float lerp(final float a, final float b, final float t) {
        return a + (b - a) * t;
    }

    private static float clamp(final float v, final float min, final float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float invSqrt(final float v) {
        if (v <= 0.0f) {
            return 0.0f;
        }
        return 1.0f / (float) Math.sqrt(v);
    }
}
