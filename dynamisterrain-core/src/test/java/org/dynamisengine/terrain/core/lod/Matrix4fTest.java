package org.dynamisengine.terrain.core.lod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.dynamisengine.terrain.api.state.Vector3f;
import org.junit.jupiter.api.Test;

class Matrix4fTest {

    private static final float EPSILON = 1e-5f;

    @Test
    void identityMatrixDiagonalOnes() {
        final Matrix4f id = Matrix4f.identity();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                assertEquals(i == j ? 1f : 0f, id.get(i, j), EPSILON);
            }
        }
    }

    @Test
    void perspectiveNearPlaneMapsCorrectly() {
        final float fov = (float) Math.toRadians(90.0);
        final float near = 0.1f;
        final float far = 100f;
        final Matrix4f p = Matrix4f.perspective(fov, 1.0f, near, far);

        // m[0][0] = f/aspect = 1/tan(45deg)/1 = 1.0
        assertEquals(1.0f, p.get(0, 0), EPSILON);
        // m[1][1] = f = 1/tan(45deg) = 1.0
        assertEquals(1.0f, p.get(1, 1), EPSILON);
        // m[2][3] = -1 (perspective divide)
        assertEquals(-1f, p.get(2, 3), EPSILON);
    }

    @Test
    void perspectiveAspectRatioScalesX() {
        final float fov = (float) Math.toRadians(90.0);
        final Matrix4f wide = Matrix4f.perspective(fov, 2.0f, 0.1f, 100f);
        final Matrix4f square = Matrix4f.perspective(fov, 1.0f, 0.1f, 100f);
        // Wider aspect → smaller m[0][0]
        assertEquals(square.get(0, 0) / 2f, wide.get(0, 0), EPSILON);
    }

    @Test
    void lookAtDownNegativeZProducesExpectedViewMatrix() {
        final Matrix4f view = Matrix4f.lookAt(
            new Vector3f(0f, 0f, 0f),
            new Vector3f(0f, 0f, -1f),
            new Vector3f(0f, 1f, 0f));

        // Right vector = (1,0,0), Up = (0,1,0), Forward stored as -f = (0,0,1)
        assertEquals(1f, view.get(0, 0), EPSILON);
        assertEquals(0f, view.get(0, 1), EPSILON);
        assertEquals(0f, view.get(1, 0), EPSILON);
        assertEquals(1f, view.get(1, 1), EPSILON);
        // -f.z = -(-1) = 1
        assertEquals(1f, view.get(2, 2), EPSILON);
    }

    @Test
    void multiplyIdentityTimesMatrixEqualsMatrix() {
        final float fov = (float) Math.toRadians(60.0);
        final Matrix4f m = Matrix4f.perspective(fov, 1.5f, 1f, 500f);
        final Matrix4f result = Matrix4f.multiply(Matrix4f.identity(), m);
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                assertEquals(m.get(r, c), result.get(r, c), EPSILON);
            }
        }
    }

    @Test
    void multiplyMatrixTimesIdentityEqualsMatrix() {
        final float fov = (float) Math.toRadians(60.0);
        final Matrix4f m = Matrix4f.perspective(fov, 1.5f, 1f, 500f);
        final Matrix4f result = Matrix4f.multiply(m, Matrix4f.identity());
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                assertEquals(m.get(r, c), result.get(r, c), EPSILON);
            }
        }
    }

    @Test
    void multiplyAssociativity() {
        final Matrix4f a = Matrix4f.perspective((float) Math.toRadians(90.0), 1f, 0.1f, 100f);
        final Matrix4f b = Matrix4f.lookAt(
            new Vector3f(5f, 3f, 5f),
            new Vector3f(0f, 0f, 0f),
            new Vector3f(0f, 1f, 0f));
        final Matrix4f c = Matrix4f.lookAt(
            new Vector3f(0f, 0f, 0f),
            new Vector3f(1f, 0f, 0f),
            new Vector3f(0f, 1f, 0f));

        final Matrix4f abThenC = Matrix4f.multiply(Matrix4f.multiply(a, b), c);
        final Matrix4f aThenBc = Matrix4f.multiply(a, Matrix4f.multiply(b, c));

        for (int r = 0; r < 4; r++) {
            for (int c2 = 0; c2 < 4; c2++) {
                assertEquals(abThenC.get(r, c2), aThenBc.get(r, c2), 1e-3f);
            }
        }
    }

    @Test
    void perspectiveTimesLookAtProducesValidViewProjection() {
        final Matrix4f proj = Matrix4f.perspective((float) Math.toRadians(90.0), 1.0f, 0.1f, 100f);
        final Matrix4f view = Matrix4f.lookAt(
            new Vector3f(0f, 0f, 5f),
            new Vector3f(0f, 0f, 0f),
            new Vector3f(0f, 1f, 0f));
        final Matrix4f vp = Matrix4f.multiply(proj, view);

        // The result should be a valid VP matrix: perspective divide row is non-trivial
        // m[2][3] should be -1 from the perspective matrix contribution
        assertEquals(-1f, vp.get(2, 3), EPSILON);
    }

    @Test
    void lookAtTranslatesEyePosition() {
        final Matrix4f view = Matrix4f.lookAt(
            new Vector3f(10f, 0f, 0f),
            new Vector3f(10f, 0f, -1f),
            new Vector3f(0f, 1f, 0f));

        // Translation component: -dot(right, eye) should be -10
        assertEquals(-10f, view.get(3, 0), EPSILON);
    }
}
