package org.dynamisterrain.core.lod;

import java.util.List;

public record CdlodFrameResult(
    List<CdlodPatch> visiblePatches,
    int patchCount,
    float[] morphFactors
) {
}
