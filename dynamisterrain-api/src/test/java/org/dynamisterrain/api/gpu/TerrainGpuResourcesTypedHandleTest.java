package org.dynamisterrain.api.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TerrainGpuResourcesTypedHandleTest {
    @Test
    void typedSamplerRefMatchesLegacySamplerHandle() {
        TerrainGpuResources resources = new TerrainGpuResources(
                11L,
                12L,
                13L,
                14L,
                15L,
                16L,
                17L,
                18L);

        assertEquals(18L, resources.sampler());
        assertEquals(18L, resources.samplerRef().handle());
    }
}
