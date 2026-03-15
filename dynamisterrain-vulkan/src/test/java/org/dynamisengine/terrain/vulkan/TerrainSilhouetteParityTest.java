package org.dynamisengine.terrain.vulkan;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import org.dynamisengine.terrain.api.descriptor.AutoSplatConfig;
import org.dynamisengine.terrain.api.descriptor.FoliageDesc;
import org.dynamisengine.terrain.api.descriptor.FoliageLayer;
import org.dynamisengine.terrain.api.descriptor.HeightmapDesc;
import org.dynamisengine.terrain.api.descriptor.HeightmapFormat;
import org.dynamisengine.terrain.api.descriptor.ProceduralDesc;
import org.dynamisengine.terrain.api.descriptor.RoadDesc;
import org.dynamisengine.terrain.api.descriptor.SplatmapDesc;
import org.dynamisengine.terrain.api.descriptor.SplatmapMode;
import org.dynamisengine.terrain.api.descriptor.TerrainDescriptor;
import org.dynamisengine.terrain.api.descriptor.TerrainLodDesc;
import org.dynamisengine.terrain.api.descriptor.TerrainMaterial;
import org.dynamisengine.terrain.api.descriptor.TessellationMode;
import org.dynamisengine.terrain.api.descriptor.WaterDesc;
import org.dynamisengine.terrain.api.descriptor.WaterMode;
import org.dynamisengine.terrain.api.state.Vector3f;
import org.dynamisengine.terrain.core.heightmap.HeightmapData;
import org.dynamisengine.terrain.core.lod.CdlodFrameResult;
import org.dynamisengine.terrain.core.lod.CdlodQuadTree;
import org.dynamisengine.terrain.core.lod.Frustum;
import org.dynamisengine.terrain.vulkan.lod.CdlodSelectionPass;
import org.dynamisengine.terrain.vulkan.lod.CdlodSelectionUbo;
import org.dynamisengine.terrain.vulkan.lod.CdlodTessellationPass;
import org.dynamisengine.terrain.vulkan.lod.CdlodTessellationUbo;
import org.dynamisengine.terrain.vulkan.lod.SilhouetteCorrectionPass;
import org.dynamisengine.terrain.vulkan.lod.SilhouetteUbo;
import org.dynamisengine.terrain.vulkan.lod.TerrainGpuLodResources;
import org.dynamisengine.terrain.vulkan.lod.TerrainLodPipeline;
import org.junit.jupiter.api.Test;

class TerrainSilhouetteParityTest {
    @Test
    void silhouettePassCompletesWithoutValidationErrors() {
        final Fixture f = fixture();
        final TerrainLodPipeline pipeline = TerrainLodPipeline.create(1L, new InMemoryGpuMemoryOps());

        final long depthHandle = 111L;
        SilhouetteCorrectionPass.registerDepthImage(depthHandle, 2, 2, new float[] {1f, 1f, 1f, 1f});
        assertDoesNotThrow(() -> pipeline.compute(1L, f.lodResources, f.ctx, f.selectionUbo, f.tessUbo, f.silhouetteUbo, depthHandle, 1L, f.totalPatchCount));
        SilhouetteCorrectionPass.unregisterDepthImage(depthHandle);
        f.destroy();
    }

    @Test
    void silhouettePassSkippedOnFrameZero() {
        final Fixture f = fixture();
        final CdlodSelectionPass sel = CdlodSelectionPass.create(1L, new InMemoryGpuMemoryOps());
        final CdlodTessellationPass tess = CdlodTessellationPass.create(1L, new InMemoryGpuMemoryOps());
        sel.select(1L, f.lodResources, f.ctx, f.selectionUbo, f.totalPatchCount);
        tess.tessellate(1L, f.lodResources, f.ctx, f.tessUbo);
        final byte[] baseline = f.lodResources.indirectDrawBytes().clone();

        final TerrainLodPipeline pipeline = TerrainLodPipeline.create(1L, new InMemoryGpuMemoryOps());
        pipeline.compute(1L, f.lodResources, f.ctx, f.selectionUbo, f.tessUbo, f.silhouetteUbo, 0L, 0L, f.totalPatchCount);
        assertArrayEquals(baseline, f.lodResources.indirectDrawBytes());
        f.destroy();
    }

    @Test
    void silhouettePassWithDepthDiscontinuityIncreasesVertexCount() {
        final Fixture f = fixture();
        final TerrainLodPipeline pipeline = TerrainLodPipeline.create(1L, new InMemoryGpuMemoryOps());

        pipeline.compute(1L, f.lodResources, f.ctx, f.selectionUbo, f.tessUbo, f.silhouetteUbo, 0L, 0L, f.totalPatchCount);
        final int before = ByteBuffer.wrap(f.lodResources.indirectDrawBytes()).order(ByteOrder.LITTLE_ENDIAN).getInt(0);

        final long depthHandle = 222L;
        final float[] discontinuity = new float[16 * 16];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                discontinuity[y * 16 + x] = x < 8 ? 0.0f : 1.0f;
            }
        }
        SilhouetteCorrectionPass.registerDepthImage(depthHandle, 16, 16, discontinuity);
        pipeline.compute(1L, f.lodResources, f.ctx, f.selectionUbo, f.tessUbo, f.silhouetteUbo, depthHandle, 1L, f.totalPatchCount);
        SilhouetteCorrectionPass.unregisterDepthImage(depthHandle);

        final int after = ByteBuffer.wrap(f.lodResources.indirectDrawBytes()).order(ByteOrder.LITTLE_ENDIAN).getInt(0);
        assertTrue(after >= before);
        f.destroy();
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

        final CdlodSelectionUbo selectionUbo = new CdlodSelectionUbo(
            fullFrustumPlanes(),
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
        final SilhouetteUbo silhouetteUbo = new SilhouetteUbo(
            1920f,
            1080f,
            0.05f,
            2.0f,
            new Vector3f(128f, 200f, 128f),
            0.1f,
            10_000f
        );

        return new Fixture(ctx, lodResources, selectionUbo, tessUbo, silhouetteUbo, patches.patchCount());
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
        SilhouetteUbo silhouetteUbo,
        int totalPatchCount
    ) {
        void destroy() {
            this.lodResources.destroy();
            this.ctx.destroy();
        }
    }
}
