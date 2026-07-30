import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const biologyRoot = "D:\\3D objects\\Biology";
const thumbnailRoot = path.join(projectRoot, "app", "build", "cdn-thumbnails");
const generatedAt = "2026-07-30T00:00:00Z";

const p = (id, title, description, parentPartId = null) => ({
  id,
  title,
  scientificName: null,
  parentPartId,
  selectable: true,
  interactionLevel: 1,
  description: {
    beginner: description,
    student: description,
    advanced: description
  }
});

const definitions = [
  {
    folder: "7. Human Physiology", file: "Heart.glb", id: "HUMAN_HEART",
    title: "Human Heart", scientificName: "Cor", categoryId: "HUMAN_PHYSIOLOGY",
    tags: ["heart", "cardiovascular", "circulation", "human body"],
    description: "The heart is a muscular pump that drives blood through the pulmonary and systemic circulations.",
    parts: [
      p("HEART_RIGHT_ATRIUM", "Right Atrium", "Receives oxygen-poor blood returning from the body."),
      p("HEART_LEFT_ATRIUM", "Left Atrium", "Receives oxygen-rich blood returning from the lungs."),
      p("HEART_RIGHT_VENTRICLE", "Right Ventricle", "Pumps blood toward the lungs."),
      p("HEART_LEFT_VENTRICLE", "Left Ventricle", "Pumps blood through the systemic circulation."),
      p("HEART_AORTA", "Aorta", "Carries oxygen-rich blood from the left ventricle."),
      p("HEART_PULMONARY_TRUNK", "Pulmonary Trunk", "Carries blood from the right ventricle toward the lungs."),
      p("HEART_VENA_CAVA", "Vena Cava", "Returns systemic blood to the right atrium."),
      p("HEART_CORONARY_ARTERIES", "Coronary Arteries", "Supply oxygenated blood to the heart muscle.")
    ]
  },
  {
    folder: "7. Human Physiology", file: "Liver.glb", id: "HUMAN_LIVER",
    title: "Human Liver", scientificName: "Hepar", categoryId: "HUMAN_PHYSIOLOGY",
    tags: ["liver", "digestive system", "metabolism", "human body"],
    description: "The liver processes nutrients, produces bile, stores energy, and helps remove harmful substances from blood.",
    parts: [
      p("LIVER_RIGHT_LOBE", "Right Lobe", "The largest anatomical lobe of the liver."),
      p("LIVER_LEFT_LOBE", "Left Lobe", "The smaller major lobe extending across the upper abdomen."),
      p("LIVER_HEPATIC_ARTERY", "Hepatic Artery", "Supplies oxygen-rich blood to liver tissue."),
      p("LIVER_PORTAL_VEIN", "Hepatic Portal Vein", "Brings nutrient-rich blood from digestive organs."),
      p("LIVER_HEPATIC_VEINS", "Hepatic Veins", "Drain processed blood toward the inferior vena cava."),
      p("LIVER_BILE_DUCTS", "Bile Ducts", "Carry bile produced by liver cells.")
    ]
  },
  {
    folder: "7. Human Physiology", file: "Lungs.glb", id: "HUMAN_LUNGS",
    title: "Human Lungs", scientificName: "Pulmones", categoryId: "HUMAN_PHYSIOLOGY",
    tags: ["lungs", "respiratory system", "gas exchange", "human body"],
    description: "The lungs exchange oxygen and carbon dioxide between inhaled air and the blood.",
    parts: [
      p("LUNGS_TRACHEA", "Trachea", "Conducts air toward the main bronchi."),
      p("LUNGS_RIGHT_LUNG", "Right Lung", "The three-lobed lung on the right side of the chest."),
      p("LUNGS_LEFT_LUNG", "Left Lung", "The two-lobed lung shaped around the heart."),
      p("LUNGS_MAIN_BRONCHI", "Main Bronchi", "Carry air from the trachea into each lung."),
      p("LUNGS_BRONCHIOLES", "Bronchioles", "Small branching airways within the lungs."),
      p("LUNGS_ALVEOLI", "Alveoli", "Microscopic air sacs where gas exchange occurs."),
      p("LUNGS_PLEURA", "Pleura", "A double membrane that surrounds each lung.")
    ]
  },
  {
    folder: "7. Human Physiology", file: "Neuron.glb", id: "HUMAN_NEURON",
    title: "Neuron", scientificName: "Neuron", categoryId: "HUMAN_PHYSIOLOGY",
    tags: ["neuron", "nervous system", "brain", "cell"],
    description: "A neuron is a specialized cell that receives, processes, and transmits electrical and chemical signals.",
    parts: [
      p("NEURON_DENDRITES", "Dendrites", "Receive signals from other cells."),
      p("NEURON_SOMA", "Cell Body", "Maintains the neuron and integrates incoming signals."),
      p("NEURON_NUCLEUS", "Nucleus", "Stores DNA and regulates cellular activity."),
      p("NEURON_AXON", "Axon", "Conducts impulses away from the cell body."),
      p("NEURON_MYELIN", "Myelin Sheath", "Insulates the axon and accelerates signal conduction."),
      p("NEURON_NODES", "Nodes of Ranvier", "Gaps in myelin where electrical signals are regenerated."),
      p("NEURON_TERMINALS", "Axon Terminals", "Release neurotransmitters at communication junctions.")
    ]
  },
  {
    folder: "1. CELL Biology", file: "Bacteriacell.glb", id: "BACTERIA_CELL",
    title: "Bacterial Cell", scientificName: "Bacterium", categoryId: "CELL_BIOLOGY",
    tags: ["bacteria", "prokaryote", "microbiology", "cell"],
    description: "A bacterial cell is a prokaryotic cell that performs life processes without a membrane-bound nucleus.",
    parts: [
      p("BACTERIA_CAPSULE", "Capsule", "Helps some bacteria adhere to surfaces and resist drying.", "BACTERIA_ENVELOPE"),
      p("BACTERIA_CELL_WALL", "Cell Wall", "Preserves cell shape and protects against osmotic pressure.", "BACTERIA_ENVELOPE"),
      p("BACTERIA_CELL_MEMBRANE", "Cell Membrane", "Controls transport and hosts important metabolic reactions.", "BACTERIA_ENVELOPE"),
      p("BACTERIA_NUCLEOID", "Nucleoid", "Contains the main circular bacterial chromosome.", "BACTERIA_INTERNAL"),
      p("BACTERIA_RIBOSOMES", "Ribosomes", "Build proteins from genetic instructions.", "BACTERIA_INTERNAL"),
      p("BACTERIA_FLAGELLUM", "Flagellum", "Propels a motile bacterium.", "BACTERIA_EXTERNAL"),
      p("BACTERIA_PILI", "Pili", "Support attachment and, in some cases, DNA transfer.", "BACTERIA_EXTERNAL")
    ]
  },
  {
    folder: "1. CELL Biology", file: "Cell Membrane.glb", id: "CELL_MEMBRANE",
    title: "Cell Membrane", scientificName: "Membrana cellularis", categoryId: "CELL_BIOLOGY",
    tags: ["membrane", "phospholipid", "transport", "cell"],
    description: "The cell membrane is a selectively permeable boundary that coordinates transport, recognition, and signaling.",
    parts: [
      p("MEMBRANE_PHOSPHOLIPID_HEADS", "Phospholipid Heads", "Interact with water on each side of the membrane."),
      p("MEMBRANE_FATTY_ACID_TAILS", "Fatty Acid Tails", "Form the water-repelling interior of the bilayer."),
      p("MEMBRANE_CHANNEL_PROTEIN", "Channel Protein", "Provides a selective route through the membrane."),
      p("MEMBRANE_GLYCOPROTEIN", "Glycoprotein", "Supports cell recognition and signaling."),
      p("MEMBRANE_CHOLESTEROL", "Cholesterol", "Helps regulate membrane fluidity.")
    ]
  },
  {
    folder: "1. CELL Biology", file: "Chloroplast.glb", id: "CHLOROPLAST",
    title: "Chloroplast", scientificName: "Chloroplastus", categoryId: "CELL_BIOLOGY",
    tags: ["chloroplast", "photosynthesis", "plant", "organelle"],
    description: "Chloroplasts capture light energy and use it to build energy-rich organic molecules.",
    parts: [
      p("CHLOROPLAST_OUTER_MEMBRANE", "Outer Membrane", "Forms the external boundary of the chloroplast."),
      p("CHLOROPLAST_INNER_MEMBRANE", "Inner Membrane", "Controls transport into the stroma."),
      p("CHLOROPLAST_STROMA", "Stroma", "Contains enzymes used during carbon fixation."),
      p("CHLOROPLAST_GRANUM", "Granum", "A stack of thylakoids."),
      p("CHLOROPLAST_THYLAKOID", "Thylakoid", "Contains pigments and proteins for light-dependent reactions."),
      p("CHLOROPLAST_LAMELLA", "Stroma Lamella", "Connects thylakoid stacks.")
    ]
  },
  {
    folder: "1. CELL Biology", file: "epithelial microvilli.glb", id: "EPITHELIAL_MICROVILLI",
    title: "Epithelial Microvilli", scientificName: "Microvilli", categoryId: "CELL_BIOLOGY",
    tags: ["microvilli", "epithelium", "absorption", "cell"],
    description: "Microvilli are membrane projections that increase the absorptive surface area of epithelial cells.",
    parts: [
      p("MICROVILLI_PROJECTIONS", "Microvilli", "Increase the surface area available for absorption."),
      p("MICROVILLI_ACTIN_CORE", "Actin Core", "Supports the shape of each microvillus."),
      p("MICROVILLI_TERMINAL_WEB", "Terminal Web", "Anchors microvilli to the cell cortex."),
      p("MICROVILLI_CELL_SURFACE", "Apical Cell Surface", "Faces the lumen or external environment.")
    ]
  },
  {
    folder: "1. CELL Biology", file: "Lysosome.glb", id: "LYSOSOME",
    title: "Lysosome", scientificName: "Lysosoma", categoryId: "CELL_BIOLOGY",
    tags: ["lysosome", "digestion", "recycling", "organelle"],
    description: "Lysosomes digest biological material and recycle damaged cellular components.",
    parts: [
      p("LYSOSOME_MEMBRANE", "Lysosomal Membrane", "Separates digestive enzymes from the cytoplasm."),
      p("LYSOSOME_LUMEN", "Acidic Lumen", "Provides the low-pH environment needed by lysosomal enzymes."),
      p("LYSOSOME_HYDROLASES", "Hydrolytic Enzymes", "Break macromolecules into reusable components."),
      p("LYSOSOME_PROTON_PUMP", "Proton Pump", "Moves hydrogen ions into the lysosome.")
    ]
  },
  {
    folder: "1. CELL Biology", file: "Mitochondrion.glb", id: "MITOCHONDRION",
    title: "Mitochondrion", scientificName: "Mitochondrium", categoryId: "CELL_BIOLOGY",
    tags: ["mitochondrion", "ATP", "respiration", "organelle"],
    description: "Mitochondria convert energy from nutrients into ATP through cellular respiration.",
    parts: [
      p("MITO_OUTER_MEMBRANE", "Outer Membrane", "Encloses the organelle."),
      p("MITO_INTERMEMBRANE_SPACE", "Intermembrane Space", "Accumulates protons used to produce ATP."),
      p("MITO_INNER_MEMBRANE", "Inner Membrane", "Contains respiratory-chain proteins."),
      p("MITO_CRISTAE", "Cristae", "Increase the area of the inner membrane."),
      p("MITO_MATRIX", "Matrix", "Contains metabolic enzymes and mitochondrial DNA."),
      p("MITO_DNA", "Mitochondrial DNA", "Carries a small set of mitochondrial genes.")
    ]
  },
  {
    folder: "1. CELL Biology", file: "plant cell wall.glb", id: "PLANT_CELL_WALL",
    title: "Plant Cell Wall", scientificName: "Paries cellularis", categoryId: "CELL_BIOLOGY",
    tags: ["cell wall", "cellulose", "plant", "cell"],
    description: "The plant cell wall supports and protects the cell while permitting controlled growth.",
    parts: [
      p("WALL_MIDDLE_LAMELLA", "Middle Lamella", "Binds neighboring plant cells together."),
      p("WALL_PRIMARY", "Primary Cell Wall", "A flexible wall deposited while the cell grows."),
      p("WALL_SECONDARY", "Secondary Cell Wall", "A stronger internal layer deposited by some cells."),
      p("WALL_PLASMODESMATA", "Plasmodesmata", "Channels that connect neighboring plant cells.")
    ]
  },
  {
    folder: "1. CELL Biology", file: "PlantCell.glb", id: "PLANT_CELL",
    title: "Plant Cell", scientificName: "Cellula vegetalis", categoryId: "CELL_BIOLOGY",
    tags: ["plant cell", "cellulose", "photosynthesis", "cell"],
    description: "A plant cell contains a cellulose wall, chloroplasts, and a large central vacuole.",
    parts: [
      p("PLANT_CELL_WALL", "Cell Wall", "Supports and protects the cell."),
      p("PLANT_CELL_MEMBRANE", "Cell Membrane", "Controls movement into and out of the cell."),
      p("PLANT_CELL_NUCLEUS", "Nucleus", "Stores DNA and coordinates cell activity."),
      p("PLANT_CELL_CHLOROPLAST", "Chloroplast", "Captures light energy for photosynthesis."),
      p("PLANT_CELL_VACUOLE", "Central Vacuole", "Stores water and supports turgor pressure."),
      p("PLANT_CELL_MITOCHONDRION", "Mitochondrion", "Produces ATP through cellular respiration."),
      p("PLANT_CELL_GOLGI", "Golgi Apparatus", "Modifies and sorts cellular products."),
      p("PLANT_CELL_ER", "Endoplasmic Reticulum", "Synthesizes proteins and lipids.")
    ]
  },
  {
    folder: "1. CELL Biology", file: "Ribosomes.glb", id: "RIBOSOME",
    title: "Ribosome", scientificName: "Ribosoma", categoryId: "CELL_BIOLOGY",
    tags: ["ribosome", "translation", "protein", "RNA"],
    description: "Ribosomes translate messenger RNA into ordered chains of amino acids.",
    parts: [
      p("RIBOSOME_LARGE_SUBUNIT", "Large Subunit", "Catalyzes peptide-bond formation."),
      p("RIBOSOME_SMALL_SUBUNIT", "Small Subunit", "Binds and decodes messenger RNA."),
      p("RIBOSOME_A_SITE", "A Site", "Accepts an incoming aminoacyl-tRNA."),
      p("RIBOSOME_P_SITE", "P Site", "Holds the tRNA carrying the growing peptide."),
      p("RIBOSOME_E_SITE", "E Site", "Releases an uncharged tRNA."),
      p("RIBOSOME_MRNA", "mRNA Channel", "Guides messenger RNA through the ribosome.")
    ]
  },
  {
    folder: "1. CELL Biology", file: "Rough Endoplasmic Reticulum.glb", id: "ROUGH_ER",
    title: "Rough Endoplasmic Reticulum", scientificName: "Reticulum endoplasmicum granulosum", categoryId: "CELL_BIOLOGY",
    tags: ["rough ER", "protein synthesis", "ribosome", "organelle"],
    description: "The rough endoplasmic reticulum synthesizes and begins processing proteins for secretion or membranes.",
    parts: [
      p("ROUGH_ER_CISTERNAE", "Cisternae", "Flattened membrane sacs that organize protein processing."),
      p("ROUGH_ER_RIBOSOMES", "Bound Ribosomes", "Synthesize proteins entering the secretory pathway."),
      p("ROUGH_ER_LUMEN", "ER Lumen", "Supports protein folding and quality control."),
      p("ROUGH_ER_TRANSLOCON", "Translocon", "Conducts new proteins across or into the ER membrane."),
      p("ROUGH_ER_VESICLE", "Transport Vesicle", "Carries products from the ER toward the Golgi apparatus.")
    ]
  },
  {
    folder: "1. CELL Biology", file: "Smooth Endoplasmic Reticulum.glb", id: "SMOOTH_ER",
    title: "Smooth Endoplasmic Reticulum", scientificName: "Reticulum endoplasmicum nongranulosum", categoryId: "CELL_BIOLOGY",
    tags: ["smooth ER", "lipid", "detoxification", "organelle"],
    description: "The smooth endoplasmic reticulum synthesizes lipids, supports detoxification, and stores calcium.",
    parts: [
      p("SMOOTH_ER_TUBULES", "Tubule Network", "Provides a large membrane reaction surface."),
      p("SMOOTH_ER_MEMBRANE", "ER Membrane", "Contains enzymes for lipid metabolism and detoxification."),
      p("SMOOTH_ER_LUMEN", "ER Lumen", "Stores and transports molecules."),
      p("SMOOTH_ER_CALCIUM", "Calcium Store", "Maintains controlled calcium reserves in specialized cells.")
    ]
  },
  {
    folder: "1. CELL Biology", file: "Vacuole.glb", id: "VACUOLE",
    title: "Vacuole", scientificName: "Vacuolum", categoryId: "CELL_BIOLOGY",
    tags: ["vacuole", "storage", "turgor", "plant"],
    description: "A vacuole stores water and dissolved substances and helps maintain plant-cell pressure.",
    parts: [
      p("VACUOLE_TONOPLAST", "Tonoplast", "The selective membrane surrounding the vacuole."),
      p("VACUOLE_CELL_SAP", "Cell Sap", "Stores water, ions, pigments, and other solutes."),
      p("VACUOLE_TRANSPORTERS", "Transport Proteins", "Control movement across the tonoplast.")
    ]
  },
  {
    folder: "1. CELL Biology", file: "WhiteBloodCell.glb", id: "WHITE_BLOOD_CELL",
    title: "White Blood Cell", scientificName: "Leukocytus", categoryId: "CELL_BIOLOGY",
    tags: ["white blood cell", "leukocyte", "immune system", "cell"],
    description: "White blood cells detect threats, coordinate immune responses, and remove harmful material.",
    parts: [
      p("WBC_MEMBRANE", "Cell Membrane", "Allows the cell to change shape and interact with its environment."),
      p("WBC_NUCLEUS", "Nucleus", "Contains DNA and can help identify the leukocyte type."),
      p("WBC_CYTOPLASM", "Cytoplasm", "Contains machinery for movement and immune activity."),
      p("WBC_GRANULES", "Granules", "Store molecules used in immune defense.")
    ]
  }
];

function sha256(filePath) {
  const hash = crypto.createHash("sha256");
  hash.update(fs.readFileSync(filePath));
  return hash.digest("hex");
}

function slug(value) {
  return value.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
}

function writeJson(filePath, value) {
  fs.writeFileSync(filePath, `${JSON.stringify(value, null, 2)}\n`, "utf8");
}

const catalogModels = [];
for (const definition of definitions) {
  const sourceDirectory = path.resolve(biologyRoot, definition.folder);
  const outputRoot = path.resolve(sourceDirectory, "_cdn_packages");
  if (!outputRoot.startsWith(`${sourceDirectory}${path.sep}`)) {
    throw new Error(`Unsafe output directory: ${outputRoot}`);
  }
  fs.mkdirSync(outputRoot, { recursive: true });

  const sourceModel = path.join(sourceDirectory, definition.file);
  const modelSlug = slug(path.basename(definition.file, ".glb"));
  const sourceThumbnail = path.join(thumbnailRoot, `${modelSlug}.png`);
  if (!fs.existsSync(sourceModel) || !fs.existsSync(sourceThumbnail)) {
    throw new Error(`Missing source asset for ${definition.title}`);
  }

  const packageName = path.basename(definition.file, ".glb");
  const staging = path.join(outputRoot, packageName);
  const resolvedStaging = path.resolve(staging);
  if (!resolvedStaging.startsWith(`${outputRoot}${path.sep}`)) {
    throw new Error(`Unsafe staging directory: ${resolvedStaging}`);
  }
  fs.rmSync(staging, { recursive: true, force: true });
  fs.mkdirSync(staging, { recursive: true });

  const packagedModel = path.join(staging, "model.glb");
  const packagedThumbnail = path.join(staging, "thumbnail.png");
  fs.copyFileSync(sourceModel, packagedModel);
  fs.copyFileSync(sourceThumbnail, packagedThumbnail);

  const modelSize = fs.statSync(packagedModel).size;
  const modelHash = sha256(packagedModel);
  const thumbnailHash = sha256(packagedThumbnail);
  const parts = definition.parts.map(part => ({
    ...part,
    recognition: {
      status: "REQUIRES_SEMANTIC_MESHES",
      visibleNodeNames: [`VIS_${part.id}`],
      hitNodeNames: [`HIT_${part.id}`],
      materialNames: [`MAT_${part.id}`],
      sourceNodeNames: [],
      fallbackHotspot: null
    },
    assets: {
      iconPath: null,
      audioPath: null
    },
    animationName: null,
    cameraPreset: null
  }));

  const manifest = {
    schemaVersion: 1,
    modelVersion: 1,
    id: definition.id,
    slug: modelSlug,
    title: definition.title,
    alternativeNames: [],
    scientificName: definition.scientificName,
    categoryId: definition.categoryId,
    tags: definition.tags,
    gradeLevels: ["BEGINNER", "STUDENT", "ADVANCED"],
    description: {
      beginner: definition.description,
      student: definition.description,
      advanced: definition.description
    },
    thumbnail: {
      path: "thumbnail.png",
      mediaType: "image/png",
      sha256: thumbnailHash
    },
    model: {
      path: "model.glb",
      mediaType: "model/gltf-binary",
      quality: "HIGH",
      sizeBytes: modelSize,
      sha256: modelHash,
      lod: {
        low: null,
        medium: null,
        high: "model.glb"
      }
    },
    capabilities: {
      rotation: true,
      zoom: true,
      fullscreen: true,
      partSelection: false,
      animations: false,
      explodedView: false,
      sectionView: false,
      ar: false
    },
    sceneInspection: {
      nodeCount: 1,
      meshCount: 1,
      materialCount: 1,
      animationCount: 0,
      semanticMeshStatus: "NOT_AUTHORED"
    },
    parts,
    hierarchy: [...new Set(parts.map(part => part.parentPartId).filter(Boolean))].map(id => ({
      id,
      children: parts.filter(part => part.parentPartId === id).map(part => part.id)
    })),
    contentReview: {
      status: "DRAFT_REQUIRES_BIOLOGY_REVIEW",
      lastReviewedAt: "2026-07-30",
      sourceAttribution: []
    },
    downloadPolicy: {
      storeInAppLibrary: true,
      validateSha256: true,
      removableByUser: true
    }
  };

  writeJson(path.join(staging, "manifest.json"), manifest);
  writeJson(path.join(staging, "checksums.json"), {
    algorithm: "SHA-256",
    files: {
      "model.glb": modelHash,
      "thumbnail.png": thumbnailHash
    }
  });
  fs.writeFileSync(
    path.join(staging, "README.txt"),
    [
      `${definition.title} - AI Explorer STEM CDN package`,
      "",
      "Download manifest.json metadata from the catalogue, then download this ZIP on selection.",
      "Validate model.glb with the SHA-256 value before adding it to the App Library.",
      "",
      "IMPORTANT PART-IDENTIFICATION STATUS",
      "This GLB currently contains one mesh and one material.",
      "Exact Zygote-style part highlighting, isolation, and hiding require a new GLB export",
      "with separate VIS_<SEMANTIC_ID> and HIT_<SEMANTIC_ID> nodes.",
      "The planned semantic IDs are listed in manifest.json.",
      ""
    ].join("\r\n"),
    "utf8"
  );

  const standaloneManifest = path.join(outputRoot, `${packageName}.manifest.json`);
  writeJson(standaloneManifest, manifest);
  const zipPath = path.join(outputRoot, `${packageName}.zip`);
  fs.rmSync(zipPath, { force: true });
  const archive = spawnSync(
    "tar",
    ["-a", "-c", "-f", zipPath, "-C", staging, "."],
    { windowsHide: true, encoding: "utf8" }
  );
  if (archive.status !== 0) {
    throw new Error(`Could not create ${zipPath}: ${archive.stderr}`);
  }
  for (const packagedFile of ["model.glb", "manifest.json", "checksums.json", "README.txt"]) {
    fs.rmSync(path.join(staging, packagedFile), { force: true });
  }

  const relativeOutput = path.relative(biologyRoot, outputRoot).split(path.sep).join("/");
  catalogModels.push({
    id: definition.id,
    title: definition.title,
    scientificName: definition.scientificName,
    categoryId: definition.categoryId,
    tags: definition.tags,
    shortDescription: definition.description,
    thumbnailPath: `${relativeOutput}/${packageName}/thumbnail.png`,
    manifestPath: `${relativeOutput}/${packageName}.manifest.json`,
    packagePath: `${relativeOutput}/${packageName}.zip`,
    packageSizeBytes: fs.statSync(zipPath).size,
    packageSha256: sha256(zipPath),
    modelSizeBytes: modelSize,
    modelVersion: 1,
    partCount: parts.length,
    partIdentificationReady: false,
    supportsAr: false,
    supportsAnimations: false
  });
}

const masterCatalog = {
  schemaVersion: 1,
  catalogVersion: "2026.07.30.1",
  generatedAt,
  baseUrl: null,
  note: "Resolve relative paths against the URL of this catalogue.",
  models: catalogModels
};
writeJson(path.join(biologyRoot, "biology-catalog.json"), masterCatalog);

for (const folder of [...new Set(definitions.map(item => item.folder))]) {
  const outputRoot = path.join(biologyRoot, folder, "_cdn_packages");
  const relativePrefix = `${folder.replaceAll("\\", "/")}/_cdn_packages/`;
  const categoryModels = catalogModels
    .filter(item => item.packagePath.startsWith(relativePrefix))
    .map(item => ({
      ...item,
      thumbnailPath: item.thumbnailPath.slice(relativePrefix.length),
      manifestPath: item.manifestPath.slice(relativePrefix.length),
      packagePath: item.packagePath.slice(relativePrefix.length)
    }));
  writeJson(path.join(outputRoot, "catalog.json"), {
    schemaVersion: 1,
    catalogVersion: masterCatalog.catalogVersion,
    generatedAt,
    baseUrl: null,
    models: categoryModels
  });
}

process.stdout.write(`Created ${catalogModels.length} CDN packages.\n`);
