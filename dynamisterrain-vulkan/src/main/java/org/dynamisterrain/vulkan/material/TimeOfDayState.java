package org.dynamisterrain.vulkan.material;

public record TimeOfDayState(float normalizedTime) {
    public static final TimeOfDayState DAY = new TimeOfDayState(0.5f);
}
