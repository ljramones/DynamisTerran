package org.dynamisengine.terrain.vulkan.lod;

import org.dynamisengine.terrain.api.state.Vector3f;

public record CdlodSelectionUbo(
    float[] frustumPlanes,
    Vector3f cameraPos,
    float screenSpaceError,
    float morphStart,
    float morphEnd,
    int totalPatchCount
) {
}
