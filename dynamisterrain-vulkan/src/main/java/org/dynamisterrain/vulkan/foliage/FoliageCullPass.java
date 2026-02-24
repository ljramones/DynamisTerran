package org.dynamisterrain.vulkan.foliage;

import java.util.ArrayList;
import java.util.List;
import org.dynamisterrain.vulkan.GpuMemoryOps;

public final class FoliageCullPass {
    private final long device;
    private final GpuMemoryOps memoryOps;

    private FoliageCullPass(final long device, final GpuMemoryOps memoryOps) {
        this.device = device;
        this.memoryOps = memoryOps;
    }

    public static FoliageCullPass create(final long device, final GpuMemoryOps memoryOps) {
        return new FoliageCullPass(device, memoryOps);
    }

    public void cull(
        final long commandBuffer,
        final FoliageInstanceBuffer instanceBuf,
        final long hzbImageView,
        final long hzbSampler,
        final FoliageCullUbo ubo
    ) {
        final int layerCount = Math.max(1, ubo.layerCount());
        final int[] perLayer = new int[layerCount];
        final List<FoliageInstanceBuffer.Instance> visible = new ArrayList<>();

        for (FoliageInstanceBuffer.Instance inst : instanceBuf.instances()) {
            final float dx = inst.bsX() - ubo.cameraPos().x();
            final float dy = inst.bsY() - ubo.cameraPos().y();
            final float dz = inst.bsZ() - ubo.cameraPos().z();
            final float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > ubo.maxDrawDistance()) {
                continue;
            }

            if (!sphereInFrustum(inst.bsX(), inst.bsY(), inst.bsZ(), inst.bsRadius(), ubo.frustumPlanes())) {
                continue;
            }

            visible.add(inst);
            final int layer = Math.max(0, Math.min(layerCount - 1, inst.layerIndex()));
            perLayer[layer]++;
        }

        instanceBuf.setCullOutputs(visible, perLayer, layerCount);
    }

    public void destroy() {
        // No-op for scaffold backend.
    }

    private static boolean sphereInFrustum(
        final float x,
        final float y,
        final float z,
        final float radius,
        final float[] planes
    ) {
        if (planes == null || planes.length < 24) {
            return true;
        }
        for (int i = 0; i < 6; i++) {
            final int p = i * 4;
            final float d = planes[p] * x + planes[p + 1] * y + planes[p + 2] * z + planes[p + 3];
            if (d < -radius) {
                return false;
            }
        }
        return true;
    }
}
