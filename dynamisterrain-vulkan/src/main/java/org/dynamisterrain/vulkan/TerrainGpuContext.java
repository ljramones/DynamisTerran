package org.dynamisterrain.vulkan;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.dynamisterrain.api.descriptor.HeightmapFormat;
import org.dynamisterrain.api.descriptor.SplatmapMode;
import org.dynamisterrain.api.descriptor.TerrainDescriptor;
import org.dynamisterrain.api.gpu.TerrainGpuResources;
import org.dynamisterrain.core.flow.FlowMapData;
import org.dynamisterrain.core.heightmap.HeightmapData;
import org.dynamisterrain.vulkan.internal.gpu.NoopTerrainGpuBackendAdapter;
import org.dynamisterrain.vulkan.internal.gpu.TerrainGpuBackendAdapter;

public final class TerrainGpuContext {
    private final GpuMemoryOps memoryOps;
    private final TerrainGpuBackendAdapter gpuBackendAdapter;

    private GpuImage2D heightmapTexture;
    private GpuImage2D normalMapTexture;
    private GpuImage2D horizonMapTexture;
    private GpuImage2D flowMapTexture;
    private GpuImage2D splatmap0Texture;
    private GpuImage2D splatmap1Texture;
    private long sampler;

    private HeightmapData heightmapData;
    private FlowMapData flowMapData;
    private float[] normalData;

    private TerrainGpuContext(final GpuMemoryOps memoryOps, final TerrainGpuBackendAdapter gpuBackendAdapter) {
        this.memoryOps = memoryOps;
        this.gpuBackendAdapter = gpuBackendAdapter;
    }

    public static TerrainGpuContext allocate(final long device, final GpuMemoryOps memoryOps, final TerrainDescriptor descriptor) {
        return allocate(device, memoryOps, descriptor, new NoopTerrainGpuBackendAdapter());
    }

    static TerrainGpuContext allocate(
            final long device,
            final GpuMemoryOps memoryOps,
            final TerrainDescriptor descriptor,
            final TerrainGpuBackendAdapter gpuBackendAdapter) {
        final int width = descriptor.heightmap().width();
        final int height = descriptor.heightmap().height();

        final TerrainGpuBackendAdapter backendAdapter = gpuBackendAdapter == null
                ? new NoopTerrainGpuBackendAdapter()
                : gpuBackendAdapter;

        final TerrainGpuContext ctx = new TerrainGpuContext(memoryOps, backendAdapter);
        final int hmBpp = descriptor.heightmap().format() == HeightmapFormat.R16 ? 2 : 4;
        ctx.heightmapTexture = memoryOps.createImage2D(width, height, hmBpp);
        ctx.normalMapTexture = memoryOps.createImage2D(width, height, 4);
        ctx.horizonMapTexture = memoryOps.createImage2D(width, height, 4);
        ctx.flowMapTexture = memoryOps.createImage2D(width, height, 4);
        ctx.splatmap0Texture = memoryOps.createImage2D(width, height, 4);
        if (descriptor.splatmap() != null && descriptor.splatmap().mode() != SplatmapMode.LAYERS_4) {
            ctx.splatmap1Texture = memoryOps.createImage2D(width, height, 4);
        }
        ctx.sampler = ctx.gpuBackendAdapter.createSampler(memoryOps);
        return ctx;
    }

    public void uploadHeightmap(final HeightmapData heightmap, final long commandBuffer) {
        this.heightmapData = heightmap;
        if (heightmap.format() == HeightmapFormat.R16) {
            final ByteBuffer bb = ByteBuffer.allocate(heightmap.width() * heightmap.height() * 2).order(ByteOrder.LITTLE_ENDIAN);
            for (float v : heightmap.pixels()) {
                final int u = (int) Math.max(0, Math.min(65535, Math.round(v / Math.max(0.0001f, maxValue(heightmap.pixels())) * 65535f)));
                bb.putShort((short) (u & 0xFFFF));
            }
            this.heightmapTexture.upload(bb.array());
        } else {
            this.heightmapTexture.upload(floatsToBytes(heightmap.pixels()));
        }
    }

    public void uploadFlowMap(final FlowMapData flowMap, final long commandBuffer) {
        this.flowMapData = flowMap;
        this.flowMapTexture.upload(floatsToBytes(flowMap.rawData()));
    }

    public void uploadNormalMap(final float[] normals, final long commandBuffer) {
        this.normalData = normals.clone();
        final ByteBuffer bb = ByteBuffer.allocate((normals.length / 3) * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < normals.length; i += 3) {
            short nx = (short) Math.round(clamp(normals[i], -1f, 1f) * 32767f);
            short ny = (short) Math.round(clamp(normals[i + 1], -1f, 1f) * 32767f);
            bb.putShort(nx);
            bb.putShort(ny);
        }
        this.normalMapTexture.upload(bb.array());
    }

    public void uploadSplatmap(final byte[] splatmap0Data, final byte[] splatmap1Data, final long commandBuffer) {
        if (this.splatmap0Texture != null && splatmap0Data != null) {
            this.splatmap0Texture.upload(splatmap0Data);
        }
        if (this.splatmap1Texture != null && splatmap1Data != null) {
            this.splatmap1Texture.upload(splatmap1Data);
        }
    }

    public TerrainGpuResources toGpuResources() {
        return new TerrainGpuResources(
            handle(this.heightmapTexture),
            handle(this.normalMapTexture),
            handle(this.horizonMapTexture),
            handle(this.flowMapTexture),
            handle(this.splatmap0Texture),
            handle(this.splatmap1Texture),
            0L,
            this.sampler
        );
    }

    public GpuImage2D heightmapTexture() {
        return this.heightmapTexture;
    }

    public GpuImage2D horizonMapTexture() {
        return this.horizonMapTexture;
    }

    public HeightmapData heightmapData() {
        return this.heightmapData;
    }

    public FlowMapData flowMapData() {
        return this.flowMapData;
    }

    public float[] normalData() {
        return this.normalData;
    }

    public void destroy() {
        if (this.heightmapTexture != null) {
            this.memoryOps.destroyImage(this.heightmapTexture);
        }
        if (this.normalMapTexture != null) {
            this.memoryOps.destroyImage(this.normalMapTexture);
        }
        if (this.horizonMapTexture != null) {
            this.memoryOps.destroyImage(this.horizonMapTexture);
        }
        if (this.flowMapTexture != null) {
            this.memoryOps.destroyImage(this.flowMapTexture);
        }
        if (this.splatmap0Texture != null) {
            this.memoryOps.destroyImage(this.splatmap0Texture);
        }
        if (this.splatmap1Texture != null) {
            this.memoryOps.destroyImage(this.splatmap1Texture);
        }
        if (this.sampler != 0L) {
            this.memoryOps.destroySampler(this.sampler);
        }
        this.heightmapTexture = null;
        this.normalMapTexture = null;
        this.horizonMapTexture = null;
        this.flowMapTexture = null;
        this.splatmap0Texture = null;
        this.splatmap1Texture = null;
        this.sampler = 0L;
    }

    private static byte[] floatsToBytes(final float[] values) {
        final ByteBuffer bb = ByteBuffer.allocate(values.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float v : values) {
            bb.putFloat(v);
        }
        return bb.array();
    }

    private static float clamp(final float v, final float min, final float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float maxValue(final float[] data) {
        float max = 0f;
        for (float v : data) {
            max = Math.max(max, v);
        }
        return max;
    }

    private static long handle(final GpuImage2D image) {
        return image == null ? 0L : image.handle();
    }
}
