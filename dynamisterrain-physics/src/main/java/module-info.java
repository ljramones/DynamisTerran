module org.dynamisterrain.physics {
    requires org.dynamisterrain.api;
    requires org.dynamisterrain.core;
    // TODO: restore when DynamisCollision JPMS module is available
    // requires org.dynamiscollision;
    // TODO: restore when DynamisPhysics extracted
    // requires org.dynamisphysics.api;

    exports org.dynamisterrain.physics;
    exports org.dynamisterrain.physics.collision;
    exports org.dynamisterrain.physics.material;
}
