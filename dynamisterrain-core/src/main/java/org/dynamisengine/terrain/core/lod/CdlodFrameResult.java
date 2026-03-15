package org.dynamisengine.terrain.core.lod;

import java.util.List;

public record CdlodFrameResult(
    List<CdlodPatch> visiblePatches,
    int patchCount,
    float[] morphFactors
) {
}
