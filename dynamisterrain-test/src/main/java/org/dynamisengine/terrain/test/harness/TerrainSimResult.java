package org.dynamisengine.terrain.test.harness;

import java.util.List;
import org.dynamisengine.terrain.api.config.MaterialTag;
import org.dynamisengine.terrain.api.event.SurfaceContactEvent;
import org.dynamisengine.terrain.api.state.Vector3f;

public record TerrainSimResult(
    List<Vector3f> cameraPositions,
    List<Float> heightSamples,
    List<MaterialTag> materialSamples,
    List<SurfaceContactEvent> allContacts,
    int totalSteps,
    int totalUpdateCalls
) {
}
