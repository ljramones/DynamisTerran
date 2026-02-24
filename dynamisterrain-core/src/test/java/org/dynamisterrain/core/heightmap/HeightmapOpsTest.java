package org.dynamisterrain.core.heightmap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dynamisterrain.api.descriptor.BlendMode;
import org.dynamisterrain.api.event.DeformShape;
import org.dynamisterrain.api.event.HeightDeformEvent;
import org.junit.jupiter.api.Test;

class HeightmapOpsTest {
    @Test
    void heightAtFlatTerrainReturnsZero() {
        final HeightmapData hm = HeightmapData.empty(64, 64);
        assertEquals(0.0f, HeightmapOps.heightAt(hm, 12.3f, 47.8f, 1.0f, 1.0f), 0.0f);
    }

    @Test
    void heightAtBilinearInterpolatesCorrectly() {
        final HeightmapData hm = HeightmapData.ofR32F(new float[] {0f, 10f, 0f, 10f}, 2, 2);
        assertEquals(5.0f, HeightmapOps.heightAt(hm, 0.5f, 0.5f, 1.0f, 1.0f), 0.01f);
    }

    @Test
    void heightAtEdgeDoesNotThrow() {
        final HeightmapData hm = HeightmapData.empty(8, 8);
        assertDoesNotThrow(() -> HeightmapOps.heightAt(hm, 0.0f, 0.0f, 1.0f, 1.0f));
        assertDoesNotThrow(() -> HeightmapOps.heightAt(hm, 7.0f, 7.0f, 1.0f, 1.0f));
    }

    @Test
    void heightAtOutsideBoundaryClamps() {
        final HeightmapData hm = HeightmapData.ofR32F(new float[] {1f, 2f, 3f, 4f}, 2, 2);
        assertEquals(4.0f, HeightmapOps.heightAt(hm, 100.0f, 100.0f, 1.0f, 1.0f), 0.001f);
    }

    @Test
    void normalAtFlatTerrainPointsUp() {
        final HeightmapData hm = HeightmapData.empty(16, 16);
        final float[] normals = HeightmapOps.generateNormals(hm, 1.0f, 1.0f);
        for (int i = 0; i < normals.length; i += 3) {
            assertEquals(0.0f, normals[i], 0.001f);
            assertEquals(1.0f, normals[i + 1], 0.001f);
            assertEquals(0.0f, normals[i + 2], 0.001f);
        }
    }

    @Test
    void normalAtSlopedTerrainTiltsCorrectly() {
        final HeightmapData hm = HeightmapData.empty(8, 8);
        for (int z = 0; z < hm.height(); z++) {
            for (int x = 0; x < hm.width(); x++) {
                hm.setPixel(x, z, x);
            }
        }
        final float[] normals = HeightmapOps.generateNormals(hm, 1.0f, 1.0f);
        final int center = ((hm.height() / 2) * hm.width() + (hm.width() / 2)) * 3;
        assertTrue(normals[center] < 0.0f);
        assertTrue(normals[center + 1] > 0.0f);
        assertEquals(0.0f, normals[center + 2], 0.05f);
    }

    @Test
    void deformSphereCreatesDepression() {
        final HeightmapData hm = HeightmapData.empty(32, 32);
        final float before = hm.pixelAt(16, 16);
        HeightmapOps.deform(hm, new HeightDeformEvent(16.0f, 16.0f, 6.0f, 10.0f, DeformShape.SPHERE), 1.0f);
        assertTrue(hm.pixelAt(16, 16) < before);
    }

    @Test
    void deformSphereDoesNotAffectOutsideRadius() {
        final HeightmapData hm = HeightmapData.empty(32, 32);
        hm.setPixel(0, 0, 2.0f);
        HeightmapOps.deform(hm, new HeightDeformEvent(16.0f, 16.0f, 5.0f, 10.0f, DeformShape.SPHERE), 1.0f);
        assertEquals(2.0f, hm.pixelAt(0, 0), 0.0f);
    }

    @Test
    void stampAddBlendIncreasesHeight() {
        final HeightmapData hm = HeightmapData.empty(16, 16);
        final HeightmapData stamp = HeightmapData.ofR32F(new float[] {1f, 1f, 1f, 1f}, 2, 2);
        HeightmapOps.applyStamp(hm, stamp, BlendMode.ADD, 1.0f, 8, 8);
        assertTrue(hm.pixelAt(8, 8) > 0.0f);
    }

    @Test
    void stampSetBlendReplacesHeight() {
        final HeightmapData hm = HeightmapData.empty(16, 16);
        hm.setPixel(8, 8, 9.0f);
        final HeightmapData stamp = HeightmapData.ofR32F(new float[] {2f, 2f, 2f, 2f}, 2, 2);
        HeightmapOps.applyStamp(hm, stamp, BlendMode.SET, 1.0f, 8, 8);
        assertEquals(2.0f, hm.pixelAt(8, 8), 0.001f);
    }

    @Test
    void minMaxInRegionCorrect() {
        final HeightmapData hm = HeightmapData.empty(4, 4);
        hm.setPixel(1, 1, 0.0f);
        hm.setPixel(2, 2, 100.0f);
        final float[] mm = HeightmapOps.minMaxInRegion(hm, 1, 1, 2, 2);
        assertEquals(0.0f, mm[0], 0.0f);
        assertEquals(100.0f, mm[1], 0.0f);
    }

    @Test
    void minMaxSingleTexel() {
        final HeightmapData hm = HeightmapData.empty(4, 4);
        hm.setPixel(3, 3, 42.0f);
        final float[] mm = HeightmapOps.minMaxInRegion(hm, 3, 3, 3, 3);
        assertEquals(42.0f, mm[0], 0.0f);
        assertEquals(42.0f, mm[1], 0.0f);
    }
}
