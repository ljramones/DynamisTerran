package org.dynamisterrain.core.scatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dynamisterrain.api.descriptor.FoliageLayer;
import org.dynamisterrain.core.flow.FlowConfig;
import org.dynamisterrain.core.flow.FlowMapData;
import org.dynamisterrain.core.flow.FlowMapGenerator;
import org.dynamisterrain.core.heightmap.HeightmapData;
import org.junit.jupiter.api.Test;

class ScatterRuleEngineTest {
    @Test
    void evaluateProducesNonZeroPointsOnFlatGrass() {
        final ScatterResult r = run(flat(64, 64, 50f), layer(1f, 0f, 500f, 0f, 45f), null, 42L, ScatterConfig.defaults());
        assertTrue(r.count() > 0);
    }

    @Test
    void slopeFilterRejectsVerticalTerrain() {
        final HeightmapData hm = cliff(64, 64);
        final ScatterResult r = run(hm, layer(1f, 0f, 1000f, 0f, 30f), null, 42L, ScatterConfig.defaults());
        assertTrue(r.points().stream().noneMatch(p -> Math.abs(p.worldX() - 32f) < 1.5f));
    }

    @Test
    void altitudeFilterRejectsHighAltitude() {
        final HeightmapData hm = flat(64, 64, 0f);
        hm.setPixel(32, 32, 1000f);
        final ScatterResult r = run(hm, layer(1f, 0f, 500f, 0f, 45f), null, 42L, ScatterConfig.defaults());
        assertTrue(r.points().stream().noneMatch(p -> p.worldY() > 500f));
    }

    @Test
    void densityZeroProducesNoPoints() {
        final ScatterResult r = run(flat(64, 64, 10f), layer(0f, 0f, 500f, 0f, 45f), null, 42L, ScatterConfig.defaults());
        assertEquals(0, r.count());
    }

    @Test
    void densityOneProducesMaxPoints() {
        final ScatterConfig cfg = new ScatterConfig(2.0f, 1, 1f, 1f);
        final ScatterResult r = run(flat(32, 32, 10f), layer(1f, 0f, 500f, 0f, 45f), null, 42L, cfg);
        assertTrue(r.count() > 100);
    }

    @Test
    void scatterIsSeedDeterministic() {
        final HeightmapData hm = flat(64, 64, 10f);
        final FoliageLayer layer = layer(0.7f, 0f, 500f, 0f, 45f);
        final ScatterResult a = run(hm, layer, null, 42L, ScatterConfig.defaults());
        final ScatterResult b = run(hm, layer, null, 42L, ScatterConfig.defaults());
        assertEquals(a.points(), b.points());
    }

    @Test
    void scatterDifferentSeedsDifferentResults() {
        final HeightmapData hm = flat(64, 64, 10f);
        final FoliageLayer layer = layer(0.7f, 0f, 500f, 0f, 45f);
        final ScatterResult a = run(hm, layer, null, 42L, ScatterConfig.defaults());
        final ScatterResult b = run(hm, layer, null, 43L, ScatterConfig.defaults());
        assertNotEquals(a.points(), b.points());
    }

    @Test
    void allPointsWithinTerrainBounds() {
        final HeightmapData hm = flat(64, 64, 10f);
        final ScatterResult r = run(hm, layer(1f, 0f, 500f, 0f, 45f), null, 42L, ScatterConfig.defaults());
        for (ScatterPoint p : r.points()) {
            assertTrue(p.worldX() >= 0f && p.worldX() <= 64f);
            assertTrue(p.worldZ() >= 0f && p.worldZ() <= 64f);
        }
    }

    @Test
    void allScalesInConfiguredRange() {
        final ScatterConfig cfg = new ScatterConfig(2f, 5, 0.5f, 1.5f);
        final ScatterResult r = run(flat(64, 64, 10f), layer(1f, 0f, 500f, 0f, 45f), null, 42L, cfg);
        for (ScatterPoint p : r.points()) {
            assertTrue(p.scale() >= 0.5f && p.scale() <= 1.5f);
        }
    }

    @Test
    void flowMapIncreasesPointCountInValley() {
        final HeightmapData hm = valley(64, 64);
        final FlowMapData flow = FlowMapGenerator.generate(hm, new FlowConfig(1, 1.0f, true));
        final ScatterResult withFlow = run(hm, layer(0.8f, 0f, 500f, 0f, 45f), flow, 42L, ScatterConfig.defaults());
        long valley = withFlow.points().stream().filter(p -> Math.abs(p.worldX() - 32f) < 6f).count();
        long ridge = withFlow.points().stream().filter(p -> p.worldX() < 8f || p.worldX() > 56f).count();
        assertTrue(valley > ridge);
    }

    private static ScatterResult run(
        final HeightmapData hm,
        final FoliageLayer layer,
        final FlowMapData flow,
        final long seed,
        final ScatterConfig cfg
    ) {
        return ScatterRuleEngine.evaluate(layer, hm, flow, null, 1.0f, 1.0f, seed, cfg);
    }

    private static FoliageLayer layer(
        final float density,
        final float minAlt,
        final float maxAlt,
        final float minSlope,
        final float maxSlope
    ) {
        return new FoliageLayer("grass", density, minSlope, maxSlope, minAlt, maxAlt, 0f);
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
                hm.setPixel(x, z, x * 100f);
            }
        }
        return hm;
    }

    private static HeightmapData valley(final int w, final int h) {
        final HeightmapData hm = HeightmapData.empty(w, h);
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                hm.setPixel(x, z, Math.abs(x - w / 2f) * 5f);
            }
        }
        return hm;
    }
}
