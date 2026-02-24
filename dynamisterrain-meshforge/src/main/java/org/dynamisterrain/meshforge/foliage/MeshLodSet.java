package org.dynamisterrain.meshforge.foliage;

import java.util.List;

public record MeshLodSet(
    List<float[]> lodVertices,
    List<int[]> lodIndices,
    int lodCount,
    float[] billboardVertices,
    int[] billboardIndices
) {
}
