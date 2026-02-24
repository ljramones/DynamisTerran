package org.dynamisterrain.meshforge.road;

import java.util.ArrayList;
import java.util.List;
import org.dynamisterrain.api.descriptor.RoadDesc;
import org.dynamisterrain.api.state.Vector3f;
import org.dynamisterrain.core.heightmap.HeightmapData;
import org.dynamisterrain.core.heightmap.HeightmapOps;

public final class RoadMeshGenerator {
    private RoadMeshGenerator() {
    }

    public static GeneratedRoad generate(
        final RoadDesc road,
        final HeightmapData heightmap,
        final float worldScale,
        final float heightScale
    ) {
        final float worldW = (heightmap.width() - 1) * worldScale;
        final float worldH = (heightmap.height() - 1) * worldScale;
        final List<Vector3f> control = List.of(
            new Vector3f(0f, 0f, worldH * 0.5f),
            new Vector3f(worldW * 0.5f, 0f, worldH * 0.5f),
            new Vector3f(worldW, 0f, worldH * 0.5f)
        );
        return generate(road, heightmap, worldScale, heightScale, control);
    }

    public static GeneratedRoad generate(
        final RoadDesc road,
        final HeightmapData heightmap,
        final float worldScale,
        final float heightScale,
        final List<Vector3f> controlPoints
    ) {
        final List<SplinePoint> samples = SplineSampler.sample(controlPoints, 1.0f);
        final List<Float> verts = new ArrayList<>();
        final List<Integer> idx = new ArrayList<>();

        final float half = Math.max(road.width() * 0.5f, 0.1f);

        for (int i = 0; i < samples.size(); i++) {
            final SplinePoint p = samples.get(i);
            final Vector3f t = p.tangent();
            final Vector3f right = normalize(new Vector3f(t.z(), 0f, -t.x()));
            final float cx = p.position().x();
            final float cz = p.position().z();
            final float cy = HeightmapOps.heightAt(heightmap, cx, cz, worldScale, heightScale);

            final float lx = cx - right.x() * half;
            final float lz = cz - right.z() * half;
            final float ly = HeightmapOps.heightAt(heightmap, lx, lz, worldScale, heightScale);

            final float rx = cx + right.x() * half;
            final float rz = cz + right.z() * half;
            final float ry = HeightmapOps.heightAt(heightmap, rx, rz, worldScale, heightScale);

            putVertex(verts, lx, ly, lz, 0f, 1f, 0f, 0f, p.arcLength() / Math.max(1f, road.width()));
            putVertex(verts, rx, ry, rz, 0f, 1f, 0f, 1f, p.arcLength() / Math.max(1f, road.width()));

            if (i > 0) {
                final int b = i * 2;
                idx.add(b - 2);
                idx.add(b - 1);
                idx.add(b);
                idx.add(b);
                idx.add(b - 1);
                idx.add(b + 1);
            }
        }

        final int maskW = heightmap.width();
        final int maskH = heightmap.height();
        final float[] splat = new float[maskW * maskH];
        final float blendWidth = Math.max(road.shoulderWidth() + road.blendStrength(), 0.1f);

        for (int z = 0; z < maskH; z++) {
            for (int x = 0; x < maskW; x++) {
                final float wx = x * worldScale;
                final float wz = z * worldScale;
                float minDist = Float.MAX_VALUE;
                for (SplinePoint p : samples) {
                    final float dx = wx - p.position().x();
                    final float dz = wz - p.position().z();
                    minDist = Math.min(minDist, (float) Math.sqrt(dx * dx + dz * dz));
                }
                if (minDist <= half + blendWidth) {
                    final float weight = 1f - clamp((minDist - half) / blendWidth, 0f, 1f);
                    splat[z * maskW + x] = weight;
                }
            }
        }

        return new GeneratedRoad(
            toFloatArray(verts),
            toIntArray(idx),
            verts.size() / 8,
            idx.size(),
            splat,
            maskW,
            maskH
        );
    }

    private static void putVertex(
        final List<Float> verts,
        final float x,
        final float y,
        final float z,
        final float nx,
        final float ny,
        final float nz,
        final float u,
        final float v
    ) {
        verts.add(x);
        verts.add(y);
        verts.add(z);
        verts.add(nx);
        verts.add(ny);
        verts.add(nz);
        verts.add(u);
        verts.add(v);
    }

    private static Vector3f normalize(final Vector3f v) {
        final float len = (float) Math.sqrt(v.x() * v.x() + v.y() * v.y() + v.z() * v.z());
        if (len <= 0f) {
            return new Vector3f(1f, 0f, 0f);
        }
        return new Vector3f(v.x() / len, v.y() / len, v.z() / len);
    }

    private static float[] toFloatArray(final List<Float> src) {
        final float[] out = new float[src.size()];
        for (int i = 0; i < src.size(); i++) {
            out[i] = src.get(i);
        }
        return out;
    }

    private static int[] toIntArray(final List<Integer> src) {
        final int[] out = new int[src.size()];
        for (int i = 0; i < src.size(); i++) {
            out[i] = src.get(i);
        }
        return out;
    }

    private static float clamp(final float v, final float min, final float max) {
        return Math.max(min, Math.min(max, v));
    }
}
