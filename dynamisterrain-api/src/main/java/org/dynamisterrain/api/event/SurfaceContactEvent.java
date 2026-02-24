package org.dynamisterrain.api.event;

import org.dynamisterrain.api.config.MaterialTag;
import org.dynamisterrain.api.state.Vector3f;

public record SurfaceContactEvent(
    MaterialTag material,
    Vector3f position,
    Vector3f normal,
    Vector3f velocity,
    ContactType type
) {
}
