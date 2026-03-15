package org.dynamisengine.terrain.vulkan;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import org.dynamisengine.terrain.api.state.Vector3f;
import org.dynamisengine.terrain.core.heightmap.HeightmapData;
import org.dynamisengine.terrain.core.lod.CdlodFrameResult;
import org.dynamisengine.terrain.core.lod.CdlodQuadTree;
import org.dynamisengine.terrain.core.lod.Frustum;
import org.dynamisengine.terrain.core.lod.Matrix4f;
import org.dynamisengine.terrain.vulkan.lod.CdlodSelectionUbo;
import org.dynamisengine.terrain.vulkan.lod.CdlodTessellationUbo;
import org.dynamisengine.terrain.vulkan.lod.SilhouetteUbo;
import org.dynamisengine.terrain.vulkan.lod.TerrainGpuLodResources;
import org.dynamisengine.terrain.vulkan.lod.TerrainLodPipeline;
import org.dynamisengine.terrain.vulkan.material.SunState;
import org.dynamisengine.terrain.vulkan.material.TerrainDrawPipeline;
import org.dynamisengine.terrain.vulkan.material.TerrainMaterialAtlas;
import org.dynamisengine.terrain.vulkan.material.TerrainMaterialDescriptorSets;
import org.dynamisengine.terrain.vulkan.material.TerrainMaterialUbo;
import org.dynamisengine.terrain.vulkan.material.TimeOfDayState;
import org.dynamisengine.terrain.vulkan.material.WeatherState;
import org.junit.jupiter.api.Test;

class TerrainMaterialParityTest {
    @Test
    void materialAtlasRegistersTexturesWithoutError() {
        final TerrainMaterialAtlas atlas = TerrainMaterialAtlas.create(
            1L,
            new InMemoryGpuMemoryOps(),
            1L,
            materials(),
            1L
        );
        assertTrue(atlas.slotFor(0, 0) >= 0);
        assertTrue(atlas.fallbackSlot(0) >= 0);
        atlas.destroy();
    }

    @Test
    void drawPipelineCreatesWithoutError() {
        final TerrainDrawPipeline pipeline = TerrainDrawPipeline.create(1L, 2L, 3L);
        assertNotEquals(0L, pipeline.pipelineHandle());
        pipeline.destroy();
    }

    @Test
    void descriptorSetsWriteWeatherUboWithoutError() {
        final Fixture f = fixture();
        assertDoesNotThrow(() -> f.descriptorSets.writeWeatherUbo(
            WeatherState.HEAVY_RAIN,
            SunState.NOON,
            TimeOfDayState.DAY,
            0
        ));
        f.destroy();
    }

    @Test
    void descriptorSetsWriteAerialLutWithoutError() {
        final Fixture f = fixture();
        assertDoesNotThrow(() -> f.descriptorSets.writeAerialLut(999L, 0));
        assertTrue(f.descriptorSets.aerialLut(0) == 999L);
        f.destroy();
    }

    @Test
    void fullDrawCallRecordsWithoutValidationErrors() {
        final Fixture f = fixture();
        final TerrainLodPipeline lodPipeline = TerrainLodPipeline.create(1L, new InMemoryGpuMemoryOps());
        final TerrainDrawPipeline drawPipeline = TerrainDrawPipeline.create(1L, 2L, 3L);
        lodPipeline.attachDrawPipeline(drawPipeline);

        assertDoesNotThrow(() -> {
            lodPipeline.compute(1L, f.lodResources, f.ctx, f.selectionUbo, f.tessUbo, f.silhouetteUbo, 0L, 0L, f.totalPatchCount);
            lodPipeline.recordDraw(1L, f.lodResources, f.descriptorSets, Matrix4f.identity(), 0);
        });

        assertTrue(drawPipeline.lastRecordedDrawCount() > 0);
        f.destroy();
    }

    @Test
    void weatherUboSnowIntensityReflectedInUbo() {
        final Fixture f = fixture();
        f.descriptorSets.writeWeatherUbo(
            new WeatherState(0.8f, 0.2f, 0.1f, 3f, new Vector3f(1f, 0f, 0f)),
            SunState.NOON,
            TimeOfDayState.DAY,
            0
        );

        final float snow = TerrainMaterialDescriptorSets.readFloat(f.descriptorSets.weatherUboBytes(0), 0);
        assertTrue(Math.abs(snow - 0.8f) < 0.001f);
        f.destroy();
    }

    @Test
    void terrainMaterialUboWaterElevationReflected() {
        final Fixture f = fixture();
        f.descriptorSets.writeTerrainMaterialUbo(
            new TerrainMaterialUbo(50f, 0.3f, 4, 4, 256f, 256f, 800f, 1f, new Vector3f(0f, 1f, 0f), 10_000f, 0.1f),
            0
        );

        final float waterElevation = TerrainMaterialDescriptorSets.readFloat(f.descriptorSets.terrainMaterialUboBytes(0), 0);
        assertTrue(Math.abs(waterElevation - 50f) < 0.001f);
        f.destroy();
    }

    private static Fixture fixture() {
        final TerrainDescriptor descriptor = descriptor(256, 256);
        final TerrainGpuContext ctx = TerrainGpuContext.allocate(1L, new InMemoryGpuMemoryOps(), descriptor);
        ctx.uploadHeightmap(hill(256, 256), 1L);

        final CdlodQuadTree tree = CdlodQuadTree.build(256, 256, descriptor.lod(), ctx.heightmapData(), 1.0f, 1.0f);
        final CdlodFrameResult patches = tree.select(new Vector3f(128f, 200f, 128f), Frustum.infinite(), 2.0f);
        final TerrainGpuLodResources lodResources = TerrainGpuLodResources.allocate(
            1L,
            new InMemoryGpuMemoryOps(),
            Math.max(1, patches.patchCount()),
            Math.max(1, patches.patchCount() * 4225)
        );
        lodResources.uploadPatchList(patches, 1L);

        final TerrainMaterialAtlas atlas = TerrainMaterialAtlas.create(1L, new InMemoryGpuMemoryOps(), 1L, materials(), 1L);
        final TerrainMaterialDescriptorSets descriptorSets = TerrainMaterialDescriptorSets.create(1L, ctx, atlas, 777L, 1L, 2);

        final CdlodSelectionUbo selectionUbo = new CdlodSelectionUbo(
            fullFrustumPlanes(),
            new Vector3f(128f, 200f, 128f),
            2.0f,
            descriptor.lod().morphStart(),
            descriptor.lod().morphEnd(),
            patches.patchCount()
        );
        final CdlodTessellationUbo tessUbo = new CdlodTessellationUbo(
            descriptor.worldScale(),
            descriptor.heightScale(),
            descriptor.lod().patchSize(),
            descriptor.heightmap().width(),
            descriptor.heightmap().height(),
            descriptor.heightmap().width() * descriptor.worldScale(),
            descriptor.heightmap().height() * descriptor.worldScale()
        );
        final SilhouetteUbo silhouetteUbo = new SilhouetteUbo(
            1920f,
            1080f,
            0.1f,
            2.0f,
            new Vector3f(128f, 200f, 128f),
            0.1f,
            10_000f
        );

        return new Fixture(ctx, lodResources, atlas, descriptorSets, selectionUbo, tessUbo, silhouetteUbo, patches.patchCount());
    }

    private static List<TerrainMaterial> materials() {
        return List.of(
            new TerrainMaterial("grass", "", "", "", 4f, 0f, false, false),
            new TerrainMaterial("rock", "", "", "", 2f, 0f, true, false),
            new TerrainMaterial("snow", "", "", "", 6f, 0f, false, false),
            new TerrainMaterial("dirt", "", "", "", 3f, 0f, false, false)
        );
    }

    private static float[] fullFrustumPlanes() {
        return new float[] {
            1f, 0f, 0f, 1_000_000f,
            -1f, 0f, 0f, 1_000_000f,
            0f, 1f, 0f, 1_000_000f,
            0f, -1f, 0f, 1_000_000f,
            0f, 0f, 1f, 1_000_000f,
            0f, 0f, -1f, 1_000_000f
        };
    }

    private static TerrainDescriptor descriptor(final int w, final int h) {
        return new TerrainDescriptor(
            "tile-0-0",
            new HeightmapDesc(HeightmapFormat.R32F, "", w, h),
            new SplatmapDesc(SplatmapMode.LAYERS_4, "", null, materials()),
            new FoliageDesc(42L, 200f, true, List.of(new FoliageLayer("grass", 0.5f, 0f, 45f, 0f, 1000f, 0.2f))),
            new WaterDesc(WaterMode.PLANAR, 0f, 1.5f),
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

    private record Fixture(
        TerrainGpuContext ctx,
        TerrainGpuLodResources lodResources,
        TerrainMaterialAtlas atlas,
        TerrainMaterialDescriptorSets descriptorSets,
        CdlodSelectionUbo selectionUbo,
        CdlodTessellationUbo tessUbo,
        SilhouetteUbo silhouetteUbo,
        int totalPatchCount
    ) {
        void destroy() {
            this.descriptorSets.destroy();
            this.atlas.destroy();
            this.lodResources.destroy();
            this.ctx.destroy();
        }
    }
}
