package org.dynamisterrain.api.event;

public record MaterialPaintEvent(
    float centerX,
    float centerZ,
    float radius,
    int layerIndex,
    float strength,
    float falloff
) {
}
