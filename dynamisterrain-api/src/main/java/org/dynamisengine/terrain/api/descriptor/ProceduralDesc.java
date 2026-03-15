package org.dynamisengine.terrain.api.descriptor;

import java.util.List;

public record ProceduralDesc(
    long seed,
    int octaves,
    float frequency,
    int erosionPasses,
    int thermalErosionPasses,
    List<HeightStamp> stamps
) {
}
