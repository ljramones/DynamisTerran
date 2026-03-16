package org.dynamisengine.terrain.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.dynamisengine.terrain.api.descriptor.BlendMode;
import org.dynamisengine.terrain.api.descriptor.HeightStamp;
import org.dynamisengine.terrain.api.descriptor.ProceduralDesc;
import org.dynamisengine.terrain.core.heightmap.HeightmapData;
import org.junit.jupiter.api.Test;

class ProceduralHeightmapGeneratorTest {
    @Test
    void generatesNonFlatTerrain() {
        final HeightmapData hm = ProceduralHeightmapGenerator.generate(desc(42, 6, 0, 0, List.of()), 128, 128, 800f);
        final float mean = mean(hm.pixels());
        float var = 0f;
        for (float v : hm.pixels()) {
            final float d = v - mean;
            var += d * d;
        }
        final float std = (float) Math.sqrt(var / hm.pixels().length);
        assertTrue(std > 0f);
    }

    @Test
    void hydraulicErosionReducesPeakSharpness() {
        final HeightmapData a = ProceduralHeightmapGenerator.generate(desc(42, 6, 0, 0, List.of()), 128, 128, 800f);
        final HeightmapData b = ProceduralHeightmapGenerator.generate(desc(42, 6, 50, 0, List.of()), 128, 128, 800f);
        assertTrue(peakSharpness(b) < peakSharpness(a));
    }

    @Test
    void thermalErosionReducesSteepSlopes() {
        final HeightmapData b = ProceduralHeightmapGenerator.generate(desc(42, 8, 0, 20, List.of()), 128, 128, 800f);
        assertTrue(meanSlope(b) >= 0.0f);
    }

    @Test
    void stampAppliedCorrectly() {
        final HeightmapData a = ProceduralHeightmapGenerator.generate(desc(42, 6, 0, 0, List.of()), 128, 128, 800f);
        final HeightmapData b = ProceduralHeightmapGenerator.generate(
            desc(42, 6, 0, 0, List.of(new HeightStamp("stamp", BlendMode.ADD, 1.0f))),
            128,
            128,
            800f);
        assertTrue(b.pixelAt(64, 64) > a.pixelAt(64, 64));
    }

    @Test
    void outputClampedToHeightScale() {
        final float hs = 500f;
        final HeightmapData hm = ProceduralHeightmapGenerator.generate(desc(42, 8, 40, 20, List.of()), 128, 128, hs);
        for (float v : hm.pixels()) {
            assertTrue(v >= 0f && v <= hs);
        }
    }

    @Test
    void differentSeedsProduceDifferentTerrain() {
        final HeightmapData a = ProceduralHeightmapGenerator.generate(desc(1, 6, 0, 0, List.of()), 64, 64, 800f);
        final HeightmapData b = ProceduralHeightmapGenerator.generate(desc(2, 6, 0, 0, List.of()), 64, 64, 800f);
        int diff = 0;
        for (int i = 0; i < a.pixels().length; i++) {
            if (Math.abs(a.pixels()[i] - b.pixels()[i]) > 0.001f) {
                diff++;
            }
        }
        assertTrue(diff > a.pixels().length * 0.1f);
    }

    @Test
    void sameSeedProducesIdenticalOutput() {
        final HeightmapData a = ProceduralHeightmapGenerator.generate(desc(42, 6, 0, 0, List.of()), 64, 64, 800f);
        final HeightmapData b = ProceduralHeightmapGenerator.generate(desc(42, 6, 0, 0, List.of()), 64, 64, 800f);
        for (int i = 0; i < a.pixels().length; i++) {
            assertEquals(a.pixels()[i], b.pixels()[i], 0.0f);
        }
    }

    @Test
    void dimensionsMatchRequest() {
        final HeightmapData hm = ProceduralHeightmapGenerator.generate(desc(42, 6, 0, 0, List.of()), 256, 128, 800f);
        assertEquals(256, hm.width());
        assertEquals(128, hm.height());
    }

    private static ProceduralDesc desc(
        final long seed,
        final int octaves,
        final int erosion,
        final int thermal,
        final List<HeightStamp> stamps
    ) {
        return new ProceduralDesc(seed, octaves, 0.003f, erosion, thermal, stamps);
    }

    private static float mean(final float[] data) {
        float s = 0f;
        for (float v : data) {
            s += v;
        }
        return s / data.length;
    }

    private static float peakSharpness(final HeightmapData hm) {
        int maxIdx = 0;
        for (int i = 1; i < hm.pixels().length; i++) {
            if (hm.pixels()[i] > hm.pixels()[maxIdx]) {
                maxIdx = i;
            }
        }
        final int x = maxIdx % hm.width();
        final int z = maxIdx / hm.width();
        final float center = hm.pixelAt(x, z);
        final float n = (hm.pixelAt(x - 1, z) + hm.pixelAt(x + 1, z) + hm.pixelAt(x, z - 1) + hm.pixelAt(x, z + 1)) * 0.25f;
        return center - n;
    }

    @Test
    void largerResolutionProducesMoreSamples() {
        final HeightmapData small = ProceduralHeightmapGenerator.generate(desc(42, 6, 0, 0, List.of()), 32, 32, 800f);
        final HeightmapData large = ProceduralHeightmapGenerator.generate(desc(42, 6, 0, 0, List.of()), 128, 128, 800f);
        assertTrue(large.pixels().length > small.pixels().length);
        assertEquals(32 * 32, small.pixels().length);
        assertEquals(128 * 128, large.pixels().length);
    }

    @Test
    void erosionPassesSmoothTerrain() {
        final HeightmapData noErosion = ProceduralHeightmapGenerator.generate(desc(42, 6, 0, 0, List.of()), 64, 64, 800f);
        final HeightmapData withErosion = ProceduralHeightmapGenerator.generate(desc(42, 6, 30, 0, List.of()), 64, 64, 800f);
        assertTrue(variance(withErosion.pixels()) < variance(noErosion.pixels()),
            "Erosion should reduce height variance");
    }

    @Test
    void edgeValuesAreValid() {
        final HeightmapData hm = ProceduralHeightmapGenerator.generate(desc(42, 6, 10, 5, List.of()), 64, 64, 800f);
        final int w = hm.width();
        final int h = hm.height();
        for (int x = 0; x < w; x++) {
            assertFinite(hm.pixelAt(x, 0));
            assertFinite(hm.pixelAt(x, h - 1));
        }
        for (int z = 0; z < h; z++) {
            assertFinite(hm.pixelAt(0, z));
            assertFinite(hm.pixelAt(w - 1, z));
        }
        assertFinite(hm.pixelAt(0, 0));
        assertFinite(hm.pixelAt(w - 1, 0));
        assertFinite(hm.pixelAt(0, h - 1));
        assertFinite(hm.pixelAt(w - 1, h - 1));
    }

    private static void assertFinite(final float v) {
        assertTrue(Float.isFinite(v), "Expected finite value but got " + v);
    }

    private static float variance(final float[] data) {
        final float m = mean(data);
        float var = 0f;
        for (float v : data) {
            final float d = v - m;
            var += d * d;
        }
        return var / data.length;
    }

    private static float meanSlope(final HeightmapData hm) {
        float sum = 0f;
        int count = 0;
        for (int z = 1; z < hm.height() - 1; z++) {
            for (int x = 1; x < hm.width() - 1; x++) {
                final float dx = hm.pixelAt(x + 1, z) - hm.pixelAt(x - 1, z);
                final float dz = hm.pixelAt(x, z + 1) - hm.pixelAt(x, z - 1);
                sum += (float) Math.sqrt(dx * dx + dz * dz);
                count++;
            }
        }
        return sum / Math.max(1, count);
    }
}
