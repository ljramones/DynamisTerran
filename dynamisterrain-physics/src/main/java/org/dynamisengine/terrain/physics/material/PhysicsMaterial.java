package org.dynamisengine.terrain.physics.material;

public record PhysicsMaterial(
    float friction,
    float restitution,
    String name
) {
    public static final PhysicsMaterial DEFAULT = new PhysicsMaterial(0.6f, 0.3f, "default");

    public static PhysicsMaterial of(final float friction, final float restitution, final String name) {
        return new PhysicsMaterial(friction, restitution, name);
    }
}
