# DynamisTerrain

**Physically-based terrain, foliage, and world-surface library for the Dynamis engine ecosystem.**

Bruneton atmosphere-integrated heightmap terrain with CDLOD geometry, compute-based tessellation with silhouette correction, horizon-mapped self-shadowing, 16-layer virtual PBR material system, GPU-driven seed-deterministic foliage scatter, planar water with shoreline foam and capillary wicking, spline road mesh generation, chunked heightmap collision, and full DynamisSky weather integration. Vulkan compute pipeline throughout.

---

## Features

### 0.1.0 — Core World Surface

**Geometry**
- R16 and R32F heightmap support
- CDLOD (Continuous Distance-Dependent LOD) — no popping, smooth transitions, crack-free patch stitching
- Compute-based tessellation (MoltenVK-safe) with optional hull/domain fast path for desktop Vulkan
- Silhouette-aware tessellation correction — terrain profile matches displacement at horizon edges
- Procedural heightmap generation — FastNoise fractal + hydraulic erosion + thermal erosion + height stamps

**Materials**
- 4-layer, 8-layer, and 16-layer virtual PBR material system
- Full PBR per layer — albedo, normal, ORM, emissive, SSS, clear-coat, anisotropy
- Slope-based, height-based, and flow-accumulation-driven material blending
- Triplanar projection for cliff faces
- Micro-SDF / Parallax Occlusion Mapping for footprints and tire tracks in snow and mud

**Atmosphere & Weather**
- Horizon mapping — terrain self-shadowing at low sun angles, accurate at sunrise/sunset
- Full DynamisSky 0.1.0 integration — aerial perspective, sun color tint, fog altitude
- Snow accumulation from `WeatherState.snowIntensity`
- Surface wet darkening from `WeatherState.wetness`
- Mud and puddle formation from `WeatherState.rainIntensity`
- Capillary wicking — terrain above the water line appears naturally saturated
- Burned surface layer driven by VFX fire proximity events

**Foliage**
- GPU-driven scatter — compute cull dispatch, indirect draw, HZB occlusion culling
- Seed-deterministic placement — same world seed + position = identical foliage (multiplayer and replay safe)
- Slope, altitude, and flow-accumulation placement rules
- Wind animation driven by `DynamisSky` wind direction and speed
- LOD crossfade billboards
- DynamisCollision capsule collision proxies per foliage layer

**Water**
- Planar water surface — screen-space reflections, refraction
- Depth-based shoreline foam
- Capillary wicking shader at the waterline
- DynamisVFX splash emitter events on water contact
- Weather-responsive wave normal strength and ripple scale

**Roads**
- Spline-based road mesh generation via MeshForge
- Road material blending into splatmap along the spline corridor
- Terrain heightmap conforming

**Physics**
- Chunked heightmap collision shapes — only active tiles within simulation radius maintain physics bodies
- Per-material surface properties (friction, restitution) — grass, rock, mud, snow, asphalt, sand
- DynamisPhysics static terrain body

**Streaming Architecture**
- Tile-based data model from day one — streaming-ready API even in single-tile 0.1.0
- Async loader interface contract — synchronous loader in 0.1.0, real streaming loader in 0.2.0 without API changes

**Debug & Diagnostics**
- LOD chunk wireframe and color-coded LOD level overlay
- Splatmap weight visualization
- Foliage density map visualization
- Collision shape debug draw
- Performance stats — chunk count, triangle count, foliage instance count
- Horizon map self-shadow contribution overlay

### 0.2.0 — Planned

Virtual tiled heightmap (sparse binding, `VK_EXT_sparse_binding`), predictive tile loading (zero pop-in at 300 km/h), runtime heightmap deformation with GPU erosion settle pass, deformation delta persistence (`.deltaterrain` files), FFT ocean (JONSWAP spectrum), river system with heightmap-driven flow velocity, dynamic navmesh, foliage biological persistence (burned forests stay burned), cloud shadow projection from DynamisSky 0.2.0.

### 0.3.0 — Planned

Real-world DEM import (GeoTIFF, ASTER, SRTM, Cesium tilesets), spherical/planetary projection mode for 1000+ km scenes, biome/region system, bridge and tunnel auto-generation from splines, OSM/GeoJSON road import, GI voxelization hook for DLE VXGI.

---

## Architecture

DynamisTerrain is the heaviest consumer in the Dynamis ecosystem. It depends on DynamisSky, DynamisCollision, DynamisGPU, Vectrix, MeshForge, and FastNoiseLiteNouveau. It integrates directly with DynamicLightEngine via `VulkanTerrainIntegration` and emits `SurfaceContactEvent` consumed by DynamisVFX for footstep and splash effects.

```
Vectrix 1.10.10  ·  FastNoiseLiteNouveau  ·  MeshForge 1.1.0
                           ↓
                   DynamisGPU 1.0.1
                           ↓
  DynamisSky 0.1.0 ──── DynamisCollision 1.1.0 ──── DynamisVFX 0.1.0
                  ↘              ↓              ↙
                      DynamisTerrain 0.1.0
                           ↓
                  DynamicLightEngine (consumer)
```

### Module Structure

| Module | Purpose |
|--------|---------|
| `dynamisterrain-api` | Zero-dependency interfaces, descriptors, value types. Safe for all consumers. |
| `dynamisterrain-core` | CPU-side: CDLOD calculator, heightmap ops, flow map derivation, scatter rule engine, procedural generation, auto-splatmap. |
| `dynamisterrain-vulkan` | GPU rendering: compute tessellation pipeline, horizon map bake, foliage indirect draw, material system, water, DLE adapter. |
| `dynamisterrain-meshforge` | Road mesh generation from splines, foliage LOD prep via MeshForge. |
| `dynamisterrain-physics` | Chunked heightmap collision, per-material surface properties, DynamisPhysics body management. |
| `dynamisterrain-test` | `MockTerrainService`, deterministic sim harness, `TerrainAssertions`. |
| `dynamisterrain-bench` | JMH benchmarks — CDLOD throughput, scatter throughput, flow map derivation, horizon map bake time. |

### GPU Pipeline

Three-pass compute tessellation:

1. **Selection pass** (`cdlod_select.comp`) — evaluates visible patches, computes morph factors, populates `VisiblePatchBuffer` via atomic append
2. **Tessellation pass** (`cdlod_tessellate.comp`) — generates subdivided vertex data per visible patch, writes `TerrainVertexBuffer` and `IndirectDrawBuffer`
3. **Silhouette correction pass** (`terrain_silhouette.comp`) — detects screen-space terrain silhouette edges, increases subdivision for affected patches

Horizon map bake (`horizon_map_bake.comp`) runs once at tile load. For each texel it marches 8 compass directions and stores the maximum elevation angle visible in each direction as a packed RGBA8 texture. Fragment shader performs a single texture sample + comparison to determine self-shadowing — O(1) per fragment.

### Descriptor (JSON)

```json
{
  "id": "overworld_tile_0_0",
  "heightmap": {
    "format": "R16",
    "path": "assets/terrain/heightmap_0_0.r16",
    "width": 2048,
    "height": 2048
  },
  "splatmap": {
    "mode": "LAYERS_16",
    "splatmap0Path": "assets/terrain/splatmap0.png",
    "materials": [
      { "id": "grass",  "albedoPath": "...", "normalPath": "...", "ormPath": "...", "tileScale": 4.0,  "triplanar": false },
      { "id": "rock",   "albedoPath": "...", "normalPath": "...", "ormPath": "...", "tileScale": 2.0,  "triplanar": true  },
      { "id": "snow",   "albedoPath": "...", "normalPath": "...", "ormPath": "...", "tileScale": 6.0,  "triplanar": false },
      { "id": "dirt",   "albedoPath": "...", "normalPath": "...", "ormPath": "...", "tileScale": 3.0,  "triplanar": false }
    ]
  },
  "foliage": {
    "worldSeed": 8675309,
    "maxDrawDistance": 500.0,
    "windEnabled": true,
    "layers": [
      { "meshId": "pine_tree", "density": 0.6, "minSlope": 0.0, "maxSlope": 35.0, "minAlt": 50.0, "maxAlt": 900.0 },
      { "meshId": "grass_clump", "density": 0.9, "minSlope": 0.0, "maxSlope": 25.0 }
    ]
  },
  "water": {
    "mode": "PLANAR",
    "elevation": 0.0,
    "foamDepthThreshold": 1.5
  },
  "lod": {
    "lodLevels": 6,
    "tessellationMode": "COMPUTE",
    "screenSpaceError": 2.0,
    "patchSize": 65
  },
  "worldScale": 1.0,
  "heightScale": 800.0,
  "deformable": false
}
```

---

## Quick Start

### Maven

```xml
<!-- API only (game logic layer) -->
<dependency>
  <groupId>org.dynamisengine.terrain</groupId>
  <artifactId>dynamisterrain-api</artifactId>
  <version>0.1.0</version>
</dependency>

<!-- Full Vulkan renderer (engine layer) -->
<dependency>
  <groupId>org.dynamisengine.terrain</groupId>
  <artifactId>dynamisterrain-vulkan</artifactId>
  <version>0.1.0</version>
</dependency>
```

### Initialize

```java
VulkanTerrainIntegration terrain = VulkanTerrainIntegration.create(
    device, mainRenderPass,
    memoryOps, bindlessHeap,
    TerrainConfig.builder()
        .descriptor(TerrainDescriptor.fromJson(
            Path.of("assets/terrain/overworld_tile_0_0.json")))
        .skySource(skyIntegration)   // DynamisSky wired automatically
        .build()
);
```

### Per-Frame Update (DLE Frame Loop)

```java
// Before depth prepass — CDLOD selection, foliage cull, tessellation dispatch
terrain.update(commandBuffer, camera, deltaTime, frameIndex);

// During GBuffer pass — terrain geometry + horizon self-shadow
terrain.recordTerrain(commandBuffer, frameIndex);

// After terrain GBuffer — foliage instanced indirect draw
terrain.recordFoliage(commandBuffer, frameIndex);

// After opaque, forward pass — planar water surface
terrain.recordWater(commandBuffer, invViewProj, frameIndex);

// Game logic — drain surface contact events for VFX
List<SurfaceContactEvent> contacts = terrain.drainContactEvents();
contacts.forEach(vfxSystem::onSurfaceContact);
```

### CPU Terrain Queries

```java
// Height and material at any world position (GPU readback, call before recordTerrain)
float   height   = terrain.heightAt(worldX, worldZ);
Vector3f normal  = terrain.normalAt(worldX, worldZ);
MaterialTag mat  = terrain.materialAt(worldX, worldZ);

// Material tag drives VFX emitter selection:
// GRASS → dust puff,  MUD → mud splash,  SNOW → snow burst,  WATER → water splash
```

---

## CDLOD System

Continuous Distance-Dependent LOD (Strugar 2010). The heightmap is divided into a quadtree. Each frame, a compute shader traverses the quadtree from the camera position and computes a per-patch morph factor that continuously interpolates between LOD levels.

```java
// CdlodQuadTree usage (internal to VulkanTerrainIntegration)
CdlodQuadTree tree = CdlodQuadTree.build(2048, 2048, lodDesc);

CdlodFrameResult result = tree.select(
    cameraPosition,
    frustum,
    screenSpaceErrorThreshold   // default 2.0 pixels
);

// result.visiblePatches() — ordered front-to-back
// result.morphFactors()   — per-patch, 0.0 = stable, 1.0 = fully morphed
```

The morph factor is passed as a per-patch uniform to the tessellation vertex shader. At `morphFactor = 0.0` the patch renders at full resolution. At `morphFactor = 1.0` vertices snap to the coarser LOD positions exactly — no crack, no pop.

---

## Foliage System

GPU-driven scatter with three compute passes:

```
FoliageInstanceBuffer (all scatter points, uploaded once at tile load)
        ↓
foliage_cull.comp   — frustum cull + HZB occlusion cull + LOD selection
        ↓
VisibleInstanceBuffer (compacted, atomic append)
        ↓
vkCmdDrawIndirect   — one indirect draw per foliage layer
```

**Scatter is seed-deterministic:**

```java
FoliageDesc foliage = FoliageDesc.builder()
    .worldSeed(8675309L)   // same seed = identical foliage across sessions and clients
    .layer(FoliageLayer.builder()
        .meshId("pine_tree")
        .density(0.6f)
        .minSlope(0f).maxSlope(35f)
        .minAlt(50f).maxAlt(900f)
        .windStrength(0.4f)
        .build())
    .build();
```

Flow accumulation automatically increases foliage density in valleys (where water pools) and reduces it on ridges. No manual density painting required for realistic distribution.

---

## Horizon Mapping

Terrain self-shadowing computed at tile load and applied per fragment at O(1) cost:

```glsl
// terrain_material.frag (simplified)
float horizonAngle = sampleHorizonMap(uv, sunAzimuth);
float sunAltitude  = asin(sunDirection.y);
float shadowFactor = sunAltitude > horizonAngle ? 1.0 : SHADOW_AMBIENT;
ambientOcclusion  *= shadowFactor;
```

The horizon map is a 2048×2048 RGBA8 texture encoding the maximum elevation angle in 8 compass directions per texel. Bake time at tile load is approximately 80ms on a mid-range GPU. It is invalidated and rebuilt automatically when the heightmap is deformed (0.2.0).

---

## Weather Integration

DynamisSky drives terrain surface appearance every frame automatically:

```java
terrain.setSkySource(skyIntegration);  // wire once at startup
```

| WeatherState field | Terrain effect |
|---|---|
| `snowIntensity` | Snow accumulation layer blended over base material |
| `wetness` | Surface roughness reduced, albedo darkened |
| `rainIntensity` | Mud/puddle material blend, water ripple normal strength |
| `windSpeed` / `windDirection` | Foliage wind animation direction and amplitude |

**Capillary wicking** — the 0.3m of terrain immediately above the water elevation automatically appears darker and more saturated, creating a natural waterline transition without authoring:

```glsl
float wickFactor = 1.0 - smoothstep(0.0, 0.3, worldY - waterElevation);
albedo = mix(albedo, albedo * WET_DARKEN, wickFactor * wetness);
roughness = mix(roughness, roughness * WET_ROUGHNESS_SCALE, wickFactor);
```

---

## Surface Contact Events

Terrain emits `SurfaceContactEvent` when physics bodies contact the surface. DynamisVFX consumes these to drive emitter type selection:

```java
record SurfaceContactEvent(
    MaterialTag  material,    // GRASS / ROCK / DIRT / SAND / SNOW / MUD / WATER / ASPHALT
    Vector3f     position,
    Vector3f     normal,
    Vector3f     velocity,
    ContactType  type         // FOOT / VEHICLE / PROJECTILE
)

// VFX system maps material + type → emitter preset:
// SNOW  + FOOT      → snow burst + footprint decal
// MUD   + VEHICLE   → mud rooster-tail splash
// WATER + PROJECTILE → water entry splash
// GRASS + FOOT      → dust puff
```

---

## Flow Map

The flow/accumulation map is derived from the heightmap at tile load using the D8 flow routing algorithm. It encodes where water would naturally flow and pool:

- **High accumulation** → valleys, riverbeds, natural hollows → denser vegetation, richer soil material
- **Low accumulation** → ridges, drainage divides → sparse vegetation, exposed rock

```java
FlowMapData flow = FlowMapGenerator.generate(heightmap,
    FlowConfig.builder()
        .iterations(3)
        .slopeExponent(1.0f)
        .normalizeOutput(true)
        .build());
```

The flow map feeds the scatter rule engine (density modulation), the auto-splatmap generator (material weight derivation), and the terrain material fragment shader (procedural weight contribution at runtime).

---

## Procedural Heightmap

```java
HeightmapData hm = ProceduralHeightmapGenerator.generate(
    ProceduralDesc.builder()
        .seed(42L)
        .octaves(8)
        .frequency(0.003f)
        .erosionPasses(50)           // hydraulic erosion iterations
        .thermalErosionPasses(20)    // talus angle relaxation
        .stamps(List.of(
            HeightStamp.of("crater_large.r16", BlendMode.ADD, 0.8f)
        ))
        .build(),
    2048, 2048
);
```

Hydraulic erosion creates realistic river channels and sediment deposition. Thermal erosion creates natural talus slopes below cliff faces. Height stamps blend authored features (craters, mountain peaks, valleys) into the procedural base.

---

## Benchmarks

Run the full benchmark suite:

```bash
mvn -pl dynamisterrain-bench package -DskipTests
java -jar dynamisterrain-bench/target/dynamisterrain-bench.jar -wi 3 -i 5 -f 1
```

Smoke run (one iteration, no fork):

```bash
java -jar dynamisterrain-bench/target/dynamisterrain-bench.jar -wi 1 -i 1 -f 0 -t 1
```

### Design Targets (RTX 3070 class GPU, 1080p)

| System | Benchmark | Target |
|--------|-----------|--------|
| CDLOD selection compute | 2048×2048, 6 LOD levels | < 0.2ms GPU |
| Tessellation generate | 2000 visible patches at LOD 2 | < 0.5ms GPU |
| Horizon map bake | 2048×2048, radius 128 texels | < 100ms (one-time) |
| Terrain GBuffer draw | Full tile, highest LOD | < 1.0ms GPU |
| Foliage cull compute | 500,000 instances | < 0.3ms GPU |
| Foliage indirect draw | 50,000 visible, 3 layers | < 0.8ms GPU |
| Water surface draw | Full-coverage planar | < 0.3ms GPU |
| **Total terrain frame budget** | **All passes at 1080p** | **< 3.5ms GPU** |
| ScatterRuleEngine (CPU) | 2048×2048, 5 layers | < 200ms (load) |
| Flow map derivation (CPU) | 2048×2048 | < 50ms (load) |
| `heightAt()` query (CPU) | Single bilinear sample | < 100ns |

---

## macOS (MoltenVK)

```bash
MVK_CONFIG_USE_METAL_ARGUMENT_BUFFERS=1 \
java -jar dynamisterrain-bench/target/dynamisterrain-bench.jar ...
```

Compute-based tessellation is the primary tessellation path specifically because it is MoltenVK-safe. Hull/domain shaders are available as an opt-in fast path for native Vulkan drivers only.

---

## Parity Gate

All parity tests must pass before any commit to `dynamisterrain-vulkan`:

```bash
MVK_CONFIG_USE_METAL_ARGUMENT_BUFFERS=1 \
mvn -pl dynamisterrain-vulkan -am test \
  -Ddle.terrain.parity.tests=true \
  -Dvk.validation=true
```

Full reactor:

```bash
mvn test
```

---

## Requirements

| Dependency | Version | Role |
|---|---|---|
| Java | 25+ | Runtime, JPMS |
| Vulkan | 1.2+ | GPU API |
| DynamisGPU | 1.0.1 | Buffer/texture/pipeline management |
| DynamisSky | 0.1.0 | Weather, aerial perspective, sun direction |
| DynamisCollision | 1.1.0 | Heightmap collision shapes |
| DynamisPhysics | 0.1.0 | Terrain static body, surface material properties |
| Vectrix | 1.10.10 | Math (Vector3f, Matrix4f, SIMD ops) |
| MeshForge | 1.1.0 | Road mesh generation, foliage LOD prep |
| LWJGL | 3.4.1 | Vulkan bindings |
| FastNoiseLiteNouveau | bundled | Heightmap procedural generation |
| JUnit Jupiter | 5.10.2 | Test framework |
| JMH | 1.37 | Benchmarks |

---

## Ecosystem

```
Vectrix              1.10.10  ✅
MeshForge            1.1.0    ✅
DynamisCollision     1.1.0    ✅
Animis               1.0.0    ✅
DynamisGPU           1.0.1    ✅
DynamisVFX           0.1.0    ✅
DynamisSky           0.1.0    ✅
DynamicLightEngine            ✅  (VFX + Sky wired)
DynamisTerrain       0.1.0    🚧  in development
DynamisPhysics       TBD      📋  (extraction pending)
DynamisAudio         TBD      📋
DynamisScene         TBD      📋
```

---

## License

[License TBD]

---

*DynamisTerrain · Dynamis Engine Ecosystem*
