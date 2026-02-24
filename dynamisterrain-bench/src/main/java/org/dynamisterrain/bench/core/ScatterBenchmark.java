package org.dynamisterrain.bench.core;

import java.util.concurrent.TimeUnit;
import org.dynamisterrain.api.descriptor.FoliageLayer;
import org.dynamisterrain.bench.BenchmarkFixtures;
import org.dynamisterrain.core.flow.FlowConfig;
import org.dynamisterrain.core.flow.FlowMapData;
import org.dynamisterrain.core.flow.FlowMapGenerator;
import org.dynamisterrain.core.heightmap.HeightmapData;
import org.dynamisterrain.core.scatter.ScatterConfig;
import org.dynamisterrain.core.scatter.ScatterResult;
import org.dynamisterrain.core.scatter.ScatterRuleEngine;
import org.dynamisterrain.test.synthetic.SyntheticHeightmapFactory;
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
public class ScatterBenchmark {
    @Param({"256", "512", "1024"})
    public int size;

    private HeightmapData heightmap;
    private FlowMapData flowMap;
    private FoliageLayer layer;

    @Setup
    public void setup() {
        this.heightmap = SyntheticHeightmapFactory.hill(this.size, 800f);
        this.flowMap = FlowMapGenerator.generate(this.heightmap, new FlowConfig(3, 1.0f, true));
        this.layer = new FoliageLayer("tree", 0.5f, 0f, 45f, 0f, 1000f, 1f);
    }

    @Benchmark
    public ScatterResult evaluate(final Blackhole bh) {
        final ScatterResult r = ScatterRuleEngine.evaluate(
            this.layer,
            this.heightmap,
            this.flowMap,
            null,
            1f,
            800f,
            BenchmarkFixtures.WORLD_SEED,
            new ScatterConfig(2.0f, 30, 0.8f, 1.2f)
        );
        bh.consume(r);
        return r;
    }
}
