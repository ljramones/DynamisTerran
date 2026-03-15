module org.dynamisengine.terrain.meshforge {
    requires org.dynamisengine.terrain.api;
    requires org.dynamisengine.terrain.core;
    // TODO: restore when MeshForge JPMS module is available
    // requires org.dynamisengine.meshforge;
    requires org.dynamisengine.vectrix;

    exports org.dynamisengine.terrain.meshforge;
    exports org.dynamisengine.terrain.meshforge.road;
    exports org.dynamisengine.terrain.meshforge.foliage;
}
