package org.dynamisengine.terrain.api.descriptor;

public record HeightStamp(
    String path,
    BlendMode blendMode,
    float strength
) {
}
