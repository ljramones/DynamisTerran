module org.dynamisengine.terrain.core {
    requires org.dynamisengine.terrain.api;
    requires org.dynamisengine.vectrix;

    exports org.dynamisengine.terrain.core;
    exports org.dynamisengine.terrain.core.heightmap;
    exports org.dynamisengine.terrain.core.lod;
    exports org.dynamisengine.terrain.core.flow;
    exports org.dynamisengine.terrain.core.scatter;
    exports org.dynamisengine.terrain.core.material;
    exports org.dynamisengine.terrain.core.procedural;
    exports org.dynamisengine.terrain.core.builder;
}
