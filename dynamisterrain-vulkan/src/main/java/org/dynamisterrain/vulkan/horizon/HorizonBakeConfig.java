package org.dynamisterrain.vulkan.horizon;

public record HorizonBakeConfig(
    int searchRadius,
    float worldScale,
    float heightScale
) {
    public static HorizonBakeConfig defaults(final float worldScale, final float heightScale) {
        return new HorizonBakeConfig(128, worldScale, heightScale);
    }
}
