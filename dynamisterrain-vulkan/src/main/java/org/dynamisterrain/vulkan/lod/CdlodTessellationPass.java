package org.dynamisterrain.vulkan.lod;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import org.dynamisterrain.core.heightmap.HeightmapOps;
import org.dynamisterrain.core.lod.CdlodPatch;
import org.dynamisterrain.vulkan.GpuMemoryOps;
import org.dynamisterrain.vulkan.TerrainGpuContext;

public final class CdlodTessellationPass {
    private final long device;
    private final GpuMemoryOps memoryOps;

    private CdlodTessellationPass(final long device, final GpuMemoryOps memoryOps) {
        this.device = device;
        this.memoryOps = memoryOps;
    }

    public static CdlodTessellationPass create(final long device, final GpuMemoryOps memoryOps) {
        return new CdlodTessellationPass(device, memoryOps);
    }

    public void tessellate(
        final long commandBuffer,
        final TerrainGpuLodResources lodResources,
        final TerrainGpuContext ctx,
        final CdlodTessellationUbo ubo
    ) {
        final List<CdlodPatch> visible = lodResources.visiblePatches();
        final float[] morphs = lodResources.morphFactors();
        final int patchSize = ubo.patchSize();
        final int vertsPerPatch = patchSize * patchSize;

        final ByteBuffer vertices = ByteBuffer
            .allocate(Math.max(1, visible.size() * vertsPerPatch * 48))
            .order(ByteOrder.LITTLE_ENDIAN);
        final ByteBuffer draws = ByteBuffer
            .allocate(Math.max(1, visible.size() * 16))
            .order(ByteOrder.LITTLE_ENDIAN);

        for (int p = 0; p < visible.size(); p++) {
            final CdlodPatch patch = visible.get(p);
            final float morph = morphs[Math.min(p, morphs.length - 1)];
            final float stepX = patch.worldWidth() / Math.max(1f, patchSize - 1f);
            final float stepZ = patch.worldDepth() / Math.max(1f, patchSize - 1f);

            for (int z = 0; z < patchSize; z++) {
                for (int x = 0; x < patchSize; x++) {
                    final float wx = patch.worldX() + x * stepX;
                    final float wz = patch.worldZ() + z * stepZ;

                    float h = HeightmapOps.heightAt(ctx.heightmapData(), wx, wz, ubo.worldScale(), ubo.heightScale());

                    final float coarserStepX = stepX * 2.0f;
                    final float coarserStepZ = stepZ * 2.0f;
                    final float snapX = (float) Math.floor(wx / Math.max(0.0001f, coarserStepX)) * coarserStepX;
                    final float snapZ = (float) Math.floor(wz / Math.max(0.0001f, coarserStepZ)) * coarserStepZ;
                    final float snapH = HeightmapOps.heightAt(ctx.heightmapData(), snapX, snapZ, ubo.worldScale(), ubo.heightScale());
                    h = h + (snapH - h) * morph;

                    final float texStep = ubo.worldScale();
                    final float hL = HeightmapOps.heightAt(ctx.heightmapData(), wx - texStep, wz, ubo.worldScale(), ubo.heightScale());
                    final float hR = HeightmapOps.heightAt(ctx.heightmapData(), wx + texStep, wz, ubo.worldScale(), ubo.heightScale());
                    final float hD = HeightmapOps.heightAt(ctx.heightmapData(), wx, wz - texStep, ubo.worldScale(), ubo.heightScale());
                    final float hU = HeightmapOps.heightAt(ctx.heightmapData(), wx, wz + texStep, ubo.worldScale(), ubo.heightScale());
                    float nx = hL - hR;
                    float ny = 2.0f * ubo.worldScale();
                    float nz = hD - hU;
                    final float invLen = invSqrt(nx * nx + ny * ny + nz * nz);
                    nx *= invLen;
                    ny *= invLen;
                    nz *= invLen;

                    final float u = wx / Math.max(1f, ubo.terrainWorldSizeX());
                    final float v = wz / Math.max(1f, ubo.terrainWorldSizeZ());

                    vertices.putFloat(wx);
                    vertices.putFloat(h);
                    vertices.putFloat(wz);
                    vertices.putFloat(0f);
                    vertices.putFloat(nx);
                    vertices.putFloat(ny);
                    vertices.putFloat(nz);
                    vertices.putFloat(0f);
                    vertices.putFloat(u);
                    vertices.putFloat(v);
                    vertices.putFloat(morph);
                    vertices.putFloat(0f);
                }
            }

            draws.putInt(vertsPerPatch);
            draws.putInt(1);
            draws.putInt(p * vertsPerPatch);
            draws.putInt(0);
        }

        lodResources.setTessellationOutput(vertices.array(), draws.array());
    }

    public void destroy() {
        // No-op for scaffold pass.
    }

    private static float invSqrt(final float v) {
        if (v <= 0f) {
            return 0f;
        }
        return 1.0f / (float) Math.sqrt(v);
    }
}
