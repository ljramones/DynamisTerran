package org.dynamisterrain.api.service;

/**
 * Typed source for terrain-relevant sky state.
 */
@FunctionalInterface
public interface TerrainSkyStateSource {
    TerrainSkyState currentTerrainSkyState();
}
