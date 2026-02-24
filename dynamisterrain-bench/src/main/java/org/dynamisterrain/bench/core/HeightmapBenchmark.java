package org.dynamisterrain.bench.core;

import java.util.concurrent.TimeUnit;
import org.dynamisterrain.bench.BenchmarkFixtures;
import org.dynamisterrain.core.heightmap.HeightmapOps;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class HeightmapBenchmark {
    @Benchmark
    public float heightAtBilinear(final Blackhole bh) {
        final float h = HeightmapOps.heightAt(BenchmarkFixtures.HILL_2048, 512f, 512f, 1f, 800f);
        bh.consume(h);
        return h;
    }

    @Benchmark
    public float[] generateNormals(final Blackhole bh) {
        final float[] n = HeightmapOps.generateNormals(BenchmarkFixtures.HILL_2048, 1f, 800f);
        bh.consume(n);
        return n;
    }
}
