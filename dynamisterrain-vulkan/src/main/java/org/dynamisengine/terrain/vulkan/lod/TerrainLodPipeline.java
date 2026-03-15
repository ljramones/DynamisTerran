package org.dynamisengine.terrain.vulkan.lod;

import org.dynamisengine.terrain.vulkan.GpuMemoryOps;
import org.dynamisengine.terrain.vulkan.TerrainGpuContext;
import org.dynamisengine.terrain.vulkan.material.TerrainDrawPipeline;
import org.dynamisengine.terrain.vulkan.material.TerrainMaterialDescriptorSets;
import org.dynamisengine.terrain.core.lod.Matrix4f;

public final class TerrainLodPipeline {
    private final CdlodSelectionPass selectionPass;
    private final CdlodTessellationPass tessellationPass;
    private final SilhouetteCorrectionPass silhouettePass;
    private TerrainDrawPipeline drawPipeline;

    private TerrainLodPipeline(
        final CdlodSelectionPass selectionPass,
        final CdlodTessellationPass tessellationPass,
        final SilhouetteCorrectionPass silhouettePass
    ) {
        this.selectionPass = selectionPass;
        this.tessellationPass = tessellationPass;
        this.silhouettePass = silhouettePass;
    }

    public static TerrainLodPipeline create(final long device, final GpuMemoryOps memoryOps) {
        return new TerrainLodPipeline(
            CdlodSelectionPass.create(device, memoryOps),
            CdlodTessellationPass.create(device, memoryOps),
            SilhouetteCorrectionPass.create(device, memoryOps)
        );
    }

    public void compute(
        final long commandBuffer,
        final TerrainGpuLodResources lodResources,
        final TerrainGpuContext ctx,
        final CdlodSelectionUbo selectionUbo,
        final CdlodTessellationUbo tessUbo,
        final SilhouetteUbo silhouetteUbo,
        final long previousDepthImageView,
        final long depthSampler,
        final int totalPatchCount
    ) {
        this.selectionPass.select(commandBuffer, lodResources, ctx, selectionUbo, totalPatchCount);
        TerrainLodBarriers.selectionToTessellation(commandBuffer, lodResources);

        this.tessellationPass.tessellate(commandBuffer, lodResources, ctx, tessUbo);
        TerrainLodBarriers.tessellationToDraw(commandBuffer, lodResources);

        if (previousDepthImageView != 0L) {
            this.silhouettePass.correct(
                commandBuffer,
                lodResources,
                previousDepthImageView,
                depthSampler,
                silhouetteUbo,
                lodResources.visibleCount()
            );
            TerrainLodBarriers.silhouetteToIndirectDraw(commandBuffer, lodResources);
        }
    }

    public void destroy() {
        this.selectionPass.destroy();
        this.tessellationPass.destroy();
        this.silhouettePass.destroy();
        if (this.drawPipeline != null) {
            this.drawPipeline.destroy();
            this.drawPipeline = null;
        }
    }

    public void attachDrawPipeline(final TerrainDrawPipeline drawPipeline) {
        this.drawPipeline = drawPipeline;
    }

    public void recordDraw(
        final long commandBuffer,
        final TerrainGpuLodResources lodResources,
        final TerrainMaterialDescriptorSets descriptorSets,
        final Matrix4f viewProj,
        final int frameIndex
    ) {
        if (this.drawPipeline == null) {
            return;
        }
        this.drawPipeline.record(commandBuffer, lodResources, descriptorSets, frameIndex);
    }
}
