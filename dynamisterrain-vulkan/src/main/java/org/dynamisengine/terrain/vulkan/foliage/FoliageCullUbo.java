package org.dynamisengine.terrain.vulkan.foliage;

import org.dynamisengine.terrain.api.state.Vector3f;

public record FoliageCullUbo(
    Vector3f cameraPos,
    float maxDrawDistance,
    float hzbWidth,
    float hzbHeight,
    int totalInstances,
    int layerCount,
    float[] frustumPlanes
) {
}
