package org.dynamisengine.terrain.core.lod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dynamisengine.terrain.api.state.Vector3f;
import org.junit.jupiter.api.Test;

class FrustumTest {

    @Test
    void aabbFullyInsideFrustumIntersects() {
        final Frustum frustum = Frustum.axisAligned(-10f, -10f, -10f, 10f, 10f, 10f);
        assertTrue(frustum.intersectsAabb(new Vector3f(-1f, -1f, -1f), new Vector3f(1f, 1f, 1f)));
    }

    @Test
    void aabbFullyOutsideLeftPlane() {
        final Frustum frustum = Frustum.axisAligned(-10f, -10f, -10f, 10f, 10f, 10f);
        assertFalse(frustum.intersectsAabb(new Vector3f(-20f, -1f, -1f), new Vector3f(-15f, 1f, 1f)));
    }

    @Test
    void aabbFullyOutsideRightPlane() {
        final Frustum frustum = Frustum.axisAligned(-10f, -10f, -10f, 10f, 10f, 10f);
        assertFalse(frustum.intersectsAabb(new Vector3f(15f, -1f, -1f), new Vector3f(20f, 1f, 1f)));
    }

    @Test
    void aabbFullyOutsideBottomPlane() {
        final Frustum frustum = Frustum.axisAligned(-10f, -10f, -10f, 10f, 10f, 10f);
        assertFalse(frustum.intersectsAabb(new Vector3f(-1f, -20f, -1f), new Vector3f(1f, -15f, 1f)));
    }

    @Test
    void aabbFullyOutsideTopPlane() {
        final Frustum frustum = Frustum.axisAligned(-10f, -10f, -10f, 10f, 10f, 10f);
        assertFalse(frustum.intersectsAabb(new Vector3f(-1f, 15f, -1f), new Vector3f(1f, 20f, 1f)));
    }

    @Test
    void aabbFullyOutsideNearPlane() {
        final Frustum frustum = Frustum.axisAligned(-10f, -10f, -10f, 10f, 10f, 10f);
        assertFalse(frustum.intersectsAabb(new Vector3f(-1f, -1f, -20f), new Vector3f(1f, 1f, -15f)));
    }

    @Test
    void aabbFullyOutsideFarPlane() {
        final Frustum frustum = Frustum.axisAligned(-10f, -10f, -10f, 10f, 10f, 10f);
        assertFalse(frustum.intersectsAabb(new Vector3f(-1f, -1f, 15f), new Vector3f(1f, 1f, 20f)));
    }

    @Test
    void aabbStraddlingFrustumEdgeIntersects() {
        final Frustum frustum = Frustum.axisAligned(-10f, -10f, -10f, 10f, 10f, 10f);
        assertTrue(frustum.intersectsAabb(new Vector3f(8f, -1f, -1f), new Vector3f(12f, 1f, 1f)));
    }

    @Test
    void sphereFullyInsideIntersects() {
        final Frustum frustum = Frustum.axisAligned(-10f, -10f, -10f, 10f, 10f, 10f);
        assertTrue(frustum.intersectsSphere(new Vector3f(0f, 0f, 0f), 2f));
    }

    @Test
    void sphereFullyOutsideDoesNotIntersect() {
        final Frustum frustum = Frustum.axisAligned(-10f, -10f, -10f, 10f, 10f, 10f);
        assertFalse(frustum.intersectsSphere(new Vector3f(25f, 0f, 0f), 2f));
    }

    @Test
    void sphereTouchingFrustumBoundaryIntersects() {
        final Frustum frustum = Frustum.axisAligned(-10f, -10f, -10f, 10f, 10f, 10f);
        assertTrue(frustum.intersectsSphere(new Vector3f(11f, 0f, 0f), 1.5f));
    }

    @Test
    void largeAabbContainingEntireFrustumIntersects() {
        final Frustum frustum = Frustum.axisAligned(-10f, -10f, -10f, 10f, 10f, 10f);
        assertTrue(frustum.intersectsAabb(new Vector3f(-100f, -100f, -100f), new Vector3f(100f, 100f, 100f)));
    }

    @Test
    void zeroSizeAabbAtOriginInsideFrustumIntersects() {
        final Frustum frustum = Frustum.axisAligned(-10f, -10f, -10f, 10f, 10f, 10f);
        assertTrue(frustum.intersectsAabb(new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, 0f)));
    }

    @Test
    void veryFarAabbBeyondFarPlaneDoesNotIntersect() {
        final Frustum frustum = Frustum.axisAligned(-10f, -10f, -10f, 10f, 10f, 10f);
        assertFalse(frustum.intersectsAabb(
            new Vector3f(1000f, 1000f, 1000f), new Vector3f(2000f, 2000f, 2000f)));
    }

    @Test
    void frustumFromIdentityMatrix() {
        final Frustum frustum = Frustum.fromViewProjection(Matrix4f.identity());
        // Identity clip space: x,y,z in [-1,1]. Points inside should intersect.
        assertTrue(frustum.intersectsAabb(new Vector3f(-0.5f, -0.5f, -0.5f), new Vector3f(0.5f, 0.5f, 0.5f)));
        // Points far outside should not intersect.
        assertFalse(frustum.intersectsAabb(new Vector3f(10f, 10f, 10f), new Vector3f(20f, 20f, 20f)));
    }

    @Test
    void frustumFromPerspectiveLookAtCombination() {
        final float fov = (float) Math.toRadians(90.0);
        final Matrix4f proj = Matrix4f.perspective(fov, 1.0f, 0.1f, 100f);
        final Matrix4f view = Matrix4f.lookAt(
            new Vector3f(0f, 0f, 0f),
            new Vector3f(0f, 0f, -1f),
            new Vector3f(0f, 1f, 0f));
        final Matrix4f viewProj = Matrix4f.multiply(proj, view);
        final Frustum frustum = Frustum.fromViewProjection(viewProj);

        // Point directly in front of camera, within frustum
        assertTrue(frustum.intersectsAabb(
            new Vector3f(-1f, -1f, -10f), new Vector3f(1f, 1f, -5f)));

        // Point behind camera should be culled
        assertFalse(frustum.intersectsAabb(
            new Vector3f(-1f, -1f, 5f), new Vector3f(1f, 1f, 10f)));

        // Point beyond far plane should be culled
        assertFalse(frustum.intersectsAabb(
            new Vector3f(-1f, -1f, -200f), new Vector3f(1f, 1f, -150f)));
    }

    @Test
    void infiniteFrustumAcceptsEverything() {
        final Frustum frustum = Frustum.infinite();
        assertTrue(frustum.intersectsAabb(
            new Vector3f(-99999f, -99999f, -99999f), new Vector3f(99999f, 99999f, 99999f)));
        assertTrue(frustum.intersectsSphere(new Vector3f(50000f, 50000f, 50000f), 1f));
    }
}
