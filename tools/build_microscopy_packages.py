#!/usr/bin/env python3
"""Build CDN-ready Deep Zoom microscopy packages from the starter slides."""

from __future__ import annotations

import argparse
import json
import math
import shutil
from pathlib import Path

from PIL import Image


SLIDES = [
    {
        "id": "onion-epidermis",
        "source": "onion-epidermis.png",
        "title": "Onion Epidermis",
        "summary": (
            "A synthetic educational bright-field view of onion epidermal cells. "
            "Use the regular cell walls and darker nuclei to compare plant cell structure."
        ),
        "category": "Plant Histology",
        "tissue": "Epidermis",
        "organ": "Onion bulb scale leaf",
        "species": "Onion",
        "scientificName": "Allium cepa",
        "stain": "Iodine-style educational stain",
        "magnification": "40x-400x",
        "micronsPerPixel": 0.45,
        "annotations": [
            {
                "id": "cell-wall",
                "label": "Cell wall",
                "description": "The rigid cellulose-rich boundary that supports the plant cell.",
                "challengePrompt": "Tap a clearly visible cell wall",
                "x": 0.36,
                "y": 0.34,
                "radius": 0.055,
            },
            {
                "id": "nucleus",
                "label": "Nucleus",
                "description": "The darker rounded structure containing most of the cell's DNA.",
                "challengePrompt": "Find a stained nucleus",
                "x": 0.455,
                "y": 0.455,
                "radius": 0.04,
            },
            {
                "id": "cytoplasm",
                "label": "Cytoplasm",
                "description": "The lightly stained material inside the cell boundary.",
                "challengePrompt": "Tap inside the cytoplasm of an epidermal cell",
                "x": 0.695,
                "y": 0.605,
                "radius": 0.07,
            },
        ],
    },
    {
        "id": "human-blood-smear",
        "source": "human-blood-smear.png",
        "title": "Human Blood Smear",
        "summary": (
            "A synthetic educational peripheral blood smear showing erythrocytes, "
            "two leukocyte types, and platelets."
        ),
        "category": "Human Histology",
        "tissue": "Peripheral blood",
        "organ": "Blood",
        "species": "Human",
        "scientificName": "Homo sapiens",
        "stain": "Wright-Giemsa style",
        "magnification": "100x-1000x",
        "micronsPerPixel": 0.18,
        "annotations": [
            {
                "id": "erythrocyte",
                "label": "Erythrocyte",
                "scientificName": "Erythrocytus",
                "description": "A biconcave red blood cell specialized for oxygen transport.",
                "challengePrompt": "Identify an erythrocyte",
                "x": 0.235,
                "y": 0.64,
                "radius": 0.045,
            },
            {
                "id": "neutrophil",
                "label": "Neutrophil",
                "description": "A granulocyte recognized by its segmented, multi-lobed nucleus.",
                "challengePrompt": "Find the leukocyte with a segmented nucleus",
                "x": 0.432,
                "y": 0.39,
                "radius": 0.075,
            },
            {
                "id": "lymphocyte",
                "label": "Lymphocyte",
                "description": "A white blood cell with a dense round nucleus and a thin cytoplasmic rim.",
                "challengePrompt": "Find the small lymphocyte",
                "x": 0.735,
                "y": 0.66,
                "radius": 0.06,
            },
            {
                "id": "platelet",
                "label": "Platelet",
                "description": "A small cell fragment involved in blood clotting.",
                "challengePrompt": "Tap a tiny purple platelet",
                "x": 0.85,
                "y": 0.075,
                "radius": 0.035,
            },
        ],
    },
    {
        "id": "cardiac-muscle",
        "source": "cardiac-muscle.png",
        "title": "Cardiac Muscle",
        "summary": (
            "A synthetic educational longitudinal section of branching cardiac muscle "
            "with central nuclei, striations, intercalated discs, and a capillary."
        ),
        "category": "Human Histology",
        "tissue": "Cardiac muscle",
        "organ": "Heart",
        "species": "Human",
        "scientificName": "Homo sapiens",
        "stain": "H&E style",
        "magnification": "40x-400x",
        "micronsPerPixel": 0.32,
        "annotations": [
            {
                "id": "cardiomyocyte",
                "label": "Cardiomyocyte",
                "description": "A branching contractile muscle cell that helps pump blood.",
                "challengePrompt": "Tap the body of a cardiomyocyte",
                "x": 0.30,
                "y": 0.55,
                "radius": 0.065,
            },
            {
                "id": "central-nucleus",
                "label": "Central nucleus",
                "description": "Cardiac muscle cells usually contain one centrally placed nucleus.",
                "challengePrompt": "Find a centrally located cardiomyocyte nucleus",
                "x": 0.55,
                "y": 0.65,
                "radius": 0.045,
            },
            {
                "id": "intercalated-disc",
                "label": "Intercalated disc",
                "description": "A dark junction that mechanically and electrically links adjacent cells.",
                "challengePrompt": "Find a dark intercalated disc",
                "x": 0.64,
                "y": 0.225,
                "radius": 0.045,
            },
            {
                "id": "capillary",
                "label": "Capillary",
                "description": "A small blood vessel supplying oxygen and nutrients to the myocardium.",
                "challengePrompt": "Find the capillary containing erythrocytes",
                "x": 0.65,
                "y": 0.50,
                "radius": 0.065,
            },
        ],
    },
]


def build_dzi(image: Image.Image, destination: Path, slide_id: str) -> tuple[int, int]:
    width, height = image.size
    tile_size = 256
    maximum_level = math.ceil(math.log2(max(width, height)))
    tiles_root = destination / f"{slide_id}_files"
    tiles_root.mkdir(parents=True, exist_ok=True)

    for level in range(maximum_level + 1):
        scale = 2 ** (maximum_level - level)
        level_width = max(1, math.ceil(width / scale))
        level_height = max(1, math.ceil(height / scale))
        level_image = image.resize((level_width, level_height), Image.Resampling.LANCZOS)
        level_directory = tiles_root / str(level)
        level_directory.mkdir(parents=True, exist_ok=True)
        columns = math.ceil(level_width / tile_size)
        rows = math.ceil(level_height / tile_size)
        for column in range(columns):
            for row in range(rows):
                left = column * tile_size
                top = row * tile_size
                tile = level_image.crop(
                    (
                        left,
                        top,
                        min(left + tile_size, level_width),
                        min(top + tile_size, level_height),
                    )
                )
                tile.save(
                    level_directory / f"{column}_{row}.jpg",
                    "JPEG",
                    quality=88,
                    optimize=True,
                )

    descriptor = (
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        '<Image TileSize="256" Overlap="0" Format="jpg" '
        'xmlns="http://schemas.microsoft.com/deepzoom/2008">\n'
        f'  <Size Width="{width}" Height="{height}"/>\n'
        "</Image>\n"
    )
    (destination / f"{slide_id}.dzi").write_text(descriptor, encoding="utf-8")
    return width, height


def build_package(source_root: Path, output_root: Path) -> None:
    output_root.mkdir(parents=True, exist_ok=True)
    catalog_slides = []

    for definition in SLIDES:
        source = source_root / definition["source"]
        if not source.is_file():
            raise FileNotFoundError(f"Missing microscopy source image: {source}")

        slide_id = definition["id"]
        destination = output_root / slide_id
        if destination.exists():
            shutil.rmtree(destination)
        destination.mkdir(parents=True)

        with Image.open(source) as opened:
            image = opened.convert("RGB")
            width, height = build_dzi(image, destination, slide_id)
            thumbnail = image.copy()
            thumbnail.thumbnail((640, 420), Image.Resampling.LANCZOS)
            thumbnail.save(
                destination / "thumbnail.jpg",
                "JPEG",
                quality=86,
                optimize=True,
                progressive=True,
            )

        entry = {
            key: value
            for key, value in definition.items()
            if key not in {"source", "micronsPerPixel"}
        }
        entry["thumbnailPath"] = f"{slide_id}/thumbnail.jpg"
        entry["source"] = {
            "type": "dzi",
            "path": f"{slide_id}/{slide_id}.dzi",
            "width": width,
            "height": height,
            "micronsPerPixel": definition["micronsPerPixel"],
        }
        entry["attribution"] = {
            "title": "AI Explorer STEM synthetic educational microscopy",
            "license": "Indian Servers educational content",
        }
        entry["reviewedAt"] = "2026-07-31"
        catalog_slides.append(entry)

    catalog = {
        "schemaVersion": 1,
        "catalogVersion": "2026.07.31.1",
        "generatedAt": "2026-07-31T00:00:00Z",
        "note": (
            "Starter slides are synthetic educational images and must not be used "
            "for clinical diagnosis."
        ),
        "slides": catalog_slides,
    }
    (output_root / "catalog.json").write_text(
        json.dumps(catalog, indent=2, ensure_ascii=True) + "\n",
        encoding="utf-8",
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--source",
        type=Path,
        default=Path(r"D:\3D objects\Biology\Microscopy\_source"),
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path(r"D:\3D objects\Biology\Microscopy\cdn"),
    )
    args = parser.parse_args()
    build_package(args.source.resolve(), args.output.resolve())
    print(f"Microscopy CDN package created at {args.output.resolve()}")


if __name__ == "__main__":
    main()
