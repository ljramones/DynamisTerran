package org.dynamisterrain.meshforge.road;

import org.dynamisterrain.api.state.Vector3f;

public record SplinePoint(
    Vector3f position,
    Vector3f tangent,
    Vector3f normal,
    float arcLength
) {
}
