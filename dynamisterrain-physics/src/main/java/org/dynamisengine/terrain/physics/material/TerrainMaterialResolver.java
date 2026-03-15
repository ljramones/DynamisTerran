package org.dynamisengine.terrain.physics.material;

import java.util.List;
import org.dynamisengine.terrain.api.config.MaterialTag;
import org.dynamisengine.terrain.api.descriptor.TerrainMaterial;

public final class TerrainMaterialResolver {
    private TerrainMaterialResolver() {
    }

    public static MaterialTag resolve(
        final float worldX,
        final float worldZ,
        final byte[] splatmap0Data,
        final float worldScale,
        final int splatmapWidth,
        final List<TerrainMaterial> materials
    ) {
        if (splatmap0Data == null || splatmap0Data.length == 0 || materials == null || materials.isEmpty() || splatmapWidth <= 0) {
            return MaterialTag.DIRT;
        }

        final int pixelCount = splatmap0Data.length / 4;
        final int splatmapHeight = Math.max(1, pixelCount / splatmapWidth);
        final int tx = clamp((int) (worldX / Math.max(worldScale, 0.0001f)), 0, splatmapWidth - 1);
        final int tz = clamp((int) (worldZ / Math.max(worldScale, 0.0001f)), 0, splatmapHeight - 1);
        final int idx = (tz * splatmapWidth + tx) * 4;

        int dominantChannel = 0;
        int max = channel(splatmap0Data, idx);
        for (int c = 1; c < 4; c++) {
            final int v = channel(splatmap0Data, idx + c);
            if (v > max) {
                max = v;
                dominantChannel = c;
            }
        }

        final int materialIndex = Math.min(dominantChannel, materials.size() - 1);
        return tagForMaterialId(materials.get(materialIndex).id());
    }

    public static MaterialTag tagForMaterialId(final String materialId) {
        if (materialId == null) {
            return MaterialTag.DIRT;
        }
        return switch (materialId.toLowerCase()) {
            case "grass", "grass_clump" -> MaterialTag.GRASS;
            case "rock", "cliff" -> MaterialTag.ROCK;
            case "dirt", "ground" -> MaterialTag.DIRT;
            case "sand", "beach" -> MaterialTag.SAND;
            case "snow" -> MaterialTag.SNOW;
            case "mud" -> MaterialTag.MUD;
            case "asphalt", "road" -> MaterialTag.ASPHALT;
            case "wood" -> MaterialTag.WOOD;
            case "metal" -> MaterialTag.METAL;
            case "water" -> MaterialTag.WATER;
            default -> MaterialTag.DIRT;
        };
    }

    private static int channel(final byte[] splatmap0Data, final int index) {
        if (index < 0 || index >= splatmap0Data.length) {
            return 0;
        }
        return splatmap0Data[index] & 0xFF;
    }

    private static int clamp(final int v, final int min, final int max) {
        return Math.max(min, Math.min(max, v));
    }
}
