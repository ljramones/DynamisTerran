package org.dynamisengine.terrain.api.event;

import org.dynamisengine.terrain.api.config.MaterialTag;
import org.dynamisengine.terrain.api.state.Vector3f;

public record SurfaceContactEvent(
    MaterialTag material,
    Vector3f position,
    Vector3f normal,
    Vector3f velocity,
    ContactType type
) {
}
