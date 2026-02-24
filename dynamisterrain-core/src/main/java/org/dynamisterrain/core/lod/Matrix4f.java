package org.dynamisterrain.core.lod;

import org.dynamisterrain.api.state.Vector3f;

public final class Matrix4f {
    private final float[] m;

    private Matrix4f(final float[] m) {
        this.m = m;
    }

    public static Matrix4f identity() {
        return new Matrix4f(new float[] {
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        });
    }

    public static Matrix4f perspective(final float fovYRadians, final float aspect, final float near, final float far) {
        final float f = 1.0f / (float) Math.tan(fovYRadians * 0.5f);
        return new Matrix4f(new float[] {
            f / aspect, 0f, 0f, 0f,
            0f, f, 0f, 0f,
            0f, 0f, (far + near) / (near - far), -1f,
            0f, 0f, (2f * far * near) / (near - far), 0f
        });
    }

    public static Matrix4f lookAt(final Vector3f eye, final Vector3f center, final Vector3f up) {
        final Vector3f f = normalize(sub(center, eye));
        final Vector3f s = normalize(cross(f, up));
        final Vector3f u = cross(s, f);

        return new Matrix4f(new float[] {
            s.x(), u.x(), -f.x(), 0f,
            s.y(), u.y(), -f.y(), 0f,
            s.z(), u.z(), -f.z(), 0f,
            -dot(s, eye), -dot(u, eye), dot(f, eye), 1f
        });
    }

    public static Matrix4f multiply(final Matrix4f a, final Matrix4f b) {
        final float[] out = new float[16];
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                out[r * 4 + c] =
                    a.get(r, 0) * b.get(0, c)
                        + a.get(r, 1) * b.get(1, c)
                        + a.get(r, 2) * b.get(2, c)
                        + a.get(r, 3) * b.get(3, c);
            }
        }
        return new Matrix4f(out);
    }

    public float get(final int row, final int col) {
        return this.m[row * 4 + col];
    }

    private static Vector3f sub(final Vector3f a, final Vector3f b) {
        return new Vector3f(a.x() - b.x(), a.y() - b.y(), a.z() - b.z());
    }

    private static float dot(final Vector3f a, final Vector3f b) {
        return a.x() * b.x() + a.y() * b.y() + a.z() * b.z();
    }

    private static Vector3f cross(final Vector3f a, final Vector3f b) {
        return new Vector3f(
            a.y() * b.z() - a.z() * b.y(),
            a.z() * b.x() - a.x() * b.z(),
            a.x() * b.y() - a.y() * b.x()
        );
    }

    private static Vector3f normalize(final Vector3f v) {
        final float len = (float) Math.sqrt(v.x() * v.x() + v.y() * v.y() + v.z() * v.z());
        if (len == 0.0f) {
            return new Vector3f(0f, 0f, 0f);
        }
        return new Vector3f(v.x() / len, v.y() / len, v.z() / len);
    }
}
