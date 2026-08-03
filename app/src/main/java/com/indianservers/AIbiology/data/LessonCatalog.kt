package com.indianservers.AIbiology.data

enum class LessonLevel(val label: String, val shortLabel: String) {
    FOUNDATION("Grade 6–8", "6–8"),
    SECONDARY("Grade 9–10", "9–10"),
    SENIOR_SECONDARY("Grade 11–12", "11–12"),
    UNDERGRADUATE("Undergraduate", "UG"),
    POSTGRADUATE("Postgraduate", "PG")
}

data class LessonArea(
    val number: Int,
    val id: String,
    val title: String,
    val startingLevel: LessonLevel,
    val concepts: List<String>
) {
    val caption: String get() = LessonEditorial.areaCaption(this)
}

data class LessonPhase(
    val title: String,
    val explanation: String,
    val example: String,
    val didYouKnow: String
)

/**
 * Reading and revision material for one curriculum concept.
 *
 * It deliberately lives in the data layer. The next UI/design phase can present
 * these blocks as cards, tabs, audio prompts, or printable notes without having
 * to rewrite the lesson copy.
 */
data class LessonLearningContent(
    val detailedExplanation: String,
    val easyWayToLearn: List<String>,
    val realLifeExamples: List<String>,
    val importantPoints: List<String>,
    val commonMistake: String,
    val quickCheckQuestion: String,
    val quickCheckAnswer: String
)

data class Lesson(
    val id: String,
    val area: LessonArea,
    val title: String,
    val subtopic: String,
    val subtopicCaption: String,
    val subtitle: String,
    val languageTag: String = "en",
    val phases: List<LessonPhase>,
    val learningContent: LessonLearningContent,
    val relatedConcepts: List<String>,
    val tags: List<String>
) {
    val learningOutcomes: List<String> get() = LessonEditorial.outcomes(this)
}

/**
 * Curriculum-backed lesson catalogue. Content is kept outside the Fragment and
 * carries a language tag and stable IDs so translated catalogues can be added
 * later without changing navigation or UI code.
 */
object LessonCatalog {
    val areas = listOf(
        area(1, "Introduction to Biology", LessonLevel.FOUNDATION,
            "What is Biology?", "Characteristics of Living Organisms", "Branches of Biology",
            "Scope of Biology", "Scientific Method", "Biological Research", "Laboratory Safety",
            "Ethics in Biology", "Levels of Organization", "Emergent Properties", "Homeostasis",
            "Adaptation", "Evolution", "Diversity of Life", "SI Units", "Scientific Notation",
            "Significant Figures", "Biological Data", "Statistics Basics"),
        area(2, "Biomolecules", LessonLevel.FOUNDATION,
            "Biomolecules", "Water", "Hydrogen Bonds", "Carbohydrates", "Monosaccharides", "Disaccharides",
            "Polysaccharides", "Glycogen", "Starch", "Cellulose", "Chitin", "Lipids",
            "Fatty Acids", "Triglycerides", "Phospholipids", "Steroids", "Waxes", "Proteins",
            "Amino Acids", "Peptide Bonds", "Protein Structure", "Protein Folding",
            "Denaturation", "Protein Functions", "Nucleic Acids", "DNA", "RNA", "ATP",
            "Nucleotides", "Vitamins", "Minerals"),
        area(3, "Cell Biology", LessonLevel.FOUNDATION,
            "What is a Cell?", "Discovery of the Cell", "Cell Theory", "Prokaryotic Cells",
            "Eukaryotic Cells", "Animal Cell", "Plasma Membrane",
            "Cell Wall", "Cytoplasm", "Nucleus", "Nucleolus", "Ribosomes", "Mitochondria",
            "Chloroplast", "Endoplasmic Reticulum", "Golgi Apparatus", "Lysosomes", "Vacuoles",
            "Cytoskeleton", "Centrosome", "Peroxisomes", "Cilia", "Flagella",
            "Fluid Mosaic Model", "Endocytosis", "Exocytosis", "Cell Communication",
            "Cell Junctions", "Cell Cycle", "Cell Death", "Stem Cells", "Cell Differentiation",
            "Cell Size", "Cell Shape", "Surface Area to Volume Ratio", "Cell as a Factory",
            "Comparing Plant and Animal Cells", "Specialised Cells",
            "Unicellular and Multicellular Organisms", "Cell Growth", "Cell Repair",
            "Cell Receptors", "Extracellular Matrix", "Collagen", "Cell Adhesion",
            "Gap Junctions", "Plasmodesmata", "Review of Cell Biology",
            "Importance of Cell Biology", "Applications of Cell Biology"),
        area(4, "Cell Division", LessonLevel.SECONDARY,
            "Cell Cycle", "Mitosis", "Meiosis", "Cytokinesis", "Chromosomes", "Chromatin",
            "Cell Cycle Regulation", "Cancer Biology"),
        area(5, "Biological Membranes & Transport", LessonLevel.SECONDARY,
            "Diffusion", "Osmosis", "Facilitated Diffusion", "Active Transport",
            "Bulk Transport", "Cell Transport", "Transport Proteins", "Membrane Potential",
            "Ion Channels", "Pumps"),
        area(6, "Enzymes", LessonLevel.SECONDARY,
            "Enzymes", "Enzyme Structure", "Active Site", "Lock and Key Model", "Induced Fit",
            "Cofactors", "Coenzymes", "Enzyme Inhibitors", "Factors Affecting Enzyme Activity",
            "Michaelis–Menten Kinetics", "Industrial Applications of Enzymes"),
        area(7, "Bioenergetics", LessonLevel.SECONDARY,
            "Cellular Respiration", "Glycolysis", "Link Reaction", "Krebs Cycle",
            "Electron Transport Chain", "ATP Synthesis", "Fermentation", "Photosynthesis",
            "Photosynthetic Pigments", "Light Reactions", "Calvin Cycle", "Photorespiration",
            "CAM Plants", "C4 Plants"),
        area(8, "Molecular Biology", LessonLevel.SENIOR_SECONDARY,
            "DNA Structure", "DNA Replication", "RNA", "Transcription", "Translation",
            "Genetic Code", "Protein Synthesis", "Gene Regulation", "Epigenetics", "Operons",
            "RNA Processing"),
        area(9, "Genetics", LessonLevel.SECONDARY,
            "Genes", "Mutation", "Mendelian Laws", "Monohybrid Cross", "Dihybrid Cross", "Test Cross", "Back Cross",
            "Incomplete Dominance", "Codominance", "Multiple Alleles", "Polygenic Inheritance",
            "Epistasis", "Linkage", "Crossing Over", "Sex Linkage", "Population Genetics",
            "Human Genetics", "Cytogenetics", "Quantitative Genetics"),
        area(10, "Biotechnology", LessonLevel.SENIOR_SECONDARY,
            "Recombinant DNA", "PCR", "Gel Electrophoresis", "DNA Sequencing", "Gene Cloning",
            "CRISPR", "GMOs", "Gene Therapy", "Stem Cell Technology", "Vaccines",
            "Synthetic Biology", "Bioinformatics"),
        area(11, "Evolution", LessonLevel.SECONDARY,
            "Origin of Life", "Fossils", "Lamarckism", "Darwinism", "Modern Synthesis",
            "Natural Selection", "Speciation", "Adaptive Radiation", "Human Evolution",
            "Molecular Evolution", "Evolutionary Developmental Biology"),
        area(12, "Diversity of Life (Taxonomy)", LessonLevel.FOUNDATION,
            "Classification", "Domains", "Kingdoms", "Archaea", "Bacteria", "Protists",
            "Fungi", "Plants", "Animals", "Taxonomic Hierarchy", "Binomial Nomenclature",
            "Phylogeny"),
        area(13, "Microbiology", LessonLevel.SECONDARY,
            "Bacteria", "Viruses", "Fungi", "Protozoa", "Algae", "Archaea",
            "Microbial Genetics", "Medical Microbiology", "Industrial Microbiology",
            "Food Microbiology", "Environmental Microbiology"),
        area(14, "Plant Biology", LessonLevel.FOUNDATION,
            "Plant Cell", "Plant Tissues", "Roots", "Stem", "Leaves", "Flowers", "Fruits",
            "Seeds", "Plant Anatomy", "Water Transport", "Mineral Nutrition", "Photosynthesis",
            "Plant Respiration", "Transpiration", "Plant Hormones", "Tropisms",
            "Photoperiodism", "Vernalization", "Plant Reproduction", "Plant Biotechnology"),
        area(15, "Animal Biology", LessonLevel.FOUNDATION,
            "Animal Tissues", "Body Plans", "Symmetry", "Germ Layers", "Organ Systems",
            "Animal Development", "Animal Behavior", "Comparative Anatomy"),
        area(16, "Human Anatomy", LessonLevel.FOUNDATION,
            "Anatomical Terms", "Body Planes", "Body Cavities", "Organs", "Organ Systems",
            "Histology"),
        area(17, "Human Physiology", LessonLevel.SECONDARY,
            "Integumentary System", "Skeletal System", "Muscular System", "Nervous System",
            "Endocrine System", "Cardiovascular System", "Lymphatic System", "Immune System",
            "Respiratory System", "Digestive System", "Urinary System", "Reproductive System",
            "Special Senses", "Homeostasis"),
        area(18, "Immunology", LessonLevel.SENIOR_SECONDARY,
            "Innate Immunity", "Adaptive Immunity", "Antibodies", "Vaccines",
            "Autoimmune Diseases", "Hypersensitivity", "Immunodeficiency",
            "Organ Transplantation", "Immunotherapy"),
        area(19, "Developmental Biology", LessonLevel.SENIOR_SECONDARY,
            "Gametogenesis", "Fertilization", "Cleavage", "Gastrulation", "Organogenesis",
            "Embryonic Development", "Placenta", "Stem Cells", "Regeneration"),
        area(20, "Histology", LessonLevel.SENIOR_SECONDARY,
            "Epithelial Tissue", "Connective Tissue", "Muscle Tissue", "Nervous Tissue",
            "Organ Histology"),
        area(21, "Ecology", LessonLevel.FOUNDATION,
            "Ecosystems", "Biomes", "Food Chains", "Food Webs", "Energy Flow",
            "Nutrient Cycles", "Population Ecology", "Community Ecology", "Succession",
            "Conservation Biology", "Climate Change", "Biodiversity"),
        area(22, "Environmental Biology", LessonLevel.FOUNDATION,
            "Pollution", "Waste Management", "Water Quality", "Air Quality", "Sustainability",
            "Renewable Resources", "Ecological Restoration"),
        area(23, "Ethology (Animal Behaviour)", LessonLevel.SENIOR_SECONDARY,
            "Instinct", "Learning", "Communication", "Social Behaviour", "Migration",
            "Navigation", "Foraging"),
        area(24, "Marine Biology", LessonLevel.UNDERGRADUATE,
            "Ocean Ecosystems", "Coral Reefs", "Marine Mammals", "Fisheries", "Plankton",
            "Deep Sea Biology"),
        area(25, "Agricultural Biology", LessonLevel.SENIOR_SECONDARY,
            "Crop Science", "Soil Biology", "Plant Breeding", "Pest Management", "Irrigation",
            "Organic Farming", "Precision Agriculture"),
        area(26, "Medical Biology", LessonLevel.SENIOR_SECONDARY,
            "Pathology", "Disease Mechanisms", "Diagnostics", "Epidemiology",
            "Pharmacology Basics", "Clinical Genetics"),
        area(27, "Parasitology", LessonLevel.SENIOR_SECONDARY,
            "Protozoan Parasites", "Helminths", "Arthropod Parasites", "Parasite Life Cycles",
            "Disease Control"),
        area(28, "Virology", LessonLevel.SENIOR_SECONDARY,
            "Virus Structure", "Viral Replication", "Viral Genetics", "Vaccines",
            "Emerging Viruses"),
        area(29, "Mycology", LessonLevel.SENIOR_SECONDARY,
            "Fungal Diversity", "Fungal Classification", "Pathogenic Fungi",
            "Industrial Fungi", "Lichens"),
        area(30, "Entomology", LessonLevel.SENIOR_SECONDARY,
            "Insect Anatomy", "Insect Physiology", "Insect Classification", "Insect Ecology",
            "Economic Importance of Insects"),
        area(31, "Zoology", LessonLevel.SENIOR_SECONDARY,
            "Porifera", "Cnidaria", "Platyhelminthes", "Nematoda", "Annelida", "Arthropoda",
            "Mollusca", "Echinodermata", "Fish", "Amphibians", "Reptiles", "Birds", "Mammals"),
        area(32, "Botany", LessonLevel.SENIOR_SECONDARY,
            "Bryophytes", "Pteridophytes", "Gymnosperms", "Angiosperms", "Plant Systematics",
            "Economic Botany", "Ethnobotany"),
        area(33, "Biochemistry", LessonLevel.UNDERGRADUATE,
            "Metabolism", "Carbohydrate Metabolism", "Lipid Metabolism", "Protein Metabolism",
            "Nucleotide Metabolism", "Hormonal Regulation"),
        area(34, "Biophysics", LessonLevel.UNDERGRADUATE,
            "Diffusion Physics", "Membrane Potential", "Fluid Dynamics", "Biomechanics",
            "Biological Imaging", "Electrophysiology"),
        area(35, "Systems Biology", LessonLevel.POSTGRADUATE,
            "Biological Networks", "Signal Transduction", "Gene Networks",
            "Metabolic Networks", "Computational Models"),
        area(36, "Bioinformatics", LessonLevel.UNDERGRADUATE,
            "Sequence Analysis", "Biological Databases", "Genome Assembly", "Protein Modeling",
            "AI in Biology", "Drug Discovery"),
        area(37, "Neuroscience", LessonLevel.UNDERGRADUATE,
            "Neurons", "Synapses", "Brain Anatomy", "Memory", "Learning", "Neurotransmitters",
            "Sensory Systems", "Motor Systems"),
        area(38, "Endocrinology", LessonLevel.UNDERGRADUATE,
            "Hormones", "Endocrine Glands", "Hormonal Disorders", "Feedback Mechanisms"),
        area(39, "Reproductive Biology", LessonLevel.SENIOR_SECONDARY,
            "Male Reproduction", "Female Reproduction", "Fertility", "Pregnancy", "Birth",
            "IVF", "Contraception"),
        area(40, "Genomics & Omics", LessonLevel.UNDERGRADUATE,
            "Genomics", "Transcriptomics", "Proteomics", "Metabolomics", "Metagenomics",
            "Single-cell Biology"),
        area(41, "Cancer Biology", LessonLevel.UNDERGRADUATE,
            "Cancer Cells", "Normal vs Cancer Cells", "Oncogenes", "Tumor Suppressors",
            "Cell Cycle", "Metastasis", "Cancer Diagnostics",
            "Precision Medicine"),
        area(42, "Public Health Biology", LessonLevel.SECONDARY,
            "Epidemiology", "Vaccination", "Nutrition", "Hygiene", "Disease Prevention",
            "Global Health"),
        area(43, "Laboratory Biology", LessonLevel.SECONDARY,
            "Microscopy", "Magnification", "Staining", "Onion Peel Experiment",
            "Cheek Cell Experiment", "Histology", "Cell Culture", "Spectrophotometry",
            "Chromatography", "ELISA", "Western Blot", "Flow Cytometry", "Centrifugation",
            "Experimental Design", "Biosafety"),
        area(44, "Advanced Biology (PG / Research)", LessonLevel.POSTGRADUATE,
            "Advanced Genetics", "Advanced Molecular Biology", "Structural Biology",
            "Evolutionary Genomics", "Systems Physiology", "Computational Biology",
            "Synthetic Biology", "Developmental Genetics", "Regenerative Medicine",
            "Nanobiotechnology", "Neurobiology", "Molecular Medicine", "Precision Medicine",
            "Drug Discovery", "Biomaterials", "Aging Biology", "Chronobiology", "Extremophiles",
            "Astrobiology"),
        area(45, "Grade 6 Biology", LessonLevel.FOUNDATION,
            "Diversity Around Us", "Herbs, Shrubs and Trees", "Leaf Venation and Root Types",
            "Monocot and Dicot Plants", "Grouping Animals", "Habitats and Adaptations",
            "Biodiversity and Conservation", "Food Diversity and Traditions", "Nutrients in Food",
            "Food Tests", "Balanced Diet", "Deficiency Diseases", "Millets and Healthy Grains",
            "Food Miles", "Living and Non-living Things", "Seed Germination",
            "Growth and Movement in Plants", "Plant Life Cycle", "Mosquito and Frog Life Cycles",
            "Natural Resources", "Air and Water as Resources", "Forests, Soil and Minerals",
            "Renewable and Non-renewable Resources", "Conserving Natural Resources"),
        area(46, "Grade 7 Biology", LessonLevel.FOUNDATION,
            "Adolescence and Puberty", "Physical Changes During Adolescence",
            "Menstruation", "Emotional Changes in Adolescence", "Healthy Adolescence",
            "Hormones During Puberty", "Avoiding Addictive Substances", "Life Processes",
            "Human Digestive System", "Digestion and Absorption", "Digestion in Other Animals",
            "Breathing and Respiration", "Human Respiratory System", "Alveoli and Gas Exchange",
            "Circulatory System", "Breathing in Other Animals", "Photosynthesis",
            "Leaves and Stomata", "Requirements for Photosynthesis", "Xylem Transport",
            "Phloem Transport", "Respiration in Plants")
    )

    val conceptCount: Int = areas.sumOf { it.concepts.size }

    fun lessons(area: LessonArea): List<Lesson> =
        area.concepts.mapIndexed { index, title -> lesson(area, title, index) }

    fun allLessons(): List<Lesson> = areas.flatMap(::lessons)

    private fun lesson(area: LessonArea, title: String, index: Int): Lesson {
        val related = buildList {
            area.concepts.getOrNull(index - 1)?.let(::add)
            area.concepts.getOrNull(index + 1)?.let(::add)
            area.concepts.firstOrNull { it != title && it !in this }?.let(::add)
        }.distinct().take(3)
        val phases =
            Grade67LessonContent.phases(area.number, title)
                ?: WorkbookCellLessonContent.phases(title)
                ?: authoredPhases[slug(title)]
                ?: generalPhases(area.title, title)
        val subtopic = LessonEditorial.subtopic(area, title, index)
        val tags = (
            listOf(area.title, area.startingLevel.label) +
                title.split(Regex("[^A-Za-z0-9]+")).filter { it.length > 2 }
            ).distinct().take(6)
        return Lesson(
            id = "${area.id}.${slug(title)}",
            area = area,
            title = title,
            subtopic = subtopic,
            subtopicCaption = LessonEditorial.subtopicCaption(subtopic),
            subtitle = LessonEditorial.lessonSubtitle(title, subtopic),
            phases = phases,
            learningContent =
                WorkbookCellLessonContent.learningContent(title, phases)
                    ?: learningContent(title, phases),
            relatedConcepts = related,
            tags = tags
        )
    }

    /**
     * Builds the complete saved lesson notes from the three progressively harder
     * explanations. Keeping one source of truth prevents the short lesson and the
     * detailed notes from contradicting each other.
     */
    private fun learningContent(
        concept: String,
        phases: List<LessonPhase>
    ): LessonLearningContent {
        val simple = phases[0]
        val connected = phases[1]
        val advanced = phases[2]
        return LessonLearningContent(
            detailedExplanation = listOf(
                simple.explanation,
                connected.explanation,
                advanced.explanation
            ).joinToString("\n\n"),
            easyWayToLearn = listOf(
                "SAY IT: Read the first explanation and describe $concept in your own simple words.",
                "SEE IT: Picture this example — ${simple.example.removePrefix("Example: ")}",
                "LINK IT: Connect $concept to this bigger idea — ${connected.explanation}",
                "TEST IT: Close the lesson and explain the idea to another person in one minute."
            ),
            realLifeExamples = phases.map { it.example.removePrefix("Example: ").trim() },
            importantPoints = listOf(
                "Definition and foundation: ${simple.explanation}",
                "Core biological link: ${connected.explanation}",
                "Advanced understanding: ${advanced.explanation}",
                "Example to remember: ${simple.example.removePrefix("Example: ").trim()}",
                "Evidence and accuracy: ${advanced.didYouKnow}"
            ),
            commonMistake =
                "Do not learn only the name “$concept”. Explain what it means, give an example, " +
                    "and connect the example back to the biological idea.",
            quickCheckQuestion =
                "In your own words, what is $concept? Give one example and explain why it is an example.",
            quickCheckAnswer = "${simple.explanation} ${simple.example}"
        )
    }

    private fun generalPhases(area: String, concept: String): List<LessonPhase> {
        val profile = LessonAreaProfiles.forArea(area)
        val focus = conceptFocus(area, concept)
        val investigation = investigationFor(area, concept)
        return listOf(
        LessonPhase(
            title = "1 · Start simple",
            explanation =
                "${profile.foundation} $focus " +
                    "Learn its defining features first, then connect those features with its biological role.",
            example = "Example: $investigation",
            didYouKnow =
                "A strong biological definition of $concept states what it is, where it occurs, " +
                    "and the feature that distinguishes it from a related idea."
        ),
        LessonPhase(
            title = "2 · Understand connections",
            explanation =
                "$concept should not be learned as an isolated name. ${profile.importance} " +
                    "Trace a cause-and-effect chain: identify what influences $concept, what it directly " +
                    "changes, and how that change affects a cell, organism, population, or environment.",
            example =
                "Example: ${profile.example} Use this case to identify the relevant part of $concept, " +
                    "the expected outcome, and one observation that would support the explanation.",
            didYouKnow =
                "In an exam, a linked explanation earns more credit than a list: write the cause, " +
                    "the biological mechanism, and the resulting effect in that order."
        ),
        LessonPhase(
            title = "3 · Explore deeper",
            explanation =
                "Advanced study of $concept uses ${profile.advanced}. Researchers compare evidence " +
                    "across levels of organization and ask whether a pattern shows correlation or a " +
                    "tested causal mechanism.",
            example =
                "Example: Compare a control with a changed condition for $concept, measure a relevant " +
                    "outcome, repeat the observations, and explain what the evidence supports and what " +
                    "it cannot establish.",
            didYouKnow =
                "Reliable conclusions about $concept depend on suitable controls, adequate samples, " +
                    "repeatable measurements, uncertainty, and plausible alternative explanations."
        )
        )
    }

    /**
     * Gives every catalogue concept a domain-specific study lens. This prevents
     * un-authored lessons from repeating one generic paragraph for an entire area.
     */
    private fun conceptFocus(area: String, concept: String): String = when (area) {
        "Introduction to Biology" ->
            "For $concept, separate observation from interpretation and ask which claim can be tested with measurable evidence."
        "Biomolecules", "Biochemistry" ->
            "For $concept, connect chemical building blocks and bonds with shape, energy change, molecular interactions, and cellular function."
        "Cell Biology" ->
            "For $concept, identify its cellular location and structure, explain its function, and predict what changes if that function is disrupted."
        "Cell Division" ->
            "For $concept, follow DNA and chromosome behaviour through the correct stage and distinguish its outcome in growth, meiosis, or disease."
        "Biological Membranes & Transport" ->
            "For $concept, state what moves, its direction, the driving gradient, whether a membrane protein is involved, and whether energy is required."
        "Enzymes" ->
            "For $concept, connect substrate binding and active-site shape with specificity, reaction rate, regulation, and experimental conditions."
        "Bioenergetics" ->
            "For $concept, track matter, electrons, proton gradients, and ATP carefully; cells transform energy rather than create it."
        "Molecular Biology" ->
            "For $concept, follow information accurately among DNA, RNA, and protein, including the enzymes, direction, and cellular location involved."
        "Genetics" ->
            "For $concept, distinguish genes, alleles, genotypes, and phenotypes, then use probability or population evidence where appropriate."
        "Biotechnology" ->
            "For $concept, explain the target, biological tool, result, controls, limitations, safety concerns, and ethical questions."
        "Evolution" ->
            "For $concept, identify heritable variation and explain population change across generations rather than change caused by individual need."
        "Diversity of Life (Taxonomy)", "Zoology", "Botany" ->
            "For $concept, compare defining characters and ancestry, and distinguish a classification label from evidence of evolutionary relationship."
        "Microbiology", "Mycology", "Virology" ->
            "For $concept, identify cellular organization or particle structure, reproduction, metabolism where present, ecological role, and host interaction."
        "Plant Biology" ->
            "For $concept, connect plant structure with transport, photosynthesis, growth, reproduction, or response to the environment."
        "Animal Biology" ->
            "For $concept, compare body organization, development, physiology, behaviour, and evolutionary relationships."
        "Human Anatomy" ->
            "For $concept, use standard anatomical position and terms to describe structure, location, boundaries, and nearby relationships."
        "Human Physiology" ->
            "For $concept, identify the regulated variable, receptors, control centre, effectors, and feedback that support homeostasis."
        "Immunology" ->
            "For $concept, identify the cells or molecules involved, what they recognize, how the response begins, and whether specificity or memory develops."
        "Developmental Biology" ->
            "For $concept, follow cell division, movement, signalling, gene expression, and differentiation across the correct time and place."
        "Histology" ->
            "For $concept, recognize cells, extracellular material, layers, and spatial arrangement, then connect microscopic structure with function."
        "Ecology", "Environmental Biology", "Marine Biology" ->
            "For $concept, state the biological level and scale, identify environmental drivers, and distinguish energy flow from the cycling of matter."
        "Ethology (Animal Behaviour)" ->
            "For $concept, distinguish immediate mechanism and development from survival value and evolutionary history."
        "Agricultural Biology" ->
            "For $concept, evaluate effects on production, soil and water, pests, genetic diversity, costs, and long-term sustainability."
        "Medical Biology" ->
            "For $concept, move from normal biology to disease mechanism, measurable evidence, diagnosis, prevention, and treatment."
        "Parasitology" ->
            "For $concept, identify the parasite stage, host or vector, route of transmission, site of infection, and point where control can interrupt the life cycle."
        "Entomology" ->
            "For $concept, connect insect structure and life cycle with physiology, behaviour, ecological role, economic effect, or vector control."
        "Biophysics" ->
            "For $concept, define the physical quantity and units, identify the relevant gradient, force, energy, or signal, and relate the model to measurements."
        "Systems Biology" ->
            "For $concept, identify network components, interactions, feedback, time-dependent behaviour, assumptions, and testable predictions."
        "Bioinformatics" ->
            "For $concept, define the question, input data, algorithm or database, output, validation, uncertainty, and reproducibility requirements."
        "Neuroscience" ->
            "For $concept, connect membrane signals, synapses, cells, circuits, brain regions, behaviour, and plasticity at the right level."
        "Endocrinology" ->
            "For $concept, identify the hormone source, stimulus, transport, receptor, target response, and negative or positive feedback."
        "Reproductive Biology" ->
            "For $concept, follow anatomy, gametes, hormones, timing, fertilization, or pregnancy accurately and use respectful health language."
        "Genomics & Omics" ->
            "For $concept, identify what is measured, sample preparation, normalization, pattern detection, uncertainty, and independent validation."
        "Cancer Biology" ->
            "For $concept, connect genetic or epigenetic change with altered cell behaviour, tissue invasion, tumour evolution, diagnosis, or treatment."
        "Public Health Biology" ->
            "For $concept, define the population and outcome, compare rates with correct denominators, and consider bias, confounding, access, and equity."
        "Laboratory Biology" ->
            "For $concept, explain the method's principle, preparation, controls, measured signal, interpretation, quality checks, limitations, and safety."
        "Advanced Biology (PG / Research)" ->
            "For $concept, integrate molecular mechanism with quantitative evidence, model assumptions, reproducibility, ethics, and possible translation."
        else ->
            "For $concept, connect an accurate definition with mechanism, evidence, application, and limitations."
    }

    private fun investigationFor(area: String, concept: String): String {
        val lower = concept.lowercase()
        return when {
            lower.contains("structure") || lower.contains("anatom") ||
                lower.contains("tissue") || lower.contains("organelle") ->
                "Examine a labelled image or specimen of $concept, identify its defining parts, and explain how one structural feature supports its function."
            lower.contains("cycle") || lower.contains("replication") ||
                lower.contains("division") || lower.contains("synthesis") ->
                "Arrange the stages of $concept in order, track important inputs and outputs, and predict the result if one stage is blocked."
            lower.contains("transport") || lower.contains("diffusion") ||
                lower.contains("osmosis") || lower.contains("flow") ->
                "Compare movement under two conditions, identify the driving gradient for $concept, and measure its direction or rate."
            lower.contains("disease") || lower.contains("patholog") ||
                lower.contains("cancer") || lower.contains("disorder") ->
                "Compare healthy and affected evidence for $concept, link the change with a mechanism, and state one limit of the comparison."
            lower.contains("classification") || lower.contains("phylogen") ||
                area in setOf("Diversity of Life (Taxonomy)", "Zoology", "Botany") ->
                "Compare representative organisms for $concept using homologous characters or sequence evidence, then justify the grouping."
            lower.contains("method") || lower.contains("analysis") ||
                lower.contains("sequencing") || lower.contains("microscopy") ||
                area in setOf("Laboratory Biology", "Bioinformatics") ->
                "Apply $concept to a known positive, a known negative, and an unknown sample; compare the outputs before interpreting the unknown."
            area in setOf("Ecology", "Environmental Biology", "Marine Biology") ->
                "Measure $concept at comparable sites or times, record relevant environmental variables, and avoid claiming causation from correlation alone."
            area in setOf("Genetics", "Molecular Biology", "Genomics & Omics") ->
                "Use a cross, family, sequence, or expression dataset to test a prediction about $concept against a control or null expectation."
            area in setOf("Human Physiology", "Endocrinology", "Neuroscience") ->
                "Measure a relevant body variable before and after a controlled change related to $concept, then explain the response through signalling or feedback."
            else ->
                "Compare a control with one carefully chosen change related to $concept, measure the outcome, and decide whether the evidence supports the prediction."
        }
    }

    private val authoredPhases = mapOf(
        "what-is-biology" to phases(
            "Biology is the study of life. It looks at living things, how their parts work, how they grow, and how they interact with their surroundings.",
            "A student watching a seed germinate is studying biology because the student observes how a living thing grows.",
            "The word biology comes from Greek words meaning “life” and “study”.",
            "Biology connects observations at many levels—from molecules and cells to organisms and ecosystems.",
            "A fever can be studied through molecules, immune cells, the whole body, and the spread of infection in a population.",
            "No single branch can answer every question about life, so biologists often work with chemists, physicists, doctors, and computer scientists.",
            "Modern biology tests explanations with measurable evidence, repeatable methods, and models that make predictions.",
            "Researchers may combine DNA sequences, microscopy, statistics, and field observations to answer one biological question.",
            "A biological conclusion is reliable only within the limits of its data and experimental design."
        ),
        "characteristics-of-living-organisms" to phases(
            "Living things are made of cells and carry out life processes such as using energy, growing, responding, and reproducing.",
            "A plant turns toward light, uses water and carbon dioxide, grows new leaves, and makes seeds.",
            "Some non-living things show one life-like feature, but living systems show an organized group of features.",
            "Life maintains internal order by exchanging matter and energy with the environment.",
            "A person sweats when hot. This response helps keep body temperature within a safe range.",
            "Viruses make the boundary between living and non-living difficult because they reproduce only inside host cells.",
            "Scientists describe life using cellular organization, metabolism, information, regulation, reproduction, and evolution.",
            "A bacterium senses nutrients, changes gene activity, uses energy, divides, and evolves across generations.",
            "There is no single perfect checklist for life, especially when studying viruses or possible life beyond Earth."
        ),
        "scientific-method" to phases(
            "The scientific method is a careful way to ask a question, make a testable idea, collect evidence, and decide what the evidence shows.",
            "To test whether light affects plant growth, keep the plant type and water the same but change the amount of light.",
            "A hypothesis is not a guess without reason; it is an explanation that can be tested.",
            "Good investigations change one main factor, measure an outcome, use controls, and repeat observations.",
            "Several identical plants in each light condition give more dependable results than one plant.",
            "Unexpected results are useful because they can reveal hidden variables or a weak explanation.",
            "Research combines experimental design, statistics, peer review, replication, and ethical reporting.",
            "A clinical trial may use random assignment and blinding to reduce bias when testing a treatment.",
            "Science does not prove every claim forever; it builds the best explanation supported by current evidence."
        ),
        "cell-theory" to phases(
            "Cell theory says all living things are made of cells, the cell is the basic unit of life, and new cells come from existing cells.",
            "Your skin heals because existing cells divide and produce new cells.",
            "A human body contains many specialized cell types, but every one follows the basic rules of cell life.",
            "Cell theory connects growth, reproduction, inheritance, and disease to events inside cells.",
            "A bacterial colony grows when individual bacterial cells divide again and again.",
            "Microscopes helped scientists discover cells and improve cell theory over time.",
            "Modern cell theory includes information flow, energy transformation, shared chemistry, and evolutionary continuity.",
            "Mitochondria and chloroplasts contain their own DNA, supporting the idea that they evolved from ancient cells.",
            "Cell theory is a powerful framework, while acellular agents such as viruses require separate explanation."
        ),
        "diffusion" to phases(
            "Diffusion is the net movement of particles from a region where they are more concentrated to a region where they are less concentrated.",
            "The smell of perfume spreads through a room as its molecules move and mix with air.",
            "Particles move randomly in every direction; diffusion describes the overall result of that movement.",
            "Diffusion becomes faster with a steeper concentration difference, higher temperature, shorter distance, or larger surface area.",
            "Oxygen diffuses from tiny air sacs in the lungs into nearby blood because its concentration is higher in the air sacs.",
            "Cells stay small partly because short diffusion distances help materials move quickly.",
            "Diffusion can be described with flux, gradients, membrane permeability, and Fick’s laws.",
            "Thickening of the lung diffusion barrier can reduce oxygen transfer even when breathing continues.",
            "Diffusion does not require cellular energy, but maintaining the gradient may require energy."
        ),
        "osmosis" to phases(
            "Osmosis is the net movement of water through a selectively permeable membrane toward the side with more dissolved particles.",
            "A dry raisin swells in water because water enters its cells.",
            "Osmosis is about water movement, not the movement of every dissolved substance.",
            "Water potential helps predict osmosis. Pressure and dissolved particles both affect the direction of water movement.",
            "Plant cells become firm in fresh water because water enters and presses the membrane against the cell wall.",
            "Medical fluids must have a suitable concentration so blood cells do not swell or shrink too much.",
            "Advanced osmosis is explained using chemical potential, osmotic pressure, tonicity, and aquaporin channels.",
            "Kidneys regulate body water partly by changing aquaporin abundance in collecting-duct cells.",
            "A solution can be hypertonic to a cell even when some solutes can cross the membrane."
        ),
        "mitosis" to phases(
            "Mitosis is cell division that produces two nuclei with the same chromosome information as the original nucleus.",
            "Mitosis makes new cells for growth and helps replace worn-out skin cells.",
            "DNA is copied before mitosis begins, during an earlier part of the cell cycle.",
            "Chromosomes condense, line up, separate, and form two nuclei before the cell usually divides its cytoplasm.",
            "Root tips contain many dividing cells, so they are useful for viewing stages of mitosis.",
            "Control checkpoints help prevent damaged DNA from being passed to new cells.",
            "Mitosis depends on spindle microtubules, kinetochores, motor proteins, checkpoints, and coordinated chromosome dynamics.",
            "Drugs that disturb microtubules can stop rapidly dividing cancer cells, but they may also affect healthy cells.",
            "The two daughter cells can later become different because gene activity and cell signals may change."
        ),
        "photosynthesis" to phases(
            "Photosynthesis uses light energy to make energy-rich food molecules from carbon dioxide and water. Oxygen is released.",
            "A green leaf in sunlight makes sugars that can support growth and later be used in respiration.",
            "Most of a plant’s dry mass comes from carbon dioxide in the air, not from soil.",
            "Light reactions capture energy and make ATP and NADPH. The Calvin cycle uses them to help build carbohydrates.",
            "A plant kept in darkness cannot continue making sugar even if it has enough water and carbon dioxide.",
            "Different pigments absorb different wavelengths, helping plants use more of the available light.",
            "Photosynthesis couples electron transport, proton gradients, carbon fixation, enzyme regulation, and environmental responses.",
            "C4 and CAM plants reduce photorespiration in hot or dry conditions using different spatial or time-based strategies.",
            "Improving photosynthesis in crops requires balancing carbon gain, water loss, nutrients, temperature, and plant development."
        ),
        "dna-structure" to phases(
            "DNA is a long molecule that stores biological instructions. It has two strands twisted into a double helix.",
            "DNA instructions help a cell build proteins such as enzymes and parts of muscles.",
            "If the DNA in one human cell were stretched out, it would be much longer than the cell.",
            "Each strand has a sugar-phosphate backbone. Bases pair specifically: A with T, and G with C.",
            "The sequence ATG on one strand pairs with TAC on the other strand.",
            "Complementary base pairing helps DNA copy and repair its information.",
            "DNA structure includes antiparallel strands, base stacking, grooves, supercoiling, chromatin organization, and sequence-dependent shape.",
            "Regulatory proteins can recognize both base sequences and the three-dimensional shape of DNA grooves.",
            "DNA is chemically stable for information storage, but controlled damage and repair still occur continually."
        ),
        "natural-selection" to phases(
            "Natural selection happens when inherited differences help some organisms survive and reproduce more successfully than others.",
            "In a cold environment, animals with inherited thicker fur may leave more offspring over many generations.",
            "Individuals do not evolve because they need to; populations change across generations.",
            "Selection needs variation, inheritance, differences in reproductive success, and many generations.",
            "Antibiotic treatment can leave resistant bacteria alive, allowing resistance to become more common.",
            "Natural selection can maintain several forms of a trait instead of always producing one perfect form.",
            "Selection changes allele frequencies and interacts with mutation, gene flow, genetic drift, recombination, and trade-offs.",
            "Researchers can estimate selection by comparing genotype frequencies with survival or reproductive success.",
            "Natural selection is not goal-directed, and adaptation is limited by history, variation, and environmental change."
        ),
        "ecosystems" to phases(
            "An ecosystem includes living organisms and the non-living environment interacting in one place.",
            "A pond ecosystem includes algae, insects, fish, microbes, water, light, minerals, and temperature.",
            "Decomposers return nutrients from dead material so those nutrients can be used again.",
            "Energy moves through food relationships, while matter such as carbon and nitrogen cycles between organisms and the environment.",
            "Removing a predator may change prey numbers, plant growth, and even water or soil conditions.",
            "The boundaries of an ecosystem depend on the question being studied.",
            "Ecosystem science studies networks, productivity, nutrient budgets, disturbance, resilience, feedback, and change across scales.",
            "Long-term data can reveal whether a forest recovers after fire or shifts into a different stable state.",
            "A diverse ecosystem may be resilient, but diversity alone does not guarantee stability under every disturbance."
        ),
        "vaccines" to phases(
            "A vaccine safely teaches the immune system to recognize a disease-causing organism or one of its parts.",
            "After vaccination, memory cells can respond faster if the real pathogen enters the body.",
            "Vaccines protect individuals and can also reduce spread through a community.",
            "Different vaccines use weakened organisms, inactive material, proteins, viral vectors, or genetic instructions.",
            "A booster dose reminds the immune system and can raise protection when immunity has decreased.",
            "Vaccines are tested for safety, immune response, and protection before and after approval.",
            "Vaccine science examines antigen design, adjuvants, delivery, correlates of protection, population effectiveness, and pathogen evolution.",
            "Genomic surveillance can help update a vaccine when important pathogen variants appear.",
            "No medical intervention has zero risk; decisions compare carefully measured risks with the harm prevented."
        )
    )

    private fun phases(
        simple: String,
        simpleExample: String,
        simpleFact: String,
        connected: String,
        connectedExample: String,
        connectedFact: String,
        advanced: String,
        advancedExample: String,
        advancedFact: String
    ) = listOf(
        LessonPhase("1 · Start simple", simple, simpleExample, simpleFact),
        LessonPhase("2 · Understand connections", connected, connectedExample, connectedFact),
        LessonPhase("3 · Explore deeper", advanced, advancedExample, advancedFact)
    )

    private fun area(
        number: Int,
        title: String,
        level: LessonLevel,
        vararg concepts: String
    ) = LessonArea(number, slug(title), title, level, concepts.toList())

    private fun slug(value: String): String =
        value.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
}
