package org.dynamisengine.terrain.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.dynamisengine.terrain.api.config.MaterialTag;
import org.dynamisengine.terrain.api.descriptor.TerrainMaterial;
import org.dynamisengine.terrain.physics.material.TerrainMaterialResolver;
import org.junit.jupiter.api.Test;

class TerrainMaterialResolverTest {
    @Test
    void resolveReturnsDominantMaterial() {
        final byte[] splat = new byte[16 * 16 * 4];
        final int idx = (10 * 16 + 10) * 4;
        splat[idx] = (byte) 255;

        final List<TerrainMaterial> materials = List.of(
            new TerrainMaterial("grass", "", "", "", 1f, 0f, false, false),
            new TerrainMaterial("rock", "", "", "", 1f, 0f, false, false)
        );

        final MaterialTag tag = TerrainMaterialResolver.resolve(10f, 10f, splat, 1f, 16, materials);
        assertEquals(MaterialTag.GRASS, tag);
    }

    @Test
    void tagForMaterialIdMapsKnownIds() {
        assertEquals(MaterialTag.GRASS, TerrainMaterialResolver.tagForMaterialId("grass"));
        assertEquals(MaterialTag.ROCK, TerrainMaterialResolver.tagForMaterialId("rock"));
        assertEquals(MaterialTag.ASPHALT, TerrainMaterialResolver.tagForMaterialId("asphalt"));
    }

    @Test
    void tagForMaterialIdFallsBackToDirt() {
        assertEquals(MaterialTag.DIRT, TerrainMaterialResolver.tagForMaterialId("unknown_material_xyz"));
    }
}
