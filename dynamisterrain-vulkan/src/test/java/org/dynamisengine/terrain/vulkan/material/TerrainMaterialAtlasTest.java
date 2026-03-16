package org.dynamisengine.terrain.vulkan.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.dynamisengine.terrain.api.descriptor.TerrainMaterial;
import org.dynamisengine.terrain.vulkan.GpuBuffer;
import org.dynamisengine.terrain.vulkan.GpuImage2D;
import org.dynamisengine.terrain.vulkan.GpuMemoryOps;
import org.junit.jupiter.api.Test;

class TerrainMaterialAtlasTest {

    @Test
    void slotForReturnsCorrectIndexForValidMaterial() {
        final TerrainMaterialAtlas atlas = createAtlas(3);
        // material 0, albedo (type 0) -> slot 0
        assertEquals(0, atlas.slotFor(0, 0));
        // material 0, normal (type 1) -> slot 1
        assertEquals(1, atlas.slotFor(0, 1));
        // material 0, orm (type 2) -> slot 2
        assertEquals(2, atlas.slotFor(0, 2));
        // material 1, albedo -> slot 3
        assertEquals(3, atlas.slotFor(1, 0));
        // material 2, orm -> slot 8
        assertEquals(8, atlas.slotFor(2, 2));
    }

    @Test
    void slotForReturnsFollbackForOutOfBoundsMaterial() {
        final TerrainMaterialAtlas atlas = createAtlas(2);
        final int fallbackAlbedo = atlas.fallbackSlot(0);
        assertEquals(fallbackAlbedo, atlas.slotFor(2, 0));
        assertEquals(fallbackAlbedo, atlas.slotFor(-1, 0));
    }

    @Test
    void slotForReturnsFollbackForInvalidTextureType() {
        final TerrainMaterialAtlas atlas = createAtlas(2);
        final int fallbackAlbedo = atlas.fallbackSlot(0);
        assertEquals(fallbackAlbedo, atlas.slotFor(0, -1));
        assertEquals(fallbackAlbedo, atlas.slotFor(0, 3));
    }

    @Test
    void fallbackSlotDistinguishesTextureTypes() {
        final TerrainMaterialAtlas atlas = createAtlas(1);
        final int albedo = atlas.fallbackSlot(0);
        final int normal = atlas.fallbackSlot(1);
        final int orm = atlas.fallbackSlot(2);
        // All three fallback slots should be distinct
        assertTrue(albedo != normal);
        assertTrue(normal != orm);
        assertTrue(albedo != orm);
    }

    @Test
    void fallbackSlotsAreAfterMaterialSlots() {
        final TerrainMaterialAtlas atlas = createAtlas(4);
        // 4 materials * 3 textures = 12 material slots, fallbacks start at 12
        assertEquals(12, atlas.fallbackSlot(0));
        assertEquals(13, atlas.fallbackSlot(1));
        assertEquals(14, atlas.fallbackSlot(2));
    }

    @Test
    void materialCountMatchesInput() {
        assertEquals(0, createAtlas(0).materialCount());
        assertEquals(1, createAtlas(1).materialCount());
        assertEquals(5, createAtlas(5).materialCount());
    }

    @Test
    void createWithNullMaterialsProducesZeroCount() {
        final TerrainMaterialAtlas atlas = TerrainMaterialAtlas.create(
            0L, stubMemoryOps(), 0L, null, 0L);
        assertEquals(0, atlas.materialCount());
        // Fallback slots should still work
        assertNotNull(atlas);
        assertTrue(atlas.fallbackSlot(0) >= 0);
    }

    @Test
    void destroyReleasesAllImages() {
        final AtomicInteger destroyed = new AtomicInteger(0);
        final GpuMemoryOps ops = new GpuMemoryOps() {
            @Override
            public GpuImage2D createImage2D(int width, int height, int bytesPerPixel) {
                return new GpuImage2D(0L, width, height, bytesPerPixel);
            }

            @Override
            public GpuBuffer createBuffer(int sizeBytes) {
                return new GpuBuffer(0L, sizeBytes);
            }

            @Override
            public long createSampler() {
                return 0L;
            }

            @Override
            public void destroyImage(GpuImage2D image) {
                destroyed.incrementAndGet();
            }

            @Override
            public void destroyBuffer(GpuBuffer buffer) {
            }

            @Override
            public void destroySampler(long sampler) {
            }
        };
        final TerrainMaterialAtlas atlas = TerrainMaterialAtlas.create(
            0L, ops, 0L, dummyMaterials(2), 0L);
        atlas.destroy();
        // 2 materials * 3 textures + 3 fallbacks = 9 images
        assertEquals(9, destroyed.get());
    }

    @Test
    void fallbackSlotDefaultsToAlbedoForUnknownType() {
        final TerrainMaterialAtlas atlas = createAtlas(1);
        // textureType outside 1 and 2 should return albedo fallback
        assertEquals(atlas.fallbackSlot(0), atlas.fallbackSlot(99));
        assertEquals(atlas.fallbackSlot(0), atlas.fallbackSlot(-5));
    }

    private static TerrainMaterialAtlas createAtlas(final int materialCount) {
        return TerrainMaterialAtlas.create(
            0L, stubMemoryOps(), 0L, dummyMaterials(materialCount), 0L);
    }

    private static GpuMemoryOps stubMemoryOps() {
        return new GpuMemoryOps() {
            @Override
            public GpuImage2D createImage2D(int width, int height, int bytesPerPixel) {
                return new GpuImage2D(0L, width, height, bytesPerPixel);
            }

            @Override
            public GpuBuffer createBuffer(int sizeBytes) {
                return new GpuBuffer(0L, sizeBytes);
            }

            @Override
            public long createSampler() {
                return 0L;
            }

            @Override
            public void destroyImage(GpuImage2D image) {
            }

            @Override
            public void destroyBuffer(GpuBuffer buffer) {
            }

            @Override
            public void destroySampler(long sampler) {
            }
        };
    }

    private static List<TerrainMaterial> dummyMaterials(final int count) {
        final List<TerrainMaterial> materials = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            materials.add(new TerrainMaterial("mat_" + i, null, null, null, 1f, 0f, false, false));
        }
        return materials;
    }
}
