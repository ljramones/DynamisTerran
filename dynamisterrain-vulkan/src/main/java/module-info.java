module org.dynamisterrain.vulkan {
    requires org.dynamisterrain.api;
    requires org.dynamisterrain.core;
    requires org.dynamisgpu.api;
    requires org.dynamisgpu.vulkan;
    requires org.dynamissky.api;
    requires org.dynamissky.vulkan;
    requires org.lwjgl.vulkan;

    exports org.dynamisterrain.vulkan;
    exports org.dynamisterrain.vulkan.lod;
    exports org.dynamisterrain.vulkan.horizon;
    exports org.dynamisterrain.vulkan.material;
    exports org.dynamisterrain.vulkan.foliage;
    exports org.dynamisterrain.vulkan.water;
    exports org.dynamisterrain.vulkan.road;
    exports org.dynamisterrain.vulkan.integration;
}
