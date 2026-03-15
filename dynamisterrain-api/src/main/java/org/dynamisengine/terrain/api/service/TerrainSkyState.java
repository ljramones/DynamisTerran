package org.dynamisengine.terrain.api.service;

import org.dynamisengine.terrain.api.state.Vector3f;

/**
 * Typed sky state consumed by terrain-atmosphere linking.
 *
 * This intentionally carries only terrain-facing atmosphere inputs.
 */
public record TerrainSkyState(
        Vector3f sunDirection,
        float ambientIntensity
) {
    public TerrainSkyState {
        sunDirection = sunDirection == null ? new Vector3f(0f, 1f, 0f) : sunDirection;
        ambientIntensity = java.lang.Math.max(0f, ambientIntensity);
    }

    public static TerrainSkyState defaultState() {
        return new TerrainSkyState(new Vector3f(0f, 1f, 0f), 1f);
    }
}
