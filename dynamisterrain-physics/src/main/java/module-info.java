module org.dynamisengine.terrain.physics {
    requires org.dynamisengine.terrain.api;
    requires org.dynamisengine.terrain.core;
    // TODO: restore when DynamisCollision JPMS module is available
    // requires org.dynamiscollision;
    // TODO: restore when DynamisPhysics extracted
    // requires org.dynamisphysics.api;

    exports org.dynamisengine.terrain.physics;
    exports org.dynamisengine.terrain.physics.collision;
    exports org.dynamisengine.terrain.physics.material;
}
