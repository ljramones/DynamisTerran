package org.dynamisterrain.meshforge.road;

public record GeneratedRoad(
    float[] vertices,
    int[] indices,
    int vertexCount,
    int indexCount,
    float[] splatmaskData,
    int splatmaskWidth,
    int splatmaskHeight
) {
}
