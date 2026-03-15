package org.dynamisengine.terrain.vulkan;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import org.dynamisengine.terrain.core.flow.FlowMapData;
import org.dynamisengine.terrain.core.heightmap.HeightmapData;
import org.dynamisengine.terrain.vulkan.horizon.HorizonBakeConfig;
import org.dynamisengine.terrain.vulkan.horizon.HorizonMapBaker;
import org.junit.jupiter.api.Test;

class TerrainGpuParityTest {
    @Test
    void terrainGpuContextAllocatesWithoutError() {
        final TerrainGpuContext ctx = TerrainGpuContext.allocate(1L, new InMemoryGpuMemoryOps(), descriptor(256, 256));
        assertNotEquals(0L, ctx.toGpuResources().heightmapImage());
        assertNotEquals(0L, ctx.toGpuResources().normalMap());
        assertNotEquals(0L, ctx.toGpuResources().horizonMap());
        assertNotEquals(0L, ctx.toGpuResources().flowMap());
        assertNotEquals(0L, ctx.toGpuResources().splatmap0());
        assertNotEquals(0L, ctx.toGpuResources().sampler());
        ctx.destroy();
    }

    @Test
    void heightmapUploadCompletesWithoutError() {
        final TerrainGpuContext ctx = TerrainGpuContext.allocate(1L, new InMemoryGpuMemoryOps(), descriptor(256, 256));
        ctx.uploadHeightmap(HeightmapData.empty(256, 256), 1L);
        assertTrue(ctx.heightmapTexture().data().length > 0);
        ctx.destroy();
    }

    @Test
    void flowMapUploadCompletesWithoutError() {
        final TerrainGpuContext ctx = TerrainGpuContext.allocate(1L, new InMemoryGpuMemoryOps(), descriptor(128, 128));
        ctx.uploadFlowMap(FlowMapData.of(new float[128 * 128], 128, 128), 1L);
        assertTrue(ctx.toGpuResources().flowMap() != 0L);
        ctx.destroy();
    }

    @Test
    void horizonMapBakeProducesNonZeroOutput() {
        final TerrainGpuContext ctx = TerrainGpuContext.allocate(1L, new InMemoryGpuMemoryOps(), descriptor(64, 64));
        ctx.uploadHeightmap(hill(64, 64), 1L);

        final HorizonMapBaker baker = HorizonMapBaker.create(1L, new InMemoryGpuMemoryOps());
        baker.bake(1L, ctx, new HorizonBakeConfig(32, 1.0f, 800f));

        final byte[] data = ctx.horizonMapTexture().data();
        int nonZero = 0;
        for (byte b : data) {
            if ((b & 0xFF) > 0) {
                nonZero++;
                break;
            }
        }
        assertTrue(nonZero > 0);
        ctx.destroy();
    }

    @Test
    void horizonMapBakeIsIdempotent() {
        final TerrainGpuContext ctx = TerrainGpuContext.allocate(1L, new InMemoryGpuMemoryOps(), descriptor(64, 64));
        ctx.uploadHeightmap(hill(64, 64), 1L);

        final HorizonMapBaker baker = HorizonMapBaker.create(1L, new InMemoryGpuMemoryOps());
        final HorizonBakeConfig cfg = new HorizonBakeConfig(32, 1.0f, 800f);
        baker.bake(1L, ctx, cfg);
        final byte[] first = ctx.horizonMapTexture().data().clone();
        baker.bake(1L, ctx, cfg);
        final byte[] second = ctx.horizonMapTexture().data().clone();
        assertArrayEquals(first, second);
        ctx.destroy();
    }

    @Test
    void horizonMapValleyTexelHasHigherAngleThanRidgeTexel() {
        final TerrainGpuContext ctx = TerrainGpuContext.allocate(1L, new InMemoryGpuMemoryOps(), descriptor(64, 64));
        ctx.uploadHeightmap(valley(64, 64), 1L);

        final HorizonMapBaker baker = HorizonMapBaker.create(1L, new InMemoryGpuMemoryOps());
        baker.bake(1L, ctx, new HorizonBakeConfig(32, 1.0f, 800f));

        final int valleyIdx = (32 * 64 + 32) * 4;
        final int ridgeIdx = (32 * 64 + 0) * 4;
        final byte[] data = ctx.horizonMapTexture().data();

        float valleyMax = 0f;
        float ridgeMax = 0f;
        for (int c = 0; c < 4; c++) {
            valleyMax = Math.max(valleyMax, HorizonMapBaker.unpackDirectionAngle(data[valleyIdx + c], true));
            valleyMax = Math.max(valleyMax, HorizonMapBaker.unpackDirectionAngle(data[valleyIdx + c], false));

            ridgeMax = Math.max(ridgeMax, HorizonMapBaker.unpackDirectionAngle(data[ridgeIdx + c], true));
            ridgeMax = Math.max(ridgeMax, HorizonMapBaker.unpackDirectionAngle(data[ridgeIdx + c], false));
        }

        assertTrue(valleyMax > 0.05f);
        assertTrue(valleyMax > ridgeMax);
        ctx.destroy();
    }

    private static TerrainDescriptor descriptor(final int w, final int h) {
        return new TerrainDescriptor(
            "tile-0-0",
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

    private static HeightmapData hill(final int w, final int h) {
        final HeightmapData hm = HeightmapData.empty(w, h);
        final float cx = w * 0.5f;
        final float cz = h * 0.5f;
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                final float dx = x - cx;
                final float dz = z - cz;
                hm.setPixel(x, z, Math.max(0f, 200f - (float) Math.sqrt(dx * dx + dz * dz) * 6f));
            }
        }
        return hm;
    }

    private static HeightmapData valley(final int w, final int h) {
        final HeightmapData hm = HeightmapData.empty(w, h);
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                hm.setPixel(x, z, Math.abs(x - w * 0.5f) * 8f);
            }
        }
        return hm;
    }
}
