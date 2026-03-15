package org.dynamisengine.terrain.meshforge;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.dynamisengine.terrain.api.descriptor.RoadDesc;
import org.dynamisengine.terrain.api.state.Vector3f;
import org.dynamisengine.terrain.core.heightmap.HeightmapData;
import org.dynamisengine.terrain.core.heightmap.HeightmapOps;
import org.dynamisengine.terrain.meshforge.road.GeneratedRoad;
import org.dynamisengine.terrain.meshforge.road.RoadMeshGenerator;
import org.junit.jupiter.api.Test;

class RoadMeshGeneratorTest {
    @Test
    void generateProducesNonZeroVertexCount() {
        final GeneratedRoad road = RoadMeshGenerator.generate(defaultRoad(), hillHeightmap(128), 1f, 1f, straightControlPoints());
        assertTrue(road.vertexCount() > 0);
        assertTrue(road.indexCount() > 0);
    }

    @Test
    void roadVerticesConformToHeightmap() {
        final HeightmapData hm = hillHeightmap(128);
        final GeneratedRoad road = RoadMeshGenerator.generate(defaultRoad(), hm, 1f, 1f, straightControlPoints());

        final float[] v = road.vertices();
        for (int i = 0; i < road.vertexCount(); i++) {
            final float x = v[i * 8];
            final float y = v[i * 8 + 1];
            final float z = v[i * 8 + 2];
            final float h = HeightmapOps.heightAt(hm, x, z, 1f, 1f);
            assertTrue(Math.abs(y - h) <= 0.5f);
        }
    }

    @Test
    void roadUVsInUnitRange() {
        final GeneratedRoad road = RoadMeshGenerator.generate(defaultRoad(), HeightmapData.empty(64, 64), 1f, 1f, straightControlPoints());
        final float[] v = road.vertices();
        for (int i = 0; i < road.vertexCount(); i++) {
            final float u = v[i * 8 + 6];
            final float vv = v[i * 8 + 7];
            assertTrue(u >= 0f && u <= 1f);
            assertTrue(vv >= 0f);
        }
    }

    @Test
    void splatmaskNonZeroAlongSpline() {
        final GeneratedRoad road = RoadMeshGenerator.generate(defaultRoad(), HeightmapData.empty(128, 128), 1f, 1f, straightControlPoints());
        final int z = 64;
        assertTrue(road.splatmaskData()[z * road.splatmaskWidth() + 64] > 0f);
    }

    @Test
    void splatmaskZeroFarFromSpline() {
        final GeneratedRoad road = RoadMeshGenerator.generate(defaultRoad(), HeightmapData.empty(128, 128), 1f, 1f, straightControlPoints());
        assertTrue(road.splatmaskData()[0] == 0f);
    }

    @Test
    void splatmaskDimensionsMatchHeightmap() {
        final GeneratedRoad road = RoadMeshGenerator.generate(defaultRoad(), HeightmapData.empty(96, 80), 1f, 1f, straightControlPointsFor(95f, 40f));
        assertEquals(96, road.splatmaskWidth());
        assertEquals(80, road.splatmaskHeight());
    }

    @Test
    void straightRoadHasSymmetricEdges() {
        final GeneratedRoad road = RoadMeshGenerator.generate(defaultRoad(), HeightmapData.empty(128, 128), 1f, 1f, straightControlPoints());
        final float[] v = road.vertices();
        final int midPair = (road.vertexCount() / 4) * 2;

        final float lx = v[midPair * 8];
        final float lz = v[midPair * 8 + 2];
        final float rx = v[(midPair + 1) * 8];
        final float rz = v[(midPair + 1) * 8 + 2];

        final float cx = (lx + rx) * 0.5f;
        final float cz = (lz + rz) * 0.5f;

        final float dl = (float) Math.sqrt((lx - cx) * (lx - cx) + (lz - cz) * (lz - cz));
        final float dr = (float) Math.sqrt((rx - cx) * (rx - cx) + (rz - cz) * (rz - cz));
        assertTrue(Math.abs(dl - dr) < 0.01f);
    }

    @Test
    void generateHandlesSingleSegmentRoad() {
        assertDoesNotThrow(() -> RoadMeshGenerator.generate(
            defaultRoad(),
            HeightmapData.empty(64, 64),
            1f,
            1f,
            List.of(new Vector3f(0f, 0f, 32f), new Vector3f(63f, 0f, 32f))
        ));
    }

    private static RoadDesc defaultRoad() {
        return new RoadDesc(true, 8f, 2f, 1f);
    }

    private static List<Vector3f> straightControlPoints() {
        return straightControlPointsFor(127f, 64f);
    }

    private static List<Vector3f> straightControlPointsFor(final float maxX, final float z) {
        return List.of(
            new Vector3f(0f, 0f, z),
            new Vector3f(maxX * 0.5f, 0f, z),
            new Vector3f(maxX, 0f, z)
        );
    }

    private static HeightmapData hillHeightmap(final int size) {
        final HeightmapData hm = HeightmapData.empty(size, size);
        final float cx = size * 0.5f;
        final float cz = size * 0.5f;
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                final float dx = x - cx;
                final float dz = z - cz;
                hm.setPixel(x, z, Math.max(0f, 50f - (float) Math.sqrt(dx * dx + dz * dz)));
            }
        }
        return hm;
    }
}
