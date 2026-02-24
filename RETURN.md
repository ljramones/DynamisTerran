# DynamisTerrain — Return Context

## Status at pause

**0.1.0 implementation: COMPLETE**

All 14 steps committed and green. Full reactor BUILD SUCCESS.
Vulkan parity tests: 34 total, 0 failures.
Benchmarks running. README.md committed.

### Commit chain

| Step | Hash    | Deliverable |
|------|---------|-------------|
| 1    | b2d263a | dynamisterrain-api — all interfaces, descriptors, value types |
| 2    | 9bb0ef6 | HeightmapData, HeightmapOps |
| 3    | d8b8dfa | Frustum, CdlodQuadTree, morph factor |
| 4    | 02c5915 | FlowMapGenerator, ScatterRuleEngine, AutoSplatmapGenerator |
| 5    | 9df3c32 | ProceduralHeightmapGenerator, TerrainDescriptorBuilder |
| 6    | 6d988e2 | TerrainGpuContext, HorizonMapBaker, TerrainGpuLodResources |
| 7    | 352d8c7 | CdlodSelectionPass, CdlodTessellationPass, TerrainLodPipeline |
| 8    | d142a31 | SilhouetteCorrectionPass |
| 9    | 2a0945e | TerrainMaterialAtlas, TerrainDrawPipeline, terrain.vert/frag |
| 10   | a59682f | FoliageInstanceBuffer, FoliageCullPass, FoliageDrawPipeline |
| 11   | 77d3880 | WaterRenderPass, water.vert/frag |
| 12   | 9a04b70 | RoadMeshGenerator, SplineSampler, FoliageMeshPrep |
| 13   | 2b46634 | TerrainCollisionManager, TerrainMaterialResolver |
| 14   | c0e15a7 | MockTerrainService, TerrainSimHarness, JMH bench suite |

---

## Why we paused

`dynamisterrain-physics` has a commented-out dependency on `dynamisphysics-api`
which did not exist yet. Paused to extract DynamisPhysics as a proper library
first so the TODO can be resolved cleanly.

```
// dynamisterrain-physics/pom.xml
<!-- TODO: restore when DynamisPhysics extracted -->
<!-- <dependency>dynamisphysics-api</dependency> -->

// dynamisterrain-physics/src/main/java/module-info.java
// TODO: restore when DynamisPhysics extracted
// requires org.dynamisphysics.api;
```

---

## First three tasks on return

### 1 — Restore DynamisPhysics dependency (30 minutes)

Once `dynamisphysics-api 0.1.0-SNAPSHOT` is installed locally:

- Uncomment `dynamisphysics-api` dependency in
  `dynamisterrain-physics/pom.xml`
- Uncomment `requires org.dynamisphysics.api` in
  `dynamisterrain-physics/src/main/java/module-info.java`
- Update `TerrainCollisionManager` to use `PhysicsWorld` from the API
  rather than the stub it currently uses
- Update `TerrainMaterialResolver` to return `PhysicsMaterial` instances
  from `PhysicsMaterial.of(friction, restitution, tag)`
- Run `mvn -pl dynamisterrain-physics -am test` — 9 tests, all green

Commit:
```
fix: dynamisterrain-physics — restore dynamisphysics-api dependency and wire TerrainCollisionManager to PhysicsWorld API
```

### 2 — DynamicLightEngine terrain wiring (same pattern as VFX + Sky)

Six steps mirroring DLE's VFX integration (Steps A–F):

- **Step A** — terrain indirect buffer separation from engine main pass
- **Step B** — depth buffer exposure for terrain water refraction
- **Step C** — GBuffer normal exposure for terrain decal renderer
- **Step D** — terrain texture registration into bindless heap
- **Step E** — DynamisPhysics terrain body handoff into DLE physics step
- **Step F** — VulkanTerrainIntegration wired into DLE frame loop
  (same as VulkanSkyIntegration in d0baa72 — update, recordTerrain,
  recordFoliage, recordWater called at correct phase positions,
  SurfaceContactEvent drain forwarded to VFX system)

Parity gate after each step:
```bash
MVK_CONFIG_USE_METAL_ARGUMENT_BUFFERS=1 \
mvn -pl engine-host-sample test \
  -Ddle.bindless.parity.tests=true \
  -Dvk.validation=true
```

### 3 — Begin DynamisTerrain 0.2.0

Architecture doc (DynamisTerrain_Architecture.docx §1) already defines
0.2.0 scope. First task is a focused architecture session for the
streaming subsystem before any code is written — same discipline as
DynamisSky and DynamisTerrain 0.1.0.

Key 0.2.0 design decisions to make in that session:
- Virtual tiled heightmap page size (candidate: 256×256 texels per page)
- Sparse binding strategy — VK_EXT_sparse_binding vs manual atlas
- Predictive loader thread model — how far ahead to stream at 300 km/h
- Deformation delta file format (.deltaterrain spec)
- FFT ocean — confirm Vectrix FFT algo is suitable for JONSWAP
  displacement map generation (Vectrix has FFT — verify API before
  designing the compute pipeline)

---

## Benchmark numbers for README (recorded at 0.1.0 close)

```
heightAtBilinear          345M ops/s    (~2.9ns)
CdlodBenchmark.select     306K ops/s    (~3.3µs)
generateNormals            78 ops/s     (~12.8ms at 2048²)
FlowMap  256²               4.2ms       one-time load
FlowMap  512²              19.3ms       one-time load
FlowMap 1024²              89.1ms       one-time load  ← extrapolates ~356ms at 2048²
Scatter  256²               8.2ms       one-time load
Scatter  512²              41.2ms       one-time load
Scatter 1024²             174.7ms       one-time load  ← extrapolates ~1400ms at 2048²
```

Flow map and scatter at full 2048² will exceed architecture doc targets.
Both are one-time load operations (not per-frame) so frame budget is
unaffected. Flag as optimization candidates for 0.2.0:
- FlowMapGenerator — good GPU compute candidate
- ScatterRuleEngine — parallelizes with ForkJoinPool, target < 200ms

---

## Ecosystem state at pause

```
Vectrix              1.10.10  ✅
MeshForge            1.1.0    ✅
DynamisCollision     1.1.0    ✅
Animis               1.0.0    ✅
DynamisGPU           1.0.1    ✅
DynamisVFX           0.1.0    ✅
DynamisSky           0.1.0    ✅
DynamicLightEngine            ✅  VFX + Sky wired
DynamisTerrain       0.1.0    ✅  COMPLETE — DLE wiring pending
DynamisPhysics       TBD      🚧  extraction in progress
DynamisAudio         TBD      📋
DynamisScene         TBD      📋
```

---

## Architecture and wish list docs

```
docs/DynamisTerrain_Architecture.docx   — full 13-section arch doc
docs/DynamisTerrain_WishList.docx       — 150 features across 15 sections
```
