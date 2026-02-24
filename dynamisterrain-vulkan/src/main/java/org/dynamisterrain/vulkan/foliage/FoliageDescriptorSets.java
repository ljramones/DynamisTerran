package org.dynamisterrain.vulkan.foliage;

import java.util.List;
import org.dynamisterrain.api.descriptor.FoliageLayer;
import org.dynamisterrain.vulkan.GpuBuffer;
import org.dynamisterrain.vulkan.GpuMemoryOps;
import org.dynamisterrain.vulkan.InMemoryGpuMemoryOps;
import org.dynamisterrain.vulkan.material.WeatherState;

public final class FoliageDescriptorSets {
    private final GpuMemoryOps memoryOps;
    private final FoliageInstanceBuffer instanceBuf;
    private final long[][] descriptorSets;
    private final GpuBuffer[] windBuffers;

    private FoliageDescriptorSets(
        final GpuMemoryOps memoryOps,
        final FoliageInstanceBuffer instanceBuf,
        final long[][] descriptorSets,
        final GpuBuffer[] windBuffers
    ) {
        this.memoryOps = memoryOps;
        this.instanceBuf = instanceBuf;
        this.descriptorSets = descriptorSets;
        this.windBuffers = windBuffers;
    }

    public static FoliageDescriptorSets create(
        final long device,
        final FoliageInstanceBuffer instanceBuf,
        final long bindlessHeap,
        final List<FoliageLayer> layers,
        final int frameCount
    ) {
        final int frames = Math.max(1, frameCount);
        final GpuMemoryOps memoryOps = new InMemoryGpuMemoryOps();
        final long[][] sets = new long[frames][3];
        final GpuBuffer[] windBuffers = new GpuBuffer[frames];

        for (int i = 0; i < frames; i++) {
            sets[i][0] = (long) ((i + 1) * 1000 + 0);
            sets[i][1] = (long) ((i + 1) * 1000 + 1);
            sets[i][2] = (long) ((i + 1) * 1000 + 2);
            windBuffers[i] = memoryOps.createBuffer(FoliageWindUbo.SIZE_BYTES);
            windBuffers[i].upload(FoliageWindUbo.fromWeather(WeatherState.CLEAR, 0f, 1f).toBytes());
        }

        return new FoliageDescriptorSets(memoryOps, instanceBuf, sets, windBuffers);
    }

    public void writeWindUbo(final WeatherState weather, final float gameTime, final int frameIndex) {
        final int frame = normalizeFrame(frameIndex);
        this.windBuffers[frame].upload(FoliageWindUbo.fromWeather(weather, gameTime, 1f).toBytes());
    }

    public long descriptorSet(final int setIndex, final int frameIndex) {
        final int frame = normalizeFrame(frameIndex);
        if (setIndex < 0 || setIndex >= this.descriptorSets[frame].length) {
            return 0L;
        }
        return this.descriptorSets[frame][setIndex];
    }

    public byte[] windUboBytes(final int frameIndex) {
        return this.windBuffers[normalizeFrame(frameIndex)].data();
    }

    public void destroy() {
        for (GpuBuffer windBuffer : this.windBuffers) {
            this.memoryOps.destroyBuffer(windBuffer);
        }
    }

    private int normalizeFrame(final int frameIndex) {
        final int frames = this.windBuffers.length;
        final int mod = frameIndex % frames;
        return mod < 0 ? mod + frames : mod;
    }
}
