package org.dynamisengine.terrain.api.config;

import org.dynamisengine.terrain.api.state.Vector3f;

public record AtmosphereLink(
    boolean useSkySource,
    Vector3f manualSunDirection
) {
}
