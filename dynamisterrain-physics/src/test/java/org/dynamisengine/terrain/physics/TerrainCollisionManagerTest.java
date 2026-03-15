package org.dynamisengine.terrain.physics;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.dynamisengine.terrain.api.config.MaterialTag;
import org.dynamisengine.terrain.api.state.Vector3f;
import org.dynamisengine.terrain.core.heightmap.HeightmapData;
import org.dynamisengine.terrain.physics.collision.CollisionWorld;
import org.dynamisengine.terrain.physics.collision.TerrainCollisionManager;
import org.junit.jupiter.api.Test;

class TerrainCollisionManagerTest {
    @Test
    void createSucceedsWithFlatHeightmap() {
        assertDoesNotThrow(() -> TerrainCollisionManager.create(HeightmapData.empty(256, 256), 1f, 1f, new FakeCollisionWorld()));
    }

    @Test
    void updateActivatesChunksNearCamera() {
        final TerrainCollisionManager mgr = TerrainCollisionManager.create(HeightmapData.empty(256, 256), 1f, 1f, new FakeCollisionWorld());
        mgr.update(new Vector3f(128f, 0f, 128f));
        assertTrue(mgr.activeChunkCount() > 0);
        mgr.destroy();
    }

    @Test
    void updateDeactivatesChunksFarFromCamera() {
        final FakeCollisionWorld world = new FakeCollisionWorld();
        final TerrainCollisionManager mgr = TerrainCollisionManager.create(HeightmapData.empty(1024, 1024), 1f, 1f, world);

        mgr.update(new Vector3f(128f, 0f, 128f));
        final int initial = mgr.activeChunkCount();
        mgr.update(new Vector3f(10_000f, 0f, 10_000f));

        assertTrue(mgr.activeChunkCount() > 0);
        assertTrue(world.removedBodies.size() >= initial);
        mgr.destroy();
    }

    @Test
    void hasCollisionAtReturnsTrueForActiveChunk() {
        final TerrainCollisionManager mgr = TerrainCollisionManager.create(HeightmapData.empty(1024, 1024), 1f, 1f, new FakeCollisionWorld());
        mgr.update(new Vector3f(512f, 0f, 512f));
        assertTrue(mgr.hasCollisionAt(512f, 512f));
        mgr.destroy();
    }

    @Test
    void hasCollisionAtReturnsFalseOutsideRadius() {
        final TerrainCollisionManager mgr = TerrainCollisionManager.create(HeightmapData.empty(1024, 1024), 1f, 1f, new FakeCollisionWorld());
        mgr.update(new Vector3f(0f, 0f, 0f));
        assertTrue(!mgr.hasCollisionAt(2000f, 2000f));
        mgr.destroy();
    }

    @Test
    void materialForTagReturnsCorrectFriction() {
        final TerrainCollisionManager mgr = TerrainCollisionManager.create(HeightmapData.empty(256, 256), 1f, 1f, new FakeCollisionWorld());
        assertTrue(mgr.materialForTag(MaterialTag.ROCK).friction() > mgr.materialForTag(MaterialTag.MUD).friction());
        assertTrue(mgr.materialForTag(MaterialTag.ASPHALT).friction() > mgr.materialForTag(MaterialTag.SNOW).friction());
        mgr.destroy();
    }

    private static final class FakeCollisionWorld implements CollisionWorld {
        private final AtomicLong ids = new AtomicLong(1L);
        private final Set<Long> removedBodies = new HashSet<>();

        @Override
        public long createHeightfieldShape(final int chunkX, final int chunkZ, final int width, final int height, final float[] heights, final float worldScale) {
            return this.ids.getAndIncrement();
        }

        @Override
        public long createStaticBody(final long shapeHandle, final float worldOriginX, final float worldOriginZ) {
            return this.ids.getAndIncrement();
        }

        @Override
        public void removeRigidBody(final long rigidBodyHandle) {
            this.removedBodies.add(rigidBodyHandle);
        }

        @Override
        public void destroyShape(final long shapeHandle) {
            // no-op
        }
    }
}
