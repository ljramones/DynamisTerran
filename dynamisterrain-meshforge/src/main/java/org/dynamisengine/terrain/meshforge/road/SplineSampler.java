package org.dynamisengine.terrain.meshforge.road;

import java.util.ArrayList;
import java.util.List;
import org.dynamisengine.terrain.api.state.Vector3f;

public final class SplineSampler {
    private SplineSampler() {
    }

    public static List<SplinePoint> sample(final List<Vector3f> controlPoints, final float segmentLengthMetres) {
        if (controlPoints == null || controlPoints.size() < 2) {
            throw new IllegalArgumentException("At least 2 control points are required");
        }

        final float step = Math.max(0.1f, segmentLengthMetres);
        final List<SplinePoint> out = new ArrayList<>();
        float arc = 0f;
        Vector3f prev = null;

        final int segments = controlPoints.size() - 1;
        for (int i = 0; i < segments; i++) {
            final Vector3f p0 = controlPoints.get(Math.max(0, i - 1));
            final Vector3f p1 = controlPoints.get(i);
            final Vector3f p2 = controlPoints.get(i + 1);
            final Vector3f p3 = controlPoints.get(Math.min(controlPoints.size() - 1, i + 2));

            final float segLength = distance(p1, p2);
            final int samples = Math.max(1, (int) Math.ceil(segLength / step));
            for (int s = 0; s <= samples; s++) {
                if (i > 0 && s == 0) {
                    continue;
                }
                final float t = s / (float) samples;
                final Vector3f pos = catmullRom(p0, p1, p2, p3, t);
                final Vector3f tan = normalize(sub(catmullRom(p0, p1, p2, p3, Math.min(1f, t + 0.01f)), pos));
                final Vector3f normal = new Vector3f(0f, 1f, 0f);

                if (prev != null) {
                    arc += distance(prev, pos);
                }
                prev = pos;
                out.add(new SplinePoint(pos, tan, normal, arc));
            }
        }

        return List.copyOf(out);
    }

    private static Vector3f catmullRom(final Vector3f p0, final Vector3f p1, final Vector3f p2, final Vector3f p3, final float t) {
        final float t2 = t * t;
        final float t3 = t2 * t;

        final float x = 0.5f * ((2f * p1.x()) + (-p0.x() + p2.x()) * t
            + (2f * p0.x() - 5f * p1.x() + 4f * p2.x() - p3.x()) * t2
            + (-p0.x() + 3f * p1.x() - 3f * p2.x() + p3.x()) * t3);
        final float y = 0.5f * ((2f * p1.y()) + (-p0.y() + p2.y()) * t
            + (2f * p0.y() - 5f * p1.y() + 4f * p2.y() - p3.y()) * t2
            + (-p0.y() + 3f * p1.y() - 3f * p2.y() + p3.y()) * t3);
        final float z = 0.5f * ((2f * p1.z()) + (-p0.z() + p2.z()) * t
            + (2f * p0.z() - 5f * p1.z() + 4f * p2.z() - p3.z()) * t2
            + (-p0.z() + 3f * p1.z() - 3f * p2.z() + p3.z()) * t3);
        return new Vector3f(x, y, z);
    }

    private static Vector3f sub(final Vector3f a, final Vector3f b) {
        return new Vector3f(a.x() - b.x(), a.y() - b.y(), a.z() - b.z());
    }

    private static Vector3f normalize(final Vector3f v) {
        final float len = (float) Math.sqrt(v.x() * v.x() + v.y() * v.y() + v.z() * v.z());
        if (len <= 0f) {
            return new Vector3f(1f, 0f, 0f);
        }
        return new Vector3f(v.x() / len, v.y() / len, v.z() / len);
    }

    private static float distance(final Vector3f a, final Vector3f b) {
        final float dx = a.x() - b.x();
        final float dy = a.y() - b.y();
        final float dz = a.z() - b.z();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
