package org.dynamisengine.terrain.vulkan;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dynamisengine.terrain.api.descriptor.WaterDesc;
import org.dynamisengine.terrain.api.descriptor.WaterMode;
import org.dynamisengine.terrain.core.lod.Matrix4f;
import org.dynamisengine.terrain.vulkan.material.WeatherState;
import org.dynamisengine.terrain.vulkan.water.WaterDescriptorSets;
import org.dynamisengine.terrain.vulkan.water.WaterRenderPass;
import org.junit.jupiter.api.Test;

class WaterParityTest {
    @Test
    void waterRenderPassCreatesWithoutError() {
        final WaterRenderPass pass = WaterRenderPass.create(1L, 2L, new InMemoryGpuMemoryOps());
        assertNotEquals(0L, pass.pipelineHandle());
        pass.destroy();
    }

    @Test
    void waterDescriptorSetsCreateWithoutError() {
        assertDoesNotThrow(() -> {
            final WaterDescriptorSets sets = WaterDescriptorSets.create(1L, 11L, 12L, 13L, 14L, 15L, 16L, 2);
            sets.destroy();
        });
    }

    @Test
    void waterUboWritesWithoutError() {
        final WaterDescriptorSets sets = WaterDescriptorSets.create(1L, 11L, 12L, 13L, 14L, 15L, 16L, 2);
        assertDoesNotThrow(() -> sets.writeWaterUbo(new WaterDesc(WaterMode.PLANAR, 0f, 1.5f), 10f, 256f, 256f, 0));
        sets.destroy();
    }

    @Test
    void waterWeatherUboWritesHeavyRainWithoutError() {
        final WaterDescriptorSets sets = WaterDescriptorSets.create(1L, 11L, 12L, 13L, 14L, 15L, 16L, 2);
        assertDoesNotThrow(() -> sets.writeWeatherUbo(WeatherState.HEAVY_RAIN, 0));
        sets.destroy();
    }

    @Test
    void waterRecordCompletesWithoutValidationErrors() {
        final WaterDescriptorSets sets = WaterDescriptorSets.create(1L, 11L, 12L, 13L, 14L, 15L, 16L, 2);
        final WaterRenderPass pass = WaterRenderPass.create(1L, 2L, new InMemoryGpuMemoryOps());

        assertDoesNotThrow(() -> pass.record(1L, sets, Matrix4f.identity(), Matrix4f.identity(), 0));
        assertTrue(pass.recordedCalls() >= 1);

        pass.destroy();
        sets.destroy();
    }

    @Test
    void waterAerialLutBindingAcceptsNewHandle() {
        final WaterDescriptorSets sets = WaterDescriptorSets.create(1L, 11L, 12L, 13L, 14L, 15L, 16L, 2);
        sets.writeAerialLut(99L, 1);
        assertTrue(sets.aerialLut(1) == 99L);
        sets.destroy();
    }
}
