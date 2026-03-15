package org.dynamisengine.terrain.vulkan.material;

import org.dynamisengine.terrain.api.state.Vector3f;

public record WeatherState(
    float snowIntensity,
    float wetness,
    float rainIntensity,
    float windSpeed,
    Vector3f windDirection
) {
    public static final WeatherState CLEAR = new WeatherState(0f, 0f, 0f, 0f, new Vector3f(0f, 0f, 1f));
    public static final WeatherState HEAVY_RAIN = new WeatherState(0f, 1f, 1f, 14f, new Vector3f(1f, 0f, 0f));
}
