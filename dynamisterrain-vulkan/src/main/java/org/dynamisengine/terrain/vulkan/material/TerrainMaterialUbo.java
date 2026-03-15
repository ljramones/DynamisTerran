package org.dynamisengine.terrain.vulkan.material;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.dynamisengine.terrain.api.state.Vector3f;

public record TerrainMaterialUbo(
    float waterElevation,
    float wickingRange,
    int splatmapMode,
    int materialCount,
    float terrainSizeX,
    float terrainSizeZ,
    float heightScale,
    float worldScale,
    Vector3f sunDirection,
    float farPlane,
    float nearPlane
) {
    public static final int SIZE_BYTES = 64;

    public static TerrainMaterialUbo defaults() {
        return new TerrainMaterialUbo(
            0f,
            0.3f,
            4,
            1,
            1f,
            1f,
            1f,
            1f,
            new Vector3f(0f, 1f, 0f),
            10_000f,
            0.1f
        );
    }

    public byte[] toBytes() {
        final ByteBuffer bb = ByteBuffer.allocate(SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN);
        bb.putFloat(this.waterElevation);
        bb.putFloat(this.wickingRange);
        bb.putInt(this.splatmapMode);
        bb.putInt(this.materialCount);
        bb.putFloat(this.terrainSizeX);
        bb.putFloat(this.terrainSizeZ);
        bb.putFloat(this.heightScale);
        bb.putFloat(this.worldScale);
        bb.putFloat(this.sunDirection.x());
        bb.putFloat(this.sunDirection.y());
        bb.putFloat(this.sunDirection.z());
        bb.putFloat(0f);
        bb.putFloat(this.farPlane);
        bb.putFloat(this.nearPlane);
        bb.putFloat(0f);
        bb.putFloat(0f);
        return bb.array();
    }
}
