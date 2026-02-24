package org.dynamisterrain.api.config;

import org.dynamisterrain.api.state.Vector3f;

public record AtmosphereLink(
    boolean useSkySource,
    Vector3f manualSunDirection
) {
}
