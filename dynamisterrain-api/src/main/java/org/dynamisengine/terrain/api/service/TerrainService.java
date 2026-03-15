package org.dynamisengine.terrain.api.service;

import org.dynamisengine.terrain.api.config.MaterialTag;
import org.dynamisengine.terrain.api.descriptor.HeightStamp;
import org.dynamisengine.terrain.api.descriptor.TerrainDescriptor;
import org.dynamisengine.terrain.api.event.HeightDeformEvent;
import org.dynamisengine.terrain.api.event.MaterialPaintEvent;
import org.dynamisengine.terrain.api.gpu.TerrainGpuResources;
import org.dynamisengine.terrain.api.state.TerrainFrameContext;
import org.dynamisengine.terrain.api.state.TerrainHandle;
import org.dynamisengine.terrain.api.state.TerrainStats;
import org.dynamisengine.terrain.api.state.Vector3f;

public interface TerrainService {
    TerrainHandle loadTile(TerrainDescriptor descriptor);

    void unloadTile(String tileId);

    TerrainHandle getActiveTile();

    void update(TerrainFrameContext frameContext);

    void recordTerrain(TerrainFrameContext frameContext);

    void recordFoliage(TerrainFrameContext frameContext);

    void recordWater(TerrainFrameContext frameContext);

    float heightAt(float worldX, float worldZ);

    Vector3f normalAt(float worldX, float worldZ);

    MaterialTag materialAt(float worldX, float worldZ);

    void deformHeight(HeightDeformEvent event);

    void paintMaterial(MaterialPaintEvent event);

    void stampHeight(HeightStamp stamp, int centerX, int centerZ);

    /**
     * Typed Terrain/Sky seam (preferred path).
     */
    void setSkyStateSource(TerrainSkyStateSource skyStateSource);

    /**
     * Legacy weakly typed compatibility path.
     */
    @Deprecated(since = "0.1.0")
    default void setSkySource(Object skySource) {
        if (skySource instanceof TerrainSkyStateSource typedSource) {
            setSkyStateSource(typedSource);
        }
    }

    TerrainStats getStats();

    TerrainGpuResources getGpuResources();
}
