package org.dynamisengine.terrain.core.builder;

import java.util.ArrayList;
import java.util.List;
import org.dynamisengine.terrain.api.descriptor.SplatmapDesc;
import org.dynamisengine.terrain.api.descriptor.SplatmapMode;
import org.dynamisengine.terrain.api.descriptor.TerrainMaterial;

public final class SplatmapDescBuilder {
    private SplatmapMode mode = SplatmapMode.LAYERS_4;
    private String splatmap0Path;
    private String splatmap1Path;
    private final List<TerrainMaterial> materials = new ArrayList<>();

    private SplatmapDescBuilder() {
    }

    public static SplatmapDescBuilder create() {
        return new SplatmapDescBuilder();
    }

    public SplatmapDescBuilder mode(final SplatmapMode mode) {
        this.mode = mode;
        return this;
    }

    public SplatmapDescBuilder splatmap0Path(final String path) {
        this.splatmap0Path = path;
        return this;
    }

    public SplatmapDescBuilder splatmap1Path(final String path) {
        this.splatmap1Path = path;
        return this;
    }

    public SplatmapDescBuilder material(final TerrainMaterial material) {
        this.materials.add(material);
        return this;
    }

    public SplatmapDesc build() {
        return new SplatmapDesc(this.mode, this.splatmap0Path, this.splatmap1Path, List.copyOf(this.materials));
    }
}
