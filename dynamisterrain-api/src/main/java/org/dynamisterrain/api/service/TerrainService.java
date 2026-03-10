package org.dynamisterrain.api.service;

import org.dynamisterrain.api.config.MaterialTag;
import org.dynamisterrain.api.descriptor.HeightStamp;
import org.dynamisterrain.api.descriptor.TerrainDescriptor;
import org.dynamisterrain.api.event.HeightDeformEvent;
import org.dynamisterrain.api.event.MaterialPaintEvent;
import org.dynamisterrain.api.gpu.TerrainGpuResources;
import org.dynamisterrain.api.state.TerrainFrameContext;
import org.dynamisterrain.api.state.TerrainHandle;
import org.dynamisterrain.api.state.TerrainStats;
import org.dynamisterrain.api.state.Vector3f;

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
     * Typed Terrain/Sky seam for new integration code.
     */
    default void setSkyStateSource(TerrainSkyStateSource skyStateSource) {
        setSkySource(skyStateSource);
    }

    /**
     * Legacy weakly typed compatibility path. Prefer {@link #setSkyStateSource(TerrainSkyStateSource)}.
     */
    @Deprecated(since = "0.1.0")
    void setSkySource(Object skySource);

    TerrainStats getStats();

    TerrainGpuResources getGpuResources();
}
