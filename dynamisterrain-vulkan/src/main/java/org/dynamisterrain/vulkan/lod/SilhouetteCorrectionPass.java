package org.dynamisterrain.vulkan.lod;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.dynamisterrain.vulkan.GpuMemoryOps;

public final class SilhouetteCorrectionPass {
    private static final Map<Long, DepthImage> DEPTH_REGISTRY = new ConcurrentHashMap<>();

    private final long device;
    private final GpuMemoryOps memoryOps;

    private SilhouetteCorrectionPass(final long device, final GpuMemoryOps memoryOps) {
        this.device = device;
        this.memoryOps = memoryOps;
    }

    public static SilhouetteCorrectionPass create(final long device, final GpuMemoryOps memoryOps) {
        return new SilhouetteCorrectionPass(device, memoryOps);
    }

    public static void registerDepthImage(final long imageViewHandle, final int width, final int height, final float[] depthData) {
        DEPTH_REGISTRY.put(imageViewHandle, new DepthImage(width, height, depthData.clone()));
    }

    public static void unregisterDepthImage(final long imageViewHandle) {
        DEPTH_REGISTRY.remove(imageViewHandle);
    }

    public void correct(
        final long commandBuffer,
        final TerrainGpuLodResources lodResources,
        final long previousDepthImageView,
        final long depthSampler,
        final SilhouetteUbo ubo,
        final int visiblePatchCount
    ) {
        if (previousDepthImageView == 0L || visiblePatchCount <= 0) {
            return;
        }

        final DepthImage depth = DEPTH_REGISTRY.get(previousDepthImageView);
        if (depth == null) {
            return;
        }

        final byte[] bytes = lodResources.indirectDrawBytes().clone();
        final ByteBuffer draws = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        final int patchCount = Math.min(visiblePatchCount, bytes.length / 16);
        for (int i = 0; i < patchCount; i++) {
            final int offset = i * 16;
            final int currentVertexCount = draws.getInt(offset);
            if (currentVertexCount <= 0) {
                continue;
            }

            final boolean silhouette = hasSilhouetteEdge(depth, ubo.silhouetteThreshold());
            if (!silhouette) {
                continue;
            }

            final int side = (int) Math.sqrt(currentVertexCount);
            final int newSide = Math.max(side, (int) Math.floor(side * Math.max(1.0f, ubo.maxSubdivMultiplier())));
            final int newVertexCount = newSide * newSide;
            draws.putInt(offset, newVertexCount);
        }

        lodResources.overwriteIndirectDrawBytes(bytes);
    }

    public void destroy() {
        // No-op in scaffold backend.
    }

    private static boolean hasSilhouetteEdge(final DepthImage depth, final float threshold) {
        final int midY = Math.max(0, depth.height / 2);
        for (int x = 1; x < depth.width - 1; x++) {
            final float d0 = depth.at(x, midY);
            final float dE = depth.at(x + 1, midY);
            final float dW = depth.at(x - 1, midY);
            final float diff = Math.max(Math.abs(d0 - dE), Math.abs(d0 - dW));
            if (diff > threshold) {
                return true;
            }
        }
        return false;
    }

    private record DepthImage(int width, int height, float[] data) {
        float at(final int x, final int y) {
            final int cx = Math.max(0, Math.min(this.width - 1, x));
            final int cy = Math.max(0, Math.min(this.height - 1, y));
            return this.data[cy * this.width + cx];
        }
    }
}
