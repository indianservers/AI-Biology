from __future__ import annotations

from collections import deque
from pathlib import Path
from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE = Path(r"C:\Users\saisa\Downloads\ChatGPT Image Aug 3, 2026, 10_03_25 PM.png")
OUTPUT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"

MAIN_TOPICS = [
    "introduction_to_biology", "biomolecules", "cell_biology", "cell_division",
    "membranes_transport", "enzymes", "bioenergetics", "molecular_biology",
    "genetics", "biotechnology", "evolution", "diversity_of_life",
    "microbiology", "plant_biology", "animal_biology", "human_anatomy",
    "human_physiology", "immunology", "developmental_biology", "histology",
    "ecology", "environmental_biology", "ethology", "marine_biology",
    "agricultural_biology", "medical_biology", "parasitology", "virology",
    "mycology", "entomology", "zoology", "botany", "biochemistry", "biophysics",
    "systems_biology", "bioinformatics",
]

SUBTOPIC_GROUPS = [
    ["cell_structure", "organelles", "cell_membrane", "cell_cycle", "cell_communication"],
    ["mendelian_genetics", "inheritance_patterns", "chromosomes", "dna_and_rna", "population_genetics"],
    ["plant_cell", "plant_tissues", "photosynthesis", "plant_hormones", "plant_reproduction"],
    ["nervous_system", "digestive_system", "circulatory_system", "respiratory_system", "endocrine_system"],
    ["bacteria", "virus", "fungi", "protozoa", "algae"],
    ["genetic_engineering", "pcr", "gel_electrophoresis", "cloning", "biotech_stem_cells"],
    ["ecosystems", "food_chain", "energy_flow", "biogeochemical_cycles", "biodiversity"],
    ["fertilization", "embryonic_development", "organogenesis", "development_stem_cells", "regeneration"],
    ["carbohydrates", "proteins", "lipids", "nucleic_acids", "vitamins"],
    ["glycolysis", "krebs_cycle", "electron_transport_chain", "atp_synthesis", "fermentation"],
    ["epithelial_tissue", "connective_tissue", "muscle_tissue", "nervous_tissue", "blood"],
    ["skeletal_system", "muscular_system", "organs", "body_regions", "histology_slides"],
]


def edge_connected_background(image: Image.Image) -> Image.Image:
    rgba = image.convert("RGBA")
    pixels = rgba.load()
    width, height = rgba.size
    seen: set[tuple[int, int]] = set()
    queue: deque[tuple[int, int]] = deque()

    def background_like(x: int, y: int) -> bool:
        red, green, blue, _ = pixels[x, y]
        return min(red, green, blue) >= 224 and max(red, green, blue) - min(red, green, blue) <= 22

    for x in range(width):
        queue.extend(((x, 0), (x, height - 1)))
    for y in range(height):
        queue.extend(((0, y), (width - 1, y)))

    while queue:
        point = queue.popleft()
        if point in seen:
            continue
        x, y = point
        if not background_like(x, y):
            continue
        seen.add(point)
        if x > 0:
            queue.append((x - 1, y))
        if x + 1 < width:
            queue.append((x + 1, y))
        if y > 0:
            queue.append((x, y - 1))
        if y + 1 < height:
            queue.append((x, y + 1))

    for x, y in seen:
        pixels[x, y] = (*pixels[x, y][:3], 0)

    alpha = rgba.getchannel("A")
    bounds = alpha.getbbox()
    if bounds is None:
        raise ValueError("Crop became fully transparent")
    cropped = rgba.crop(bounds)
    side = max(cropped.size)
    padding = max(5, side // 14)
    canvas = Image.new("RGBA", (side + padding * 2, side + padding * 2), (0, 0, 0, 0))
    canvas.alpha_composite(
        cropped,
        ((canvas.width - cropped.width) // 2, (canvas.height - cropped.height) // 2),
    )
    return canvas.resize((96, 96), Image.Resampling.LANCZOS)


def save_icon(source: Image.Image, box: tuple[int, int, int, int], name: str) -> int:
    icon = edge_connected_background(source.crop(box))
    target = OUTPUT / f"{name}.webp"
    icon.save(target, "WEBP", quality=78, method=4, exact=True)
    return target.stat().st_size


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for previous in OUTPUT.glob("lesson_topic_*.webp"):
        previous.unlink()
    source = Image.open(SOURCE).convert("RGB")
    if source.size != (1536, 1024):
        raise ValueError(f"Expected 1536x1024 source, found {source.size}")

    written: list[tuple[str, int]] = []

    main_x = [19 + column * 123 for column in range(12)]
    main_y = [110, 258, 405]
    for index, topic in enumerate(MAIN_TOPICS):
        x = main_x[index % 12]
        y = main_y[index // 12]
        written.append(
            (f"lesson_topic_{index + 1:02d}", save_icon(
                source,
                (x + 19, y + 8, x + 92, y + 89),
                f"lesson_topic_{index + 1:02d}",
            ))
        )

    group_crop_x = [36, 393, 762, 1118]
    group_step_x = [66, 67, 65, 77]
    group_y = [610, 738, 849]
    for group_index, names in enumerate(SUBTOPIC_GROUPS):
        column = group_index % 4
        base_x = group_crop_x[column]
        base_y = group_y[group_index // 4]
        for icon_index, name in enumerate(names):
            x = base_x + icon_index * group_step_x[column]
            written.append(
                (f"lesson_subtopic_{name}", save_icon(
                    source,
                    (x, base_y + 8, x + 44, base_y + 58),
                    f"lesson_subtopic_{name}",
                ))
            )

    total = sum(size for _, size in written)
    print(f"Wrote {len(written)} icons ({total} bytes / {total / 1024:.1f} KiB)")
    print(f"Largest icon: {max(written, key=lambda item: item[1])}")


if __name__ == "__main__":
    main()
