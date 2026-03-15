package org.dynamisengine.terrain.api.descriptor;

import java.util.List;

public record FoliageDesc(
    long worldSeed,
    float maxDrawDistance,
    boolean windEnabled,
    List<FoliageLayer> layers
) {
}
