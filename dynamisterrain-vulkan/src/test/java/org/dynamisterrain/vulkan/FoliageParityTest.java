package org.dynamisterrain.vulkan;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import org.dynamisterrain.api.descriptor.FoliageLayer;
import org.dynamisterrain.api.state.Vector3f;
import org.dynamisterrain.core.scatter.ScatterPoint;
import org.dynamisterrain.core.scatter.ScatterResult;
import org.dynamisterrain.vulkan.foliage.FoliageCullPass;
import org.dynamisterrain.vulkan.foliage.FoliageCullUbo;
import org.dynamisterrain.vulkan.foliage.FoliageDescriptorSets;
import org.dynamisterrain.vulkan.foliage.FoliageDrawPipeline;
import org.dynamisterrain.vulkan.foliage.FoliageInstanceBuffer;
import org.dynamisterrain.vulkan.material.WeatherState;
import org.junit.jupiter.api.Test;

class FoliageParityTest {
    @Test
    void foliageInstanceBufferUploadsWithoutError() {
        final FoliageInstanceBuffer buf = FoliageInstanceBuffer.upload(
            1L,
            new InMemoryGpuMemoryOps(),
            List.of(scatterResult(100, 0, 10f)),
            1f,
            1f,
            1L
        );
        assertNotEquals(0L, buf.instanceBuffer().handle());
        assertTrue(buf.totalInstanceCount() == 100);
        buf.destroy();
    }

    @Test
    void foliageCullPassCompletesWithoutValidationErrors() {
        final FoliageInstanceBuffer buf = FoliageInstanceBuffer.upload(
            1L,
            new InMemoryGpuMemoryOps(),
            List.of(scatterResult(100, 0, 20f)),
            1f,
            1f,
            1L
        );
        final FoliageCullPass pass = FoliageCullPass.create(1L, new InMemoryGpuMemoryOps());

        assertDoesNotThrow(() -> pass.cull(1L, buf, 0L, 0L, fullViewUbo(100, 1)));
        buf.destroy();
    }

    @Test
    void foliageCullPassReducesInstancesWithNarrowFrustum() {
        final FoliageInstanceBuffer buf = FoliageInstanceBuffer.upload(
            1L,
            new InMemoryGpuMemoryOps(),
            List.of(scatterResult(100, 0, 1000f)),
            1f,
            1f,
            1L
        );
        final FoliageCullPass pass = FoliageCullPass.create(1L, new InMemoryGpuMemoryOps());

        pass.cull(1L, buf, 0L, 0L, narrowViewUbo(100, 1));
        final int visible = ByteBuffer.wrap(buf.visibleCountBuffer().data()).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
        assertTrue(visible < 100);
        buf.destroy();
    }

    @Test
    void foliageDrawPipelineCreatesWithoutError() {
        final FoliageDrawPipeline pipeline = FoliageDrawPipeline.create(1L, 2L, 3L);
        assertNotEquals(0L, pipeline.pipelineHandle());
        pipeline.destroy();
    }

    @Test
    void foliageWindUboWritesWithoutError() {
        final FoliageInstanceBuffer buf = FoliageInstanceBuffer.upload(
            1L,
            new InMemoryGpuMemoryOps(),
            List.of(scatterResult(10, 0, 10f)),
            1f,
            1f,
            1L
        );
        final FoliageDescriptorSets sets = FoliageDescriptorSets.create(
            1L,
            buf,
            1L,
            List.of(new FoliageLayer("grass", 1f, 0f, 45f, 0f, 1000f, 1f)),
            2
        );
        assertDoesNotThrow(() -> sets.writeWindUbo(WeatherState.HEAVY_RAIN, 12.5f, 0));
        sets.destroy();
        buf.destroy();
    }

    @Test
    void fullFoliagePassRecordsWithoutValidationErrors() {
        final FoliageInstanceBuffer buf = FoliageInstanceBuffer.upload(
            1L,
            new InMemoryGpuMemoryOps(),
            List.of(scatterResult(100, 0, 15f)),
            1f,
            1f,
            1L
        );
        final FoliageCullPass pass = FoliageCullPass.create(1L, new InMemoryGpuMemoryOps());
        final FoliageDescriptorSets sets = FoliageDescriptorSets.create(
            1L,
            buf,
            1L,
            List.of(new FoliageLayer("grass", 1f, 0f, 45f, 0f, 1000f, 1f)),
            1
        );
        final FoliageDrawPipeline pipeline = FoliageDrawPipeline.create(1L, 2L, 3L);

        assertDoesNotThrow(() -> {
            pass.cull(1L, buf, 0L, 0L, fullViewUbo(100, 1));
            pipeline.record(1L, buf, sets, 1, 0);
        });
        assertTrue(pipeline.lastRecordedDrawCount() >= 1);

        pipeline.destroy();
        sets.destroy();
        buf.destroy();
    }

    private static FoliageCullUbo fullViewUbo(final int totalInstances, final int layerCount) {
        return new FoliageCullUbo(
            new Vector3f(0f, 0f, 0f),
            5000f,
            256f,
            256f,
            totalInstances,
            layerCount,
            new float[] {
                1f, 0f, 0f, 1_000_000f,
                -1f, 0f, 0f, 1_000_000f,
                0f, 1f, 0f, 1_000_000f,
                0f, -1f, 0f, 1_000_000f,
                0f, 0f, 1f, 1_000_000f,
                0f, 0f, -1f, 1_000_000f
            }
        );
    }

    private static FoliageCullUbo narrowViewUbo(final int totalInstances, final int layerCount) {
        return new FoliageCullUbo(
            new Vector3f(0f, 0f, 0f),
            5000f,
            256f,
            256f,
            totalInstances,
            layerCount,
            new float[] {
                1f, 0f, 0f, 1_000_000f,
                -1f, 0f, 0f, 1_000_000f,
                0f, 1f, 0f, 1_000_000f,
                0f, -1f, 0f, 1_000_000f,
                0f, 0f, -1f, 0f,
                0f, 0f, -1f, 1_000_000f
            }
        );
    }

    private static ScatterResult scatterResult(final int count, final int layerIndex, final float z) {
        final List<ScatterPoint> points = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            points.add(new ScatterPoint(i, 10f, z, 0f, 1f));
        }
        return new ScatterResult(points, points.size(), layerIndex);
    }
}
