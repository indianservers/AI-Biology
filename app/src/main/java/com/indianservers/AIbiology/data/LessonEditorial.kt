package com.indianservers.AIbiology.data

/**
 * Learner-facing editorial copy used across the Lessons experience.
 * Captions give each curriculum area a memorable identity while outcomes state
 * what the learner should be able to do, not merely what they will read.
 */
internal object LessonEditorial {
    private val areaCaptions = mapOf(
        1 to "Ask better questions. Discover what makes life alive.",
        2 to "Meet the molecules that build, fuel, and instruct every cell.",
        3 to "Journey inside life's smallest complete working units.",
        4 to "See how one cell becomes two—and how life continues.",
        5 to "Cross the living boundary where every molecule needs permission.",
        6 to "Explore nature's tiny catalysts and the reactions they unlock.",
        7 to "Follow energy from sunlight and food into every living action.",
        8 to "Read the molecular language that turns genes into life.",
        9 to "Trace traits, variation, and inheritance across generations.",
        10 to "Use life's own tools to measure, design, and heal.",
        11 to "Discover how populations change and new forms of life arise.",
        12 to "Organize life's diversity and uncover its shared ancestry.",
        13 to "Enter the unseen world that shapes health, food, and Earth.",
        14 to "Learn how plants build themselves from light, water, and time.",
        15 to "Compare the body plans, tissues, and behaviours of animals.",
        16 to "Map the human body from visible form to microscopic detail.",
        17 to "Watch the body's systems cooperate to keep life in balance.",
        18 to "Meet the cells and signals that remember, defend, and sometimes misfire.",
        19 to "Follow the remarkable journey from one cell to an organized body.",
        20 to "Read the microscopic patterns hidden inside every organ.",
        21 to "Connect organisms, energy, matter, and change across living landscapes.",
        22 to "Understand environmental damage—and how living systems can recover.",
        23 to "Decode why animals communicate, learn, migrate, and choose.",
        24 to "Dive from sunlit reefs to the deepest living ocean.",
        25 to "Grow food through smarter partnerships with soil, water, and life.",
        26 to "Connect biological mechanisms with diagnosis, prevention, and care.",
        27 to "Track the hidden life cycles that move between parasites and hosts.",
        28 to "Explore the compact biological agents that take over living cells.",
        29 to "Discover the recyclers, partners, producers, and pathogens of the fungal world.",
        30 to "Enter the six-legged world that pollinates, transforms, and adapts.",
        31 to "Travel across the animal kingdom, from sponges to mammals.",
        32 to "Explore plant diversity, evolution, usefulness, and cultural knowledge.",
        33 to "Follow the chemical pathways that power and rebuild living systems.",
        34 to "Use force, flow, energy, and electricity to explain life.",
        35 to "See biology as a living network, not a collection of isolated parts.",
        36 to "Turn biological data into patterns, predictions, and discoveries.",
        37 to "Explore the electrical and chemical networks behind mind and movement.",
        38 to "Follow the chemical messages that coordinate the whole body.",
        39 to "Understand reproduction from gametes to birth and fertility care.",
        40 to "Study life at scale—across genomes, cells, proteins, and metabolites.",
        41 to "Understand how cells escape control, evolve, invade, and respond to treatment.",
        42 to "Protect health at population scale through evidence and prevention.",
        43 to "Transform biological questions into safe, measurable evidence.",
        44 to "Work at biology's frontier, where disciplines combine and questions stay open.",
        45 to "Observe life around you, eat wisely, and care for nature.",
        46 to "Understand growing bodies and the life processes of animals and plants."
    )

    fun areaCaption(area: LessonArea): String =
        requireNotNull(areaCaptions[area.number]) { "Missing caption for area ${area.number}" }

    fun subtopic(area: LessonArea, concept: String, index: Int): String {
        val value = concept.lowercase()
        return when (area.number) {
            1 -> when {
                index <= 7 -> "Biology Basics"
                index <= 13 -> "Nature of Life"
                else -> "Measurement and Data"
            }
            2 -> when {
                value.contains("water") || value.contains("hydrogen") -> "Water and Life"
                value.contains(Regex("carbo|sacchar|glycogen|starch|cellulose|chitin")) -> "Carbohydrates"
                value.contains(Regex("lipid|fatty|triglycer|phospholipid|steroid|wax")) -> "Lipids"
                value.contains(Regex("protein|amino|peptide|fold|denatur")) -> "Proteins"
                value.contains(Regex("nucleic|dna|rna|atp|nucleotide")) -> "Nucleic Acids and ATP"
                else -> "Vitamins and Minerals"
            }
            3 -> when {
                index <= 2 -> "Cell Theory and Cell Types"
                value.contains(Regex("membrane|wall|mosaic|endocyt|exocyt")) -> "Cell Boundaries"
                value.contains(Regex("cytoplasm|nucleus|nucleolus|ribosome|mitochond|chloroplast|reticulum|golgi|lysosome|vacuole|peroxisome")) -> "Cell Organelles"
                value.contains(Regex("cytoskeleton|centrosome|cilia|flagella")) -> "Cell Shape and Movement"
                else -> "Cell Communication and Life Cycle"
            }
            7 -> if (index <= 6) "Cellular Respiration" else "Photosynthesis"
            9 -> when {
                index <= 4 -> "Mendelian Genetics"
                index <= 12 -> "Beyond Mendel"
                else -> "Population and Human Genetics"
            }
            14 -> when {
                index <= 8 -> "Plant Structure"
                index <= 17 -> "Plant Physiology"
                else -> "Plant Reproduction and Technology"
            }
            17 -> "Human Organ Systems"
            21 -> when {
                index <= 5 -> "Ecosystems and Energy"
                index <= 8 -> "Populations and Communities"
                else -> "Conservation and Global Change"
            }
            31 -> if (index <= 7) "Invertebrate Zoology" else "Vertebrate Zoology"
            43 -> when {
                index <= 2 -> "Imaging and Specimens"
                index <= 9 -> "Cell and Molecular Methods"
                else -> "Research Design and Safety"
            }
            44 -> when {
                index <= 7 -> "Molecular and Computational Frontiers"
                index <= 14 -> "Medical and Regenerative Frontiers"
                else -> "Life Across Time and Space"
            }
            45 -> when {
                index <= 6 -> "Diversity in the Living World"
                index <= 13 -> "Mindful Eating"
                index <= 18 -> "Living Creatures"
                else -> "Nature's Treasures"
            }
            46 -> when {
                index <= 6 -> "Adolescence"
                index <= 15 -> "Life Processes in Animals"
                else -> "Life Processes in Plants"
            }
            else -> when {
                value.contains(Regex("structure|anatom|tissue|cell|organ|gland")) ->
                    "Structure and Organization"
                value.contains(Regex("process|cycle|transport|replication|metabolism|physiology|function|regulation")) ->
                    "Processes and Function"
                value.contains(Regex("disease|pathogen|medical|clinical|therapy|diagnostic|immunity|vaccine")) ->
                    "Health and Disease"
                value.contains(Regex("ecology|environment|population|community|conservation|climate")) ->
                    "Environment and Interactions"
                value.contains(Regex("gene|genetic|genom|inherit|dna|rna")) ->
                    "Genes and Information"
                value.contains(Regex("method|analysis|model|data|research|design|safety")) ->
                    "Methods and Evidence"
                else -> "${area.title.substringBefore(" (")} Essentials"
            }
        }
    }

    fun subtopicCaption(subtopic: String): String = when {
        subtopic.contains("Diversity in the Living World") ->
            "Observe, group, compare, and protect the living world around you."
        subtopic.contains("Mindful Eating") ->
            "Learn what food contains and how everyday choices support health."
        subtopic.contains("Living Creatures") ->
            "Use observations and life cycles to understand what it means to be alive."
        subtopic.contains("Nature's Treasures") ->
            "Discover the resources life depends on and how to use them responsibly."
        subtopic == "Adolescence" ->
            "Understand growing up with accurate facts, health, and respect."
        subtopic.contains("Life Processes in Animals") ->
            "Follow food, air, blood, and energy through animal bodies."
        subtopic.contains("Life Processes in Plants") ->
            "See how plants make food, transport materials, and release energy."
        subtopic.contains("Basics") -> "Build the ideas that make every later lesson easier."
        subtopic.contains("Nature of Life") -> "Connect organization, balance, adaptation, and change."
        subtopic.contains("Measurement") || subtopic.contains("Data") ->
            "Turn observations into evidence you can trust."
        subtopic.contains("Structure") || subtopic.contains("Organization") ->
            "See how biological form creates biological function."
        subtopic.contains("Process") || subtopic.contains("Function") ->
            "Follow the steps, signals, and changes that keep life working."
        subtopic.contains("Energy") || subtopic.contains("Respiration") ||
            subtopic.contains("Photosynthesis") ->
            "Track where energy comes from, where it moves, and what it makes possible."
        subtopic.contains("Gene") || subtopic.contains("Mendel") ||
            subtopic.contains("Information") ->
            "Follow biological information from inheritance to expression."
        subtopic.contains("Health") || subtopic.contains("Disease") ->
            "Connect normal biology with prevention, diagnosis, and change."
        subtopic.contains("Ecology") || subtopic.contains("Environment") ||
            subtopic.contains("Conservation") ->
            "Explore relationships that connect organisms with a changing world."
        subtopic.contains("Method") || subtopic.contains("Evidence") ||
            subtopic.contains("Research") ->
            "Learn how careful methods turn questions into reliable knowledge."
        subtopic.contains("Frontier") ->
            "Combine advanced tools to investigate questions with no simple answer."
        else -> "Move from a clear foundation to deeper biological understanding."
    }

    fun lessonSubtitle(concept: String, subtopic: String): String =
        "$concept made clear through ${subtopic.lowercase()}, real examples, and deeper evidence."

    fun outcomes(lesson: Lesson): List<String> = listOf(
        "Explain ${lesson.title} clearly using accurate biological language.",
        "Use a real example to connect ${lesson.title} with ${lesson.subtopic.lowercase()}.",
        "Recognize the main mechanism, evidence, application, or limitation used to study ${lesson.title}."
    )
}
