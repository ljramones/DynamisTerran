package org.dynamisengine.terrain.vulkan.material;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.dynamisengine.terrain.vulkan.GpuBuffer;
import org.dynamisengine.terrain.vulkan.GpuMemoryOps;
import org.dynamisengine.terrain.vulkan.TerrainGpuContext;

public final class TerrainMaterialDescriptorSets {
    private final GpuMemoryOps memoryOps;
    private final TerrainMaterialAtlas atlas;
    private final GpuBuffer[] terrainUboBuffers;
    private final GpuBuffer[] weatherUboBuffers;
    private final long[][] descriptorHandles;
    private final long[] aerialLutByFrame;

    private TerrainMaterialDescriptorSets(
        final GpuMemoryOps memoryOps,
        final TerrainMaterialAtlas atlas,
        final GpuBuffer[] terrainUboBuffers,
        final GpuBuffer[] weatherUboBuffers,
        final long[][] descriptorHandles,
        final long[] aerialLutByFrame
    ) {
        this.memoryOps = memoryOps;
        this.atlas = atlas;
        this.terrainUboBuffers = terrainUboBuffers;
        this.weatherUboBuffers = weatherUboBuffers;
        this.descriptorHandles = descriptorHandles;
        this.aerialLutByFrame = aerialLutByFrame;
    }

    public static TerrainMaterialDescriptorSets create(
        final long device,
        final TerrainGpuContext ctx,
        final TerrainMaterialAtlas atlas,
        final long aerialPerspectiveLut,
        final long sampler3D,
        final int frameCount
    ) {
        final int frames = Math.max(1, frameCount);
        final GpuMemoryOps memoryOps = new org.dynamisengine.terrain.vulkan.InMemoryGpuMemoryOps();
        final GpuBuffer[] terrainBuffers = new GpuBuffer[frames];
        final GpuBuffer[] weatherBuffers = new GpuBuffer[frames];
        final long[][] handles = new long[frames][5];
        final long[] aerialByFrame = new long[frames];

        for (int i = 0; i < frames; i++) {
            terrainBuffers[i] = memoryOps.createBuffer(TerrainMaterialUbo.SIZE_BYTES);
            weatherBuffers[i] = memoryOps.createBuffer(WeatherUbo.SIZE_BYTES);
            terrainBuffers[i].upload(TerrainMaterialUbo.defaults().toBytes());
            weatherBuffers[i].upload(WeatherUbo.fromState(WeatherState.CLEAR).toBytes());
            aerialByFrame[i] = aerialPerspectiveLut;
            for (int s = 0; s < 5; s++) {
                handles[i][s] = (long) ((i + 1) * 100 + s);
            }
        }

        return new TerrainMaterialDescriptorSets(memoryOps, atlas, terrainBuffers, weatherBuffers, handles, aerialByFrame);
    }

    public void writeWeatherUbo(
        final WeatherState weather,
        final SunState sun,
        final TimeOfDayState timeOfDay,
        final int frameIndex
    ) {
        final int frame = normalizeFrame(frameIndex);
        final WeatherState safeWeather = weather == null ? WeatherState.CLEAR : weather;
        this.weatherUboBuffers[frame].upload(WeatherUbo.fromState(safeWeather).toBytes());
    }

    public void writeAerialLut(final long newAerialLut, final int frameIndex) {
        final int frame = normalizeFrame(frameIndex);
        this.aerialLutByFrame[frame] = newAerialLut;
    }

    public void writeTerrainMaterialUbo(final TerrainMaterialUbo ubo, final int frameIndex) {
        final int frame = normalizeFrame(frameIndex);
        final TerrainMaterialUbo safe = ubo == null ? TerrainMaterialUbo.defaults() : ubo;
        this.terrainUboBuffers[frame].upload(safe.toBytes());
    }

    public long descriptorSet(final int setIndex, final int frameIndex) {
        final int frame = normalizeFrame(frameIndex);
        if (setIndex < 0 || setIndex >= this.descriptorHandles[frame].length) {
            return 0L;
        }
        return this.descriptorHandles[frame][setIndex];
    }

    public byte[] weatherUboBytes(final int frameIndex) {
        return this.weatherUboBuffers[normalizeFrame(frameIndex)].data();
    }

    public byte[] terrainMaterialUboBytes(final int frameIndex) {
        return this.terrainUboBuffers[normalizeFrame(frameIndex)].data();
    }

    public long aerialLut(final int frameIndex) {
        return this.aerialLutByFrame[normalizeFrame(frameIndex)];
    }

    public TerrainMaterialAtlas atlas() {
        return this.atlas;
    }

    public void destroy() {
        for (GpuBuffer b : this.terrainUboBuffers) {
            this.memoryOps.destroyBuffer(b);
        }
        for (GpuBuffer b : this.weatherUboBuffers) {
            this.memoryOps.destroyBuffer(b);
        }
    }

    private int normalizeFrame(final int frameIndex) {
        final int frames = this.terrainUboBuffers.length;
        if (frames == 0) {
            return 0;
        }
        final int mod = frameIndex % frames;
        return mod < 0 ? mod + frames : mod;
    }

    public static float readFloat(final byte[] src, final int offsetBytes) {
        return ByteBuffer.wrap(src).order(ByteOrder.LITTLE_ENDIAN).getFloat(offsetBytes);
    }
}
