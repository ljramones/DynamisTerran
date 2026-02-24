module org.dynamisterrain.vulkan {
    requires org.dynamisterrain.api;
    requires org.dynamisterrain.core;
    requires dynamis.gpu.api;
    requires dynamis.gpu.vulkan;
    requires org.dynamissky.api;
    requires org.dynamissky.vulkan;
    requires org.lwjgl.vulkan;

    exports org.dynamisterrain.vulkan;
    exports org.dynamisterrain.vulkan.lod;
    exports org.dynamisterrain.vulkan.horizon;
}
