module org.dynamisengine.terrain.bench {
    requires org.dynamisengine.terrain.api;
    requires org.dynamisengine.terrain.core;
    requires org.dynamisengine.terrain.vulkan;
    requires org.dynamisengine.terrain.test;
    requires jmh.core;

    exports org.dynamisengine.terrain.bench;
    exports org.dynamisengine.terrain.bench.core;
}
