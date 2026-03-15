package org.dynamisengine.terrain.api.state;

public record TerrainFrameContext(
    long commandBuffer,
    CameraState cameraState,
    float deltaSeconds,
    int frameIndex
) {
}
