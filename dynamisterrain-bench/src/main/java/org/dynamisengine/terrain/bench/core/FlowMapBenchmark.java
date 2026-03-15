package org.dynamisengine.terrain.bench.core;

import java.util.concurrent.TimeUnit;
import org.dynamisengine.terrain.core.flow.FlowConfig;
import org.dynamisengine.terrain.core.flow.FlowMapData;
import org.dynamisengine.terrain.core.flow.FlowMapGenerator;
import org.dynamisengine.terrain.core.heightmap.HeightmapData;
import org.dynamisengine.terrain.test.synthetic.SyntheticHeightmapFactory;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class FlowMapBenchmark {
    @Param({"256", "512", "1024"})
    public int size;

    private HeightmapData heightmap;

    @Setup
    public void setup() {
        this.heightmap = SyntheticHeightmapFactory.hill(this.size, 800f);
    }

    @Benchmark
    public FlowMapData generate(final Blackhole bh) {
        final FlowMapData f = FlowMapGenerator.generate(this.heightmap, new FlowConfig(3, 1.0f, true));
        bh.consume(f);
        return f;
    }
}
