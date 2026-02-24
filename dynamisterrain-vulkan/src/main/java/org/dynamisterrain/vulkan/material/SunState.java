package org.dynamisterrain.vulkan.material;

import org.dynamisterrain.api.state.Vector3f;

public record SunState(Vector3f direction) {
    public static final SunState NOON = new SunState(new Vector3f(0f, 1f, 0f));
}
