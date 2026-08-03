import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const catalogPath = path.join(
  root,
  "app/src/main/java/com/indianservers/AIbiology/data/LessonCatalog.kt",
);
const outputPath = path.join(root, "requiredinlineimages.md");
const source = fs.readFileSync(catalogPath, "utf8");

function readAreaCalls(text) {
  const start = text.indexOf("val areas = listOf(");
  const end = text.indexOf("\n\n    val conceptCount", start);
  const block = text.slice(start, end);
  const calls = [];
  let cursor = 0;
  while ((cursor = block.indexOf("area(", cursor)) !== -1) {
    let depth = 0;
    let quote = false;
    let escaped = false;
    let finish = cursor;
    for (let index = cursor; index < block.length; index += 1) {
      const char = block[index];
      if (escaped) {
        escaped = false;
      } else if (char === "\\") {
        escaped = true;
      } else if (char === '"') {
        quote = !quote;
      } else if (!quote && char === "(") {
        depth += 1;
      } else if (!quote && char === ")") {
        depth -= 1;
        if (depth === 0) {
          finish = index + 1;
          break;
        }
      }
    }
    calls.push(block.slice(cursor, finish));
    cursor = finish;
  }
  return calls.map((call) => {
    const number = Number(call.match(/^area\((\d+)/)?.[1]);
    const quoted = [...call.matchAll(/"([^"]+)"/g)].map((match) => match[1]);
    return { number, topic: quoted[0], concepts: quoted.slice(1) };
  });
}

const groupRules = {
  1: [
    ["Biology Basics", /Biology|Living Organisms|Branches|Scope|Scientific Method|Research|Safety|Ethics/],
    ["Nature of Life", /Organization|Emergent|Homeostasis|Adaptation|Evolution|Diversity/],
    ["Measurement and Data", /Units|Notation|Figures|Data|Statistics/],
  ],
  2: [
    ["Water", /Water|Hydrogen/],
    ["Carbohydrates", /Carbo|sacchar|Glycogen|Starch|Cellulose|Chitin/],
    ["Lipids", /Lipid|Fatty|Triglyceride|Phospholipid|Steroid|Wax/],
    ["Proteins", /Protein|Amino|Peptide|Folding|Denaturation/],
    ["Nucleic Acids", /Nucleic|DNA|RNA|ATP|Nucleotide/],
    ["Micronutrients", /Vitamin|Mineral/],
  ],
  3: [
    ["Cell Types and Theory", /Theory|Prokaryotic|Eukaryotic/],
    ["Cell Boundary", /Membrane|Cell Wall|Fluid Mosaic|Endocytosis|Exocytosis/],
    ["Cell Organelles", /Cytoplasm|Nucleus|Nucleolus|Ribosome|Mitochond|Chloroplast|Reticulum|Golgi|Lysosome|Vacuole|Peroxisome|Centrosome/],
    ["Cell Shape and Movement", /Cytoskeleton|Cilia|Flagella/],
    ["Cell Coordination and Life Cycle", /Communication|Junction|Cycle|Death|Stem/],
  ],
  7: [
    ["Cellular Respiration", /Respiration|Glycolysis|Link Reaction|Krebs|Electron|ATP|Fermentation/],
    ["Photosynthesis", /Photo|Pigment|Light Reactions|Calvin|CAM|C4/],
  ],
  9: [
    ["Mendelian Genetics", /Mendel|Monohybrid|Dihybrid|Test Cross|Back Cross/],
    ["Inheritance Beyond Mendel", /Dominance|Codominance|Alleles|Polygenic|Epistasis|Linkage|Crossing|Sex Linkage/],
    ["Advanced Genetics", /Population|Human|Cytogenetics|Quantitative/],
  ],
  14: [
    ["Plant Structure", /Cell|Tissue|Root|Stem|Leaves|Flowers|Fruits|Seeds|Anatomy/],
    ["Plant Physiology", /Transport|Nutrition|Photosynthesis|Respiration|Transpiration|Hormone|Tropism|Photoperiod|Vernal/],
    ["Plant Reproduction and Technology", /Reproduction|Biotechnology/],
  ],
  17: [["Human Organ Systems", /.*/]],
  21: [
    ["Ecosystem Basics", /Ecosystem|Biome|Food|Energy|Nutrient/],
    ["Ecological Interactions", /Population|Community|Succession/],
    ["Global Ecology and Conservation", /Conservation|Climate|Biodiversity/],
  ],
  31: [
    ["Invertebrate Zoology", /Porifera|Cnidaria|Platy|Nematoda|Annelida|Arthropoda|Mollusca|Echinodermata/],
    ["Vertebrate Zoology", /Fish|Amphibian|Reptile|Bird|Mammal/],
  ],
  43: [
    ["Imaging and Specimen Preparation", /Microscopy|Histology|Staining/],
    ["Cell and Molecular Methods", /Culture|Spectro|Chromato|ELISA|Western|Flow|Centrif/],
    ["Research Quality and Safety", /Experimental|Biosafety/],
  ],
  44: [
    ["Molecular and Computational Research", /Genetic|Molecular|Structural|Genomic|Computational|Synthetic|Drug/],
    ["Medical and Regenerative Research", /Physiology|Developmental|Regenerative|Nano|Neuro|Medicine|Precision|Biomaterial|Aging/],
    ["Life Across Time and Space", /Chrono|Extremophile|Astrobiology/],
  ],
};

function subtopicFor(area, concept) {
  const rules = groupRules[area.number] ?? [];
  for (const [name, pattern] of rules) {
    if (pattern.test(concept)) return name;
  }
  const topic = area.topic
    .replace(/\s*\([^)]*\)\s*/g, " ")
    .replace(/\s*&\s*/g, " and ")
    .trim();
  return `${topic} Concepts`;
}

function slug(value) {
  return value.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
}

const areas = readAreaCalls(source);
function visualClass(area, concept) {
  const value = `${area.topic} ${concept}`;
  if (/Method|Research|Safety|Data|Statistics|Notation|Figures|Units|Microscopy|Culture|Staining|Spectro|Chromato|ELISA|Western|Flow Cytometry|Centrifugation|Experimental|Biosafety|PCR|Electrophoresis|Sequencing|Bioinformatics|Database|Assembly|Model|Imaging|Diagnostics/i.test(value)) {
    return "method";
  }
  if (/Cell|Tissue|Membrane|Wall|Nucleus|Nucleolus|Ribosome|Mitochond|Chloroplast|Reticulum|Golgi|Lysosome|Vacuole|Cytoskeleton|Centrosome|Peroxisome|Cilia|Flagella|Root|Stem|Leaves|Flower|Fruit|Seed|Anatom|\bOrgan\b|\b[A-Za-z]+ System\b|Neuron|Synapse|Gland|Brain|Placenta/i.test(value)) {
    return "structure";
  }
  if (/Bacteria|Virus|Fungi|Fungal|Protozo|Algae|Archaea|Parasite|Helminth|Arthropod|Insect|Porifera|Cnidaria|Platy|Nematoda|Annelida|Mollusca|Echinoderm|Fish|Amphibian|Reptile|Bird|Mammal|Bryophyte|Pteridophyte|Gymnosperm|Angiosperm|Plankton|Coral|Lichen|Extremophile/i.test(value)) {
    return "organism";
  }
  if (/Respiration|Glycolysis|Reaction|Cycle|Synthesis|Fermentation|Photosynthesis|Transport|Diffusion|Osmosis|Replication|Transcription|Translation|Processing|Regulation|Division|Mitosis|Meiosis|Fertilization|Cleavage|Gastrulation|Organogenesis|Reproduction|Transpiration|Nutrition|Metabolism|Potential|Channel|Pump|Signalling|Transduction/i.test(value)) {
    return "process";
  }
  if (/Disease|Cancer|Pathology|Medical|Clinical|Immunity|Immune|Antibod|Vaccine|Hypersensitivity|Immunodeficiency|Transplant|Therapy|Pharmacology|Fertility|Pregnancy|Birth|IVF|Contraception|Hormonal Disorder|Precision Medicine/i.test(value)) {
    return "health";
  }
  if (/Ecology|Ecosystem|Biome|Food Chain|Food Web|Energy Flow|Cycle|Population|Community|Succession|Conservation|Climate|Biodiversity|Pollution|Quality|Sustainability|Restoration|Marine|Agricultur|Crop|Soil|Pest|Irrigation|Fisheries/i.test(value)) {
    return "ecology";
  }
  return "concept";
}

function imagePlans(area, concept) {
  const kind = visualClass(area, concept);
  const shared = [
    {
      role: "Opening visual",
      need:
        `Ultra-realistic hero image of ${concept} in its correct ${area.topic} context. Show the subject clearly at a useful learning angle, ` +
        "with scientifically correct form, proportions, environment, and relationships. No title, watermark, decorative DNA, or unrelated objects.",
    },
  ];
  const byKind = {
    structure: [
      {
        role: "External structure",
        need: `Ultra-detailed external or whole-structure view of ${concept}, showing its correct shape, orientation, neighbouring structures, and biological scale. Use a clean natural or clinical setting; no labels.`,
      },
      {
        role: "Cutaway and internal organization",
        need: `Scientifically accurate cutaway or sectional view of ${concept}. Reveal the important internal regions and boundaries without distorting their relative positions. Leave clean callout space, but do not add text.`,
      },
      {
        role: "Function in action",
        need: `Realistic action view showing ${concept} performing its main biological function. Include the relevant cells, molecules, tissue, or surrounding system and show direction of movement only through natural composition, not decorative arrows.`,
      },
      {
        role: "Scale bridge",
        need: `A scientifically accurate scale sequence for ${concept}, moving from organism or organ context to tissue, cell, and molecular detail where relevant. Make every magnification visually distinct and proportionally credible.`,
      },
      {
        role: "Comparison",
        need: `Side-by-side realistic comparison of ${concept} with the closest commonly confused structure or with a normal changed state. Keep camera angle, scale, lighting, and crop matched so the biological difference is obvious.`,
      },
    ],
    process: [
      {
        role: "Starting conditions",
        need: `Realistic biological scene showing the starting materials, location, and conditions required for ${concept}. Show correct cellular compartment, organism, tissue, or environment; no labels.`,
      },
      {
        role: "Stage sequence",
        need: `Scientifically accurate multi-stage visual sequence of ${concept} from start to finish. Use consistent scale and orientation, show only accepted major stages, and leave space for later reviewed labels.`,
      },
      {
        role: "Mechanism close-up",
        need: `Ultra-detailed close-up of the key mechanism behind ${concept}, including the correct interacting structures or molecules and their spatial relationship. Avoid symbolic gears, sparks, or fantasy energy effects.`,
      },
      {
        role: "Inputs and outputs in context",
        need: `Realistic context view showing where inputs enter and outputs leave during ${concept}. Present matter and energy relationships accurately without turning the image into an infographic.`,
      },
      {
        role: "Changed condition",
        need: `Matched comparison showing ${concept} under normal conditions and after one important variable changes. Keep all unrelated factors visually constant so the cause-and-effect lesson is clear.`,
      },
    ],
    organism: [
      {
        role: "Habitat portrait",
        need: `Ultra-realistic portrait of a representative example of ${concept} in its correct habitat, showing diagnostic body form, surface texture, colour range, behaviour, and scale without anthropomorphism.`,
      },
      {
        role: "Anatomy or cell plan",
        need: `Scientifically accurate cutaway, microscopy-style, or transparent-body view showing the defining anatomy or cellular organization of ${concept}. Preserve real proportions and leave room for reviewed labels.`,
      },
      {
        role: "Diversity lineup",
        need: `Matched-scale lineup of several representative forms within ${concept}, selected to show genuine biological diversity rather than cosmetic variation. Use consistent viewing angle and neutral background.`,
      },
      {
        role: "Life cycle",
        need: `Realistic life-cycle sequence for ${concept}, showing the correct major stages, host changes or metamorphosis where relevant, and the environment in which each stage occurs.`,
      },
      {
        role: "Ecological interaction",
        need: `Natural-history scene showing ${concept} interacting with food, host, predator, pollinator, symbiotic partner, or physical habitat in a biologically accurate way.`,
      },
    ],
    method: [
      {
        role: "Real laboratory setup",
        need: `Photorealistic, technically correct setup used to study ${concept}. Include only required instruments, samples, controls, and personal protective equipment; arrange every component as it would be used in a real laboratory.`,
      },
      {
        role: "Workflow",
        need: `Step-by-step photographic workflow for ${concept}, from sample preparation through measurement or analysis. Show correct containers, volumes, orientation, and sequence; reserve space for later numbering.`,
      },
      {
        role: "Instrument or operation close-up",
        need: `Macro view of the critical operation in ${concept}, showing correct hand position, sample placement, instrument settings area, and safety practice without readable brand names.`,
      },
      {
        role: "Real output",
        need: `Scientifically plausible output produced by ${concept}, such as a micrograph, band pattern, curve, sequence readout, plate result, or model. Use internally consistent data, not decorative random marks.`,
      },
      {
        role: "Controls and errors",
        need: `Matched comparison of a valid result, positive or negative control where relevant, and one common technical failure for ${concept}. Make the visual difference accurate enough for learners to diagnose the error.`,
      },
    ],
    health: [
      {
        role: "Normal biological baseline",
        need: `Ultra-realistic view of the normal cells, tissue, organ, or immune process related to ${concept}. Show correct anatomy and scale so later changed-state images have a trustworthy baseline.`,
      },
      {
        role: "Mechanism",
        need: `Scientifically accurate visual of the cellular or molecular mechanism central to ${concept}, placed inside the correct tissue or body context. Avoid frightening exaggeration and unsupported visual claims.`,
      },
      {
        role: "Normal versus changed state",
        need: `Matched clinical or histological comparison relevant to ${concept}, with identical orientation, magnification, and lighting. Show only established differences and avoid implying diagnosis from appearance alone.`,
      },
      {
        role: "Diagnosis or measurement",
        need: `Realistic scene showing how ${concept} is investigated or measured, including appropriate sample, imaging view, laboratory output, or population data. Protect patient identity and avoid branded equipment.`,
      },
      {
        role: "Prevention or treatment context",
        need: `Evidence-aligned visual showing a prevention, monitoring, or treatment context related to ${concept}. Do not show a guaranteed outcome, dosage, product endorsement, or patient-specific medical instruction.`,
      },
    ],
    ecology: [
      {
        role: "Wide ecosystem context",
        need: `Ultra-realistic wide environmental scene for ${concept}, showing correct climate, water, soil, dominant organisms, spatial scale, and human influence where relevant.`,
      },
      {
        role: "Interaction close-up",
        need: `Natural-history close-up showing the key organism-environment or species interaction involved in ${concept}. Include accurate behaviour, season, life stage, and habitat details.`,
      },
      {
        role: "Flow or cycle sequence",
        need: `Scientifically accurate landscape-to-organism sequence showing how matter, energy, organisms, or disturbance move through ${concept}. Keep quantities visually plausible and reserve space for reviewed arrows.`,
      },
      {
        role: "Field evidence",
        need: `Photorealistic field-research scene used to measure ${concept}, with correct sampling design, quadrats, transects, sensors, tags, water tests, or remote-sensing context as appropriate.`,
      },
      {
        role: "Before and after",
        need: `Matched viewpoint comparison showing ${concept} before and after a real disturbance, management action, seasonal shift, or restoration period. Keep time scale and ecological response plausible.`,
      },
    ],
    concept: [
      {
        role: "Concrete biological example",
        need: `Ultra-realistic biological example that makes ${concept} visible in a real organism, cell, specimen, population, or research setting. Choose one unambiguous example and exclude decorative filler.`,
      },
      {
        role: "Parts or factors",
        need: `Scientifically accurate composition showing the essential parts, factors, or levels involved in ${concept}. Use real biological objects and spatial relationships instead of generic symbols.`,
      },
      {
        role: "How it works",
        need: `Realistic sequence or layered view explaining how ${concept} works. Show the accepted causal order and reserve clean space for later reviewed labels without embedding explanatory text.`,
      },
      {
        role: "Evidence view",
        need: `A credible observation, specimen, experiment, fossil, micrograph, scan, or dataset view that scientists use as evidence when studying ${concept}. The visual result must be internally consistent.`,
      },
      {
        role: "Comparison and misconception",
        need: `Matched visual comparison of ${concept} and the nearest idea learners commonly confuse with it. Make the true distinction visible through accurate examples, not red crosses, cartoons, or decorative symbols.`,
      },
    ],
  };
  return [...shared, ...byKind[kind]];
}

const concepts = areas.flatMap((area) =>
  area.concepts.map((concept) => ({
    topic: area.topic,
    subtopic: subtopicFor(area, concept),
    concept,
    base: `biology/lessons/${slug(area.topic)}/${slug(concept)}`,
    plans: imagePlans(area, concept),
  })),
);

const rows = concepts.flatMap((entry) =>
  entry.plans.map((plan, index) => ({
    ...entry,
    role: plan.role,
    need: plan.need,
    filename: `inline-${String(index + 1).padStart(2, "0")}.webp`,
  })),
);

const lines = [
  "# Required Inline Lesson Images",
  "",
  "This is the production brief for inline visuals that appear inside text lessons.",
  "",
  "- These are **inline lesson images**, not infographics. Infographics remain separate in the dedicated **Infographics** tab.",
  "- Each concept has an opening visual plus five supporting views. Images should appear beside the exact paragraph they explain.",
  "- Preferred format: WebP, sRGB, no watermark, minimum 1600 px on the long edge.",
  "- Keep important content away from the outer 6% safe area so images crop well on phones, tablets, and TVs.",
  "",
  `Total topics: **${areas.length}**`,
  `Total concepts: **${concepts.length}**`,
  `Total requested inline images: **${rows.length}**`,
  `Images per concept: **6**`,
  "",
  "| # | Topic name | Subtopic name | Concept name | Inline role | Filename | What image is needed | Asset folder |",
  "|---:|---|---|---|---|---|---|---|",
  ...rows.map(
    (row, index) =>
      `| ${index + 1} | ${row.topic.replaceAll("|", "\\|")} | ${row.subtopic.replaceAll("|", "\\|")} | ` +
      `${row.concept.replaceAll("|", "\\|")} | ${row.role} | \`${row.filename}\` | ${row.need} | ` +
      `\`${row.base}/\` |`,
  ),
  "",
  "## Required filenames inside every concept folder",
  "",
  "- `inline-01.webp` - opening hero visual",
  "- `inline-02.webp` - first concept-specific supporting view",
  "- `inline-03.webp` - second concept-specific supporting view",
  "- `inline-04.webp` - third concept-specific supporting view",
  "- `inline-05.webp` - fourth concept-specific supporting view",
  "- `inline-06.webp` - fifth concept-specific supporting view",
  "",
  "Infographics use their own `infographic-01.webp` path and are intentionally excluded from this inline-image list.",
  "",
];

fs.writeFileSync(outputPath, lines.join("\n"), "utf8");
console.log(`Wrote ${rows.length} inline image briefs for ${concepts.length} concepts to ${outputPath}`);
