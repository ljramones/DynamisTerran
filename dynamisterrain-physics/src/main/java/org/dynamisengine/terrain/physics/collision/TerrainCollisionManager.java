package org.dynamisengine.terrain.physics.collision;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.dynamisengine.terrain.api.config.MaterialTag;
import org.dynamisengine.terrain.api.state.Vector3f;
import org.dynamisengine.terrain.core.heightmap.HeightmapData;
import org.dynamisengine.terrain.physics.material.PhysicsMaterial;

public final class TerrainCollisionManager {
    public static final float ACTIVE_RADIUS_METRES = 512f;
    public static final int CHUNK_SIZE_TEXELS = 64;

    private final HeightmapData heightmap;
    private final float worldScale;
    private final float heightScale;
    private final CollisionWorld collisionWorld;
    private final Map<Long, TerrainChunk> activeChunks = new HashMap<>();

    private TerrainCollisionManager(
        final HeightmapData heightmap,
        final float worldScale,
        final float heightScale,
        final CollisionWorld collisionWorld
    ) {
        this.heightmap = heightmap;
        this.worldScale = worldScale;
        this.heightScale = heightScale;
        this.collisionWorld = collisionWorld;
    }

    public static TerrainCollisionManager create(
        final HeightmapData heightmap,
        final float worldScale,
        final float heightScale,
        final CollisionWorld collisionWorld
    ) {
        return new TerrainCollisionManager(heightmap, worldScale, heightScale, collisionWorld);
    }

    public void update(final Vector3f cameraPosition) {
        final float chunkWorldSize = CHUNK_SIZE_TEXELS * this.worldScale;
        final int cx = (int) Math.floor(cameraPosition.x() / chunkWorldSize);
        final int cz = (int) Math.floor(cameraPosition.z() / chunkWorldSize);
        final int radiusChunks = Math.max(1, (int) Math.ceil(ACTIVE_RADIUS_METRES / chunkWorldSize));

        final Set<Long> desired = new HashSet<>();
        for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
            for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
                final int chunkX = cx + dx;
                final int chunkZ = cz + dz;

                final float centerX = (chunkX + 0.5f) * chunkWorldSize;
                final float centerZ = (chunkZ + 0.5f) * chunkWorldSize;
                final float dist = distance(cameraPosition.x(), cameraPosition.z(), centerX, centerZ);
                if (dist > ACTIVE_RADIUS_METRES) {
                    continue;
                }

                final long key = key(chunkX, chunkZ);
                desired.add(key);
                if (!this.activeChunks.containsKey(key)) {
                    this.activeChunks.put(key, buildChunk(chunkX, chunkZ));
                }
            }
        }

        final Set<Long> toRemove = new HashSet<>(this.activeChunks.keySet());
        toRemove.removeAll(desired);
        for (Long key : toRemove) {
            final TerrainChunk chunk = this.activeChunks.remove(key);
            this.collisionWorld.removeRigidBody(chunk.rigidBodyHandle());
            this.collisionWorld.destroyShape(chunk.collisionShapeHandle());
        }
    }

    public PhysicsMaterial materialForTag(final MaterialTag tag) {
        return switch (tag) {
            case GRASS -> PhysicsMaterial.of(0.6f, 0.4f, "grass");
            case ROCK -> PhysicsMaterial.of(0.9f, 0.1f, "rock");
            case MUD -> PhysicsMaterial.of(0.4f, 0.8f, "mud");
            case SNOW -> PhysicsMaterial.of(0.3f, 0.6f, "snow");
            case SAND -> PhysicsMaterial.of(0.5f, 0.7f, "sand");
            case ASPHALT -> PhysicsMaterial.of(0.8f, 0.2f, "asphalt");
            case WATER -> PhysicsMaterial.of(0.1f, 0.0f, "water");
            default -> PhysicsMaterial.DEFAULT;
        };
    }

    public int activeChunkCount() {
        return this.activeChunks.size();
    }

    public boolean hasCollisionAt(final float worldX, final float worldZ) {
        final float chunkWorldSize = CHUNK_SIZE_TEXELS * this.worldScale;
        final int chunkX = (int) Math.floor(worldX / chunkWorldSize);
        final int chunkZ = (int) Math.floor(worldZ / chunkWorldSize);
        return this.activeChunks.containsKey(key(chunkX, chunkZ));
    }

    public void destroy() {
        for (TerrainChunk chunk : this.activeChunks.values()) {
            this.collisionWorld.removeRigidBody(chunk.rigidBodyHandle());
            this.collisionWorld.destroyShape(chunk.collisionShapeHandle());
        }
        this.activeChunks.clear();
    }

    private TerrainChunk buildChunk(final int chunkX, final int chunkZ) {
        final int x0 = chunkX * CHUNK_SIZE_TEXELS;
        final int z0 = chunkZ * CHUNK_SIZE_TEXELS;
        final int x1 = x0 + CHUNK_SIZE_TEXELS;
        final int z1 = z0 + CHUNK_SIZE_TEXELS;

        final int width = CHUNK_SIZE_TEXELS + 1;
        final int height = CHUNK_SIZE_TEXELS + 1;
        final float[] heights = new float[width * height];

        int i = 0;
        for (int z = z0; z <= z1; z++) {
            for (int x = x0; x <= x1; x++) {
                heights[i++] = this.heightmap.pixelAt(x, z) * this.heightScale;
            }
        }

        final long shape = this.collisionWorld.createHeightfieldShape(chunkX, chunkZ, width, height, heights, this.worldScale);
        final float worldOriginX = x0 * this.worldScale;
        final float worldOriginZ = z0 * this.worldScale;
        final float worldSize = CHUNK_SIZE_TEXELS * this.worldScale;
        final long body = this.collisionWorld.createStaticBody(shape, worldOriginX, worldOriginZ);

        return new TerrainChunk(chunkX, chunkZ, CHUNK_SIZE_TEXELS, worldOriginX, worldOriginZ, worldSize, shape, body);
    }

    private static float distance(final float x0, final float z0, final float x1, final float z1) {
        final float dx = x1 - x0;
        final float dz = z1 - z0;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    private static long key(final int x, final int z) {
        return (((long) x) << 32) ^ (z & 0xFFFFFFFFL);
    }
}
