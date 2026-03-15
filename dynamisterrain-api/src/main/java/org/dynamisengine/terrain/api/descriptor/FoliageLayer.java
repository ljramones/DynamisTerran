package org.dynamisengine.terrain.api.descriptor;

public record FoliageLayer(
    String meshId,
    float density,
    float minSlope,
    float maxSlope,
    float minAlt,
    float maxAlt,
    float windStrength
) {
}
