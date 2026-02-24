package org.dynamisterrain.test.harness;

import java.util.List;
import org.dynamisterrain.api.config.MaterialTag;
import org.dynamisterrain.api.event.SurfaceContactEvent;
import org.dynamisterrain.api.state.Vector3f;

public record TerrainSimResult(
    List<Vector3f> cameraPositions,
    List<Float> heightSamples,
    List<MaterialTag> materialSamples,
    List<SurfaceContactEvent> allContacts,
    int totalSteps,
    int totalUpdateCalls
) {
}
