package org.dynamisterrain.bench.core;

import java.util.concurrent.TimeUnit;
import org.dynamisterrain.api.descriptor.TerrainLodDesc;
import org.dynamisterrain.api.descriptor.TessellationMode;
import org.dynamisterrain.api.state.Vector3f;
import org.dynamisterrain.bench.BenchmarkFixtures;
import org.dynamisterrain.core.lod.CdlodFrameResult;
import org.dynamisterrain.core.lod.CdlodQuadTree;
import org.dynamisterrain.core.lod.Frustum;
import org.dynamisterrain.core.lod.Matrix4f;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class CdlodBenchmark {
    private CdlodQuadTree tree;
    private Frustum frustum;
    private Vector3f camera;

    @Setup
    public void setup() {
        final TerrainLodDesc lod = new TerrainLodDesc(6, TessellationMode.COMPUTE, 2f, 65, 0.6f, 0.9f);
        this.tree = CdlodQuadTree.build(2048, 2048, lod, BenchmarkFixtures.HILL_2048, 1f, 800f);
        this.camera = new Vector3f(1024f, 300f, 1024f);
        this.frustum = Frustum.fromViewProjection(buildViewProj(this.camera));
    }

    @Benchmark
    public CdlodFrameResult select(final Blackhole bh) {
        final CdlodFrameResult r = this.tree.select(this.camera, this.frustum, 2.0f);
        bh.consume(r);
        return r;
    }

    private static Matrix4f buildViewProj(final Vector3f camera) {
        final Matrix4f view = Matrix4f.lookAt(camera, new Vector3f(1024f, 0f, 1024f), new Vector3f(0f, 1f, 0f));
        final Matrix4f proj = Matrix4f.perspective((float) Math.toRadians(60), 16f / 9f, 0.1f, 10_000f);
        return Matrix4f.multiply(proj, view);
    }
}
