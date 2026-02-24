package org.dynamisterrain.core.flow;

import java.util.Arrays;
import org.dynamisterrain.core.heightmap.HeightmapData;

public final class FlowMapGenerator {
    private static final int[] DX = new int[] {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] DZ = new int[] {-1, -1, 0, 1, 1, 1, 0, -1};

    private FlowMapGenerator() {
    }

    public static FlowMapData generate(final HeightmapData heightmap, final FlowConfig config) {
        final int w = heightmap.width();
        final int h = heightmap.height();
        final int n = w * h;

        final Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Float.compare(heightmap.pixels()[b], heightmap.pixels()[a]));

        final float[] upstream = new float[n];
        for (int idx : order) {
            final int x = idx % w;
            final int z = idx / w;
            int bestNeighbor = -1;
            float bestSlope = 0.0f;
            final float base = heightmap.pixels()[idx];

            for (int d = 0; d < 8; d++) {
                final int nx = x + DX[d];
                final int nz = z + DZ[d];
                if (nx < 0 || nz < 0 || nx >= w || nz >= h) {
                    continue;
                }
                final int nIdx = nz * w + nx;
                final float dh = base - heightmap.pixels()[nIdx];
                if (dh <= 0f) {
                    continue;
                }
                final float distance = (d % 2 == 0) ? 1.0f : 1.4142135f;
                final float slope = (float) Math.pow(dh / distance, config.slopeExponent());
                if (slope > bestSlope) {
                    bestSlope = slope;
                    bestNeighbor = nIdx;
                }
            }

            if (bestNeighbor >= 0) {
                upstream[bestNeighbor] += upstream[idx] + 1.0f;
            }
        }

        float[] out = upstream;
        for (int i = 0; i < Math.max(config.iterations(), 0); i++) {
            out = blur(out, w, h);
        }

        if (config.normalizeOutput()) {
            float max = 0f;
            for (float v : out) {
                max = Math.max(max, v);
            }
            if (max > 0f) {
                for (int i = 0; i < out.length; i++) {
                    out[i] = out[i] / max;
                }
            }
        }

        return FlowMapData.of(out, w, h);
    }

    private static float[] blur(final float[] in, final int w, final int h) {
        final float[] out = new float[in.length];
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                float sum = 0f;
                int count = 0;
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        final int nx = x + dx;
                        final int nz = z + dz;
                        if (nx < 0 || nz < 0 || nx >= w || nz >= h) {
                            continue;
                        }
                        sum += in[nz * w + nx];
                        count++;
                    }
                }
                out[z * w + x] = sum / count;
            }
        }
        return out;
    }
}
