package org.dynamisterrain.test.harness;

public record WeatherState(
    float snowIntensity,
    float wetness,
    float rainIntensity,
    float windSpeed
) {
    public static final WeatherState CLEAR = new WeatherState(0f, 0f, 0f, 0f);
}
