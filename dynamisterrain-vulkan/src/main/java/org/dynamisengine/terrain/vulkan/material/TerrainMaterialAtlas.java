package org.dynamisengine.terrain.vulkan.material;

import java.util.ArrayList;
import java.util.List;
import org.dynamisengine.terrain.api.descriptor.TerrainMaterial;
import org.dynamisengine.terrain.vulkan.GpuImage2D;
import org.dynamisengine.terrain.vulkan.GpuMemoryOps;

public final class TerrainMaterialAtlas {
    private static final int ALBEDO_FALLBACK_RGBA = 0xFFFFFFFF;
    private static final int NORMAL_FALLBACK_RGBA = 0xFF8080FF;
    private static final int ORM_FALLBACK_RGBA = 0xFFFFFFFF;

    private final GpuMemoryOps memoryOps;
    private final List<GpuImage2D> images;
    private final int materialCount;
    private final int fallbackAlbedoSlot;
    private final int fallbackNormalSlot;
    private final int fallbackOrmSlot;

    private TerrainMaterialAtlas(
        final GpuMemoryOps memoryOps,
        final List<GpuImage2D> images,
        final int materialCount,
        final int fallbackAlbedoSlot,
        final int fallbackNormalSlot,
        final int fallbackOrmSlot
    ) {
        this.memoryOps = memoryOps;
        this.images = images;
        this.materialCount = materialCount;
        this.fallbackAlbedoSlot = fallbackAlbedoSlot;
        this.fallbackNormalSlot = fallbackNormalSlot;
        this.fallbackOrmSlot = fallbackOrmSlot;
    }

    public static TerrainMaterialAtlas create(
        final long device,
        final GpuMemoryOps memoryOps,
        final long bindlessHeap,
        final List<TerrainMaterial> materials,
        final long commandBuffer
    ) {
        final List<TerrainMaterial> safeMaterials = materials == null ? List.of() : materials;
        final List<GpuImage2D> images = new ArrayList<>();

        for (int i = 0; i < safeMaterials.size(); i++) {
            images.add(createSolid(memoryOps, ALBEDO_FALLBACK_RGBA));
            images.add(createSolid(memoryOps, NORMAL_FALLBACK_RGBA));
            images.add(createSolid(memoryOps, ORM_FALLBACK_RGBA));
        }

        final int fallbackAlbedoSlot = images.size();
        images.add(createSolid(memoryOps, ALBEDO_FALLBACK_RGBA));
        final int fallbackNormalSlot = images.size();
        images.add(createSolid(memoryOps, NORMAL_FALLBACK_RGBA));
        final int fallbackOrmSlot = images.size();
        images.add(createSolid(memoryOps, ORM_FALLBACK_RGBA));

        return new TerrainMaterialAtlas(
            memoryOps,
            images,
            safeMaterials.size(),
            fallbackAlbedoSlot,
            fallbackNormalSlot,
            fallbackOrmSlot
        );
    }

    public int slotFor(final int materialIndex, final int textureType) {
        if (textureType < 0 || textureType > 2 || materialIndex < 0 || materialIndex >= this.materialCount) {
            return this.fallbackSlot(textureType);
        }
        return materialIndex * 3 + textureType;
    }

    public int fallbackSlot(final int textureType) {
        if (textureType == 1) {
            return this.fallbackNormalSlot;
        }
        if (textureType == 2) {
            return this.fallbackOrmSlot;
        }
        return this.fallbackAlbedoSlot;
    }

    public int materialCount() {
        return this.materialCount;
    }

    public void destroy() {
        for (GpuImage2D image : this.images) {
            this.memoryOps.destroyImage(image);
        }
        this.images.clear();
    }

    private static GpuImage2D createSolid(final GpuMemoryOps memoryOps, final int rgba) {
        final GpuImage2D image = memoryOps.createImage2D(1, 1, 4);
        image.upload(new byte[] {
            (byte) ((rgba >>> 24) & 0xFF),
            (byte) ((rgba >>> 16) & 0xFF),
            (byte) ((rgba >>> 8) & 0xFF),
            (byte) (rgba & 0xFF)
        });
        return image;
    }
}
