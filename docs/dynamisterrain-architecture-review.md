# DynamisTerrain Architecture Review

## Repo Overview

- Repository: `DynamisTerrain`
- Modules:
  - `dynamisterrain-api`
  - `dynamisterrain-core`
  - `dynamisterrain-vulkan`
  - `dynamisterrain-meshforge`
  - `dynamisterrain-physics`
  - `dynamisterrain-test`
  - `dynamisterrain-bench`
- Build/runtime profile:
  - Java 25 target, Maven multi-module
  - Vulkan module depends on `dynamis-gpu-api`, `dynamis-gpu-vulkan`, and `dynamissky-*`

Grounded structure:

- `api` exposes terrain descriptors, runtime service contracts, events, and GPU resource handles.
- `core` owns CPU-side terrain logic (heightmaps, CDLOD selection structures, flow/scatter/material/procedural helpers).
- `vulkan` owns terrain backend passes/resources (LOD/tessellation/material/foliage/water/horizon) and backend GPU abstractions.
- `meshforge` and `physics` modules exist as terrain-local integration layers.

## Strict Ownership Statement

### What DynamisTerrain should own

DynamisTerrain should own **terrain feature authority**:

- Terrain-specific runtime state and descriptor semantics.
- Terrain-local chunk/tile/heightfield/material/foliage/water logic.
- Terrain-local LOD and feature-level streaming decisions.
- Render-facing terrain data production for downstream rendering consumers.
- Terrain-local consumption of sky/weather and physics/collision inputs.

### What DynamisTerrain must not own

DynamisTerrain must **not** own:

- Global render planning/frame-graph ownership (LightEngine concern).
- Generic GPU orchestration/resource lifecycle authority (DynamisGPU concern).
- Global world/environment authority (WorldEngine concern).
- Scene graph ownership (SceneGraph concern).
- Generalized geometry preparation authority (MeshForge concern).
- Generic collision/physics simulation authority (Collision/Physics concerns).
- Session/content/scripting control-plane authority.

## Dependency Rules

### Allowed dependencies for DynamisTerrain

- `DynamisGPU` as execution substrate for terrain backend.
- `DynamisSky` as an environment input source.
- `MeshForge` for terrain-specific geometry prep integration hooks.
- `DynamisCollision`/`DynamisPhysics` via narrow terrain-consumer adapters.

### Forbidden dependencies for DynamisTerrain

- Dependence on LightEngine internals for owning render policy.
- Dependence on WorldEngine internals for world authority.
- Owning generic GPU allocator/scheduler policy that belongs in DynamisGPU.
- Owning generalized geometry pipeline concerns beyond terrain feature needs.
- Owning generalized simulation authority beyond terrain surface/collision consumption.

### Who may depend on DynamisTerrain

- LightEngine (terrain rendering consumption).
- World-level orchestrators (load/update/tile lifecycle coordination).
- VFX/physics systems as consumers of terrain outputs (`SurfaceContactEvent`, material/height queries).

## Public vs Internal Boundary Assessment

### Canonical public boundary

Primary public seam should be:

- `dynamisterrain-api` (`TerrainService`, descriptors, state/events).
- `dynamisterrain-core` deterministic CPU logic and builders.

This split is present and mostly coherent.

### Boundary concerns in current public surface

1. API currently exposes backend-oriented low-level handles:
   - `TerrainFrameContext.commandBuffer` is a raw `long` backend handle.
   - `TerrainGpuResources` is a collection of raw resource handles.

2. API uses weakly-typed external integration seams:
   - `TerrainService.setSkySource(Object skySource)` is untyped and broad.

3. Vulkan module has broad backend coupling:
   - depends on `dynamis-gpu-vulkan` directly.
   - exports multiple backend packages (`lod`, `horizon`, `material`, `foliage`, `water`) publicly.

4. Terrain repo includes dedicated `meshforge` and `physics` submodules with functional code; this is useful, but increases overlap risk if these become generalized non-terrain authorities.

### Internal/implementation areas (appropriate)

Appropriate internals to keep implementation-scoped:

- Vulkan pass classes, descriptor sets, UBO structs, pipeline helpers.
- Terrain backend resource alloc/caching helpers.
- Terrain-local integration adapters to sky/physics/collision.
- test harnesses and benchmark infrastructure.

## Policy Leakage / Overlap Findings

### DynamisLightEngine overlap

- Direct LightEngine integration classes are largely not implemented yet (integration package is scaffold-only), which reduces current leakage.
- Risk remains if terrain pass ordering/policy gets embedded in terrain backend instead of LightEngine host orchestration.

### DynamisGPU overlap (primary technical risk)

- Terrain Vulkan backend depends directly on `dynamis-gpu-vulkan` internals and uses raw backend handles broadly.
- This is a coupling risk similar to VFX/Sky, and should remain constrained.

### DynamisWorldEngine overlap

- No concrete WorldEngine ownership implementation found.
- Conceptual risk: terrain weather/time mutability could become de facto environment authority if not fed from world-owned state.

### DynamisSceneGraph overlap

- No direct scene ownership implementation detected.
- Terrain appears as a feature service with frame/camera consumption rather than scene-graph authority.

### MeshForge overlap

- `dynamisterrain-meshforge` includes terrain-specific road/foliage prep helpers.
- Current implementations are terrain-local and acceptable, but boundary risk exists if this evolves into generalized mesh processing outside terrain domain.

### VFX / Sky overlap

- Intended pattern is sound: terrain consumes sky/weather inputs and emits surface contacts for VFX consumption.
- Keep one-way data consumption; avoid terrain owning cross-feature policy.

### Collision / Physics overlap

- `dynamisterrain-physics` currently uses a terrain-local `CollisionWorld` abstraction with TODOs for proper external dependencies.
- This is acceptable as an adapter seam, but should not grow into full simulation ownership.

## Ratification Result

**Result: ratified with constraints**

Why:

- Terrain feature ownership is largely coherent across API/core/backend modules.
- The strongest issues are seam quality and coupling breadth, not outright role confusion:
  - raw backend-handle exposure in API contracts,
  - weakly typed sky integration seam (`Object`),
  - broad backend package exposure and direct GPU-internal dependency.
- Integration packages for higher-level orchestration are still largely scaffolding, which limits immediate leakage but leaves boundary hardening deferred.

## Recommended Next Step

1. Proceed with the planned graphics-cluster synthesis pass across:
   - `DynamisLightEngine`
   - `DynamisGPU`
   - `DynamisVFX`
   - `DynamisSky`
   - `DynamisTerrain`
2. Use that synthesis pass to decide targeted tightening priorities (typed feature seams, public surface narrowing, backend-coupling constraints).
3. Do not run broad refactors yet; keep this as mapping-first until the cross-repo plan is agreed.

This review is a boundary-ratification document only and does not propose immediate package moves or API-breaking changes.
