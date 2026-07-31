# Virtual Microscopy CDN

The app reads the production catalogue from:

`https://indianservers.com/edutech/biology/microscopy/catalog.json`

## Build the starter collection

The source images are stored outside the Android project:

`D:\3D objects\Biology\Microscopy\_source`

Build the upload-ready Deep Zoom collection with:

```powershell
python tools/build_microscopy_packages.py
```

The generated folder is:

`D:\3D objects\Biology\Microscopy\cdn`

Upload the **contents** of that `cdn` folder to:

`indianservers.com/edutech/biology/microscopy/`

Keep the generated folder structure unchanged. Each slide has a `.dzi`
descriptor, JPEG tile pyramid, and thumbnail. The app downloads the catalogue
and thumbnails first; OpenSeadragon requests only the tiles needed for the
current zoom and viewport.

The initial images are synthetic educational microscopy content. They are
appropriate for app demonstrations and learning activities, but not for
clinical diagnosis. Replace or supplement them with reviewed, licensed
histology scans before clinical or examination-critical use.

## Add another slide

1. Add the full-resolution image to the `_source` folder.
2. Add its metadata and normalized annotation coordinates to `SLIDES` in
   `tools/build_microscopy_packages.py`.
3. Rebuild and upload the generated `cdn` folder.
4. Increment `catalogVersion` whenever published content changes.
