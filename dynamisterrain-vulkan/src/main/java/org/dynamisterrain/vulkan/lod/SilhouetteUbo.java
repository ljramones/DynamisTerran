package org.dynamisterrain.vulkan.lod;

import org.dynamisterrain.api.state.Vector3f;

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
