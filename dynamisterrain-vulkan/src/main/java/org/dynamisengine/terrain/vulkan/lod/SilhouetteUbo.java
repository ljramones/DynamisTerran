package org.dynamisengine.terrain.vulkan.lod;

import org.dynamisengine.terrain.api.state.Vector3f;

public record SilhouetteUbo(
    float viewportWidth,
    float viewportHeight,
    float silhouetteThreshold,
    float maxSubdivMultiplier,
    Vector3f cameraPos,
    float nearPlane,
    float farPlane
) {
}
