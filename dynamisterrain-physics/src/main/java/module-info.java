module org.dynamisengine.terrain.physics {
    requires org.dynamisengine.terrain.api;
    requires org.dynamisengine.terrain.core;
    requires collision.detection;
    requires org.dynamisphysics.api;

    exports org.dynamisengine.terrain.physics;
    exports org.dynamisengine.terrain.physics.collision;
    exports org.dynamisengine.terrain.physics.material;
}
