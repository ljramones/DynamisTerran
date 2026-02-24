package org.dynamisterrain.test.assertions;

import java.util.List;
import org.dynamisterrain.api.config.MaterialTag;
import org.dynamisterrain.api.descriptor.FoliageLayer;
import org.dynamisterrain.api.event.SurfaceContactEvent;
import org.dynamisterrain.api.state.Vector3f;
import org.dynamisterrain.core.heightmap.HeightmapData;
import org.dynamisterrain.core.heightmap.HeightmapOps;
import org.dynamisterrain.core.scatter.ScatterResult;

public final class TerrainAssertions {
    private TerrainAssertions() {
    }

    public static void assertHeightInRange(final float height, final float minM, final float maxM) {
        if (height < minM || height > maxM) {
            throw new AssertionError("Height out of range: " + height + " not in [" + minM + ", " + maxM + "]");
        }
    }

    public static void assertNormalizedNormal(final Vector3f normal) {
        final float len = (float) Math.sqrt(normal.x() * normal.x() + normal.y() * normal.y() + normal.z() * normal.z());
        if (Math.abs(len - 1f) > 0.01f) {
            throw new AssertionError("Normal not normalized: len=" + len);
        }
    }

    public static void assertMaterialTagValid(final MaterialTag tag) {
        if (tag == null) {
            throw new AssertionError("Material tag is null");
        }
    }

    public static void assertScatterDeterministic(final ScatterResult r1, final ScatterResult r2) {
        if (r1.count() != r2.count()) {
            throw new AssertionError("Scatter count mismatch");
        }
        for (int i = 0; i < r1.count(); i++) {
            if (r1.points().get(i).worldX() != r2.points().get(i).worldX()
                || r1.points().get(i).worldY() != r2.points().get(i).worldY()
                || r1.points().get(i).worldZ() != r2.points().get(i).worldZ()) {
                throw new AssertionError("Scatter point mismatch at index " + i);
            }
        }
    }

    public static void assertScatterRulesRespected(
        final ScatterResult result,
        final FoliageLayer layer,
        final HeightmapData hm,
        final float worldScale,
        final float heightScale
    ) {
        final float[] normals = HeightmapOps.generateNormals(hm, worldScale, heightScale);
        for (var p : result.points()) {
            final float h = HeightmapOps.heightAt(hm, p.worldX(), p.worldZ(), worldScale, heightScale);
            if (h < layer.minAlt() || h > layer.maxAlt()) {
                throw new AssertionError("Point altitude outside layer range");
            }
            final int x = clamp((int) (p.worldX() / worldScale), 0, hm.width() - 1);
            final int z = clamp((int) (p.worldZ() / worldScale), 0, hm.height() - 1);
            final int idx = (z * hm.width() + x) * 3;
            final float ny = Math.max(-1f, Math.min(1f, normals[idx + 1]));
            final float slope = (float) Math.toDegrees(Math.acos(ny));
            if (slope < layer.minSlope() || slope > layer.maxSlope()) {
                throw new AssertionError("Point slope outside layer range");
            }
        }
    }

    public static void assertContactEventsHaveValidMaterials(final List<SurfaceContactEvent> events) {
        for (SurfaceContactEvent event : events) {
            if (event.material() == null) {
                throw new AssertionError("Contact material is null");
            }
        }
    }

    public static void assertHeightMonotonicallyIncreases(final List<Float> heights) {
        for (int i = 1; i < heights.size(); i++) {
            if (heights.get(i) < heights.get(i - 1)) {
                throw new AssertionError("Height not monotonic at " + i);
            }
        }
    }

    public static void assertAllHeightsNonNegative(final List<Float> heights) {
        for (Float h : heights) {
            if (h < 0f) {
                throw new AssertionError("Height is negative: " + h);
            }
        }
    }

    private static int clamp(final int v, final int min, final int max) {
        return Math.max(min, Math.min(max, v));
    }
}
