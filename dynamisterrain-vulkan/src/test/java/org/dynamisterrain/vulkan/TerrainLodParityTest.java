package org.dynamisterrain.vulkan;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import org.dynamisterrain.api.descriptor.AutoSplatConfig;
import org.dynamisterrain.api.descriptor.FoliageDesc;
import org.dynamisterrain.api.descriptor.FoliageLayer;
import org.dynamisterrain.api.descriptor.HeightmapDesc;
import org.dynamisterrain.api.descriptor.HeightmapFormat;
import org.dynamisterrain.api.descriptor.ProceduralDesc;
import org.dynamisterrain.api.descriptor.RoadDesc;
import org.dynamisterrain.api.descriptor.SplatmapDesc;
import org.dynamisterrain.api.descriptor.SplatmapMode;
import org.dynamisterrain.api.descriptor.TerrainDescriptor;
import org.dynamisterrain.api.descriptor.TerrainLodDesc;
import org.dynamisterrain.api.descriptor.TerrainMaterial;
import org.dynamisterrain.api.descriptor.TessellationMode;
import org.dynamisterrain.api.descriptor.WaterDesc;
import org.dynamisterrain.api.descriptor.WaterMode;
import org.dynamisterrain.api.state.Vector3f;
import org.dynamisterrain.core.heightmap.HeightmapData;
import org.dynamisterrain.core.lod.CdlodFrameResult;
import org.dynamisterrain.core.lod.CdlodQuadTree;
import org.dynamisterrain.core.lod.Frustum;
import org.dynamisterrain.vulkan.lod.CdlodSelectionUbo;
import org.dynamisterrain.vulkan.lod.CdlodTessellationUbo;
import org.dynamisterrain.vulkan.lod.TerrainGpuLodResources;
import org.dynamisterrain.vulkan.lod.TerrainLodPipeline;
import org.junit.jupiter.api.Test;

class TerrainLodParityTest {
    @Test
    void selectionPassProducesNonZeroVisibleCount() {
        final Fixture f = fixture();
        final TerrainLodPipeline pipeline = TerrainLodPipeline.create(1L, new InMemoryGpuMemoryOps());
        pipeline.compute(1L, f.lodResources, f.ctx, f.selectionUbo, f.tessUbo, f.totalPatchCount);
        assertTrue(f.lodResources.visibleCount() > 0);
        f.destroy();
    }

    @Test
    void tessellationPassProducesNonZeroVertexBuffer() {
        final Fixture f = fixture();
        final TerrainLodPipeline pipeline = TerrainLodPipeline.create(1L, new InMemoryGpuMemoryOps());
        pipeline.compute(1L, f.lodResources, f.ctx, f.selectionUbo, f.tessUbo, f.totalPatchCount);

        final ByteBuffer bb = ByteBuffer.wrap(f.lodResources.terrainVertexBytes()).order(ByteOrder.LITTLE_ENDIAN);
        boolean foundNonZero = false;
        final int floatsToScan = Math.min(bb.capacity() / 4, 256);
        for (int i = 0; i < floatsToScan; i++) {
            if (bb.getFloat(i * 4) != 0.0f) {
                foundNonZero = true;
                break;
            }
        }
        assertTrue(foundNonZero);
        f.destroy();
    }

    @Test
    void indirectDrawBufferPopulated() {
        final Fixture f = fixture();
        final TerrainLodPipeline pipeline = TerrainLodPipeline.create(1L, new InMemoryGpuMemoryOps());
        pipeline.compute(1L, f.lodResources, f.ctx, f.selectionUbo, f.tessUbo, f.totalPatchCount);

        final ByteBuffer bb = ByteBuffer.wrap(f.lodResources.indirectDrawBytes()).order(ByteOrder.LITTLE_ENDIAN);
        final int vertexCount = bb.getInt(0);
        assertTrue(vertexCount == 65 * 65);
        f.destroy();
    }

    @Test
    void morphFactorBufferInUnitRange() {
        final Fixture f = fixture();
        final TerrainLodPipeline pipeline = TerrainLodPipeline.create(1L, new InMemoryGpuMemoryOps());
        pipeline.compute(1L, f.lodResources, f.ctx, f.selectionUbo, f.tessUbo, f.totalPatchCount);

        for (float m : f.lodResources.morphFactors()) {
            assertTrue(m >= 0.0f && m <= 1.0f);
        }
        f.destroy();
    }

    @Test
    void barriersBetweenPassesProduceNoValidationErrors() {
        final Fixture f = fixture();
        final TerrainLodPipeline pipeline = TerrainLodPipeline.create(1L, new InMemoryGpuMemoryOps());
        assertDoesNotThrow(() -> pipeline.compute(1L, f.lodResources, f.ctx, f.selectionUbo, f.tessUbo, f.totalPatchCount));
        f.destroy();
    }

    @Test
    void computeIsDeterministic() {
        final Fixture f1 = fixture();
        final TerrainLodPipeline p1 = TerrainLodPipeline.create(1L, new InMemoryGpuMemoryOps());
        p1.compute(1L, f1.lodResources, f1.ctx, f1.selectionUbo, f1.tessUbo, f1.totalPatchCount);
        final byte[] a = f1.lodResources.terrainVertexBytes().clone();
        f1.destroy();

        final Fixture f2 = fixture();
        final TerrainLodPipeline p2 = TerrainLodPipeline.create(1L, new InMemoryGpuMemoryOps());
        p2.compute(1L, f2.lodResources, f2.ctx, f2.selectionUbo, f2.tessUbo, f2.totalPatchCount);
        final byte[] b = f2.lodResources.terrainVertexBytes().clone();
        f2.destroy();

        assertArrayEquals(a, b);
    }

    private static Fixture fixture() {
        final TerrainDescriptor descriptor = descriptor(256, 256);
        final TerrainGpuContext ctx = TerrainGpuContext.allocate(1L, new InMemoryGpuMemoryOps(), descriptor);
        ctx.uploadHeightmap(hill(256, 256), 1L);

        final CdlodQuadTree tree = CdlodQuadTree.build(256, 256, descriptor.lod(), ctx.heightmapData(), 1.0f, 1.0f);
        final CdlodFrameResult patches = tree.select(new Vector3f(128f, 200f, 128f), Frustum.infinite(), 2.0f);

        final int maxPatches = Math.max(1, patches.patchCount());
        final int maxVertices = Math.max(1, maxPatches * 4225);
        final TerrainGpuLodResources lodResources = TerrainGpuLodResources.allocate(1L, new InMemoryGpuMemoryOps(), maxPatches, maxVertices);
        lodResources.uploadPatchList(patches, 1L);

        final float[] planes = fullFrustumPlanes();
        final CdlodSelectionUbo selectionUbo = new CdlodSelectionUbo(
            planes,
            new Vector3f(128f, 200f, 128f),
            2.0f,
            descriptor.lod().morphStart(),
            descriptor.lod().morphEnd(),
            patches.patchCount()
        );

        final CdlodTessellationUbo tessUbo = new CdlodTessellationUbo(
            descriptor.worldScale(),
            descriptor.heightScale(),
            descriptor.lod().patchSize(),
            descriptor.heightmap().width(),
            descriptor.heightmap().height(),
            descriptor.heightmap().width() * descriptor.worldScale(),
            descriptor.heightmap().height() * descriptor.worldScale()
        );

        return new Fixture(ctx, lodResources, selectionUbo, tessUbo, patches.patchCount());
    }

    private static float[] fullFrustumPlanes() {
        return new float[] {
            1f, 0f, 0f, 1_000_000f,
            -1f, 0f, 0f, 1_000_000f,
            0f, 1f, 0f, 1_000_000f,
            0f, -1f, 0f, 1_000_000f,
            0f, 0f, 1f, 1_000_000f,
            0f, 0f, -1f, 1_000_000f
        };
    }

    private static TerrainDescriptor descriptor(final int w, final int h) {
        return new TerrainDescriptor(
            "tile-0-0",
            new HeightmapDesc(HeightmapFormat.R32F, "", w, h),
            new SplatmapDesc(
                SplatmapMode.LAYERS_4,
                "",
                null,
                List.of(new TerrainMaterial("dirt", "", "", "", 1f, 0f, false, false))),
            new FoliageDesc(42L, 200f, true, List.of(new FoliageLayer("grass", 0.5f, 0f, 45f, 0f, 1000f, 0.2f))),
            new WaterDesc(WaterMode.NONE, 0f, 1.5f),
            new RoadDesc(false, 4f, 1f, 0.5f),
            new TerrainLodDesc(6, TessellationMode.COMPUTE, 2f, 65, 0.6f, 0.9f),
            new ProceduralDesc(42L, 4, 0.003f, 0, 0, List.of()),
            new AutoSplatConfig(true, 1f, 1f, 1f),
            1.0f,
            800f,
            false
        );
    }

    private static HeightmapData hill(final int w, final int h) {
        final HeightmapData hm = HeightmapData.empty(w, h);
        final float cx = w * 0.5f;
        final float cz = h * 0.5f;
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                final float dx = x - cx;
                final float dz = z - cz;
                hm.setPixel(x, z, Math.max(0f, 200f - (float) Math.sqrt(dx * dx + dz * dz) * 6f));
            }
        }
        return hm;
    }

    private record Fixture(
        TerrainGpuContext ctx,
        TerrainGpuLodResources lodResources,
        CdlodSelectionUbo selectionUbo,
        CdlodTessellationUbo tessUbo,
        int totalPatchCount
    ) {
        void destroy() {
            this.lodResources.destroy();
            this.ctx.destroy();
        }
    }
}
