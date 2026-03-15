package org.dynamisengine.terrain.core.builder;

import java.util.ArrayList;
import java.util.List;
import org.dynamisengine.terrain.api.descriptor.FoliageDesc;
import org.dynamisengine.terrain.api.descriptor.FoliageLayer;

public final class FoliageDescBuilder {
    private long worldSeed = 0L;
    private float maxDrawDistance = 500.0f;
    private boolean windEnabled = true;
    private final List<FoliageLayer> layers = new ArrayList<>();

    private FoliageDescBuilder() {
    }

    public static FoliageDescBuilder create() {
        return new FoliageDescBuilder();
    }

    public FoliageDescBuilder worldSeed(final long worldSeed) {
        this.worldSeed = worldSeed;
        return this;
    }

    public FoliageDescBuilder maxDrawDistance(final float maxDrawDistance) {
        this.maxDrawDistance = maxDrawDistance;
        return this;
    }

    public FoliageDescBuilder windEnabled(final boolean windEnabled) {
        this.windEnabled = windEnabled;
        return this;
    }

    public FoliageDescBuilder layer(final FoliageLayer layer) {
        this.layers.add(layer);
        return this;
    }

    public FoliageDesc build() {
        return new FoliageDesc(this.worldSeed, this.maxDrawDistance, this.windEnabled, List.copyOf(this.layers));
    }
}
