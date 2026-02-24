package org.dynamisterrain.vulkan.water;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.dynamisterrain.api.descriptor.WaterDesc;
import org.dynamisterrain.api.state.Vector3f;

public record WaterUbo(
    float waterElevation,
    float foamDepthThreshold,
    float normalScrollSpeed,
    float normalTiling,
    Vector3f shallowColor,
    Vector3f deepColor,
    float gameTime,
    float terrainSizeX,
    float terrainSizeZ
) {
    public static final int SIZE_BYTES = 64;

    public static WaterUbo fromDesc(final WaterDesc waterDesc, final float gameTime, final float terrainSizeX, final float terrainSizeZ) {
        final WaterDesc safe = waterDesc == null ? new WaterDesc(null, 0f, 1.5f) : waterDesc;
        return new WaterUbo(
            safe.elevation(),
            safe.foamDepthThreshold(),
            0.02f,
            8.0f,
            new Vector3f(0.13f, 0.37f, 0.48f),
            new Vector3f(0.02f, 0.1f, 0.18f),
            gameTime,
            terrainSizeX,
            terrainSizeZ
        );
    }

    public byte[] toBytes() {
        final ByteBuffer bb = ByteBuffer.allocate(SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN);
        bb.putFloat(this.waterElevation);
        bb.putFloat(this.foamDepthThreshold);
        bb.putFloat(this.normalScrollSpeed);
        bb.putFloat(this.normalTiling);
        bb.putFloat(this.shallowColor.x());
        bb.putFloat(this.shallowColor.y());
        bb.putFloat(this.shallowColor.z());
        bb.putFloat(0f);
        bb.putFloat(this.deepColor.x());
        bb.putFloat(this.deepColor.y());
        bb.putFloat(this.deepColor.z());
        bb.putFloat(0f);
        bb.putFloat(this.gameTime);
        bb.putFloat(this.terrainSizeX);
        bb.putFloat(this.terrainSizeZ);
        bb.putFloat(0f);
        return bb.array();
    }
}
