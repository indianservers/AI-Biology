# Remote Infographic Library

Infographic metadata is stored in the app's SQLite catalogue. Thumbnails are cached
separately, and full infographic images are downloaded only when a learner selects
`Save offline`.

## Configure

Set the website catalogue URL outside source control:

```properties
# ~/.gradle/gradle.properties
biologyInfographicCatalogUrl=https://cdn.example.com/biology/infographics/catalog.json
```

Or provide it during a build:

```powershell
.\gradlew.bat :app:assembleDebug `
  -PbiologyInfographicCatalogUrl=https://cdn.example.com/biology/infographics/catalog.json
```

## Catalogue Contract

Paths may be absolute URLs or relative to the catalogue URL.

```json
{
  "schemaVersion": 1,
  "catalogVersion": "2026.07.30.1",
  "generatedAt": "2026-07-30T00:00:00Z",
  "infographics": [
    {
      "id": "ANIMAL_CELL_OVERVIEW",
      "title": "Inside the Animal Cell",
      "summary": "A visual guide to major organelles and their functions.",
      "category": "Cell Biology",
      "tags": ["cell", "organelles", "cytology"],
      "thumbnailPath": "cell-biology/animal-cell-thumb.webp",
      "filePath": "cell-biology/animal-cell.png",
      "mediaType": "image/png",
      "fileSizeBytes": 2457600,
      "sha256": "lowercase-sha256-of-the-full-image",
      "version": 1,
      "gradeLevels": ["Beginner", "Student"],
      "source": {
        "title": "AI Explorer Biology Review Board",
        "url": "https://example.com/content-policy"
      },
      "reviewedAt": "2026-07-30"
    }
  ]
}
```

Supported full-image formats are PNG, JPEG, and WebP. Keep thumbnails below 8 MB and
full infographic files below 80 MB. Use WebP thumbnails around 480 pixels wide, while
retaining a readable high-resolution source image for offline viewing.

## Update Behavior

- Catalogue metadata remains available offline from SQLite.
- Missing local files are repaired back to `Not saved`.
- A higher version or changed checksum keeps the prior offline image usable and shows
  `Update offline`.
- Downloads are written to a partial file, bounded by size, checksum-verified, and
  renamed only after validation.
- Removing an offline copy deletes only the image file; its catalogue record remains.
