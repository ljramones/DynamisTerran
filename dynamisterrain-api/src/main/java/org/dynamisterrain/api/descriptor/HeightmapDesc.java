package org.dynamisterrain.api.descriptor;

public record HeightmapDesc(
    HeightmapFormat format,
    String path,
    int width,
    int height
) {
}
