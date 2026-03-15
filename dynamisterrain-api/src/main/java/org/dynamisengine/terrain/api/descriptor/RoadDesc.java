package org.dynamisengine.terrain.api.descriptor;

public record RoadDesc(
    boolean enabled,
    float width,
    float shoulderWidth,
    float blendStrength
) {
}
