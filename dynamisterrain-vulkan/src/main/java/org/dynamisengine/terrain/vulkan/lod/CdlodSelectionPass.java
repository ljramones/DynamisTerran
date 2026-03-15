package org.dynamisengine.terrain.vulkan.lod;

import java.util.ArrayList;
import java.util.List;
import org.dynamisengine.terrain.api.state.Vector3f;
import org.dynamisengine.terrain.core.heightmap.HeightmapOps;
import org.dynamisengine.terrain.core.lod.CdlodPatch;
import org.dynamisengine.terrain.vulkan.GpuMemoryOps;
import org.dynamisengine.terrain.vulkan.TerrainGpuContext;

public final class CdlodSelectionPass {
    private final long device;
    private final GpuMemoryOps memoryOps;

    private CdlodSelectionPass(final long device, final GpuMemoryOps memoryOps) {
        this.device = device;
        this.memoryOps = memoryOps;
    }

    public static CdlodSelectionPass create(final long device, final GpuMemoryOps memoryOps) {
        return new CdlodSelectionPass(device, memoryOps);
    }

    public void select(
        final long commandBuffer,
        final TerrainGpuLodResources lodResources,
        final TerrainGpuContext ctx,
        final CdlodSelectionUbo ubo,
        final int totalPatchCount
    ) {
        final List<CdlodPatch> visible = new ArrayList<>();
        final List<Float> morphs = new ArrayList<>();

        final int count = Math.min(totalPatchCount, lodResources.patchList().size());
        for (int i = 0; i < count; i++) {
            final CdlodPatch patch = lodResources.patchList().get(i);
            final float cx = patch.centerX();
            final float cz = patch.centerZ();
            final float cy = HeightmapOps.heightAt(
                ctx.heightmapData(),
                cx,
                cz,
                1.0f,
                1.0f
            );
            final float radius = (float) Math.sqrt((patch.worldWidth() * 0.5f) * (patch.worldWidth() * 0.5f)
                + (patch.worldDepth() * 0.5f) * (patch.worldDepth() * 0.5f)
                + (Math.max(1.0f, patch.maxHeight() - patch.minHeight()) * Math.max(1.0f, patch.maxHeight() - patch.minHeight())));

            if (frustumCull(ubo.frustumPlanes(), cx, cy, cz, radius)) {
                continue;
            }

            final float dist = distance(ubo.cameraPos(), new Vector3f(cx, cy, cz));
            final float lodSwitchDist = Math.max(patch.worldWidth(), patch.worldDepth()) * (1 << patch.lodLevel());
            final float mStart = lodSwitchDist * ubo.morphStart();
            final float mEnd = lodSwitchDist * ubo.morphEnd();
            final float morph = clamp((dist - mStart) / Math.max(0.0001f, mEnd - mStart), 0.0f, 1.0f);

            visible.add(patch);
            morphs.add(morph);
        }

        final float[] outMorphs = new float[morphs.size()];
        for (int i = 0; i < morphs.size(); i++) {
            outMorphs[i] = morphs.get(i);
        }
        lodResources.setSelectionOutput(visible, outMorphs, visible.size());
    }

    public void destroy() {
        // No-op for scaffold pass.
    }

    private static boolean frustumCull(final float[] planes, final float x, final float y, final float z, final float radius) {
        if (planes == null || planes.length < 24) {
            return false;
        }
        for (int i = 0; i < 6; i++) {
            final int p = i * 4;
            final float d = planes[p] * x + planes[p + 1] * y + planes[p + 2] * z + planes[p + 3];
            if (d < -radius) {
                return true;
            }
        }
        return false;
    }

    private static float distance(final Vector3f a, final Vector3f b) {
        final float dx = a.x() - b.x();
        final float dy = a.y() - b.y();
        final float dz = a.z() - b.z();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static float clamp(final float v, final float min, final float max) {
        return Math.max(min, Math.min(max, v));
    }
}
