package org.dynamisterrain.core.lod;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dynamisterrain.api.descriptor.TerrainLodDesc;
import org.dynamisterrain.api.descriptor.TessellationMode;
import org.dynamisterrain.api.state.Vector3f;
import org.dynamisterrain.core.heightmap.HeightmapData;
import org.junit.jupiter.api.Test;

class CdlodQuadTreeTest {
    @Test
    void selectProducesNonZeroPatchesWhenCameraAboveTerrain() {
        final CdlodQuadTree tree = makeTree(1024, 1024);
        final CdlodFrameResult result = tree.select(new Vector3f(512f, 200f, 512f), defaultFrustum(), 2.0f);
        assertTrue(result.patchCount() > 0);
    }

    @Test
    void morphFactorsAllInUnitRange() {
        final CdlodQuadTree tree = makeTree(1024, 1024);
        final CdlodFrameResult result = tree.select(new Vector3f(512f, 200f, 512f), defaultFrustum(), 2.0f);
        for (float v : result.morphFactors()) {
            assertTrue(v >= 0.0f && v <= 1.0f);
        }
    }

    @Test
    void nearPatchesAtFinestLod() {
        final CdlodQuadTree tree = makeTree(1024, 1024);
        final CdlodFrameResult result = tree.select(new Vector3f(512f, 10f, 512f), defaultFrustum(), 2.0f);
        assertTrue(result.visiblePatches().stream().anyMatch(p -> p.lodLevel() == 0));
    }

    @Test
    void frustumCullReducesPatchCount() {
        final CdlodQuadTree tree = makeTree(1024, 1024);
        final CdlodFrameResult all = tree.select(new Vector3f(512f, 200f, 512f), defaultFrustum(), 2.0f);
        final Frustum away = frustumLookingAway();
        final CdlodFrameResult culled = tree.select(new Vector3f(512f, 200f, 512f), away, 2.0f);
        assertTrue(culled.patchCount() < all.patchCount());
    }

    @Test
    void lowerScreenSpaceErrorProducesMorePatches() {
        final CdlodQuadTree tree = makeTree(1024, 1024);
        final CdlodFrameResult coarse = tree.select(new Vector3f(512f, 200f, 512f), defaultFrustum(), 4.0f);
        final CdlodFrameResult fine = tree.select(new Vector3f(512f, 200f, 512f), defaultFrustum(), 1.0f);
        assertTrue(fine.patchCount() >= coarse.patchCount());
    }

    @Test
    void selectIsDeterministic() {
        final CdlodQuadTree tree = makeTree(1024, 1024);
        final Vector3f camera = new Vector3f(512f, 200f, 512f);
        final Frustum frustum = defaultFrustum();
        final CdlodFrameResult a = tree.select(camera, frustum, 2.0f);
        final CdlodFrameResult b = tree.select(camera, frustum, 2.0f);
        assertEquals(a.patchCount(), b.patchCount());
        assertEquals(a.morphFactors().length, b.morphFactors().length);
        for (int i = 0; i < a.morphFactors().length; i++) {
            assertEquals(a.morphFactors()[i], b.morphFactors()[i], 0.0f);
        }
    }

    @Test
    void buildHandlesNonPowerOfTwoDimensions() {
        assertDoesNotThrow(() -> {
            final HeightmapData hm = HeightmapData.empty(200, 150);
            CdlodQuadTree.build(200, 150, lodConfig(), hm, 1.0f, 1.0f);
        });
    }

    @Test
    void patchWorldPositionsAreWithinTerrainBounds() {
        final int width = 200;
        final int height = 150;
        final float worldScale = 2.0f;
        final CdlodQuadTree tree = CdlodQuadTree.build(width, height, lodConfig(), HeightmapData.empty(width, height), worldScale, 1.0f);
        final CdlodFrameResult result = tree.select(new Vector3f(150f, 100f, 120f), defaultFrustum(), 2.0f);
        for (CdlodPatch patch : result.visiblePatches()) {
            assertTrue(patch.worldX() >= 0f && patch.worldX() <= width * worldScale);
            assertTrue(patch.worldZ() >= 0f && patch.worldZ() <= height * worldScale);
        }
    }

    @Test
    void morphFactorZeroAtCloseRange() {
        final CdlodQuadTree tree = makeTree(1024, 1024);
        final CdlodFrameResult base = tree.select(new Vector3f(512f, 10f, 512f), defaultFrustum(), 2.0f);
        int idx = -1;
        float best = Float.MAX_VALUE;
        for (int i = 0; i < base.visiblePatches().size(); i++) {
            CdlodPatch p = base.visiblePatches().get(i);
            final float dx = p.centerX() - 512f;
            final float dz = p.centerZ() - 512f;
            final float d2 = dx * dx + dz * dz;
            if (d2 < best) {
                best = d2;
                idx = i;
            }
        }
        assertTrue(idx >= 0);
        assertTrue(base.morphFactors()[idx] <= 0.001f);
    }

    private static CdlodQuadTree makeTree(final int w, final int h) {
        return CdlodQuadTree.build(w, h, lodConfig(), HeightmapData.empty(w, h), 1.0f, 1.0f);
    }

    private static TerrainLodDesc lodConfig() {
        return new TerrainLodDesc(6, TessellationMode.COMPUTE, 2.0f, 65, 0.6f, 0.9f);
    }

    private static Frustum defaultFrustum() {
        return Frustum.infinite();
    }

    private static Frustum frustumLookingAway() {
        return Frustum.axisAligned(1500f, -100f, 1500f, 2000f, 1000f, 2000f);
    }
}
