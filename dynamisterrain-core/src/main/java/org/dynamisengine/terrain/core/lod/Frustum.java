package org.dynamisengine.terrain.core.lod;

import org.dynamisengine.terrain.api.state.Vector3f;

public final class Frustum {
    private final Plane[] planes;

    private Frustum(final Plane[] planes) {
        this.planes = planes;
    }

    public static Frustum infinite() {
        return new Frustum(new Plane[] {
            new Plane(1f, 0f, 0f, Float.MAX_VALUE),
            new Plane(-1f, 0f, 0f, Float.MAX_VALUE),
            new Plane(0f, 1f, 0f, Float.MAX_VALUE),
            new Plane(0f, -1f, 0f, Float.MAX_VALUE),
            new Plane(0f, 0f, 1f, Float.MAX_VALUE),
            new Plane(0f, 0f, -1f, Float.MAX_VALUE)
        });
    }

    public static Frustum axisAligned(
        final float minX,
        final float minY,
        final float minZ,
        final float maxX,
        final float maxY,
        final float maxZ
    ) {
        return new Frustum(new Plane[] {
            new Plane(1f, 0f, 0f, -minX),
            new Plane(-1f, 0f, 0f, maxX),
            new Plane(0f, 1f, 0f, -minY),
            new Plane(0f, -1f, 0f, maxY),
            new Plane(0f, 0f, 1f, -minZ),
            new Plane(0f, 0f, -1f, maxZ)
        });
    }

    public static Frustum fromViewProjection(final Matrix4f viewProj) {
        final Plane[] p = new Plane[6];

        p[0] = normalize(new Plane(
            viewProj.get(0, 3) + viewProj.get(0, 0),
            viewProj.get(1, 3) + viewProj.get(1, 0),
            viewProj.get(2, 3) + viewProj.get(2, 0),
            viewProj.get(3, 3) + viewProj.get(3, 0)));

        p[1] = normalize(new Plane(
            viewProj.get(0, 3) - viewProj.get(0, 0),
            viewProj.get(1, 3) - viewProj.get(1, 0),
            viewProj.get(2, 3) - viewProj.get(2, 0),
            viewProj.get(3, 3) - viewProj.get(3, 0)));

        p[2] = normalize(new Plane(
            viewProj.get(0, 3) + viewProj.get(0, 1),
            viewProj.get(1, 3) + viewProj.get(1, 1),
            viewProj.get(2, 3) + viewProj.get(2, 1),
            viewProj.get(3, 3) + viewProj.get(3, 1)));

        p[3] = normalize(new Plane(
            viewProj.get(0, 3) - viewProj.get(0, 1),
            viewProj.get(1, 3) - viewProj.get(1, 1),
            viewProj.get(2, 3) - viewProj.get(2, 1),
            viewProj.get(3, 3) - viewProj.get(3, 1)));

        p[4] = normalize(new Plane(
            viewProj.get(0, 3) + viewProj.get(0, 2),
            viewProj.get(1, 3) + viewProj.get(1, 2),
            viewProj.get(2, 3) + viewProj.get(2, 2),
            viewProj.get(3, 3) + viewProj.get(3, 2)));

        p[5] = normalize(new Plane(
            viewProj.get(0, 3) - viewProj.get(0, 2),
            viewProj.get(1, 3) - viewProj.get(1, 2),
            viewProj.get(2, 3) - viewProj.get(2, 2),
            viewProj.get(3, 3) - viewProj.get(3, 2)));

        return new Frustum(p);
    }

    public boolean intersectsSphere(final Vector3f center, final float radius) {
        for (Plane plane : this.planes) {
            if (plane.distance(center.x(), center.y(), center.z()) < -radius) {
                return false;
            }
        }
        return true;
    }

    public boolean intersectsAabb(final Vector3f min, final Vector3f max) {
        for (Plane plane : this.planes) {
            final float px = plane.a >= 0f ? max.x() : min.x();
            final float py = plane.b >= 0f ? max.y() : min.y();
            final float pz = plane.c >= 0f ? max.z() : min.z();
            if (plane.distance(px, py, pz) < 0f) {
                return false;
            }
        }
        return true;
    }

    private static Plane normalize(final Plane p) {
        final float len = (float) Math.sqrt(p.a * p.a + p.b * p.b + p.c * p.c);
        return new Plane(p.a / len, p.b / len, p.c / len, p.d / len);
    }

    private static final class Plane {
        private final float a;
        private final float b;
        private final float c;
        private final float d;

        private Plane(final float a, final float b, final float c, final float d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }

        private float distance(final float x, final float y, final float z) {
            return this.a * x + this.b * y + this.c * z + this.d;
        }
    }
}
