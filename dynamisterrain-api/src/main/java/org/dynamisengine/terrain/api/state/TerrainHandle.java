package org.dynamisengine.terrain.api.state;

import java.util.Objects;

public final class TerrainHandle {
    private final String id;
    private final boolean loaded;
    private final TerrainRenderPhase phase;

    public TerrainHandle(final String id, final boolean loaded, final TerrainRenderPhase phase) {
        this.id = Objects.requireNonNull(id, "id");
        this.loaded = loaded;
        this.phase = Objects.requireNonNull(phase, "phase");
    }

    public String id() {
        return this.id;
    }

    public boolean isLoaded() {
        return this.loaded;
    }

    public TerrainRenderPhase phase() {
        return this.phase;
    }
}
