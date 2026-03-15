package org.dynamisengine.terrain.vulkan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.dynamisengine.terrain.api.descriptor.AutoSplatConfig;
import org.dynamisengine.terrain.api.descriptor.FoliageDesc;
import org.dynamisengine.terrain.api.descriptor.FoliageLayer;
import org.dynamisengine.terrain.api.descriptor.HeightmapDesc;
import org.dynamisengine.terrain.api.descriptor.HeightmapFormat;
import org.dynamisengine.terrain.api.descriptor.ProceduralDesc;
import org.dynamisengine.terrain.api.descriptor.RoadDesc;
import org.dynamisengine.terrain.api.descriptor.SplatmapDesc;
import org.dynamisengine.terrain.api.descriptor.SplatmapMode;
import org.dynamisengine.terrain.api.descriptor.TerrainDescriptor;
import org.dynamisengine.terrain.api.descriptor.TerrainLodDesc;
import org.dynamisengine.terrain.api.descriptor.TerrainMaterial;
import org.dynamisengine.terrain.api.descriptor.TessellationMode;
import org.dynamisengine.terrain.api.descriptor.WaterDesc;
import org.dynamisengine.terrain.api.descriptor.WaterMode;
import org.dynamisengine.terrain.vulkan.internal.gpu.TerrainGpuBackendAdapter;
import org.junit.jupiter.api.Test;

class TerrainGpuAdapterSeamTest {
    @Test
    void samplerCreationRoutesThroughTerrainGpuAdapter() {
        TrackingAdapter adapter = new TrackingAdapter();
        TerrainGpuContext context = TerrainGpuContext.allocate(1L, new InMemoryGpuMemoryOps(), descriptor(64, 64), adapter);

        assertTrue(adapter.createSamplerCalled);
        assertEquals(777L, context.toGpuResources().sampler());

        context.destroy();
    }

    private static final class TrackingAdapter implements TerrainGpuBackendAdapter {
        private boolean createSamplerCalled;

        @Override
        public long createSampler(final GpuMemoryOps memoryOps) {
            this.createSamplerCalled = true;
            return 777L;
        }
    }

    private static TerrainDescriptor descriptor(final int w, final int h) {
        return new TerrainDescriptor(
                "tile-a2",
                new HeightmapDesc(HeightmapFormat.R32F, "", w, h),
                new SplatmapDesc(
                        SplatmapMode.LAYERS_4,
                        "",
                        null,
                        List.of(new TerrainMaterial("dirt", "", "", "", 1f, 0f, false, false))),
                new FoliageDesc(42L, 200f, true, List.of(new FoliageLayer("grass", 0.5f, 0f, 45f, 0f, 1000f, 0.2f))),
                new WaterDesc(WaterMode.NONE, 0f, 1.5f),
                new RoadDesc(false, 4f, 1f, 0.5f),
                new TerrainLodDesc(6, TessellationMode.COMPUTE, 2f, 65, 0.6f, 0.9f),
                new ProceduralDesc(42L, 4, 0.003f, 0, 0, List.of()),
                new AutoSplatConfig(true, 1f, 1f, 1f),
                1.0f,
                800f,
                false
        );
    }
}
