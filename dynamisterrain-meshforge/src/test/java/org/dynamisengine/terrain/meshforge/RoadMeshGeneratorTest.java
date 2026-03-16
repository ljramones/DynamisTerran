package org.dynamisengine.terrain.meshforge;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Test
    void straightRoadProducesParallelEdges() {
        final GeneratedRoad road = RoadMeshGenerator.generate(
            defaultRoad(), HeightmapData.empty(128, 128), 1f, 1f, straightControlPoints());
        final float[] v = road.vertices();
        // For a straight road along x-axis at constant z, all left vertices should share the same z,
        // and all right vertices should share the same z.
        final float leftZ = v[2]; // first left vertex z
        final float rightZ = v[8 + 2]; // first right vertex z
        for (int i = 0; i < road.vertexCount(); i += 2) {
            final float lz = v[i * 8 + 2];
            final float rz = v[(i + 1) * 8 + 2];
            assertTrue(Math.abs(lz - leftZ) < 0.5f,
                "Left edge z should be parallel, got " + lz + " vs " + leftZ);
            assertTrue(Math.abs(rz - rightZ) < 0.5f,
                "Right edge z should be parallel, got " + rz + " vs " + rightZ);
        }
    }

    @Test
    void roadWithCurveProducesNonZeroVertexCount() {
        final List<Vector3f> curvedPoints = List.of(
            new Vector3f(0f, 0f, 32f),
            new Vector3f(32f, 0f, 64f),
            new Vector3f(64f, 0f, 96f),
            new Vector3f(96f, 0f, 64f)
        );
        final GeneratedRoad road = RoadMeshGenerator.generate(
            defaultRoad(), HeightmapData.empty(128, 128), 1f, 1f, curvedPoints);
        assertTrue(road.vertexCount() > 0, "Curved road should produce vertices");
        assertTrue(road.indexCount() > 0, "Curved road should produce indices");
    }

    @Test
    void roadWidthParameterAffectsMeshWidth() {
        final RoadDesc narrow = new RoadDesc(true, 4f, 2f, 1f);
        final RoadDesc wide = new RoadDesc(true, 16f, 2f, 1f);
        final HeightmapData hm = HeightmapData.empty(128, 128);

        final GeneratedRoad narrowRoad = RoadMeshGenerator.generate(narrow, hm, 1f, 1f, straightControlPoints());
        final GeneratedRoad wideRoad = RoadMeshGenerator.generate(wide, hm, 1f, 1f, straightControlPoints());

        // Measure width at first sample: distance between left and right vertex
        final float narrowWidth = Math.abs(narrowRoad.vertices()[2] - narrowRoad.vertices()[8 + 2]);
        final float wideWidth = Math.abs(wideRoad.vertices()[2] - wideRoad.vertices()[8 + 2]);

        assertTrue(wideWidth > narrowWidth, "Wider road desc should produce wider mesh");
    }

    @Test
    void blendWeightsFallOffWithDistanceFromRoadCenter() {
        final GeneratedRoad road = RoadMeshGenerator.generate(
            defaultRoad(), HeightmapData.empty(128, 128), 1f, 1f, straightControlPoints());
        final float[] splat = road.splatmaskData();
        final int z = 64; // road runs along z=64

        // Weight at road center should be 1.0
        final float centerWeight = splat[z * 128 + 64];
        assertEquals(1f, centerWeight, 0.01f, "Center weight should be 1.0");

        // Weight should decrease as we move away from road center (z direction)
        float prevWeight = centerWeight;
        for (int offset = 1; offset <= 20; offset++) {
            final int row = z + offset;
            if (row >= 128) {
                break;
            }
            final float w = splat[row * 128 + 64];
            assertTrue(w <= prevWeight + 0.01f,
                "Weight should not increase with distance, at offset " + offset);
            prevWeight = w;
        }
    }

    @Test
    void zeroLengthRoadSinglePointHandlesGracefully() {
        // SplineSampler requires at least 2 control points; a "zero-length" road
        // uses two identical points.
        final List<Vector3f> singleSpot = List.of(
            new Vector3f(32f, 0f, 32f),
            new Vector3f(32f, 0f, 32f)
        );
        assertDoesNotThrow(() -> RoadMeshGenerator.generate(
            defaultRoad(), HeightmapData.empty(64, 64), 1f, 1f, singleSpot));
    }

    @Test
    void sharpTurnProducesValidGeometry() {
        final List<Vector3f> sharpTurn = List.of(
            new Vector3f(10f, 0f, 64f),
            new Vector3f(64f, 0f, 64f),
            new Vector3f(64f, 0f, 10f)
        );
        final GeneratedRoad road = RoadMeshGenerator.generate(
            defaultRoad(), HeightmapData.empty(128, 128), 1f, 1f, sharpTurn);

        assertTrue(road.vertexCount() > 0, "Sharp turn should produce vertices");
        assertTrue(road.indexCount() > 0, "Sharp turn should produce indices");

        // Check no degenerate triangles (indices within bounds)
        final int[] idx = road.indices();
        for (int i = 0; i < road.indexCount(); i++) {
            assertTrue(idx[i] >= 0 && idx[i] < road.vertexCount(),
                "Index " + idx[i] + " out of bounds [0, " + road.vertexCount() + ")");
        }

        // Check no zero-area triangles by verifying the three vertices aren't colinear
        final float[] v = road.vertices();
        for (int i = 0; i + 2 < road.indexCount(); i += 3) {
            final int i0 = idx[i], i1 = idx[i + 1], i2 = idx[i + 2];
            final float ax = v[i1 * 8] - v[i0 * 8];
            final float az = v[i1 * 8 + 2] - v[i0 * 8 + 2];
            final float bx = v[i2 * 8] - v[i0 * 8];
            final float bz = v[i2 * 8 + 2] - v[i0 * 8 + 2];
            final float cross = Math.abs(ax * bz - az * bx);
            assertTrue(cross > 1e-6f,
                "Triangle " + (i / 3) + " is degenerate (cross product ~ 0)");
        }
    }

    @Test
    void generatedRoadHasUnitLengthNormals() {
        final GeneratedRoad road = RoadMeshGenerator.generate(
            defaultRoad(), hillHeightmap(128), 1f, 1f, straightControlPoints());
        final float[] v = road.vertices();
        for (int i = 0; i < road.vertexCount(); i++) {
            final float nx = v[i * 8 + 3];
            final float ny = v[i * 8 + 4];
            final float nz = v[i * 8 + 5];
            final float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            assertTrue(Math.abs(len - 1f) < 0.01f,
                "Normal at vertex " + i + " should be unit length, got " + len);
        }
    }

    @Test
    void roadVerticesLieOnFlatPlaneForFlatHeightmap() {
        final GeneratedRoad road = RoadMeshGenerator.generate(
            defaultRoad(), HeightmapData.empty(128, 128), 1f, 1f, straightControlPoints());
        final float[] v = road.vertices();
        for (int i = 0; i < road.vertexCount(); i++) {
            final float y = v[i * 8 + 1];
            assertEquals(0f, y, 0.01f,
                "Vertex " + i + " y should be 0 on flat heightmap, got " + y);
        }
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
