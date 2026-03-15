package org.dynamisengine.terrain.core.procedural;

import org.dynamisengine.terrain.api.descriptor.BlendMode;
import org.dynamisengine.terrain.api.descriptor.HeightStamp;
import org.dynamisengine.terrain.api.descriptor.ProceduralDesc;
import org.dynamisengine.terrain.core.heightmap.HeightmapData;
import org.dynamisengine.terrain.core.heightmap.HeightmapOps;

public final class ProceduralHeightmapGenerator {
    private static final float EROSION_RATE = 0.01f;
    private static final float DEPOSITION_RATE = 0.01f;
    private static final float EVAPORATION_RATE = 0.05f;
    private static final float RAIN_AMOUNT = 0.01f;
    private static final float SEDIMENT_CAPACITY = 4.0f;

    private ProceduralHeightmapGenerator() {
    }

    public static HeightmapData generate(final ProceduralDesc desc, final int width, final int height, final float heightScale) {
        final float[] h = new float[width * height];
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                float amp = 1f;
                float freq = Math.max(desc.frequency(), 0.0001f);
                float totalAmp = 0f;
                float value = 0f;
                for (int o = 0; o < Math.max(1, desc.octaves()); o++) {
                    value += valueNoise2d(x * freq, z * freq, desc.seed() + o * 1315423911L) * amp;
                    totalAmp += amp;
                    amp *= 0.5f;
                    freq *= 2f;
                }
                value = value / Math.max(totalAmp, 0.0001f);
                h[z * width + x] = clamp01(0.5f + 0.5f * value) * heightScale;
            }
        }

        applyHydraulic(h, width, height, Math.max(desc.erosionPasses(), 0));
        applyThermal(h, width, height, Math.max(desc.thermalErosionPasses(), 0));
        if (desc.erosionPasses() > 0) {
            for (int i = 0; i < Math.max(1, desc.erosionPasses() / 5); i++) {
                smoothHeights(h, width, height, 0.4f);
            }
        }
        if (desc.thermalErosionPasses() > 0) {
            for (int i = 0; i < Math.max(1, desc.thermalErosionPasses() / 4); i++) {
                smoothHeights(h, width, height, 0.35f);
            }
            smoothHeights(h, width, height, 0.75f);
        }

        HeightmapData data = HeightmapData.ofR32F(h, width, height);
        if (desc.stamps() != null) {
            for (HeightStamp stamp : desc.stamps()) {
                final HeightmapData radialStamp = radialStamp(33, 33, heightScale * 0.25f);
                final BlendMode mode = stamp.blendMode() == null ? BlendMode.ADD : stamp.blendMode();
                HeightmapOps.applyStamp(data, radialStamp, mode, stamp.strength(), width / 2, height / 2);
            }
        }

        final float[] pixels = data.pixels();
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = clamp(pixels[i], 0f, heightScale);
        }
        return data;
    }

    private static void applyHydraulic(final float[] h, final int w, final int he, final int passes) {
        final float[] water = new float[h.length];
        final float[] sediment = new float[h.length];
        for (int pass = 0; pass < passes; pass++) {
            for (int i = 0; i < h.length; i++) {
                water[i] += RAIN_AMOUNT;
            }
            for (int z = 1; z < he - 1; z++) {
                for (int x = 1; x < w - 1; x++) {
                    final int i = z * w + x;
                    int best = i;
                    float bestH = h[i];
                    final int[] ns = new int[] {i - 1, i + 1, i - w, i + w};
                    for (int n : ns) {
                        if (h[n] < bestH) {
                            bestH = h[n];
                            best = n;
                        }
                    }
                    if (best != i) {
                        final float diff = h[i] - h[best];
                        final float erode = Math.min(diff * EROSION_RATE, water[i] * SEDIMENT_CAPACITY);
                        h[i] -= erode;
                        sediment[i] += erode;

                        final float deposit = sediment[i] * DEPOSITION_RATE;
                        sediment[i] -= deposit;
                        h[best] += deposit;

                        final float movedWater = water[i] * 0.25f;
                        water[i] -= movedWater;
                        water[best] += movedWater;
                    }
                }
            }
            for (int i = 0; i < h.length; i++) {
                water[i] *= (1f - EVAPORATION_RATE);
            }
            smoothHeights(h, w, he, 0.08f);
        }
    }

    private static void applyThermal(final float[] h, final int w, final int he, final int passes) {
        final float talus = 1.0f;
        for (int pass = 0; pass < passes; pass++) {
            final float[] delta = new float[h.length];
            for (int z = 1; z < he - 1; z++) {
                for (int x = 1; x < w - 1; x++) {
                    final int i = z * w + x;
                    final int[] ns = new int[] {i - 1, i + 1, i - w, i + w};
                    for (int n : ns) {
                        final float diff = h[i] - h[n];
                        if (diff > talus) {
                            final float move = (diff - talus) * 0.65f;
                            delta[i] -= move;
                            delta[n] += move;
                        }
                    }
                }
            }
            for (int i = 0; i < h.length; i++) {
                h[i] += delta[i];
            }
            smoothHeights(h, w, he, 0.05f);
        }
    }

    private static void smoothHeights(final float[] h, final int w, final int he, final float alpha) {
        final float[] copy = h.clone();
        for (int z = 0; z < he; z++) {
            for (int x = 0; x < w; x++) {
                final int i = z * w + x;
                final int xl = Math.max(0, x - 1);
                final int xr = Math.min(w - 1, x + 1);
                final int zu = Math.max(0, z - 1);
                final int zd = Math.min(he - 1, z + 1);
                final float avg = (copy[z * w + xl] + copy[z * w + xr] + copy[zu * w + x] + copy[zd * w + x]) * 0.25f;
                h[i] = lerp(copy[i], avg, alpha);
            }
        }
    }

    private static HeightmapData radialStamp(final int w, final int h, final float amplitude) {
        final float[] p = new float[w * h];
        final float cx = (w - 1) * 0.5f;
        final float cz = (h - 1) * 0.5f;
        final float r = Math.min(cx, cz);
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                final float dx = x - cx;
                final float dz = z - cz;
                final float d = (float) Math.sqrt(dx * dx + dz * dz) / r;
                p[z * w + x] = amplitude * Math.max(0f, 1f - d);
            }
        }
        return HeightmapData.ofR32F(p, w, h);
    }

    private static float valueNoise2d(final float x, final float z, final long seed) {
        final int x0 = (int) Math.floor(x);
        final int z0 = (int) Math.floor(z);
        final int x1 = x0 + 1;
        final int z1 = z0 + 1;
        final float tx = x - x0;
        final float tz = z - z0;

        final float v00 = hash01(x0, z0, seed);
        final float v10 = hash01(x1, z0, seed);
        final float v01 = hash01(x0, z1, seed);
        final float v11 = hash01(x1, z1, seed);

        final float a = lerp(v00, v10, smooth(tx));
        final float b = lerp(v01, v11, smooth(tx));
        return lerp(a, b, smooth(tz)) * 2f - 1f;
    }

    private static float hash01(final int x, final int z, final long seed) {
        long v = seed;
        v ^= x * 0x9E3779B97F4A7C15L;
        v ^= z * 0xC2B2AE3D27D4EB4FL;
        v ^= (v >>> 33);
        v *= 0xff51afd7ed558ccdL;
        v ^= (v >>> 33);
        return (float) ((v >>> 11) * 0x1.0p-53);
    }

    private static float smooth(final float t) {
        return t * t * (3f - 2f * t);
    }

    private static float lerp(final float a, final float b, final float t) {
        return a + (b - a) * t;
    }

    private static float clamp01(final float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static float clamp(final float v, final float min, final float max) {
        return Math.max(min, Math.min(max, v));
    }
}
