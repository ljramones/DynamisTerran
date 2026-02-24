package org.dynamisterrain.core.lod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.dynamisterrain.api.descriptor.TerrainLodDesc;
import org.dynamisterrain.api.state.Vector3f;
import org.dynamisterrain.core.heightmap.HeightmapData;
import org.dynamisterrain.core.heightmap.HeightmapOps;

/**
 * CDLOD quadtree selector.
 *
 * LOD switch distance model:
 * lodDistance[i] = patchSize * 2^i / screenSpaceErrorThreshold * 16.0
 *
 * morphStart = lodSwitchDistance[i] * TerrainLodDesc.morphStart   (default 0.6)
 * morphEnd   = lodSwitchDistance[i] * TerrainLodDesc.morphEnd     (default 0.9)
 * dist       = distance(cameraPos, patchCenter)
 * morphFactor = clamp((dist - morphStart) / (morphEnd - morphStart), 0.0, 1.0)
 */
public final class CdlodQuadTree {
    private static final float DISTANCE_SCALE = 16.0f;

    private final int width;
    private final int height;
    private final TerrainLodDesc config;
    private final HeightmapData heightmap;
    private final float worldScale;
    private final float heightScale;
    private final List<List<CdlodPatch>> patchesByLod;

    private CdlodQuadTree(
        final int width,
        final int height,
        final TerrainLodDesc config,
        final HeightmapData heightmap,
        final float worldScale,
        final float heightScale,
        final List<List<CdlodPatch>> patchesByLod
    ) {
        this.width = width;
        this.height = height;
        this.config = config;
        this.heightmap = heightmap;
        this.worldScale = worldScale;
        this.heightScale = heightScale;
        this.patchesByLod = patchesByLod;
    }

    public static CdlodQuadTree build(
        final int heightmapWidth,
        final int heightmapHeight,
        final TerrainLodDesc config,
        final HeightmapData heightmap,
        final float worldScale,
        final float heightScale
    ) {
        final List<List<CdlodPatch>> byLod = new ArrayList<>();
        final int lodLevels = Math.max(config.lodLevels(), 1);

        for (int lod = 0; lod < lodLevels; lod++) {
            final int step = Math.max(config.patchSize() * (1 << lod), 1);
            final List<CdlodPatch> patches = new ArrayList<>();
            for (int z0 = 0; z0 < heightmapHeight; z0 += step) {
                for (int x0 = 0; x0 < heightmapWidth; x0 += step) {
                    final int x1 = Math.min(x0 + step, heightmapWidth - 1);
                    final int z1 = Math.min(z0 + step, heightmapHeight - 1);
                    final float[] minMax = HeightmapOps.minMaxInRegion(heightmap, x0, z0, x1, z1);

                    final float worldX = x0 * worldScale;
                    final float worldZ = z0 * worldScale;
                    final float worldW = Math.max(1, x1 - x0) * worldScale;
                    final float worldD = Math.max(1, z1 - z0) * worldScale;

                    final float centerX = worldX + worldW * 0.5f;
                    final float centerZ = worldZ + worldD * 0.5f;

                    patches.add(new CdlodPatch(
                        lod,
                        worldX,
                        worldZ,
                        worldW,
                        worldD,
                        minMax[0],
                        minMax[1],
                        centerX,
                        centerZ));
                }
            }
            byLod.add(Collections.unmodifiableList(patches));
        }

        return new CdlodQuadTree(heightmapWidth, heightmapHeight, config, heightmap, worldScale, heightScale, byLod);
    }

    public CdlodFrameResult select(
        final Vector3f cameraPos,
        final Frustum frustum,
        final float screenSpaceErrorThreshold
    ) {
        final float[] lodDistances = computeLodDistances(screenSpaceErrorThreshold);
        final List<CdlodPatch> selected = new ArrayList<>();
        final List<Float> morph = new ArrayList<>();

        for (int lod = 0; lod < this.patchesByLod.size(); lod++) {
            final float currentLimit = lodDistances[lod];
            final float previousLimit = lod == 0 ? 0.0f : lodDistances[lod - 1];

            for (CdlodPatch patch : this.patchesByLod.get(lod)) {
                final Vector3f min = new Vector3f(patch.worldX(), patch.minHeight(), patch.worldZ());
                final Vector3f max = new Vector3f(
                    patch.worldX() + patch.worldWidth(),
                    patch.maxHeight(),
                    patch.worldZ() + patch.worldDepth());
                if (!frustum.intersectsAabb(min, max)) {
                    continue;
                }

                final float dist = distance(cameraPos.x(), cameraPos.z(), patch.centerX(), patch.centerZ());
                final boolean inBand = lod == 0 ? dist <= currentLimit : (dist > previousLimit && dist <= currentLimit);
                if (!inBand) {
                    continue;
                }

                selected.add(patch);
                morph.add(computeMorphFactor(lod, dist, lodDistances));
            }
        }

        final float[] morphOut = new float[morph.size()];
        for (int i = 0; i < morph.size(); i++) {
            morphOut[i] = morph.get(i);
        }
        return new CdlodFrameResult(Collections.unmodifiableList(selected), selected.size(), morphOut);
    }

    private float[] computeLodDistances(final float screenSpaceErrorThreshold) {
        final float threshold = Math.max(screenSpaceErrorThreshold, 0.0001f);
        final float[] out = new float[this.patchesByLod.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = (this.config.patchSize() * (1 << i) / threshold) * DISTANCE_SCALE;
        }
        return out;
    }

    private float computeMorphFactor(final int lod, final float dist, final float[] lodDistances) {
        final float lodSwitchDistance = lodDistances[lod];
        final float morphStart = lodSwitchDistance * this.config.morphStart();
        final float morphEnd = lodSwitchDistance * this.config.morphEnd();
        final float denom = Math.max(morphEnd - morphStart, 0.0001f);
        final float raw = (dist - morphStart) / denom;
        return clamp(raw, 0.0f, 1.0f);
    }

    private static float distance(final float x0, final float z0, final float x1, final float z1) {
        final float dx = x1 - x0;
        final float dz = z1 - z0;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    private static float clamp(final float v, final float min, final float max) {
        return Math.max(min, Math.min(max, v));
    }
}
