package org.dynamisterrain.vulkan.material;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.dynamisterrain.api.state.Vector3f;

public record WeatherUbo(
    float snowIntensity,
    float wetness,
    float rainIntensity,
    float windSpeed,
    Vector3f windDirection
) {
    public static final int SIZE_BYTES = 32;

    public static WeatherUbo fromState(final WeatherState weather) {
        return new WeatherUbo(
            weather.snowIntensity(),
            weather.wetness(),
            weather.rainIntensity(),
            weather.windSpeed(),
            weather.windDirection()
        );
    }

    public byte[] toBytes() {
        final ByteBuffer bb = ByteBuffer.allocate(SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN);
        bb.putFloat(this.snowIntensity);
        bb.putFloat(this.wetness);
        bb.putFloat(this.rainIntensity);
        bb.putFloat(this.windSpeed);
        bb.putFloat(this.windDirection.x());
        bb.putFloat(this.windDirection.y());
        bb.putFloat(this.windDirection.z());
        bb.putFloat(0f);
        return bb.array();
    }
}
