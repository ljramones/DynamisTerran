package org.dynamisterrain.vulkan.lod;

import org.dynamisterrain.vulkan.GpuMemoryOps;
import org.dynamisterrain.vulkan.TerrainGpuContext;

public final class TerrainLodPipeline {
    private final CdlodSelectionPass selectionPass;
    private final CdlodTessellationPass tessellationPass;
    private final SilhouetteCorrectionPass silhouettePass;

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
    }
}
