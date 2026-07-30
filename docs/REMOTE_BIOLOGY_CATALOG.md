# Remote Biology Catalogue

The app contains no GLB files by default. It downloads catalogue JSON and visible
thumbnails for browsing, then downloads a model package only after the user selects it.

## Configure The Catalogue

Keep one stable HTTPS URL for the master catalogue. Configure it outside source control:

```properties
# ~/.gradle/gradle.properties
biologyCatalogUrl=https://cdn.example.com/biology/biology-catalog.json
```

Or provide it to a build:

```powershell
.\gradlew.bat :app:assembleDebug `
  -PbiologyCatalogUrl=https://cdn.example.com/biology/biology-catalog.json
```

The app caches the JSON with ETag support and falls back to the last valid catalogue
when offline. Relative thumbnail, manifest, and ZIP paths resolve against the catalogue
URL. Card thumbnails are fetched only when requested by visible RecyclerView items.

## Model Download Flow

1. Read model names, descriptions, tags, sizes, and thumbnail paths from the catalogue.
2. Do not request a GLB while browsing or searching.
3. When selected, download the model ZIP to a temporary app-managed file.
4. Validate the package SHA-256 from the catalogue.
5. Reject unsafe ZIP paths and unexpectedly large extracted content.
6. Validate `manifest.json`, semantic model ID, inner GLB SHA-256, and GLB magic bytes.
7. Store the validated GLB and manifest in the App Library.
8. Load the local GLB through the existing app-local viewer URL.

## Human Anatomy Architecture

A Zygote-style anatomy explorer should not try to visually recognize parts of a known
3D model. Recognition should be deterministic: a touch ray intersects a named hit mesh,
and that node maps to a stable semantic part ID.

Use a hierarchy:

```text
Human body
  System
    Organ
      Region
        Tissue or structure
```

Do not ship the entire detailed body as one GLB. Use separately downloadable levels:

```text
human-body-low.zip
cardiovascular-system.zip
heart.zip
heart-internal.zip
```

Each selectable structure needs:

```text
VIS_HEART_LEFT_VENTRICLE
HIT_HEART_LEFT_VENTRICLE
MAT_HEART_LEFT_VENTRICLE
ANIM_HEART_BEAT
```

- `VIS_*` is the visible mesh.
- `HIT_*` is a simplified invisible touch mesh.
- `MAT_*` can be highlighted independently.
- `ANIM_*` is an optional named animation.

The manifest should define parent ID, interaction level, visible and hit nodes, camera
preset, default visibility, isolate group, description, pronunciation, and quiz tags.

## Renderer Direction

Keep the current `model-viewer` path for individual models and hotspot-based lessons.
For full-body anatomy, evaluate a dedicated renderer built on a maintained Filament or
SceneView integration, or a Three.js anatomy scene. The renderer must expose:

- Mesh-level ray casting
- Per-node visibility and opacity
- Material highlighting
- Isolation and hide/show groups
- Clipping planes and cross sections
- Exploded transforms
- Camera transitions
- LOD and incremental sub-model loading

Choose the renderer only after testing one authored organ package end to end. The first
acceptance model should be the heart with separate chambers and vessels.

## Authoring Gate

A model is not anatomy-ready until automated validation confirms:

- More than one semantic mesh
- Stable unique node names
- Every manifest part resolves to a visible or hit node
- No orphan semantic nodes
- Correct scale and origin
- Verified low/medium/high LODs where required
- Touch, highlight, hide, isolate, and camera-focus behavior
- Biology content reviewed by a qualified subject expert
