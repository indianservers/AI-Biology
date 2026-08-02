package com.indianservers.AIbiology.data

/**
 * Curriculum-oriented learning copy for the offline 3D collection.
 *
 * Core explanations follow the concepts common to NCERT Biology XI, Indian state-board
 * higher-secondary biology, and introductory university cell biology. "Know more" deliberately
 * goes one step beyond the school-level outcome without being required to use the 3D explorer.
 */
data class ConceptTheory(
    val coreTheory: String,
    val example: String,
    val knowMore: List<String>,
    val syllabusLinks: List<String>
)

object BiologyTheoryCatalog {
    private val concepts = mapOf(
        "Bacteriacell.glb" to ConceptTheory(
            coreTheory = "Bacteria are prokaryotes: their DNA occupies a nucleoid rather than a membrane-bound nucleus. A plasma membrane encloses cytoplasm containing 70S ribosomes, while most bacteria also have a peptidoglycan cell wall. Some species add a capsule, pili or rotating flagella. Because bacteria lack membrane-bound organelles, processes such as respiration, transport, transcription and translation occur at the cell membrane or in the cytoplasm.",
            example = "Example: Escherichia coli uses membrane transport proteins to absorb glucose, ribosomes to make enzymes, and binary fission to produce two daughter cells.",
            knowMore = listOf(
                "Gram-positive and Gram-negative bacteria differ in wall thickness and outer-membrane organisation.",
                "Plasmids are small, independently replicating DNA molecules that can carry traits such as antibiotic resistance.",
                "Transcription and translation can occur together because no nuclear envelope separates DNA from ribosomes."
            ),
            syllabusLinks = listOf("NCERT XI: Cell—The Unit of Life", "State boards: Prokaryotic cell", "University: Microbial cell biology")
        ),
        "Cell Membrane.glb" to ConceptTheory(
            coreTheory = "The plasma membrane is a selectively permeable phospholipid bilayer described by the fluid-mosaic model. Hydrophilic heads face water and hydrophobic tails form an internal barrier. Proteins act as channels, carriers, receptors, enzymes and anchors; carbohydrates aid recognition. Small non-polar molecules may diffuse directly, while ions and many polar molecules require proteins. Active transport uses energy to move substances against a concentration or electrochemical gradient.",
            example = "Example: oxygen diffuses into a cell, water moves by osmosis, and the sodium–potassium pump uses ATP to maintain ion gradients in animal cells.",
            knowMore = listOf(
                "Cholesterol buffers membrane fluidity in animal cells.",
                "Carrier-mediated transport can become saturated because the number of transport proteins is limited.",
                "Membrane asymmetry and lipid rafts help organise signalling and vesicle traffic."
            ),
            syllabusLinks = listOf("NCERT XI: Cell membrane", "State boards: Transport across membranes", "University: Membrane dynamics")
        ),
        "Chloroplast.glb" to ConceptTheory(
            coreTheory = "A chloroplast is a double-membrane plastid that performs photosynthesis. Thylakoids, often stacked as grana, contain chlorophyll, photosystems and electron carriers. Light reactions use light and water to form oxygen, ATP and NADPH. In the stroma, the Calvin cycle uses ATP and NADPH to reduce carbon dioxide and build carbohydrate. Chloroplasts also contain circular DNA and 70S-like ribosomes.",
            example = "Example: in a leaf mesophyll cell, light reactions release oxygen from water while Rubisco fixes carbon dioxide in the stroma.",
            knowMore = listOf(
                "Chemiosmosis couples a proton gradient across the thylakoid membrane to ATP synthase.",
                "C4 and CAM plants concentrate carbon dioxide and reduce photorespiration under particular climates.",
                "Chloroplast DNA and double membranes support the endosymbiotic origin of plastids."
            ),
            syllabusLinks = listOf("NCERT XI: Photosynthesis in Higher Plants", "State boards: Plastids", "University: Bioenergetics")
        ),
        "epithelial microvilli.glb" to ConceptTheory(
            coreTheory = "Microvilli are non-motile, finger-like extensions of the apical plasma membrane of many epithelial cells. Each projection contains a core of actin filaments linked to the terminal web. By increasing membrane surface area, microvilli provide more space for digestive enzymes and transport proteins, greatly improving absorption without greatly increasing cell volume.",
            example = "Example: microvilli form the intestinal brush border, where enzymes finish digestion and transporters absorb glucose, amino acids and ions.",
            knowMore = listOf(
                "Microvilli differ from cilia: microvilli use actin and mainly increase absorption; cilia use microtubules and often move fluid.",
                "Loss of brush-border surface in coeliac disease can reduce nutrient absorption.",
                "Polarised epithelia direct substances across a tissue by placing different transporters on apical and basal surfaces."
            ),
            syllabusLinks = listOf("NCERT XI: Epithelial tissue", "State boards: Absorption in intestine", "University: Cell polarity")
        ),
        "Lysosome.glb" to ConceptTheory(
            coreTheory = "A lysosome is a single-membrane organelle containing acid hydrolase enzymes. Proton pumps keep its lumen acidic, allowing proteins, lipids, nucleic acids and carbohydrates to be broken into reusable units. Material reaches lysosomes through endocytosis, phagocytosis or autophagy. The membrane isolates these enzymes from the cytosol and carries transporters that return useful products to the cell.",
            example = "Example: a macrophage engulfs a bacterium; the vesicle fuses with lysosomes, whose enzymes digest the bacterium.",
            knowMore = listOf(
                "Autophagy removes damaged organelles and recycles their components during stress.",
                "Failure of one lysosomal enzyme can cause a lysosomal storage disorder.",
                "Lysosomes also participate in nutrient sensing, membrane repair and regulated secretion."
            ),
            syllabusLinks = listOf("NCERT XI: Endomembrane system", "State boards: Cell organelles", "University: Autophagy")
        ),
        "Mitochondrion.glb" to ConceptTheory(
            coreTheory = "A mitochondrion is a double-membrane organelle central to aerobic respiration. Pyruvate oxidation and the citric-acid cycle in the matrix transfer energy to NADH and FADH2. Electron carriers in the inner membrane use that energy to pump protons into the intermembrane space. ATP synthase then uses the returning proton flow to make ATP. Cristae increase the area available for these reactions.",
            example = "Example: contracting muscle cells contain many mitochondria because repeated contraction creates a high demand for ATP.",
            knowMore = listOf(
                "Oxidative phosphorylation links electron transfer, oxygen reduction and chemiosmosis.",
                "Mitochondria contain circular DNA and divide, supporting an endosymbiotic origin.",
                "Mitochondrial dynamics, calcium handling and apoptosis extend their role beyond ATP production."
            ),
            syllabusLinks = listOf("NCERT XI: Respiration in Plants", "State boards: Mitochondria", "University: Oxidative phosphorylation")
        ),
        "Neuron.glb" to ConceptTheory(
            coreTheory = "A neuron is an excitable cell specialised for rapid communication. Dendrites receive inputs, the cell body integrates them, and the axon carries an action potential to terminals. Unequal ion distributions create a resting membrane potential. When threshold is reached, voltage-gated channels produce a self-propagating electrical signal. At most synapses, terminals release neurotransmitter that changes the activity of a target cell.",
            example = "Example: touching a hot surface activates sensory neurons; interneurons process the signal and motor neurons stimulate muscles to withdraw the hand.",
            knowMore = listOf(
                "Myelin enables saltatory conduction between nodes of Ranvier and greatly increases speed.",
                "Excitatory and inhibitory postsynaptic potentials are integrated near the axon hillock.",
                "Synaptic plasticity changes connection strength and contributes to learning and memory."
            ),
            syllabusLinks = listOf("NCERT XI: Neural Control and Coordination", "State boards: Nervous tissue", "University: Neurophysiology")
        ),
        "plant cell wall.glb" to ConceptTheory(
            coreTheory = "The plant cell wall lies outside the plasma membrane. Cellulose microfibrils provide tensile strength, while hemicellulose and pectin form a hydrated matrix. The flexible primary wall can expand during growth; some cells later deposit a stronger secondary wall, often containing lignin. The middle lamella joins neighbouring cells, and plasmodesmata provide cytoplasmic connections.",
            example = "Example: water entering a plant cell creates turgor pressure; the wall resists overexpansion and helps the leaf remain firm.",
            knowMore = listOf(
                "Wall-loosening proteins and acid growth allow controlled cell expansion.",
                "Lignified secondary walls support xylem vessels and help prevent collapse during water transport.",
                "The wall is a dynamic signalling and defence interface, not merely a rigid shell."
            ),
            syllabusLinks = listOf("NCERT XI: Plant cell", "State boards: Cell wall and tissues", "University: Plant cell mechanics")
        ),
        "PlantCell.glb" to ConceptTheory(
            coreTheory = "A plant cell is eukaryotic, so its DNA is enclosed in a nucleus and its functions are divided among membrane-bound organelles. Its distinguishing features include a cellulose cell wall, plastids such as chloroplasts, a large central vacuole and plasmodesmata. Chloroplasts capture light energy, mitochondria release usable energy, the vacuole maintains water balance, and the endomembrane system makes and transports molecules.",
            example = "Example: a palisade mesophyll cell has many chloroplasts for photosynthesis, whereas a root-hair cell usually lacks chloroplasts but has a long extension for absorption.",
            knowMore = listOf(
                "Plant cells communicate through plasmodesmata and long-distance vascular signals.",
                "The vacuole, wall and membrane jointly determine water potential and turgor.",
                "Different plant cell types express the same genome differently to become guard cells, vessels or fibres."
            ),
            syllabusLinks = listOf("NCERT XI: Cell—The Unit of Life", "State boards: Plant cell", "University: Plant cell differentiation")
        ),
        "Ribosomes.glb" to ConceptTheory(
            coreTheory = "Ribosomes are ribonucleoprotein machines that translate the nucleotide sequence of messenger RNA into an amino-acid sequence. The small subunit binds mRNA and checks codon–anticodon pairing; the large subunit catalyses peptide-bond formation. Transfer RNAs carry specific amino acids. Free ribosomes usually make cytosolic proteins, while ribosomes bound to rough ER make proteins for secretion, membranes or endomembrane organelles.",
            example = "Example: a pancreatic cell uses rough-ER-bound ribosomes to synthesise the protein hormone insulin.",
            knowMore = listOf(
                "Bacterial 70S ribosomes and eukaryotic cytosolic 80S ribosomes differ in size and composition.",
                "The ribosome is a ribozyme because ribosomal RNA catalyses peptide-bond formation.",
                "Polyribosomes allow several ribosomes to translate one mRNA at the same time."
            ),
            syllabusLinks = listOf("NCERT XI: Ribosomes", "State boards: Protein synthesis", "University: Translation")
        ),
        "Rough Endoplasmic Reticulum.glb" to ConceptTheory(
            coreTheory = "The rough endoplasmic reticulum is a network of flattened membrane cisternae continuous with the nuclear envelope. Ribosomes attach while translating proteins with an ER signal sequence. The growing polypeptide enters the ER, where it folds, forms bonds and may receive carbohydrate groups. Quality-control systems retain or destroy incorrectly folded proteins, while transport vesicles carry accepted cargo toward the Golgi apparatus.",
            example = "Example: an antibody-producing plasma cell has abundant rough ER because it secretes large amounts of protein.",
            knowMore = listOf(
                "Signal-recognition particle directs a translating ribosome to the ER translocon.",
                "N-linked glycosylation and chaperones assist folding and quality control.",
                "Persistent accumulation of misfolded proteins activates the unfolded-protein response."
            ),
            syllabusLinks = listOf("NCERT XI: Endoplasmic reticulum", "State boards: Endomembrane system", "University: Protein trafficking")
        ),
        "Smooth Endoplasmic Reticulum.glb" to ConceptTheory(
            coreTheory = "The smooth endoplasmic reticulum is an interconnected membrane network without attached ribosomes. Its enzymes synthesise phospholipids, cholesterol and steroid molecules, and in liver cells help modify drugs and toxins. It also stores calcium ions. In muscle, a specialised smooth ER called the sarcoplasmic reticulum releases calcium to initiate contraction and pumps it back during relaxation.",
            example = "Example: steroid-producing cells in the adrenal cortex contain extensive smooth ER for hormone synthesis.",
            knowMore = listOf(
                "Cytochrome P450 enzymes oxidise many lipid-soluble compounds during detoxification.",
                "ER calcium release acts as a rapid intracellular signal.",
                "Membrane-contact sites allow lipid and calcium exchange between ER and mitochondria."
            ),
            syllabusLinks = listOf("NCERT XI: Endoplasmic reticulum", "State boards: Cell organelles", "University: Lipid metabolism")
        ),
        "Vacuole.glb" to ConceptTheory(
            coreTheory = "A vacuole is a membrane-bound compartment; in mature plant cells the central vacuole may occupy most of the cell. Its membrane, the tonoplast, contains pumps and transporters that regulate ions, pH and solutes. Water entering the vacuole produces turgor pressure against the wall. Vacuoles also store pigments, nutrients and defensive compounds and can digest or recycle cellular material.",
            example = "Example: guard-cell vacuoles gain or lose ions and water, changing cell shape to open or close a stomatal pore.",
            knowMore = listOf(
                "Tonoplast proton pumps create gradients that power secondary transport.",
                "Vacuolar pigments such as anthocyanins contribute to flower and fruit colour.",
                "Contractile vacuoles in some freshwater protists remove excess water, illustrating a different vacuole function."
            ),
            syllabusLinks = listOf("NCERT XI: Vacuoles", "State boards: Plant-water relations", "University: Tonoplast transport")
        ),
        "WhiteBloodCell.glb" to ConceptTheory(
            coreTheory = "White blood cells, or leukocytes, are nucleated immune cells formed from bone-marrow stem cells. Neutrophils and macrophages engulf microbes; lymphocytes provide targeted adaptive responses; eosinophils, basophils and other cells specialise in parasites, inflammation or signalling. Leukocytes detect chemical cues, adhere to vessel walls and move into tissues where their receptors and secreted molecules coordinate defence.",
            example = "Example: during a bacterial infection, neutrophils leave nearby capillaries, follow chemical signals and phagocytose microbes.",
            knowMore = listOf(
                "B and T lymphocytes generate immunological memory after infection or vaccination.",
                "Diapedesis is the regulated passage of leukocytes between endothelial cells.",
                "A differential white-cell count helps clinicians relate changed cell proportions to infection, allergy or blood disease."
            ),
            syllabusLinks = listOf("NCERT XII: Human Health and Disease", "State boards: Immunity", "University: Immunology")
        )
    )

    fun forModel(model: BiologyModel): ConceptTheory = concepts[model.fileName]
        ?: ConceptTheory(
            coreTheory = model.description.ifBlank {
                "${model.title} is a biological structure whose form can be connected to its function by examining its labelled parts."
            },
            example = "Example: rotate the model, select each label, and explain how a change in that part could affect the whole structure.",
            knowMore = listOf(
                "Compare this structure with a related cell, tissue or organ.",
                "Connect its microscopic structure to the molecular process it performs.",
                "Predict how damage or environmental change would alter its function."
            ),
            syllabusLinks = listOf("School biology: Structure and function", "University biology: Systems thinking")
        )

    fun detailedPartTheory(title: String, shortDescription: String): String {
        val extension = when {
            title.contains("membrane", true) || title.contains("tonoplast", true) ->
                "Its lipid bilayer creates a controlled boundary; embedded proteins make transport selective rather than simply open or closed."
            title.contains("wall", true) || title.contains("capsule", true) ->
                "Its material composition determines protection, shape and the way the cell interacts mechanically with its environment."
            title.contains("ribosome", true) ->
                "During translation, messenger RNA provides the codon sequence and transfer RNAs deliver the corresponding amino acids."
            title.contains("nucleoid", true) || title.contains("nucleus", true) ->
                "Organising genetic material allows stored information to be copied and selectively expressed as RNA and protein."
            title.contains("axon", true) || title.contains("dendrite", true) || title.contains("terminal", true) ->
                "Its specialised geometry gives neural information a preferred direction from input, through integration, to output."
            title.contains("stroma", true) || title.contains("thylakoid", true) || title.contains("grana", true) ->
                "Separating light capture from carbon fixation lets chloroplast reactions use distinct enzymes, membranes and gradients."
            title.contains("crista", true) || title.contains("matrix", true) || title.contains("intermembrane", true) ->
                "This compartment contributes to the spatial separation required for aerobic respiration and chemiosmosis."
            title.contains("vacuole", true) || title.contains("cell sap", true) ->
                "Solute concentration influences water movement by osmosis, linking storage directly to cell pressure and shape."
            title.contains("lumen", true) || title.contains("cistern", true) || title.contains("tubule", true) ->
                "Compartmentalisation provides a controlled chemical environment and a large membrane surface for reactions."
            else ->
                "Its position and organisation show a central biological principle: structure is adapted to the process it performs."
        }
        return "$shortDescription $extension"
    }

    fun partKnowMore(title: String, shortDescription: String): List<String> {
        val application = when {
            title.contains("membrane", true) || title.contains("channel", true) ->
                "Application: predict how temperature, lipid composition or a blocked transport protein would change movement across this boundary."
            title.contains("DNA", true) || title.contains("nucle", true) ->
                "Application: connect a mutation or altered gene expression to a change in the proteins made by the cell."
            title.contains("ribosome", true) ->
                "Application: several antibiotics work because bacterial and eukaryotic ribosomes are structurally different."
            title.contains("axon", true) || title.contains("dendrite", true) ->
                "Application: loss of myelin or ion-channel function changes the speed and reliability of neural signalling."
            title.contains("wall", true) || title.contains("vacuole", true) ->
                "Application: compare a turgid plant cell with the same cell after water loss."
            else ->
                "Application: predict the effect on the whole cell if this structure were absent, damaged or overactive."
        }
        return listOf(
            detailedPartTheory(title, shortDescription),
            application,
            "Study link: relate this labelled structure to the organelle or cell-level process shown in the model briefing."
        )
    }
}
