package org.dynamisengine.terrain.core.builder;

import org.dynamisengine.terrain.api.descriptor.WaterDesc;
import org.dynamisengine.terrain.api.descriptor.WaterMode;

public final class WaterDescBuilder {
    private WaterMode mode = WaterMode.NONE;
    private float elevation = 0f;
    private float foamDepthThreshold = 1.5f;

    private WaterDescBuilder() {
    }

    public static WaterDescBuilder create() {
        return new WaterDescBuilder();
    }

    public WaterDescBuilder mode(final WaterMode mode) {
        this.mode = mode;
        return this;
    }

    public WaterDescBuilder elevation(final float elevation) {
        this.elevation = elevation;
        return this;
    }

    public WaterDescBuilder foamDepthThreshold(final float foamDepthThreshold) {
        this.foamDepthThreshold = foamDepthThreshold;
        return this;
    }

    public WaterDesc build() {
        return new WaterDesc(this.mode, this.elevation, this.foamDepthThreshold);
    }
}
