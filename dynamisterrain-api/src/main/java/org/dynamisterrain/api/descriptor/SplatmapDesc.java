package org.dynamisterrain.api.descriptor;

import java.util.List;

public record SplatmapDesc(
    SplatmapMode mode,
    String splatmap0Path,
    String splatmap1Path,
    List<TerrainMaterial> materials
) {
}
