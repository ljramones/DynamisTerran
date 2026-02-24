package org.dynamisterrain.core.material;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dynamisterrain.api.descriptor.AutoSplatConfig;
import org.dynamisterrain.core.flow.FlowConfig;
import org.dynamisterrain.core.flow.FlowMapData;
import org.dynamisterrain.core.flow.FlowMapGenerator;
import org.dynamisterrain.core.heightmap.HeightmapData;
import org.dynamisterrain.core.heightmap.HeightmapOps;
import org.junit.jupiter.api.Test;

class AutoSplatmapGeneratorTest {
    @Test
    void weightsSumToOnePerTexel() {
        final HeightmapData hm = flat(32, 32, 100f);
        final float[] normals = HeightmapOps.generateNormals(hm, 1f, 1f);
        final float[][] w = AutoSplatmapGenerator.generate(hm, normals, null, cfg(), 4, 1f, 2000f);
        for (int i = 0; i < hm.width() * hm.height(); i++) {
            float sum = 0f;
            for (float[] layer : w) {
                sum += layer[i];
            }
            assertEquals(1.0f, sum, 0.001f);
        }
    }

    @Test
    void rockAppearsOnSteepSlopes() {
        final HeightmapData hm = cliff(32, 32);
        final float[] normals = HeightmapOps.generateNormals(hm, 1f, 1f);
        final float[][] w = AutoSplatmapGenerator.generate(hm, normals, null, cfg(), 4, 1f, 2000f);
        assertTrue(w[1][16 * 32 + 15] > 0.5f);
    }

    @Test
    void snowAppearsAtHighAltitude() {
        final HeightmapData hm = peak(32, 32, 1500f);
        final float[] normals = HeightmapOps.generateNormals(hm, 1f, 1f);
        final float[][] w = AutoSplatmapGenerator.generate(hm, normals, null, cfg(), 4, 1f, 1500f);
        assertTrue(w[2][16 * 32 + 16] > 0.5f);
    }

    @Test
    void grassAppearsInHighFlowAccumulation() {
        final HeightmapData hm = valley(32, 32);
        final FlowMapData flow = FlowMapGenerator.generate(hm, new FlowConfig(1, 1.0f, true));
        final float[] normals = HeightmapOps.generateNormals(hm, 1f, 1f);
        final float[][] w = AutoSplatmapGenerator.generate(hm, normals, flow, cfg(), 4, 1f, 1000f);
        assertTrue(w[3][16 * 32 + 16] > w[3][16 * 32 + 2]);
    }

    @Test
    void flatLowAltitudeTerrainIsMostlyDirt() {
        final HeightmapData hm = flat(32, 32, 100f);
        final float[] normals = HeightmapOps.generateNormals(hm, 1f, 1f);
        final float[][] w = AutoSplatmapGenerator.generate(hm, normals, null, cfg(), 4, 1f, 1500f);
        assertTrue(w[0][10 * 32 + 10] > 0.5f);
    }

    @Test
    void layerCountOneProducesSingleWeight() {
        final HeightmapData hm = flat(16, 16, 10f);
        final float[] normals = HeightmapOps.generateNormals(hm, 1f, 1f);
        final float[][] w = AutoSplatmapGenerator.generate(hm, normals, null, cfg(), 1, 1f, 1000f);
        for (float v : w[0]) {
            assertEquals(1.0f, v, 0.0f);
        }
    }

    @Test
    void nullFlowMapSkipsFlowRule() {
        final HeightmapData hm = flat(16, 16, 10f);
        final float[] normals = HeightmapOps.generateNormals(hm, 1f, 1f);
        assertDoesNotThrow(() -> AutoSplatmapGenerator.generate(hm, normals, null, cfg(), 4, 1f, 1000f));
    }

    private static AutoSplatConfig cfg() {
        return new AutoSplatConfig(true, 1f, 1f, 1f);
    }

    private static HeightmapData flat(final int w, final int h, final float y) {
        final HeightmapData hm = HeightmapData.empty(w, h);
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                hm.setPixel(x, z, y);
            }
        }
        return hm;
    }

    private static HeightmapData cliff(final int w, final int h) {
        final HeightmapData hm = HeightmapData.empty(w, h);
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                hm.setPixel(x, z, x < w / 2 ? 0f : 2000f);
            }
        }
        return hm;
    }

    private static HeightmapData peak(final int w, final int h, final float max) {
        final HeightmapData hm = HeightmapData.empty(w, h);
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                final float dx = x - w / 2f;
                final float dz = z - h / 2f;
                hm.setPixel(x, z, Math.max(0f, max - (float) Math.sqrt(dx * dx + dz * dz) * 50f));
            }
        }
        return hm;
    }

    private static HeightmapData valley(final int w, final int h) {
        final HeightmapData hm = HeightmapData.empty(w, h);
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                hm.setPixel(x, z, Math.abs(x - w / 2f) * 10f);
            }
        }
        return hm;
    }
}
