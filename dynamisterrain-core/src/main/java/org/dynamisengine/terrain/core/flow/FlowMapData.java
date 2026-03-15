package org.dynamisengine.terrain.core.flow;

import java.util.Arrays;

public final class FlowMapData {
    private final float[] accumulation;
    private final int width;
    private final int height;

    private FlowMapData(final float[] accumulation, final int width, final int height) {
        this.accumulation = accumulation;
        this.width = width;
        this.height = height;
    }

    public static FlowMapData of(final float[] accumulation, final int w, final int h) {
        if (accumulation == null) {
            throw new IllegalArgumentException("accumulation cannot be null");
        }
        if (w <= 0 || h <= 0 || accumulation.length != w * h) {
            throw new IllegalArgumentException("invalid flow map dimensions");
        }
        return new FlowMapData(Arrays.copyOf(accumulation, accumulation.length), w, h);
    }

    public float accumulationAt(final int x, final int z) {
        final int cx = Math.max(0, Math.min(this.width - 1, x));
        final int cz = Math.max(0, Math.min(this.height - 1, z));
        return this.accumulation[cz * this.width + cx];
    }

    public float accumulationAtWorld(final float worldX, final float worldZ, final float worldScale) {
        final float tx = clamp(worldX / worldScale, 0f, this.width - 1f);
        final float tz = clamp(worldZ / worldScale, 0f, this.height - 1f);
        final int x0 = (int) Math.floor(tx);
        final int z0 = (int) Math.floor(tz);
        final int x1 = Math.min(x0 + 1, this.width - 1);
        final int z1 = Math.min(z0 + 1, this.height - 1);
        final float fx = tx - x0;
        final float fz = tz - z0;

        final float a = lerp(accumulationAt(x0, z0), accumulationAt(x1, z0), fx);
        final float b = lerp(accumulationAt(x0, z1), accumulationAt(x1, z1), fx);
        return lerp(a, b, fz);
    }

    public float[] rawData() {
        return this.accumulation;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    private static float lerp(final float a, final float b, final float t) {
        return a + (b - a) * t;
    }

    private static float clamp(final float v, final float min, final float max) {
        return Math.max(min, Math.min(max, v));
    }
}
