from pathlib import Path
from PIL import Image, ImageDraw


root = Path(__file__).resolve().parents[1]
source = root / "app" / "src" / "main" / "res" / "drawable-nodpi"
targets = sorted(source.glob("lesson_*.webp"))
cell = 128
columns = 12
rows = (len(targets) + columns - 1) // columns
sheet = Image.new("RGB", (columns * cell, rows * cell), "#07192b")
draw = ImageDraw.Draw(sheet)

for index, target in enumerate(targets):
    x = (index % columns) * cell
    y = (index // columns) * cell
    icon = Image.open(target).convert("RGBA")
    sheet.paste(icon, (x + 16, y + 4), icon)
    label = target.stem.replace("lesson_topic_", "T").replace("lesson_subtopic_", "S:")
    draw.text((x + 4, y + 103), label[:20], fill="#dce8f2")

output = root / "build" / "lesson-icon-contact-sheet.jpg"
output.parent.mkdir(parents=True, exist_ok=True)
sheet.save(output, "JPEG", quality=90, optimize=True)
print(output)
