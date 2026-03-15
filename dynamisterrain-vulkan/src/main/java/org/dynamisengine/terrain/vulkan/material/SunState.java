package org.dynamisengine.terrain.vulkan.material;

import org.dynamisengine.terrain.api.state.Vector3f;

public record SunState(Vector3f direction) {
    public static final SunState NOON = new SunState(new Vector3f(0f, 1f, 0f));
}
