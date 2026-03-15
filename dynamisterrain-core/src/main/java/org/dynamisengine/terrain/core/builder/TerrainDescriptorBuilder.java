package org.dynamisengine.terrain.core.builder;

import org.dynamisengine.terrain.api.descriptor.AutoSplatConfig;
import org.dynamisengine.terrain.api.descriptor.FoliageDesc;
import org.dynamisengine.terrain.api.descriptor.HeightmapDesc;
import org.dynamisengine.terrain.api.descriptor.ProceduralDesc;
import org.dynamisengine.terrain.api.descriptor.RoadDesc;
import org.dynamisengine.terrain.api.descriptor.SplatmapDesc;
import org.dynamisengine.terrain.api.descriptor.TerrainDescriptor;
import org.dynamisengine.terrain.api.descriptor.TerrainLodDesc;
import org.dynamisengine.terrain.api.descriptor.WaterDesc;

public final class TerrainDescriptorBuilder {
    private String id;
    private HeightmapDesc heightmap;
    private SplatmapDesc splatmap;
    private FoliageDesc foliage;
    private WaterDesc water;
    private RoadDesc road;
    private TerrainLodDesc lod;
    private ProceduralDesc procedural;
    private AutoSplatConfig autoSplat;
    private float worldScale = 1.0f;
    private float heightScale = 800.0f;
    private boolean deformable = false;

    private TerrainDescriptorBuilder() {
    }

    public static TerrainDescriptorBuilder create() {
        return new TerrainDescriptorBuilder();
    }

    public TerrainDescriptorBuilder id(final String id) {
        this.id = id;
        return this;
    }

    public TerrainDescriptorBuilder heightmap(final HeightmapDesc hm) {
        this.heightmap = hm;
        return this;
    }

    public TerrainDescriptorBuilder splatmap(final SplatmapDesc s) {
        this.splatmap = s;
        return this;
    }

    public TerrainDescriptorBuilder foliage(final FoliageDesc f) {
        this.foliage = f;
        return this;
    }

    public TerrainDescriptorBuilder water(final WaterDesc w) {
        this.water = w;
        return this;
    }

    public TerrainDescriptorBuilder roads(final RoadDesc r) {
        this.road = r;
        return this;
    }

    public TerrainDescriptorBuilder lod(final TerrainLodDesc l) {
        this.lod = l;
        return this;
    }

    public TerrainDescriptorBuilder worldScale(final float v) {
        this.worldScale = v;
        return this;
    }

    public TerrainDescriptorBuilder heightScale(final float v) {
        this.heightScale = v;
        return this;
    }

    public TerrainDescriptorBuilder deformable(final boolean v) {
        this.deformable = v;
        return this;
    }

    public TerrainDescriptorBuilder procedural(final ProceduralDesc p) {
        this.procedural = p;
        return this;
    }

    public TerrainDescriptorBuilder autoSplat(final AutoSplatConfig c) {
        this.autoSplat = c;
        return this;
    }

    public TerrainDescriptor build() {
        if (this.id == null || this.id.isBlank()) {
            throw new IllegalStateException("id is required");
        }
        if (this.heightmap == null) {
            throw new IllegalStateException("heightmap is required");
        }
        return new TerrainDescriptor(
            this.id,
            this.heightmap,
            this.splatmap,
            this.foliage,
            this.water,
            this.road,
            this.lod,
            this.procedural,
            this.autoSplat,
            this.worldScale,
            this.heightScale,
            this.deformable
        );
    }
}
