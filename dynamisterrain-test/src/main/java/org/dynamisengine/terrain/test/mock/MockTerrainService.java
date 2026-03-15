package org.dynamisengine.terrain.test.mock;

import java.util.ArrayList;
import java.util.List;
import org.dynamisengine.terrain.api.config.MaterialTag;
import org.dynamisengine.terrain.api.descriptor.BlendMode;
import org.dynamisengine.terrain.api.descriptor.HeightStamp;
import org.dynamisengine.terrain.api.descriptor.TerrainDescriptor;
import org.dynamisengine.terrain.api.event.HeightDeformEvent;
import org.dynamisengine.terrain.api.event.MaterialPaintEvent;
import org.dynamisengine.terrain.api.event.SurfaceContactEvent;
import org.dynamisengine.terrain.api.gpu.TerrainGpuResources;
import org.dynamisengine.terrain.api.service.TerrainService;
import org.dynamisengine.terrain.api.service.TerrainSkyStateSource;
import org.dynamisengine.terrain.api.state.TerrainFrameContext;
import org.dynamisengine.terrain.api.state.TerrainHandle;
import org.dynamisengine.terrain.api.state.TerrainRenderPhase;
import org.dynamisengine.terrain.api.state.TerrainStats;
import org.dynamisengine.terrain.api.state.Vector3f;
import org.dynamisengine.terrain.core.heightmap.HeightmapData;
import org.dynamisengine.terrain.core.heightmap.HeightmapOps;
import org.dynamisengine.terrain.test.synthetic.SyntheticHeightmapFactory;

public final class MockTerrainService implements TerrainService {
    private HeightmapData heightmap;
    private MaterialTag defaultMaterial = MaterialTag.GRASS;

    private int updateCallCount;
    private int recordTerrainCallCount;
    private int recordFoliageCallCount;
    private int recordWaterCallCount;

    private TerrainHandle active = new TerrainHandle("mock", false, TerrainRenderPhase.NOT_STARTED);

    private final List<HeightDeformEvent> deformHistory = new ArrayList<>();
    private final List<SurfaceContactEvent> pendingContacts = new ArrayList<>();

    private MockTerrainService(final HeightmapData heightmap) {
        this.heightmap = heightmap;
    }

    public static MockTerrainService flat(final int size) {
        return new MockTerrainService(SyntheticHeightmapFactory.flat(size, 0f));
    }

    public static MockTerrainService hill(final int size, final float peakHeight) {
        return new MockTerrainService(SyntheticHeightmapFactory.hill(size, peakHeight));
    }

    public static MockTerrainService valley(final int size, final float depth) {
        return new MockTerrainService(SyntheticHeightmapFactory.valley(size, depth));
    }

    public static MockTerrainService procedural(final int size, final long seed) {
        final float amplitude = 20f + (seed % 7);
        return new MockTerrainService(SyntheticHeightmapFactory.sinWave(size, amplitude, 0.08f));
    }

    public void injectContact(final SurfaceContactEvent event) {
        this.pendingContacts.add(event);
    }

    public List<SurfaceContactEvent> drainContactEvents() {
        final List<SurfaceContactEvent> out = List.copyOf(this.pendingContacts);
        this.pendingContacts.clear();
        return out;
    }

    @Override
    public TerrainHandle loadTile(final TerrainDescriptor descriptor) {
        this.active = new TerrainHandle(descriptor.id(), true, TerrainRenderPhase.NOT_STARTED);
        return this.active;
    }

    @Override
    public void unloadTile(final String tileId) {
        this.active = new TerrainHandle(tileId, false, TerrainRenderPhase.NOT_STARTED);
    }

    @Override
    public TerrainHandle getActiveTile() {
        return this.active;
    }

    @Override
    public void update(final TerrainFrameContext frameContext) {
        this.updateCallCount++;
        this.active = new TerrainHandle(this.active.id(), this.active.isLoaded(), TerrainRenderPhase.UPDATE_COMPLETE);
    }

    @Override
    public void recordTerrain(final TerrainFrameContext frameContext) {
        this.recordTerrainCallCount++;
        this.active = new TerrainHandle(this.active.id(), this.active.isLoaded(), TerrainRenderPhase.TERRAIN_COMPLETE);
    }

    @Override
    public void recordFoliage(final TerrainFrameContext frameContext) {
        this.recordFoliageCallCount++;
        this.active = new TerrainHandle(this.active.id(), this.active.isLoaded(), TerrainRenderPhase.FOLIAGE_COMPLETE);
    }

    @Override
    public void recordWater(final TerrainFrameContext frameContext) {
        this.recordWaterCallCount++;
        this.active = new TerrainHandle(this.active.id(), this.active.isLoaded(), TerrainRenderPhase.WATER_COMPLETE);
    }

    @Override
    public float heightAt(final float worldX, final float worldZ) {
        return HeightmapOps.heightAt(this.heightmap, worldX, worldZ, 1f, 100f);
    }

    @Override
    public Vector3f normalAt(final float worldX, final float worldZ) {
        final float eps = 1f;
        final float hL = heightAt(worldX - eps, worldZ);
        final float hR = heightAt(worldX + eps, worldZ);
        final float hD = heightAt(worldX, worldZ - eps);
        final float hU = heightAt(worldX, worldZ + eps);

        float nx = hL - hR;
        float ny = 2f;
        float nz = hD - hU;
        final float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0f) {
            nx /= len;
            ny /= len;
            nz /= len;
        }
        return new Vector3f(nx, ny, nz);
    }

    @Override
    public MaterialTag materialAt(final float worldX, final float worldZ) {
        return this.defaultMaterial;
    }

    @Override
    public void deformHeight(final HeightDeformEvent event) {
        this.deformHistory.add(event);
        HeightmapOps.deform(this.heightmap, event, 1f);
    }

    @Override
    public void paintMaterial(final MaterialPaintEvent event) {
        // no-op in mock
    }

    @Override
    public void stampHeight(final HeightStamp stamp, final int centerX, final int centerZ) {
        final HeightmapData one = HeightmapData.empty(1, 1);
        one.setPixel(0, 0, 1f);
        final BlendMode mode = stamp == null ? BlendMode.ADD : stamp.blendMode();
        final float strength = stamp == null ? 1f : stamp.strength();
        HeightmapOps.applyStamp(this.heightmap, one, mode, strength, centerX, centerZ);
    }

    @Override
    public void setSkyStateSource(final TerrainSkyStateSource skyStateSource) {
        // no-op in mock
    }

    
    public void setSkySource(final Object skySource) {
        TerrainService.super.setSkySource(skySource);
    }

    @Override
    public TerrainStats getStats() {
        return new TerrainStats(
            1,
            1,
            this.heightmap.width() * this.heightmap.height() * 2L,
            0,
            0f
        );
    }

    @Override
    public TerrainGpuResources getGpuResources() {
        return TerrainGpuResources.NULL;
    }

    public int updateCallCount() {
        return this.updateCallCount;
    }

    public int recordTerrainCallCount() {
        return this.recordTerrainCallCount;
    }

    public int recordFoliageCallCount() {
        return this.recordFoliageCallCount;
    }

    public int recordWaterCallCount() {
        return this.recordWaterCallCount;
    }

    public List<HeightDeformEvent> deformHistory() {
        return List.copyOf(this.deformHistory);
    }
}
