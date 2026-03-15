package org.dynamisengine.terrain.api.descriptor;

public record TerrainDescriptor(
    String id,
    HeightmapDesc heightmap,
    SplatmapDesc splatmap,
    FoliageDesc foliage,
    WaterDesc water,
    RoadDesc road,
    TerrainLodDesc lod,
    ProceduralDesc procedural,
    AutoSplatConfig autoSplat,
    float worldScale,
    float heightScale,
    boolean deformable
) {
}
