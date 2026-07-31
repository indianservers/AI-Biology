# IndianServers Biology CDN Upload

Generated and validated on 2026-07-31 from:

```text
D:\3D objects\Biology
```

## Website destination

Use this public HTTPS root:

```text
https://indianservers.com/edutech/biology/3d/
```

Upload these items while preserving every folder name and space:

```text
biology-catalog.json
anatomy-catalog.json
1. CELL Biology/_cdn_packages/
2. Human Anatomy/_cdn_packages/catalog.json
7. Human Physiology/_cdn_packages/
Anatomy/_cdn_packages/
```

Current upload set:

| Source | Files | Size |
|---|---:|---:|
| `biology-catalog.json` | 1 | 0.031 MiB |
| `anatomy-catalog.json` | 1 | 0.011 MiB |
| `1. CELL Biology/_cdn_packages/` | 64 | 705.983 MiB |
| `2. Human Anatomy/_cdn_packages/catalog.json` | 1 | 0.003 MiB |
| `7. Human Physiology/_cdn_packages/` | 13 | 70.413 MiB |
| `Anatomy/_cdn_packages/` | 28 | 112.451 MiB |
| **Total** | **108** | **888.890 MiB** |

Do not upload the raw authoring GLBs, `Images`, `TextPrompts`, Meshy task JSON,
or package staging files outside `_cdn_packages`. The upload folders already
contain the public thumbnails, standalone manifests, validated ZIP packages,
and category catalogues.

## Android configuration

```properties
biologyCatalogUrl=https://indianservers.com/edutech/biology/3d/biology-catalog.json
biologyAnatomyCatalogUrl=https://indianservers.com/edutech/biology/3d/anatomy-catalog.json
```

The general Explorer reads the master catalogue. The Human Anatomy module reads
the smaller Anatomy catalogue. Catalogue cards download titles, descriptions,
sizes, and thumbnails. A ZIP is requested only when the learner chooses
Download. The validated GLB is stored in the App Library and can be deleted or
downloaded again.

## Server headers

Use these content types:

```text
.json  application/json; charset=utf-8
.zip   application/zip
.png   image/png
.glb   model/gltf-binary
```

Recommended headers:

```text
catalog.json / biology-catalog.json:
  Cache-Control: public, max-age=300, must-revalidate
  ETag: enabled

*.manifest.json:
  Cache-Control: public, max-age=3600

*.zip / */thumbnail.png:
  Cache-Control: public, max-age=86400
  Accept-Ranges: bytes
```

Enable HTTPS, CORS for the Android app, range requests, and correct handling of
URL-encoded spaces. Do not rename folders after uploading because catalogue
paths are relative and case-sensitive.

## Verify after upload

These URLs must return HTTP 200:

```text
https://indianservers.com/edutech/biology/3d/biology-catalog.json
https://indianservers.com/edutech/biology/3d/anatomy-catalog.json
https://indianservers.com/edutech/biology/3d/2.%20Human%20Anatomy/_cdn_packages/catalog.json
https://indianservers.com/edutech/biology/3d/Anatomy/_cdn_packages/Skeleton/thumbnail.png
https://indianservers.com/edutech/biology/3d/Anatomy/_cdn_packages/Skeleton.manifest.json
https://indianservers.com/edutech/biology/3d/Anatomy/_cdn_packages/Skeleton.zip
```

All 34 ZIPs were checked against the SHA-256 values in the master catalogue.
Every ZIP contains `model.glb`, `manifest.json`, `thumbnail.png`,
`checksums.json`, and `README.txt`.

## Anatomy authoring limitation

The nine current Anatomy GLBs each contain one unnamed mesh and one material.
They support complete-system rotate, pan, zoom, fullscreen, download, and
offline use. They cannot yet isolate or identify individual bones, muscles,
nerves, or organs.

Future Zygote-style exports should contain:

```text
VIS_<SEMANTIC_PART_ID>   visible structure mesh
HIT_<SEMANTIC_PART_ID>   simplified tap target
MAT_<SEMANTIC_PART_ID>   optional semantic material
```

For example:

```text
VIS_SKELETON_FEMUR_LEFT
HIT_SKELETON_FEMUR_LEFT
VIS_ORGANS_HEART
HIT_ORGANS_HEART
```

Keep these node IDs aligned with each package manifest. Once semantic meshes
exist, the app can enable tap-to-name, highlight, isolate, hide, transparency,
layer combinations, and guided structure navigation.
