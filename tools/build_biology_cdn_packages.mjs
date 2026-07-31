import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const biologyRoot = "D:\\3D objects\\Biology";
const thumbnailRoot = path.join(projectRoot, "app", "build", "cdn-thumbnails");
const generatedAt = "2026-07-31T00:00:00Z";

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
    folder: "Anatomy", file: "Human Anatomy - Full Body.glb", id: "ANATOMY_FULL_BODY",
    title: "Human Anatomy", scientificName: "Anatomia humana", categoryId: "HUMAN_ANATOMY",
    tags: ["anatomy", "full body", "body systems", "human"],
    description: "A whole-body anatomy overview for studying the spatial relationships between major body systems.",
    parts: [
      p("ANATOMY_SKELETAL_SYSTEM", "Skeletal System", "Supports the body and protects internal organs."),
      p("ANATOMY_MUSCULAR_SYSTEM", "Muscular System", "Produces movement, posture, and heat."),
      p("ANATOMY_NERVOUS_SYSTEM", "Nervous System", "Coordinates sensation, thought, and body responses."),
      p("ANATOMY_CIRCULATORY_SYSTEM", "Circulatory System", "Transports blood, gases, nutrients, and wastes."),
      p("ANATOMY_INTERNAL_ORGANS", "Internal Organs", "Carry out digestion, respiration, filtration, and other essential functions.")
    ]
  },
  {
    folder: "Anatomy", file: "Skeleton.glb", id: "ANATOMY_SKELETON",
    title: "Skeleton", scientificName: "Systema skeletale", categoryId: "HUMAN_ANATOMY",
    tags: ["skeleton", "bones", "joints", "human anatomy"],
    description: "The adult skeleton forms the body's structural framework and protects vulnerable organs.",
    parts: [
      p("SKELETON_SKULL", "Skull", "Protects the brain and supports the structures of the face."),
      p("SKELETON_VERTEBRAL_COLUMN", "Vertebral Column", "Supports the trunk and protects the spinal cord."),
      p("SKELETON_THORACIC_CAGE", "Thoracic Cage", "Protects the heart and lungs."),
      p("SKELETON_UPPER_LIMBS", "Upper Limbs", "Enable reaching, manipulation, and fine movement."),
      p("SKELETON_PELVIS", "Pelvis", "Transfers body weight to the lower limbs and protects pelvic organs."),
      p("SKELETON_LOWER_LIMBS", "Lower Limbs", "Support standing, walking, and locomotion.")
    ]
  },
  {
    folder: "Anatomy", file: "Muscular System.glb", id: "ANATOMY_MUSCULAR_SYSTEM",
    title: "Muscular System", scientificName: "Systema musculare", categoryId: "HUMAN_ANATOMY",
    tags: ["muscles", "movement", "skeletal muscle", "human anatomy"],
    description: "Skeletal muscles generate movement, stabilize joints, maintain posture, and produce heat.",
    parts: [
      p("MUSCLE_HEAD_NECK", "Head and Neck Muscles", "Control facial expression, chewing, swallowing, and head movement."),
      p("MUSCLE_TRUNK", "Trunk Muscles", "Move and stabilize the spine, chest, and abdominal wall."),
      p("MUSCLE_UPPER_LIMB", "Upper Limb Muscles", "Move the shoulder, arm, forearm, and hand."),
      p("MUSCLE_LOWER_LIMB", "Lower Limb Muscles", "Power standing, balance, walking, and running.")
    ]
  },
  {
    folder: "Anatomy", file: "Nervous System.glb", id: "ANATOMY_NERVOUS_SYSTEM",
    title: "Nervous System", scientificName: "Systema nervosum", categoryId: "HUMAN_ANATOMY",
    tags: ["nerves", "brain", "spinal cord", "human anatomy"],
    description: "The nervous system receives information, processes it, and coordinates rapid responses throughout the body.",
    parts: [
      p("NERVOUS_BRAIN", "Brain", "Integrates sensory information and directs complex behavior."),
      p("NERVOUS_SPINAL_CORD", "Spinal Cord", "Carries signals between the brain and body and coordinates reflexes."),
      p("NERVOUS_CRANIAL_NERVES", "Cranial Nerves", "Connect the brain with structures of the head, neck, and internal organs."),
      p("NERVOUS_PERIPHERAL_NERVES", "Peripheral Nerves", "Carry sensory and motor signals throughout the body.")
    ]
  },
  {
    folder: "Anatomy", file: "Circulatory System.glb", id: "ANATOMY_CIRCULATORY_SYSTEM",
    title: "Circulatory System", scientificName: "Systema cardiovasculare", categoryId: "HUMAN_ANATOMY",
    tags: ["circulatory", "heart", "arteries", "veins", "human anatomy"],
    description: "The circulatory system uses the heart and blood vessels to deliver oxygen and nutrients throughout the body.",
    parts: [
      p("CIRCULATORY_HEART", "Heart", "Pumps blood through pulmonary and systemic circuits."),
      p("CIRCULATORY_ARTERIES", "Arteries", "Carry blood away from the heart."),
      p("CIRCULATORY_VEINS", "Veins", "Return blood toward the heart."),
      p("CIRCULATORY_CAPILLARIES", "Capillaries", "Allow exchange between blood and tissues.")
    ]
  },
  {
    folder: "Anatomy", file: "Human Anatomy - Organs.glb", id: "ANATOMY_INTERNAL_ORGANS",
    title: "Internal Organs", scientificName: "Organa interna", categoryId: "HUMAN_ANATOMY",
    tags: ["organs", "viscera", "thorax", "abdomen", "human anatomy"],
    description: "A spatial overview of the major organs of the thorax, abdomen, and pelvis.",
    parts: [
      p("ORGANS_HEART", "Heart", "Pumps blood through the body."),
      p("ORGANS_LUNGS", "Lungs", "Exchange oxygen and carbon dioxide."),
      p("ORGANS_LIVER", "Liver", "Processes nutrients, produces bile, and supports detoxification."),
      p("ORGANS_STOMACH", "Stomach", "Begins mechanical and chemical digestion."),
      p("ORGANS_INTESTINES", "Intestines", "Digest food, absorb nutrients, and form waste."),
      p("ORGANS_KIDNEYS", "Kidneys", "Filter blood and regulate body fluids."),
      p("ORGANS_BLADDER", "Urinary Bladder", "Stores urine before elimination.")
    ]
  },
  {
    folder: "Anatomy", file: "Human Anatomy - Muscles and Organs.glb",
    id: "ANATOMY_MUSCLES_ORGANS", title: "Muscles and Organs",
    scientificName: "Musculi et organa", categoryId: "HUMAN_ANATOMY",
    tags: ["muscles", "organs", "integrated anatomy", "human anatomy"],
    description: "An integrated view showing how superficial muscles relate spatially to internal organs.",
    parts: [
      p("MUSCLES_ORGANS_MUSCLES", "Muscular Layer", "Shows the major superficial skeletal muscles."),
      p("MUSCLES_ORGANS_THORAX", "Thoracic Organs", "Shows organs protected by the chest wall."),
      p("MUSCLES_ORGANS_ABDOMEN", "Abdominal Organs", "Shows major digestive and urinary organs.")
    ]
  },
  {
    folder: "Anatomy", file: "Male Body Surface.glb", id: "ANATOMY_BODY_SURFACE",
    title: "Male Body Surface", scientificName: "Corpus masculinum", categoryId: "HUMAN_ANATOMY",
    tags: ["surface anatomy", "body regions", "male", "human anatomy"],
    description: "A surface-anatomy reference for body regions, orientation, and external landmarks.",
    parts: [
      p("SURFACE_HEAD", "Head", "The superior body region containing the skull and face."),
      p("SURFACE_TRUNK", "Trunk", "The thoracic, abdominal, pelvic, and back regions."),
      p("SURFACE_UPPER_LIMBS", "Upper Limbs", "The shoulder, arm, forearm, wrist, and hand."),
      p("SURFACE_LOWER_LIMBS", "Lower Limbs", "The hip, thigh, leg, ankle, and foot.")
    ]
  },
  {
    folder: "Anatomy", file: "Male Clothed Body.glb", id: "ANATOMY_REFERENCE_BODY",
    title: "Human Reference Body", scientificName: "Homo sapiens", categoryId: "HUMAN_ANATOMY",
    tags: ["human body", "orientation", "reference", "anatomy"],
    description: "A clothed human reference model for anatomical position, scale, and directional terminology.",
    parts: [
      p("REFERENCE_AXIAL", "Axial Region", "The head, neck, and trunk form the body's central axis."),
      p("REFERENCE_APPENDICULAR", "Appendicular Region", "The upper and lower limbs attach to the axial body.")
    ]
  },
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
    folder: "1. CELL Biology", file: "Animal Cell.glb", id: "ANIMAL_CELL",
    title: "Animal Cell", scientificName: "Cellula animalis", categoryId: "CELL_BIOLOGY",
    tags: ["animal cell", "eukaryote", "organelle", "cell"],
    description: "An animal cell is a eukaryotic cell whose membrane-bound organelles coordinate energy use, protein production, transport, and reproduction.",
    parts: [
      p("ANIMAL_CELL_MEMBRANE", "Cell Membrane", "Controls movement of substances into and out of the cell."),
      p("ANIMAL_CELL_CYTOPLASM", "Cytoplasm", "Suspends organelles and supports many metabolic reactions."),
      p("ANIMAL_CELL_NUCLEUS", "Nucleus", "Stores DNA and coordinates gene expression."),
      p("ANIMAL_CELL_MITOCHONDRIA", "Mitochondria", "Generate ATP through cellular respiration."),
      p("ANIMAL_CELL_ROUGH_ER", "Rough Endoplasmic Reticulum", "Synthesizes and processes proteins."),
      p("ANIMAL_CELL_SMOOTH_ER", "Smooth Endoplasmic Reticulum", "Synthesizes lipids and supports detoxification."),
      p("ANIMAL_CELL_GOLGI", "Golgi Apparatus", "Modifies, sorts, and packages cellular products."),
      p("ANIMAL_CELL_LYSOSOMES", "Lysosomes", "Digest and recycle biological material.")
    ]
  },
  {
    folder: "1. CELL Biology", file: "Animal Cell - close match.glb", id: "ANIMAL_CELL_DETAILED",
    title: "Animal Cell - Detailed", scientificName: "Cellula animalis", categoryId: "CELL_BIOLOGY",
    tags: ["animal cell", "eukaryote", "cell anatomy", "detailed model"],
    description: "A detailed animal-cell model for examining the spatial relationships between the nucleus, membrane system, and energy-producing organelles.",
    parts: [
      p("DETAILED_CELL_MEMBRANE", "Cell Membrane", "Forms the selectively permeable boundary of the cell."),
      p("DETAILED_CELL_NUCLEUS", "Nucleus", "Contains the genome and regulates cellular activity."),
      p("DETAILED_CELL_NUCLEOLUS", "Nucleolus", "Produces ribosomal RNA and begins ribosome assembly."),
      p("DETAILED_CELL_MITOCHONDRIA", "Mitochondria", "Convert energy from nutrients into ATP."),
      p("DETAILED_CELL_ER", "Endoplasmic Reticulum", "Produces proteins and lipids in an interconnected membrane network."),
      p("DETAILED_CELL_GOLGI", "Golgi Apparatus", "Processes and directs proteins and lipids."),
      p("DETAILED_CELL_LYSOSOMES", "Lysosomes", "Break down macromolecules and worn cellular components."),
      p("DETAILED_CELL_CYTOSKELETON", "Cytoskeleton", "Maintains cell shape and organizes internal transport.")
    ]
  },
  {
    folder: "1. CELL Biology", file: "Cell Nucleus.glb", id: "CELL_NUCLEUS",
    title: "Cell Nucleus", scientificName: "Nucleus cellularis", categoryId: "CELL_BIOLOGY",
    tags: ["nucleus", "DNA", "chromatin", "organelle"],
    description: "The nucleus protects most of a eukaryotic cell's DNA and controls gene expression, growth, and division.",
    parts: [
      p("NUCLEUS_OUTER_MEMBRANE", "Outer Nuclear Membrane", "Connects with the endoplasmic reticulum."),
      p("NUCLEUS_INNER_MEMBRANE", "Inner Nuclear Membrane", "Faces and organizes the nuclear interior."),
      p("NUCLEUS_PORES", "Nuclear Pores", "Regulate transport between the nucleus and cytoplasm."),
      p("NUCLEUS_NUCLEOPLASM", "Nucleoplasm", "Provides the internal environment of the nucleus."),
      p("NUCLEUS_CHROMATIN", "Chromatin", "Packages DNA with proteins and regulates gene access."),
      p("NUCLEUS_NUCLEOLUS", "Nucleolus", "Produces ribosomal RNA and assembles ribosomal subunits.")
    ]
  },
  {
    folder: "1. CELL Biology", file: "Golgi Apparatus.glb", id: "GOLGI_APPARATUS",
    title: "Golgi Apparatus", scientificName: "Apparatus Golgiensis", categoryId: "CELL_BIOLOGY",
    tags: ["Golgi apparatus", "cisternae", "vesicle", "organelle"],
    description: "The Golgi apparatus modifies, sorts, and packages proteins and lipids received from the endoplasmic reticulum.",
    parts: [
      p("GOLGI_CIS_FACE", "Cis Face", "Receives transport vesicles from the endoplasmic reticulum."),
      p("GOLGI_CISTERNAE", "Cisternae", "Flattened membrane sacs that process cellular products."),
      p("GOLGI_MEDIAL_REGION", "Medial Cisternae", "Perform intermediate modification and sorting steps."),
      p("GOLGI_TRANS_FACE", "Trans Face", "Sorts products toward their cellular destinations."),
      p("GOLGI_VESICLES", "Transport Vesicles", "Carry cargo into, through, and away from the Golgi apparatus.")
    ]
  },
  {
    folder: "1. CELL Biology", file: "Neuron.glb", id: "CELL_BIOLOGY_NEURON",
    thumbnailSlug: "cell-biology-neuron",
    title: "Neuron Cell", scientificName: "Neuron", categoryId: "CELL_BIOLOGY",
    tags: ["neuron", "nerve cell", "axon", "cell"],
    description: "A neuron is a specialized cell that receives information and transmits signals through electrical impulses and chemical synapses.",
    parts: [
      p("CELL_NEURON_DENDRITES", "Dendrites", "Receive signals from nearby cells."),
      p("CELL_NEURON_SOMA", "Cell Body", "Maintains the neuron and integrates incoming signals."),
      p("CELL_NEURON_NUCLEUS", "Nucleus", "Stores DNA and regulates cell activity."),
      p("CELL_NEURON_AXON_HILLOCK", "Axon Hillock", "Initiates an action potential when stimulation reaches threshold."),
      p("CELL_NEURON_AXON", "Axon", "Conducts impulses away from the cell body."),
      p("CELL_NEURON_MYELIN", "Myelin Sheath", "Insulates the axon and accelerates conduction."),
      p("CELL_NEURON_TERMINALS", "Axon Terminals", "Release neurotransmitters to communicate with target cells.")
    ]
  },
  {
    folder: "1. CELL Biology", file: "Ovum.glb", id: "HUMAN_OVUM",
    title: "Human Ovum", scientificName: "Ovum humanum", categoryId: "CELL_BIOLOGY",
    tags: ["ovum", "oocyte", "egg cell", "reproduction"],
    description: "The human ovum is the female reproductive cell that contributes genetic material and cellular resources at fertilization.",
    parts: [
      p("OVUM_CORONA_RADIATA", "Corona Radiata", "A layer of follicular cells surrounding the ovum."),
      p("OVUM_ZONA_PELLUCIDA", "Zona Pellucida", "A protective glycoprotein coat involved in fertilization."),
      p("OVUM_CELL_MEMBRANE", "Cell Membrane", "Encloses the cell and participates in sperm fusion."),
      p("OVUM_CYTOPLASM", "Cytoplasm", "Contains organelles and resources used during early development."),
      p("OVUM_NUCLEUS", "Nucleus", "Carries the maternal haploid genome."),
      p("OVUM_CORTICAL_GRANULES", "Cortical Granules", "Help prevent additional sperm from entering after fertilization.")
    ]
  },
  {
    folder: "1. CELL Biology", file: "Red Blood Cell.glb", id: "RED_BLOOD_CELL",
    title: "Red Blood Cell", scientificName: "Erythrocytus", categoryId: "CELL_BIOLOGY",
    tags: ["red blood cell", "erythrocyte", "hemoglobin", "blood"],
    description: "A red blood cell is a flexible, biconcave cell specialized to transport oxygen and carbon dioxide through the bloodstream.",
    parts: [
      p("RBC_MEMBRANE", "Cell Membrane", "Provides a flexible boundary that withstands repeated deformation."),
      p("RBC_BICONCAVE_SURFACE", "Biconcave Surface", "Increases surface area and shortens gas-diffusion distance."),
      p("RBC_HEMOGLOBIN", "Hemoglobin-Rich Cytoplasm", "Binds and transports oxygen and contributes to carbon-dioxide transport."),
      p("RBC_CENTRAL_PALLOR", "Central Pallor", "The thinner central region created by the biconcave shape.")
    ]
  },
  {
    folder: "1. CELL Biology", file: "Sperm Cell.glb", id: "HUMAN_SPERM_CELL",
    title: "Human Sperm Cell", scientificName: "Spermatozoon humanum", categoryId: "CELL_BIOLOGY",
    tags: ["sperm", "spermatozoon", "gamete", "reproduction"],
    description: "The human sperm cell is a motile male gamete specialized to deliver paternal genetic material during fertilization.",
    parts: [
      p("SPERM_ACROSOME", "Acrosome", "Contains enzymes that help the sperm penetrate the ovum's outer layers."),
      p("SPERM_NUCLEUS", "Nucleus", "Carries the paternal haploid genome."),
      p("SPERM_NECK", "Neck", "Connects the head to the midpiece and contains organizing structures."),
      p("SPERM_MIDPIECE", "Midpiece", "Contains mitochondria that supply energy for movement."),
      p("SPERM_FLAGELLUM", "Flagellum", "Produces the movement that propels the sperm.")
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
  const sourceThumbnail = path.join(
    thumbnailRoot,
    `${definition.thumbnailSlug || modelSlug}.png`
  );
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
  catalogVersion: "2026.07.31.3",
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

const anatomySharedIds = new Set([
  "HUMAN_HEART",
  "HUMAN_LIVER",
  "HUMAN_LUNGS"
]);
const anatomyModels = catalogModels.filter(
  item => item.categoryId === "HUMAN_ANATOMY" || anatomySharedIds.has(item.id)
);
writeJson(path.join(biologyRoot, "anatomy-catalog.json"), {
  schemaVersion: 1,
  catalogVersion: masterCatalog.catalogVersion,
  generatedAt,
  baseUrl: null,
  note: "Anatomy systems plus shared organ packages. Resolve paths from this catalogue URL.",
  models: anatomyModels
});

const humanAnatomyOutput = path.join(biologyRoot, "2. Human Anatomy", "_cdn_packages");
fs.mkdirSync(humanAnatomyOutput, { recursive: true });
writeJson(path.join(humanAnatomyOutput, "catalog.json"), {
  schemaVersion: 1,
  catalogVersion: masterCatalog.catalogVersion,
  generatedAt,
  baseUrl: null,
  note: "These organ files are identical to the validated Human Physiology packages.",
  models: catalogModels
    .filter(item => anatomySharedIds.has(item.id))
    .map(item => ({
      ...item,
      thumbnailPath: `../../${item.thumbnailPath}`,
      manifestPath: `../../${item.manifestPath}`,
      packagePath: `../../${item.packagePath}`
    }))
});

process.stdout.write(`Created ${catalogModels.length} CDN packages.\n`);
