package org.dynamisengine.terrain.api.descriptor;

public record TerrainMaterial(
    String id,
    String albedoPath,
    String normalPath,
    String ormPath,
    float tileScale,
    float parallaxDepth,
    boolean triplanar,
    boolean microPom
) {
}
