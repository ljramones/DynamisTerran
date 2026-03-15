package org.dynamisengine.terrain.core.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Arrays;
import org.dynamisengine.terrain.core.heightmap.HeightmapData;
import org.junit.jupiter.api.Test;

class FlowMapGeneratorTest {
    @Test
    void flatTerrainProducesUniformFlow() {
        final HeightmapData hm = HeightmapData.empty(64, 64);
        final FlowMapData flow = FlowMapGenerator.generate(hm, new FlowConfig(0, 1.0f, false));
        final float first = flow.rawData()[0];
        for (float v : flow.rawData()) {
            assertEquals(first, v, 0.0001f);
        }
    }

    @Test
    void valleyAccumulationHigherThanRidge() {
        final HeightmapData hm = valley(64, 64);
        final FlowMapData flow = FlowMapGenerator.generate(hm, FlowConfig.defaults());
        final float valley = flow.accumulationAt(32, 32);
        final float ridge = flow.accumulationAt(2, 32);
        assertTrue(valley > ridge);
    }

    @Test
    void ridgeAccumulationNearZero() {
        final HeightmapData hm = peak(64, 64);
        final FlowMapData flow = FlowMapGenerator.generate(hm, new FlowConfig(0, 1.0f, false));
        assertTrue(flow.accumulationAt(32, 32) <= 0.0001f);
    }

    @Test
    void normalizedOutputInUnitRange() {
        final FlowMapData flow = FlowMapGenerator.generate(valley(64, 64), new FlowConfig(1, 1.0f, true));
        for (float v : flow.rawData()) {
            assertTrue(v >= 0.0f && v <= 1.0f);
        }
    }

    @Test
    void unnormalizedOutputIsPositive() {
        final FlowMapData flow = FlowMapGenerator.generate(valley(64, 64), new FlowConfig(0, 1.0f, false));
        for (float v : flow.rawData()) {
            assertTrue(v >= 0.0f);
        }
    }

    @Test
    void accumulationAtWorldBilinearInterpolates() {
        final float[] d = new float[16 * 16];
        d[10 * 16 + 10] = 0.8f;
        d[10 * 16 + 11] = 0.4f;
        final FlowMapData flow = FlowMapData.of(d, 16, 16);
        final float s = flow.accumulationAtWorld(10.5f, 10.0f, 1.0f);
        assertTrue(s > 0.4f && s < 0.8f);
    }

    @Test
    void generateIsDeterministic() {
        final HeightmapData hm = valley(64, 64);
        final FlowConfig cfg = FlowConfig.defaults();
        final FlowMapData a = FlowMapGenerator.generate(hm, cfg);
        final FlowMapData b = FlowMapGenerator.generate(hm, cfg);
        assertTrue(Arrays.equals(a.rawData(), b.rawData()));
    }

    @Test
    void largerHeightmapCompletesInReasonableTime() {
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            FlowMapGenerator.generate(valley(512, 512), FlowConfig.defaults());
        });
    }

    private static HeightmapData valley(final int w, final int h) {
        final HeightmapData hm = HeightmapData.empty(w, h);
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                final float dx = Math.abs(x - w / 2f);
                hm.setPixel(x, z, dx * 5f);
            }
        }
        return hm;
    }

    private static HeightmapData peak(final int w, final int h) {
        final HeightmapData hm = HeightmapData.empty(w, h);
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                final float dx = x - w / 2f;
                final float dz = z - h / 2f;
                hm.setPixel(x, z, 1000f - (float) Math.sqrt(dx * dx + dz * dz) * 10f);
            }
        }
        return hm;
    }
}
