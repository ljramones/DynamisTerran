package org.dynamisterrain.physics.collision;

public interface CollisionWorld {
    long createHeightfieldShape(int chunkX, int chunkZ, int width, int height, float[] heights, float worldScale);

    long createStaticBody(long shapeHandle, float worldOriginX, float worldOriginZ);

    void removeRigidBody(long rigidBodyHandle);

    void destroyShape(long shapeHandle);
}
