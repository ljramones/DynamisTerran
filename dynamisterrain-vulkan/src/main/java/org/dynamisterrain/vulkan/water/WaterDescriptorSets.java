package org.dynamisterrain.vulkan.water;

import org.dynamisterrain.api.descriptor.WaterDesc;
import org.dynamisterrain.vulkan.GpuBuffer;
import org.dynamisterrain.vulkan.GpuMemoryOps;
import org.dynamisterrain.vulkan.InMemoryGpuMemoryOps;
import org.dynamisterrain.vulkan.material.WeatherState;
import org.dynamisterrain.vulkan.material.WeatherUbo;

public final class WaterDescriptorSets {
    private final GpuMemoryOps memoryOps;
    private final long[][] descriptorSets;
    private final GpuBuffer[] waterUboBuffers;
    private final GpuBuffer[] weatherUboBuffers;
    private final long[] aerialLuts;

    private WaterDescriptorSets(
        final GpuMemoryOps memoryOps,
        final long[][] descriptorSets,
        final GpuBuffer[] waterUboBuffers,
        final GpuBuffer[] weatherUboBuffers,
        final long[] aerialLuts
    ) {
        this.memoryOps = memoryOps;
        this.descriptorSets = descriptorSets;
        this.waterUboBuffers = waterUboBuffers;
        this.weatherUboBuffers = weatherUboBuffers;
        this.aerialLuts = aerialLuts;
    }

    public static WaterDescriptorSets create(
        final long device,
        final long depthImageView,
        final long colorImageView,
        final long waterNormalImageView,
        final long aerialPerspectiveLut,
        final long sampler,
        final long sampler3D,
        final int frameCount
    ) {
        final int frames = Math.max(1, frameCount);
        final GpuMemoryOps memoryOps = new InMemoryGpuMemoryOps();
        final long[][] sets = new long[frames][2];
        final GpuBuffer[] water = new GpuBuffer[frames];
        final GpuBuffer[] weather = new GpuBuffer[frames];
        final long[] aerial = new long[frames];

        for (int i = 0; i < frames; i++) {
            sets[i][0] = (long) ((i + 1) * 2000 + 0);
            sets[i][1] = (long) ((i + 1) * 2000 + 1);
            water[i] = memoryOps.createBuffer(WaterUbo.SIZE_BYTES);
            weather[i] = memoryOps.createBuffer(WeatherUbo.SIZE_BYTES);
            water[i].upload(new byte[WaterUbo.SIZE_BYTES]);
            weather[i].upload(WeatherUbo.fromState(WeatherState.CLEAR).toBytes());
            aerial[i] = aerialPerspectiveLut;
        }
        return new WaterDescriptorSets(memoryOps, sets, water, weather, aerial);
    }

    public void writeWaterUbo(
        final WaterDesc waterDesc,
        final float gameTime,
        final float terrainSizeX,
        final float terrainSizeZ,
        final int frameIndex
    ) {
        final int frame = normalizeFrame(frameIndex);
        this.waterUboBuffers[frame].upload(WaterUbo.fromDesc(waterDesc, gameTime, terrainSizeX, terrainSizeZ).toBytes());
    }

    public void writeWeatherUbo(final WeatherState weather, final int frameIndex) {
        final int frame = normalizeFrame(frameIndex);
        this.weatherUboBuffers[frame].upload(WeatherUbo.fromState(weather == null ? WeatherState.CLEAR : weather).toBytes());
    }

    public void writeAerialLut(final long newAerialLut, final int frameIndex) {
        this.aerialLuts[normalizeFrame(frameIndex)] = newAerialLut;
    }

    public long descriptorSet(final int setIndex, final int frameIndex) {
        final int frame = normalizeFrame(frameIndex);
        if (setIndex < 0 || setIndex >= this.descriptorSets[frame].length) {
            return 0L;
        }
        return this.descriptorSets[frame][setIndex];
    }

    public byte[] waterUboBytes(final int frameIndex) {
        return this.waterUboBuffers[normalizeFrame(frameIndex)].data();
    }

    public byte[] weatherUboBytes(final int frameIndex) {
        return this.weatherUboBuffers[normalizeFrame(frameIndex)].data();
    }

    public long aerialLut(final int frameIndex) {
        return this.aerialLuts[normalizeFrame(frameIndex)];
    }

    public void destroy() {
        for (GpuBuffer b : this.waterUboBuffers) {
            this.memoryOps.destroyBuffer(b);
        }
        for (GpuBuffer b : this.weatherUboBuffers) {
            this.memoryOps.destroyBuffer(b);
        }
    }

    private int normalizeFrame(final int frameIndex) {
        final int frames = this.waterUboBuffers.length;
        final int mod = frameIndex % frames;
        return mod < 0 ? mod + frames : mod;
    }
}
