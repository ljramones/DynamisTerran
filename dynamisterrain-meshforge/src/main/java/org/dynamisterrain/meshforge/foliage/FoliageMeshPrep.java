package org.dynamisterrain.meshforge.foliage;

import java.util.ArrayList;
import java.util.List;

public final class FoliageMeshPrep {
    private FoliageMeshPrep() {
    }

    public static MeshLodSet prepare(final String meshPath, final int lodCount, final float billboardDistance) {
        final int count = Math.max(1, lodCount);
        final List<float[]> lodVertices = new ArrayList<>();
        final List<int[]> lodIndices = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            final float scale = 1f / (i + 1f);
            lodVertices.add(new float[] {
                -0.5f * scale, 0f, 0f,
                 0.5f * scale, 0f, 0f,
                 0f, 1.0f * scale, 0f
            });
            lodIndices.add(new int[] {0, 1, 2});
        }

        final float[] billboardVertices = new float[] {
            -0.5f, 0f, 0f, 0f, 0f,
             0.5f, 0f, 0f, 1f, 0f,
            -0.5f, 1f, 0f, 0f, 1f,
             0.5f, 1f, 0f, 1f, 1f
        };
        final int[] billboardIndices = new int[] {0, 1, 2, 2, 1, 3};

        return new MeshLodSet(List.copyOf(lodVertices), List.copyOf(lodIndices), count, billboardVertices, billboardIndices);
    }
}
