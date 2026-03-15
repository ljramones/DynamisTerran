package org.dynamisengine.terrain.core.scatter;

import java.util.List;

public record ScatterResult(
    List<ScatterPoint> points,
    int count,
    int layerIndex
) {
}
