package org.dynamisengine.terrain.vulkan.foliage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import org.dynamisengine.terrain.core.scatter.ScatterPoint;
import org.dynamisengine.terrain.core.scatter.ScatterResult;
import org.dynamisengine.terrain.vulkan.GpuBuffer;
import org.dynamisengine.terrain.vulkan.GpuMemoryOps;

public final class FoliageInstanceBuffer {
    public static final int INSTANCE_STRIDE = 48;

    private final GpuMemoryOps memoryOps;
    private final GpuBuffer instanceBuffer;
    private final GpuBuffer visibleInstanceBuffer;
    private final GpuBuffer indirectDrawBuffer;
    private final GpuBuffer visibleCountBuffer;
    private final List<Instance> instances;

    private FoliageInstanceBuffer(
        final GpuMemoryOps memoryOps,
        final GpuBuffer instanceBuffer,
        final GpuBuffer visibleInstanceBuffer,
        final GpuBuffer indirectDrawBuffer,
        final GpuBuffer visibleCountBuffer,
        final List<Instance> instances
    ) {
        this.memoryOps = memoryOps;
        this.instanceBuffer = instanceBuffer;
        this.visibleInstanceBuffer = visibleInstanceBuffer;
        this.indirectDrawBuffer = indirectDrawBuffer;
        this.visibleCountBuffer = visibleCountBuffer;
        this.instances = instances;
    }

    public static FoliageInstanceBuffer upload(
        final long device,
        final GpuMemoryOps memoryOps,
        final List<ScatterResult> scatterResults,
        final float worldScale,
        final float heightScale,
        final long commandBuffer
    ) {
        final List<ScatterResult> safe = scatterResults == null ? List.of() : scatterResults;
        final List<Instance> all = new ArrayList<>();

        for (ScatterResult result : safe) {
            final int layer = Math.max(0, result.layerIndex());
            for (ScatterPoint p : result.points()) {
                all.add(new Instance(
                    p.worldX(),
                    p.worldY(),
                    p.worldZ(),
                    p.rotation(),
                    p.scale(),
                    layer,
                    p.worldX(),
                    p.worldY(),
                    p.worldZ(),
                    Math.max(0.5f, p.scale())
                ));
            }
        }

        final GpuBuffer instance = memoryOps.createBuffer(Math.max(1, all.size() * INSTANCE_STRIDE));
        final GpuBuffer visible = memoryOps.createBuffer(Math.max(1, all.size() * INSTANCE_STRIDE));
        final GpuBuffer indirect = memoryOps.createBuffer(Math.max(16, safe.size() * 16));
        final GpuBuffer counts = memoryOps.createBuffer(Math.max(8, (safe.size() + 1) * 4));

        instance.upload(packInstances(all));
        visible.upload(new byte[Math.max(1, all.size() * INSTANCE_STRIDE)]);
        indirect.upload(new byte[Math.max(16, safe.size() * 16)]);
        counts.upload(new byte[Math.max(8, (safe.size() + 1) * 4)]);

        return new FoliageInstanceBuffer(memoryOps, instance, visible, indirect, counts, all);
    }

    public GpuBuffer instanceBuffer() {
        return this.instanceBuffer;
    }

    public GpuBuffer visibleInstanceBuffer() {
        return this.visibleInstanceBuffer;
    }

    public GpuBuffer indirectDrawBuffer() {
        return this.indirectDrawBuffer;
    }

    public GpuBuffer visibleCountBuffer() {
        return this.visibleCountBuffer;
    }

    public int totalInstanceCount() {
        return this.instances.size();
    }

    public List<Instance> instances() {
        return this.instances;
    }

    void setCullOutputs(final List<Instance> visibleInstances, final int[] perLayerCounts, final int layerCount) {
        this.visibleInstanceBuffer.upload(packInstances(visibleInstances));

        final ByteBuffer counts = ByteBuffer.allocate(Math.max(8, (layerCount + 1) * 4)).order(ByteOrder.LITTLE_ENDIAN);
        int total = 0;
        for (int i = 0; i < layerCount; i++) {
            final int c = i < perLayerCounts.length ? perLayerCounts[i] : 0;
            counts.putInt(c);
            total += c;
        }
        counts.putInt(total);
        this.visibleCountBuffer.upload(counts.array());

        final ByteBuffer draws = ByteBuffer.allocate(Math.max(16, layerCount * 16)).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < layerCount; i++) {
            draws.putInt(3);
            draws.putInt(i < perLayerCounts.length ? perLayerCounts[i] : 0);
            draws.putInt(0);
            draws.putInt(0);
        }
        this.indirectDrawBuffer.upload(draws.array());
    }

    public void destroy() {
        this.memoryOps.destroyBuffer(this.instanceBuffer);
        this.memoryOps.destroyBuffer(this.visibleInstanceBuffer);
        this.memoryOps.destroyBuffer(this.indirectDrawBuffer);
        this.memoryOps.destroyBuffer(this.visibleCountBuffer);
    }

    private static byte[] packInstances(final List<Instance> source) {
        final ByteBuffer bb = ByteBuffer.allocate(Math.max(1, source.size() * INSTANCE_STRIDE)).order(ByteOrder.LITTLE_ENDIAN);
        for (Instance inst : source) {
            bb.putFloat(inst.worldX);
            bb.putFloat(inst.worldY);
            bb.putFloat(inst.worldZ);
            bb.putFloat(inst.rotation);
            bb.putFloat(inst.scale);
            bb.putInt(inst.layerIndex);
            bb.putFloat(0f);
            bb.putFloat(0f);
            bb.putFloat(inst.bsX);
            bb.putFloat(inst.bsY);
            bb.putFloat(inst.bsZ);
            bb.putFloat(inst.bsRadius);
        }
        return bb.array();
    }

    public record Instance(
        float worldX,
        float worldY,
        float worldZ,
        float rotation,
        float scale,
        int layerIndex,
        float bsX,
        float bsY,
        float bsZ,
        float bsRadius
    ) {
    }
}
