package org.dynamisterrain.bench;

import org.dynamisterrain.core.heightmap.HeightmapData;
import org.dynamisterrain.test.synthetic.SyntheticHeightmapFactory;

public final class BenchmarkFixtures {
    public static final HeightmapData FLAT_2048 = SyntheticHeightmapFactory.flat(2048, 100f);
    public static final HeightmapData HILL_2048 = SyntheticHeightmapFactory.hill(2048, 800f);
    public static final long WORLD_SEED = 8_675_309L;

    private BenchmarkFixtures() {
    }
}
