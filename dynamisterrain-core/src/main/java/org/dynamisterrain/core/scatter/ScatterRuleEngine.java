package org.dynamisterrain.core.scatter;

import java.util.ArrayList;
import java.util.List;
import org.dynamisterrain.api.descriptor.FoliageLayer;
import org.dynamisterrain.core.flow.FlowMapData;
import org.dynamisterrain.core.heightmap.HeightmapData;
import org.dynamisterrain.core.heightmap.HeightmapOps;

public final class ScatterRuleEngine {
    private static final float TWO_PI = (float) (Math.PI * 2.0);

    private ScatterRuleEngine() {
    }

    public static ScatterResult evaluate(
        final FoliageLayer layer,
        final HeightmapData heightmap,
        final FlowMapData flowMap,
        final float[] densityMap,
        final float worldScale,
        final float heightScale,
        final long worldSeed,
        final ScatterConfig config
    ) {
        final List<ScatterPoint> points = new ArrayList<>();
        final float spacing = Math.max(config.minSpacing(), 0.0001f);
        final int gridX = Math.max(1, (int) Math.floor(((heightmap.width() - 1) * worldScale) / spacing));
        final int gridZ = Math.max(1, (int) Math.floor(((heightmap.height() - 1) * worldScale) / spacing));
        final float[] normals = HeightmapOps.generateNormals(heightmap, worldScale, heightScale);

        for (int gz = 0; gz <= gridZ; gz++) {
            for (int gx = 0; gx <= gridX; gx++) {
                boolean accepted = false;
                for (int attempt = 0; attempt < Math.max(1, config.maxCandidates()); attempt++) {
                    final long seed = mix(worldSeed, gx, gz, attempt, layer.meshId().hashCode());
                    final float jx = rand01(seed);
                    final float jz = rand01(seed ^ 0x9E3779B97F4A7C15L);
                    final float worldX = Math.min((gx + jx) * spacing, (heightmap.width() - 1) * worldScale);
                    final float worldZ = Math.min((gz + jz) * spacing, (heightmap.height() - 1) * worldScale);

                    final float worldY = HeightmapOps.heightAt(heightmap, worldX, worldZ, worldScale, heightScale);
                    if (worldY < layer.minAlt() || worldY > layer.maxAlt()) {
                        continue;
                    }

                    final float slope = slopeDegrees(normals, heightmap, worldX, worldZ, worldScale);
                    if (slope < layer.minSlope() || slope > layer.maxSlope()) {
                        continue;
                    }

                    float probability = clamp(layer.density(), 0f, 1f);
                    if (densityMap != null) {
                        final int tx = clampInt((int) (worldX / worldScale), 0, heightmap.width() - 1);
                        final int tz = clampInt((int) (worldZ / worldScale), 0, heightmap.height() - 1);
                        probability *= clamp(densityMap[tz * heightmap.width() + tx], 0f, 1f);
                    }
                    if (flowMap != null) {
                        final float flow = clamp(flowMap.accumulationAtWorld(worldX, worldZ, worldScale), 0f, 1f);
                        probability *= (0.2f + 0.8f * flow);
                    }

                    final float trial = rand01(seed ^ 0xD1B54A32D192ED03L);
                    if (trial > probability) {
                        continue;
                    }

                    final float rotation = rand01(seed ^ 0x94D049BB133111EBL) * TWO_PI;
                    final float scale = lerp(config.scaleMin(), config.scaleMax(), rand01(seed ^ 0x2545F4914F6CDD1DL));
                    points.add(new ScatterPoint(worldX, worldY, worldZ, rotation, scale));
                    accepted = true;
                    break;
                }
                if (!accepted) {
                    // Keep deterministic sparse pattern if no candidate in this cell passes filters.
                }
            }
        }

        return new ScatterResult(List.copyOf(points), points.size(), 0);
    }

    private static float slopeDegrees(
        final float[] normals,
        final HeightmapData heightmap,
        final float worldX,
        final float worldZ,
        final float worldScale
    ) {
        final int width = heightmap.width();
        final int height = heightmap.height();
        final int x = clampInt((int) (worldX / worldScale), 0, width - 1);
        final int z = clampInt((int) (worldZ / worldScale), 0, height - 1);
        final int idx = (z * width + x) * 3;
        final float ny = clamp(normals[idx + 1], -1f, 1f);
        final float normalSlope = (float) Math.toDegrees(Math.acos(ny));

        final float dhx = Math.abs(heightmap.pixelAt(x + 1, z) - heightmap.pixelAt(x - 1, z)) / (2f * worldScale);
        final float dhz = Math.abs(heightmap.pixelAt(x, z + 1) - heightmap.pixelAt(x, z - 1)) / (2f * worldScale);
        final float gradientSlope = (float) Math.toDegrees(Math.atan(Math.sqrt(dhx * dhx + dhz * dhz)));
        return Math.max(normalSlope, gradientSlope);
    }

    private static long mix(final long seed, final int x, final int z, final int attempt, final int layerHash) {
        long v = seed;
        v ^= (long) x * 0x9E3779B97F4A7C15L;
        v ^= (long) z * 0xC2B2AE3D27D4EB4FL;
        v ^= (long) attempt * 0x165667B19E3779F9L;
        v ^= (long) layerHash * 0x85EBCA77C2B2AE63L;
        v ^= (v >>> 33);
        v *= 0xff51afd7ed558ccdL;
        v ^= (v >>> 33);
        v *= 0xc4ceb9fe1a85ec53L;
        v ^= (v >>> 33);
        return v;
    }

    private static float rand01(final long v) {
        return (float) ((v >>> 11) * 0x1.0p-53);
    }

    private static float clamp(final float v, final float min, final float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static int clampInt(final int v, final int min, final int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float lerp(final float a, final float b, final float t) {
        return a + (b - a) * t;
    }
}
