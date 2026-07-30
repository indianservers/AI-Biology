# Virtual Microscopy CDN Contract

Configure the Android build with:

```properties
biologyMicroscopyCatalogUrl=https://cdn.example.com/biology/microscopy/catalog.json
```

The catalogue, thumbnail images, and slide pixels remain remote. The app stores
catalogue metadata and challenge results in SQLite. Slide pixels are requested
only when a user opens a slide.

Supported source types:

- `image`: one JPEG, PNG, or WebP image. Best for smaller teaching slides.
- `dzi`: a Deep Zoom `.dzi` descriptor and tile directory.
- `iiif`: an IIIF Image API `info.json` endpoint. Recommended for whole-slide images.

Coordinates use the full-resolution image space normalized to `0.0..1.0`.
`x=0,y=0` is the top-left and `x=1,y=1` is the bottom-right. `radius` is the
accepted normalized tap distance for identification challenges.

CDN requirements:

- Serve the catalogue and slide resources over HTTPS.
- Return CORS headers for DZI descriptors, IIIF `info.json`, and image tiles.
- Keep slide IDs stable across catalogue updates so learner progress is retained.
- Supply `ETag` on `catalog.json` to avoid unnecessary metadata downloads.
- Use image pyramids for large slides instead of single very large images.

See `microscopy-catalog.example.json` for the complete supported payload.
