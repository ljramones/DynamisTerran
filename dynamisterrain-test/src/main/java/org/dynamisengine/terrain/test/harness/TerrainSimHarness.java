package org.dynamisengine.terrain.test.harness;

import java.util.ArrayList;
import java.util.List;
import org.dynamisengine.terrain.api.config.MaterialTag;
import org.dynamisengine.terrain.api.event.SurfaceContactEvent;
import org.dynamisengine.terrain.api.service.TerrainService;
import org.dynamisengine.terrain.api.state.CameraState;
import org.dynamisengine.terrain.api.state.TerrainFrameContext;
import org.dynamisengine.terrain.api.state.Vector3f;
import org.dynamisengine.terrain.test.mock.MockTerrainService;

public final class TerrainSimHarness {
    private TerrainSimHarness() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private TerrainService service;
        private CameraPath cameraPath = CameraPath.STATIC;
        private int steps = 1;
        private float deltaSeconds = 1f / 60f;
        private List<WeatherState> weather = List.of(WeatherState.CLEAR);

        public Builder service(final TerrainService service) {
            this.service = service;
            return this;
        }

        public Builder cameraPath(final CameraPath path) {
            this.cameraPath = path;
            return this;
        }

        public Builder steps(final int steps) {
            this.steps = Math.max(1, steps);
            return this;
        }

        public Builder deltaSeconds(final float dt) {
            this.deltaSeconds = dt;
            return this;
        }

        public Builder weatherSequence(final List<WeatherState> weather) {
            this.weather = (weather == null || weather.isEmpty()) ? List.of(WeatherState.CLEAR) : List.copyOf(weather);
            return this;
        }

        public TerrainSimResult run() {
            final TerrainService terrain = this.service == null ? MockTerrainService.flat(128) : this.service;

            final List<Vector3f> camera = new ArrayList<>();
            final List<Float> heights = new ArrayList<>();
            final List<MaterialTag> materials = new ArrayList<>();
            final List<SurfaceContactEvent> contacts = new ArrayList<>();

            for (int i = 0; i < this.steps; i++) {
                final Vector3f pos = positionAt(i, this.steps, this.cameraPath);
                camera.add(pos);

                final CameraState cam = new CameraState(pos, 0.1f, 10_000f, pos, pos, pos);
                final TerrainFrameContext frame = new TerrainFrameContext(0L, cam, this.deltaSeconds, i);
                terrain.update(frame);

                heights.add(terrain.heightAt(pos.x(), pos.z()));
                materials.add(terrain.materialAt(pos.x(), pos.z()));

                if (terrain instanceof MockTerrainService mock) {
                    contacts.addAll(mock.drainContactEvents());
                }
            }

            final int updates = terrain instanceof MockTerrainService mock ? mock.updateCallCount() : this.steps;
            return new TerrainSimResult(List.copyOf(camera), List.copyOf(heights), List.copyOf(materials), List.copyOf(contacts), this.steps, updates);
        }

        private static Vector3f positionAt(final int index, final int steps, final CameraPath path) {
            return switch (path) {
                case STATIC -> new Vector3f(32f, 40f, 32f);
                case FLY_OVER -> new Vector3f(index * 4f, 60f, 32f);
                case CIRCLE -> {
                    final float t = (float) (index / (double) Math.max(steps, 1) * Math.PI * 2.0);
                    yield new Vector3f(64f + (float) Math.cos(t) * 32f, 60f, 64f + (float) Math.sin(t) * 32f);
                }
                case RANDOM_WALK -> {
                    final float x = 32f + (index * 13 % 17) * 1.7f;
                    final float z = 32f + (index * 7 % 19) * 1.3f;
                    yield new Vector3f(x, 60f, z);
                }
            };
        }
    }
}
