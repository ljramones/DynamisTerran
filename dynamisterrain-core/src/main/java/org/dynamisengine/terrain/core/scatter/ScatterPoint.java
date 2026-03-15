package org.dynamisengine.terrain.core.scatter;

public record ScatterPoint(
    float worldX,
    float worldY,
    float worldZ,
    float rotation,
    float scale
) {
}
