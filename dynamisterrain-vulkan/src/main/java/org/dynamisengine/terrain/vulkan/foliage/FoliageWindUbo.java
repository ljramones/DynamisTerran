package org.dynamisengine.terrain.vulkan.foliage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.dynamisengine.terrain.api.state.Vector3f;
import org.dynamisengine.terrain.vulkan.material.WeatherState;

public record FoliageWindUbo(
    Vector3f windDirection,
    float windSpeed,
    float gameTime,
    float windStrength
) {
    public static final int SIZE_BYTES = 32;

    public static FoliageWindUbo fromWeather(final WeatherState weather, final float gameTime, final float windStrength) {
        final WeatherState safe = weather == null ? WeatherState.CLEAR : weather;
        return new FoliageWindUbo(safe.windDirection(), safe.windSpeed(), gameTime, windStrength);
    }

    public byte[] toBytes() {
        final ByteBuffer bb = ByteBuffer.allocate(SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN);
        bb.putFloat(this.windDirection.x());
        bb.putFloat(this.windDirection.y());
        bb.putFloat(this.windDirection.z());
        bb.putFloat(0f);
        bb.putFloat(this.windSpeed);
        bb.putFloat(this.gameTime);
        bb.putFloat(this.windStrength);
        bb.putFloat(0f);
        return bb.array();
    }
}
