package org.dynamisengine.terrain.test.harness;

public enum CameraPath {
    STATIC,
    FLY_OVER,
    CIRCLE,
    RANDOM_WALK;

    public static CameraPath flyOver(final float speed) {
        return FLY_OVER;
    }
}
