module org.dynamisterrain.meshforge {
    requires org.dynamisterrain.api;
    requires org.dynamisterrain.core;
    // TODO: restore when MeshForge JPMS module is available
    // requires org.meshforge;
    requires org.vectrix;

    exports org.dynamisterrain.meshforge;
    exports org.dynamisterrain.meshforge.road;
    exports org.dynamisterrain.meshforge.foliage;
}
