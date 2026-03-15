package org.dynamisengine.terrain.core.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.dynamisengine.terrain.api.descriptor.AutoSplatConfig;
import org.dynamisengine.terrain.api.descriptor.FoliageDesc;
import org.dynamisengine.terrain.api.descriptor.FoliageLayer;
import org.dynamisengine.terrain.api.descriptor.HeightmapDesc;
import org.dynamisengine.terrain.api.descriptor.HeightmapFormat;
import org.dynamisengine.terrain.api.descriptor.RoadDesc;
import org.dynamisengine.terrain.api.descriptor.SplatmapDesc;
import org.dynamisengine.terrain.api.descriptor.SplatmapMode;
import org.dynamisengine.terrain.api.descriptor.TerrainDescriptor;
import org.dynamisengine.terrain.api.descriptor.TerrainLodDesc;
import org.dynamisengine.terrain.api.descriptor.TerrainMaterial;
import org.dynamisengine.terrain.api.descriptor.TessellationMode;
import org.dynamisengine.terrain.api.descriptor.WaterDesc;
import org.dynamisengine.terrain.api.descriptor.WaterMode;
import org.junit.jupiter.api.Test;

class TerrainDescriptorBuilderTest {
    @Test
    void missingIdThrowsIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> TerrainDescriptorBuilder.create().heightmap(heightmap()).build());
    }

    @Test
    void missingHeightmapThrowsIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> TerrainDescriptorBuilder.create().id("id").build());
    }

    @Test
    void minimalDescriptorBuildsSuccessfully() {
        final TerrainDescriptor d = TerrainDescriptorBuilder.create().id("t0").heightmap(heightmap()).build();
        assertNotNull(d);
    }

    @Test
    void defaultsApplied() {
        final TerrainDescriptor d = TerrainDescriptorBuilder.create().id("t0").heightmap(heightmap()).build();
        assertEquals(1.0f, d.worldScale(), 0.0f);
        assertEquals(800.0f, d.heightScale(), 0.0f);
        assertEquals(false, d.deformable());
    }

    @Test
    void fullDescriptorBuildsSuccessfully() {
        final SplatmapDesc splat = SplatmapDescBuilder.create()
            .mode(SplatmapMode.LAYERS_4)
            .splatmap0Path("s0")
            .material(new TerrainMaterial("grass", "a", "n", "o", 1f, 0f, false, false))
            .build();

        final FoliageDesc foliage = FoliageDescBuilder.create()
            .worldSeed(42L)
            .layer(new FoliageLayer("tree", 0.5f, 0f, 45f, 0f, 1000f, 0.3f))
            .build();

        final WaterDesc water = WaterDescBuilder.create().mode(WaterMode.PLANAR).elevation(0f).build();

        final TerrainDescriptor d = TerrainDescriptorBuilder.create()
            .id("t1")
            .heightmap(heightmap())
            .splatmap(splat)
            .foliage(foliage)
            .water(water)
            .roads(new RoadDesc(true, 6f, 1f, 0.5f))
            .lod(new TerrainLodDesc(6, TessellationMode.COMPUTE, 2f, 65, 0.6f, 0.9f))
            .autoSplat(new AutoSplatConfig(true, 1f, 1f, 1f))
            .worldScale(2f)
            .heightScale(900f)
            .deformable(true)
            .build();

        assertNotNull(d.splatmap());
        assertNotNull(d.foliage());
        assertNotNull(d.water());
        assertEquals(2f, d.worldScale(), 0.0f);
        assertEquals(900f, d.heightScale(), 0.0f);
        assertEquals(true, d.deformable());
    }

    private static HeightmapDesc heightmap() {
        return new HeightmapDesc(HeightmapFormat.R32F, "hm.raw", 64, 64);
    }
}
