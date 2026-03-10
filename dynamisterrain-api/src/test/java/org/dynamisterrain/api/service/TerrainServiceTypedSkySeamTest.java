package org.dynamisterrain.api.service;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.dynamisterrain.api.config.MaterialTag;
import org.dynamisterrain.api.descriptor.HeightStamp;
import org.dynamisterrain.api.descriptor.TerrainDescriptor;
import org.dynamisterrain.api.event.HeightDeformEvent;
import org.dynamisterrain.api.event.MaterialPaintEvent;
import org.dynamisterrain.api.gpu.TerrainGpuResources;
import org.dynamisterrain.api.state.TerrainFrameContext;
import org.dynamisterrain.api.state.TerrainHandle;
import org.dynamisterrain.api.state.TerrainRenderPhase;
import org.dynamisterrain.api.state.TerrainStats;
import org.dynamisterrain.api.state.Vector3f;
import org.junit.jupiter.api.Test;

class TerrainServiceTypedSkySeamTest {
    @Test
    void typedSkySourceUsesLegacyCompatibilityPath() {
        CapturingTerrainService service = new CapturingTerrainService();
        TerrainSkyStateSource source = () -> TerrainSkyState.defaultState();

        service.setSkyStateSource(source);

        assertInstanceOf(TerrainSkyStateSource.class, service.lastSkySource);
        assertSame(source, service.lastSkySource);
    }

    private static final class CapturingTerrainService implements TerrainService {
        private Object lastSkySource;

        @Override
        public TerrainHandle loadTile(TerrainDescriptor descriptor) {
            return new TerrainHandle("test", true, TerrainRenderPhase.NOT_STARTED);
        }

        @Override
        public void unloadTile(String tileId) {
        }

        @Override
        public TerrainHandle getActiveTile() {
            return new TerrainHandle("test", true, TerrainRenderPhase.NOT_STARTED);
        }

        @Override
        public void update(TerrainFrameContext frameContext) {
        }

        @Override
        public void recordTerrain(TerrainFrameContext frameContext) {
        }

        @Override
        public void recordFoliage(TerrainFrameContext frameContext) {
        }

        @Override
        public void recordWater(TerrainFrameContext frameContext) {
        }

        @Override
        public float heightAt(float worldX, float worldZ) {
            return 0;
        }

        @Override
        public Vector3f normalAt(float worldX, float worldZ) {
            return new Vector3f(0f, 1f, 0f);
        }

        @Override
        public MaterialTag materialAt(float worldX, float worldZ) {
            return MaterialTag.GRASS;
        }

        @Override
        public void deformHeight(HeightDeformEvent event) {
        }

        @Override
        public void paintMaterial(MaterialPaintEvent event) {
        }

        @Override
        public void stampHeight(HeightStamp stamp, int centerX, int centerZ) {
        }

        @Override
        public void setSkySource(Object skySource) {
            this.lastSkySource = skySource;
        }

        @Override
        public TerrainStats getStats() {
            return new TerrainStats(0, 0, 0, 0, 0f);
        }

        @Override
        public TerrainGpuResources getGpuResources() {
            return TerrainGpuResources.NULL;
        }
    }
}
