package org.dynamisengine.terrain.core.flow;

public record FlowConfig(
    int iterations,
    float slopeExponent,
    boolean normalizeOutput
) {
    public static FlowConfig defaults() {
        return new FlowConfig(3, 1.0f, true);
    }
}
