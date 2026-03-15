package org.dynamisengine.terrain.meshforge.road;

import org.dynamisengine.terrain.api.state.Vector3f;

public record SplinePoint(
    Vector3f position,
    Vector3f tangent,
    Vector3f normal,
    float arcLength
) {
}
