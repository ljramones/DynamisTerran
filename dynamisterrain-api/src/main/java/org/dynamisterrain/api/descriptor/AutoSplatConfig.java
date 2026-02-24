package org.dynamisterrain.api.descriptor;

public record AutoSplatConfig(
    boolean enabled,
    float slopeWeight,
    float heightWeight,
    float flowWeight
) {
}
