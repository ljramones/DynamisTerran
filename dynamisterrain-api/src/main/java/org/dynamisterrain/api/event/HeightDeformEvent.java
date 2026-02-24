package org.dynamisterrain.api.event;

public record HeightDeformEvent(
    float centerX,
    float centerZ,
    float radius,
    float depth,
    DeformShape shape
) {
}
