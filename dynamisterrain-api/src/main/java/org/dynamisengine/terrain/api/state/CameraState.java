package org.dynamisengine.terrain.api.state;

public record CameraState(
    Vector3f position,
    float nearPlane,
    float farPlane,
    Vector3f frustumTL,
    Vector3f frustumTR,
    Vector3f frustumBL
) {
}
