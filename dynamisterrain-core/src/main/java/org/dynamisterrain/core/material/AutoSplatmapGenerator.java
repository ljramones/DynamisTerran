package org.dynamisterrain.core.material;

import org.dynamisterrain.api.descriptor.AutoSplatConfig;
import org.dynamisterrain.core.flow.FlowMapData;
import org.dynamisterrain.core.heightmap.HeightmapData;

public final class AutoSplatmapGenerator {
    private AutoSplatmapGenerator() {
    }

    public static float[][] generate(
        final HeightmapData heightmap,
        final float[] normals,
        final FlowMapData flowMap,
        final AutoSplatConfig config,
        final int layerCount,
        final float worldScale,
        final float heightScale
    ) {
        if (layerCount <= 0) {
            throw new IllegalArgumentException("layerCount must be > 0");
        }
        final int n = heightmap.width() * heightmap.height();
        final float[][] out = new float[layerCount][n];
        if (layerCount == 1) {
            for (int i = 0; i < n; i++) {
                out[0][i] = 1.0f;
            }
            return out;
        }

        final int rockIdx = Math.min(1, layerCount - 1);
        final int snowIdx = Math.min(2, layerCount - 1);
        final int grassIdx = Math.min(3, layerCount - 1);

        final float rockSlopeThreshold = 45f;
        final float snowAltitude = heightScale * 0.65f;
        final float grassFlowMin = 0.5f;

        for (int z = 0; z < heightmap.height(); z++) {
            for (int x = 0; x < heightmap.width(); x++) {
                final int idx = z * heightmap.width() + x;
                float dirt = 1.0f;
                float rock = 0f;
                float snow = 0f;
                float grass = 0f;

                final float ny = clamp(normals[idx * 3 + 1], -1f, 1f);
                final float slope = (float) Math.toDegrees(Math.acos(ny));
                final float altitude = heightmap.pixelAt(x, z);

                if (slope > rockSlopeThreshold) {
                    rock += clamp((slope - rockSlopeThreshold) / 45f, 0f, 1f) * Math.max(config.slopeWeight(), 0.1f);
                }
                if (altitude > snowAltitude) {
                    snow += clamp((altitude - snowAltitude) / Math.max(1f, heightScale - snowAltitude), 0f, 1f)
                        * Math.max(config.heightWeight(), 0.1f);
                }
                if (flowMap != null) {
                    final float flow = flowMap.accumulationAt(x, z);
                    if (flow > grassFlowMin) {
                        grass += clamp((flow - grassFlowMin) / (1f - grassFlowMin), 0f, 1f) * Math.max(config.flowWeight(), 0.1f);
                    }
                }

                dirt = Math.max(0f, dirt - (rock + snow + grass));

                out[0][idx] = dirt;
                out[rockIdx][idx] += rock;
                out[snowIdx][idx] += snow;
                out[grassIdx][idx] += grass;

                float sum = 0f;
                for (int l = 0; l < layerCount; l++) {
                    sum += out[l][idx];
                }
                if (sum <= 0f) {
                    out[0][idx] = 1f;
                } else {
                    for (int l = 0; l < layerCount; l++) {
                        out[l][idx] /= sum;
                    }
                }
            }
        }
        return out;
    }

    private static float clamp(final float v, final float min, final float max) {
        return Math.max(min, Math.min(max, v));
    }
}
