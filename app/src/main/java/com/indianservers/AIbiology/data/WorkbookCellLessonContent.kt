package com.indianservers.AIbiology.data

/**
 * Content imported from the seven Biology Lessons workbooks.
 *
 * The import preserves source IDs and the supplied workbook wording so every
 * displayed topic can be traced back to its requested source row.
 */
internal data class WorkbookLessonSeed(
    val sourceId: Int,
    val sourceTopic: String,
    val appTitles: List<String>,
    val explanation: String,
    val keyPoints: List<String>,
    val realLifeExample: String,
    val verifiedFact: String,
    val quizQuestion: String,
    val quizAnswer: String,
    val sourceFile: String
)

internal object WorkbookCellLessonContent {
    private val records = listOf(
        WorkbookLessonSeed(
            sourceId = 1,
            sourceTopic = "What is a Cell?",
            appTitles = listOf("What is a Cell?"),
            explanation = "Every living thing, from a tiny bacterium to a giant tree, is made of cells. A cell is the smallest unit that can carry out all the activities needed for life. Some organisms have only one cell, while the human body contains trillions of them. Cells take in food, produce energy, grow, and reproduce. Different cells perform different jobs, such as carrying oxygen, sending nerve signals, or protecting the body. Understanding cells is the first step to understanding Biology.",
            keyPoints = listOf("Building blocks of life", "Smallest living unit", "Different cells have different jobs"),
            realLifeExample = "Your body is like a city, and each cell is a hardworking citizen.",
            verifiedFact = "Most cells are too small to see without a microscope.",
            quizQuestion = "What is the smallest living unit?",
            quizAnswer = "The cell.",
            sourceFile = "Biology_Workbook1_Part1_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 2,
            sourceTopic = "Discovery of the Cell",
            appTitles = listOf("Discovery of the Cell"),
            explanation = "In 1665, Robert Hooke looked at a thin slice of cork using a simple microscope. He saw tiny box-like compartments and named them 'cells'. Later, Antonie van Leeuwenhoek observed living cells for the first time. Their discoveries opened a new world that could not be seen with the naked eye. Today, powerful microscopes allow scientists to study cells in great detail and understand diseases, growth, and reproduction.",
            keyPoints = listOf("Robert Hooke", "Microscope", "Cork", "Living cells"),
            realLifeExample = "A magnifying glass helps us see small things; a microscope helps us see cells.",
            verifiedFact = "Modern electron microscopes can show structures thousands of times smaller than a human hair.",
            quizQuestion = "Who first used the word 'cell'?",
            quizAnswer = "Robert Hooke.",
            sourceFile = "Biology_Workbook1_Part1_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 3,
            sourceTopic = "Cell Theory",
            appTitles = listOf("Cell Theory"),
            explanation = "Scientists studied many living organisms and discovered that they all had something in common—they were made of cells. This led to Cell Theory, developed mainly by Schleiden and Schwann. Later, Rudolf Virchow added that every new cell comes from an existing cell. Cell Theory explains that the cell is the basic unit of life and forms the foundation of modern Biology.",
            keyPoints = listOf("All living things", "Basic unit", "New cells from existing cells"),
            realLifeExample = "A house is built from bricks just as living organisms are built from cells.",
            verifiedFact = "Cell Theory is one of the most important ideas in Biology.",
            quizQuestion = "Who said new cells come from existing cells?",
            quizAnswer = "Rudolf Virchow.",
            sourceFile = "Biology_Workbook1_Part1_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 4,
            sourceTopic = "Plant Cell",
            appTitles = listOf("Plant Cell"),
            explanation = "Plant cells have a strong cell wall that gives support and shape. They also contain chloroplasts, which use sunlight to make food through photosynthesis. A large vacuole stores water and helps keep the cell firm. Because plants cannot move to find food, these special structures help them survive and grow.",
            keyPoints = listOf("Cell wall", "Chloroplast", "Vacuole", "Photosynthesis"),
            realLifeExample = "A plant cell is like a solar-powered house with strong walls.",
            verifiedFact = "Only plant cells contain chloroplasts for making food.",
            quizQuestion = "Which organelle performs photosynthesis?",
            quizAnswer = "The chloroplast.",
            sourceFile = "Biology_Workbook1_Part1_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 5,
            sourceTopic = "Animal Cell",
            appTitles = listOf("Animal Cell"),
            explanation = "Animal cells are flexible because they do not have a cell wall. They contain a cell membrane, nucleus, mitochondria, ribosomes, and many other organelles. Animal cells work together to form tissues and organs such as muscles, skin, and the brain. Different animal cells have different shapes depending on their function.",
            keyPoints = listOf("Flexible", "No cell wall", "Organelles"),
            realLifeExample = "Muscle cells are long to help movement, while nerve cells are branched to carry messages.",
            verifiedFact = "Red blood cells lose their nucleus when they mature.",
            quizQuestion = "Do animal cells have a cell wall?",
            quizAnswer = "No. Animal cells do not have a cell wall.",
            sourceFile = "Biology_Workbook1_Part1_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 6,
            sourceTopic = "Cell Membrane",
            appTitles = listOf("Plasma Membrane"),
            explanation = "The cell membrane is a thin covering around the cell. It protects the cell and controls what enters and leaves. Useful materials such as oxygen and nutrients are allowed inside, while waste products move out. Because it selects what can pass through, it is called selectively permeable.",
            keyPoints = listOf("Protection", "Selectively permeable", "Transport"),
            realLifeExample = "A security gate checks who can enter and leave a building.",
            verifiedFact = "The membrane is made mainly of phospholipids and proteins.",
            quizQuestion = "What is selective permeability?",
            quizAnswer = "It allows some substances to cross more easily than others.",
            sourceFile = "Biology_Workbook1_Part1_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 7,
            sourceTopic = "Nucleus",
            appTitles = listOf("Nucleus"),
            explanation = "The nucleus is often called the control centre of the cell because it contains DNA and controls cell activities. It directs growth, repair, protein production, and cell division. The nucleus is surrounded by a nuclear membrane that protects the genetic material.",
            keyPoints = listOf("Control centre", "DNA", "Nuclear membrane"),
            realLifeExample = "A school principal coordinates all activities in a school.",
            verifiedFact = "Some cells, such as mature red blood cells, do not have a nucleus.",
            quizQuestion = "What important material is stored in the nucleus?",
            quizAnswer = "DNA.",
            sourceFile = "Biology_Workbook1_Part1_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 8,
            sourceTopic = "Mitochondria",
            appTitles = listOf("Mitochondria"),
            explanation = "Mitochondria release energy from food through cellular respiration. The energy is stored as ATP, which powers every activity inside the cell. Cells that need lots of energy, such as muscle cells, usually have many mitochondria. This is why mitochondria are called the powerhouse of the cell.",
            keyPoints = listOf("ATP", "Energy", "Respiration"),
            realLifeExample = "A power station supplies electricity to a city just as mitochondria supply energy to a cell.",
            verifiedFact = "Mitochondria have their own DNA.",
            quizQuestion = "Why are mitochondria called the powerhouse of the cell?",
            quizAnswer = "They transfer energy from nutrients into ATP during cellular respiration.",
            sourceFile = "Biology_Workbook1_Part1_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 9,
            sourceTopic = "Chloroplast",
            appTitles = listOf("Chloroplast"),
            explanation = "Chloroplasts are green organelles found in plant cells. They contain chlorophyll, which captures sunlight for photosynthesis. Using sunlight, water, and carbon dioxide, chloroplasts produce glucose and release oxygen. Without chloroplasts, plants could not make their own food.",
            keyPoints = listOf("Chlorophyll", "Photosynthesis", "Glucose"),
            realLifeExample = "Solar panels collect sunlight to produce energy; chloroplasts do something similar.",
            verifiedFact = "Most chloroplasts are found in leaf cells.",
            quizQuestion = "Which green pigment is present in chloroplasts?",
            quizAnswer = "Chlorophyll.",
            sourceFile = "Biology_Workbook1_Part1_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 10,
            sourceTopic = "DNA",
            appTitles = listOf("DNA"),
            explanation = "DNA is the molecule that carries genetic information from parents to their children. It contains instructions for building and maintaining every living organism. DNA determines characteristics such as eye colour, hair type, and many other inherited traits. Scientists also use DNA in medicine, forensic science, and biotechnology.",
            keyPoints = listOf("Genes", "Inheritance", "Double helix"),
            realLifeExample = "DNA is like an instruction book that tells cells what to do.",
            verifiedFact = "Almost every cell in your body contains the same DNA.",
            quizQuestion = "What does DNA stand for?",
            quizAnswer = "Deoxyribonucleic acid.",
            sourceFile = "Biology_Workbook1_Part1_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 11,
            sourceTopic = "RNA",
            appTitles = listOf("RNA"),
            explanation = "DNA stores genetic information, but RNA helps use that information to make proteins. RNA carries messages from the DNA in the nucleus to ribosomes, where proteins are produced. Unlike DNA, RNA usually has a single strand and contains the sugar ribose. It also uses the base uracil instead of thymine. Different types of RNA perform different jobs, including carrying genetic messages, bringing amino acids, and forming part of ribosomes. Without RNA, cells could not produce the proteins needed for growth, repair, enzymes, and hormones.",
            keyPoints = listOf("Single-stranded", "Protein synthesis", "Uracil"),
            realLifeExample = "A recipe book stays in the kitchen, while a written recipe is carried to the cook. RNA is like that written recipe.",
            verifiedFact = "Some viruses, including coronavirus, use RNA as their genetic material.",
            quizQuestion = "Which base is present in RNA instead of thymine?",
            quizAnswer = "Uracil.",
            sourceFile = "Biology_Workbook1_Part2_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 12,
            sourceTopic = "Ribosomes",
            appTitles = listOf("Ribosomes"),
            explanation = "Ribosomes are tiny structures that manufacture proteins for the cell. They read instructions carried by RNA and join amino acids together in the correct order. Some ribosomes float freely in the cytoplasm, while others are attached to the rough endoplasmic reticulum. Every living cell needs proteins to grow, repair damaged parts, and carry out chemical reactions. This is why ribosomes are often called the protein factories of the cell.",
            keyPoints = listOf("Protein factory", "Amino acids", "Translation"),
            realLifeExample = "A factory assembles car parts into a complete car, just as ribosomes assemble amino acids into proteins.",
            verifiedFact = "Ribosomes are found in both prokaryotic and eukaryotic cells.",
            quizQuestion = "What is the main function of ribosomes?",
            quizAnswer = "They build proteins by joining amino acids in the order specified by mRNA.",
            sourceFile = "Biology_Workbook1_Part2_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 13,
            sourceTopic = "Endoplasmic Reticulum",
            appTitles = listOf("Endoplasmic Reticulum"),
            explanation = "The endoplasmic reticulum (ER) is a network of folded membranes inside the cell. Rough ER has ribosomes attached to it and helps make proteins, while smooth ER makes lipids and helps remove harmful substances. The ER also transports materials from one part of the cell to another. It acts like an internal transport system, making sure important molecules reach the correct destination.",
            keyPoints = listOf("Rough ER", "Smooth ER", "Transport"),
            realLifeExample = "It works like roads and conveyor belts inside a large factory.",
            verifiedFact = "Muscle cells have a special type of smooth ER that stores calcium.",
            quizQuestion = "What is the difference between rough ER and smooth ER?",
            quizAnswer = "Rough ER has ribosomes and helps make membrane or secreted proteins; smooth ER lacks ribosomes and makes lipids, stores calcium, and helps detoxification.",
            sourceFile = "Biology_Workbook1_Part2_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 14,
            sourceTopic = "Golgi Apparatus",
            appTitles = listOf("Golgi Apparatus"),
            explanation = "After proteins and lipids are produced, they move to the Golgi apparatus. Here they are modified, sorted, packed, and sent to different parts of the cell or outside it. The Golgi apparatus works like a packaging and delivery centre. It also produces some lysosomes that help break down waste materials.",
            keyPoints = listOf("Packaging", "Sorting", "Secretion"),
            realLifeExample = "An online shopping warehouse packs products before delivery.",
            verifiedFact = "The Golgi apparatus was discovered by Camillo Golgi.",
            quizQuestion = "What is the main job of the Golgi apparatus?",
            quizAnswer = "It modifies, sorts, and packages proteins and lipids.",
            sourceFile = "Biology_Workbook1_Part2_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 15,
            sourceTopic = "Lysosomes",
            appTitles = listOf("Lysosomes"),
            explanation = "Lysosomes are small sacs filled with digestive enzymes. They break down old cell parts, harmful bacteria, and waste materials so the cell stays clean and healthy. If a cell becomes badly damaged, lysosomes can even help destroy it in a controlled way. Because they digest waste, lysosomes are sometimes called the cleaning crew of the cell.",
            keyPoints = listOf("Digestive enzymes", "Waste removal", "Recycling"),
            realLifeExample = "They are like garbage recycling centres in a city.",
            verifiedFact = "Lysosomes are especially active in white blood cells that destroy bacteria.",
            quizQuestion = "Why are lysosomes called the cleaning crew of the cell?",
            quizAnswer = "They digest and recycle selected cell materials using hydrolytic enzymes.",
            sourceFile = "Biology_Workbook1_Part2_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 16,
            sourceTopic = "Vacuoles",
            appTitles = listOf("Vacuoles"),
            explanation = "Vacuoles are storage sacs inside cells. They hold water, food, minerals, pigments, and waste products. Plant cells usually have one large central vacuole that stores water and keeps the cell firm. Animal cells have smaller vacuoles. By storing water, vacuoles help plants remain upright instead of becoming limp.",
            keyPoints = listOf("Storage", "Water balance", "Plant cells"),
            realLifeExample = "A water tank stores water for a building just as a vacuole stores materials for a cell.",
            verifiedFact = "The large vacuole can occupy most of a plant cell.",
            quizQuestion = "What is the main function of a vacuole?",
            quizAnswer = "Storage; in plants the central vacuole also helps maintain turgor pressure.",
            sourceFile = "Biology_Workbook1_Part2_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 17,
            sourceTopic = "Prokaryotic Cells",
            appTitles = listOf("Prokaryotic Cells"),
            explanation = "Prokaryotic cells are simple cells that do not have a true nucleus or membrane-bound organelles. Bacteria are the best-known examples. Their DNA floats freely inside the cell. Even though they are small and simple, prokaryotes perform all the activities needed for life. They reproduce quickly and can survive in many different environments.",
            keyPoints = listOf("No nucleus", "Bacteria", "Simple cells"),
            realLifeExample = "A one-room house has fewer sections than a large apartment.",
            verifiedFact = "Prokaryotes were the first living cells on Earth.",
            quizQuestion = "Give one example of a prokaryotic organism.",
            quizAnswer = "A bacterium.",
            sourceFile = "Biology_Workbook1_Part2_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 18,
            sourceTopic = "Eukaryotic Cells",
            appTitles = listOf("Eukaryotic Cells"),
            explanation = "Eukaryotic cells are larger and more complex than prokaryotic cells. They contain a true nucleus and many specialised organelles such as mitochondria, Golgi apparatus, and endoplasmic reticulum. Plants, animals, fungi, and protists are all made of eukaryotic cells. These organelles allow the cell to perform many different tasks efficiently.",
            keyPoints = listOf("True nucleus", "Organelles", "Complex"),
            realLifeExample = "A modern factory has many departments working together efficiently.",
            verifiedFact = "Human cells are eukaryotic cells.",
            quizQuestion = "What is the main difference between prokaryotic and eukaryotic cells?",
            quizAnswer = "Eukaryotic cells enclose DNA in a nucleus and contain membrane-bound organelles; prokaryotic cells do not.",
            sourceFile = "Biology_Workbook1_Part2_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 19,
            sourceTopic = "Diffusion",
            appTitles = listOf("Diffusion"),
            explanation = "Diffusion is the movement of particles from an area of high concentration to an area of low concentration until they spread evenly. This happens naturally because particles are always moving. Diffusion allows oxygen to enter our blood from the lungs and carbon dioxide to leave the body. It also helps plants exchange gases through tiny openings called stomata.",
            keyPoints = listOf("High to low concentration", "Passive movement"),
            realLifeExample = "The smell of perfume spreading across a room is diffusion.",
            verifiedFact = "Diffusion happens without using cellular energy.",
            quizQuestion = "In which direction do particles move during diffusion?",
            quizAnswer = "Down a concentration gradient, from higher to lower concentration.",
            sourceFile = "Biology_Workbook1_Part2_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 20,
            sourceTopic = "Osmosis",
            appTitles = listOf("Osmosis"),
            explanation = "Osmosis is the movement of water molecules through a selectively permeable membrane from a region with more water to a region with less water. It helps plant roots absorb water and keeps plant cells firm. If plants lose too much water, they wilt because their cells lose pressure. Osmosis is essential for maintaining water balance in all living organisms.",
            keyPoints = listOf("Water movement", "Selectively permeable membrane", "Plant cells"),
            realLifeExample = "Dry raisins swell when soaked in water because of osmosis.",
            verifiedFact = "Osmosis is a special type of diffusion involving only water.",
            quizQuestion = "What moves during osmosis?",
            quizAnswer = "Water molecules.",
            sourceFile = "Biology_Workbook1_Part2_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 21,
            sourceTopic = "Active Transport",
            appTitles = listOf("Active Transport"),
            explanation = "Imagine climbing upstairs while carrying a heavy bag. Your body uses energy because you are moving against gravity. Cells do something similar through active transport. Instead of allowing substances to move naturally from a high concentration to a low concentration, active transport pushes them in the opposite direction using energy from ATP. This process helps root cells absorb minerals from the soil even when only a small amount is available. It also allows human cells to maintain the correct balance of sodium and potassium, which is essential for nerve impulses and muscle contraction. Without active transport, many important nutrients would never reach the places where they are needed.",
            keyPoints = listOf("Uses ATP", "Low to high concentration", "Membrane proteins"),
            realLifeExample = "Plants absorb mineral salts from poor soil.",
            verifiedFact = "The sodium-potassium pump is a famous example of active transport.",
            quizQuestion = "Does active transport require energy?",
            quizAnswer = "Yes, directly or indirectly.",
            sourceFile = "Biology_Workbook1_Part3_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 22,
            sourceTopic = "Endocytosis",
            appTitles = listOf("Endocytosis"),
            explanation = "Some substances are too large to pass through the cell membrane. Instead, the membrane folds inward and surrounds the material, forming a small bubble called a vesicle. This process is called endocytosis. White blood cells use endocytosis to surround and destroy harmful bacteria. Single-celled organisms such as Amoeba also use it to capture food. Endocytosis allows cells to take in large particles safely without damaging the membrane.",
            keyPoints = listOf("Cell engulfs materials", "Vesicles", "Large particles"),
            realLifeExample = "An Amoeba surrounds food before eating it.",
            verifiedFact = "Your immune cells use endocytosis every day.",
            quizQuestion = "Which blood cells use endocytosis to destroy bacteria?",
            quizAnswer = "White blood cells such as neutrophils and macrophages.",
            sourceFile = "Biology_Workbook1_Part3_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 23,
            sourceTopic = "Exocytosis",
            appTitles = listOf("Exocytosis"),
            explanation = "Cells not only take materials in, they also send materials out. During exocytosis, a vesicle carrying proteins, hormones or waste moves to the cell membrane and releases its contents outside the cell. This process is important for communication between cells. For example, nerve cells release chemical messengers, while glands release hormones into the bloodstream. Exocytosis also removes waste that is no longer needed.",
            keyPoints = listOf("Release of materials", "Vesicles", "Cell communication"),
            realLifeExample = "Delivery trucks unload goods at different shops.",
            verifiedFact = "Insulin is released from pancreatic cells by exocytosis.",
            quizQuestion = "What happens during exocytosis?",
            quizAnswer = "A vesicle fuses with the plasma membrane and releases its contents outside the cell.",
            sourceFile = "Biology_Workbook1_Part3_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 24,
            sourceTopic = "Cell Cycle",
            appTitles = listOf("Cell Cycle"),
            explanation = "A cell follows an organised sequence of events called the cell cycle. During this cycle, the cell grows, copies its DNA and finally divides to produce new cells. This process allows the body to grow, repair injuries and replace old cells. If the cell cycle is not properly controlled, cells may divide too quickly, leading to diseases such as cancer. Scientists study the cell cycle to understand growth and develop better treatments.",
            keyPoints = listOf("Growth", "DNA replication", "Division"),
            realLifeExample = "A school timetable ensures every activity happens at the correct time.",
            verifiedFact = "Some human cells rarely divide after birth, such as most nerve cells.",
            quizQuestion = "What are the three main stages of the cell cycle?",
            quizAnswer = "Interphase, mitosis, and cytokinesis; interphase includes G1, S, and G2.",
            sourceFile = "Biology_Workbook1_Part3_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 25,
            sourceTopic = "Cell Differentiation",
            appTitles = listOf("Cell Differentiation"),
            explanation = "All cells in a developing baby start out very similar. As development continues, they become specialised through a process called cell differentiation. Some become muscle cells, others become nerve cells, blood cells or skin cells. Although they contain the same DNA, different genes are switched on in different cells, giving each one a unique function. Differentiation makes complex life possible.",
            keyPoints = listOf("Specialisation", "Different functions", "Same DNA"),
            realLifeExample = "Students in a school choose different careers later in life.",
            verifiedFact = "More than 200 specialised cell types exist in the human body.",
            quizQuestion = "What is cell differentiation?",
            quizAnswer = "The process by which cells become specialised in structure and function.",
            sourceFile = "Biology_Workbook1_Part3_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 26,
            sourceTopic = "Stem Cells",
            appTitles = listOf("Stem Cells"),
            explanation = "Stem cells are special because they can divide many times and develop into different types of cells. They help repair damaged tissues and play an important role during growth. Doctors use certain stem cells to treat diseases such as some blood cancers. Scientists continue to study stem cells because they may help regenerate damaged organs in the future. Their unique ability makes them one of the most exciting areas of modern biology.",
            keyPoints = listOf("Unspecialised", "Repair", "Regeneration"),
            realLifeExample = "Stem cells are like blank building blocks that can become different structures.",
            verifiedFact = "Bone marrow contains important adult stem cells.",
            quizQuestion = "Why are stem cells medically important?",
            quizAnswer = "They can self-renew and produce specialised cell types, enabling tissue renewal and some treatments.",
            sourceFile = "Biology_Workbook1_Part3_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 27,
            sourceTopic = "Apoptosis",
            appTitles = listOf("Cell Death"),
            explanation = "Not all cells are meant to live forever. Apoptosis is a natural process in which old, damaged or unnecessary cells destroy themselves in a controlled way. This prevents unhealthy cells from causing problems and helps shape the body during development. For example, before birth, apoptosis removes the tissue between developing fingers so that separate fingers are formed.",
            keyPoints = listOf("Programmed cell death", "Healthy development"),
            realLifeExample = "Old leaves fall from a tree to make room for new growth.",
            verifiedFact = "Failure of apoptosis can contribute to cancer.",
            quizQuestion = "What is apoptosis also called?",
            quizAnswer = "Programmed cell death.",
            sourceFile = "Biology_Workbook1_Part3_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 28,
            sourceTopic = "Cilia and Flagella",
            appTitles = listOf("Cilia", "Flagella"),
            explanation = "Some cells can move using tiny structures called cilia and flagella. Cilia are short and numerous, while flagella are long and usually fewer in number. In humans, cilia sweep dust and mucus out of the airways, helping keep the lungs clean. A sperm cell uses a flagellum to swim towards the egg. These structures are important for movement and transport.",
            keyPoints = listOf("Movement", "Cilia", "Flagella"),
            realLifeExample = "A boat moves using a propeller, similar to a flagellum.",
            verifiedFact = "Cilia beat together in a coordinated rhythm.",
            quizQuestion = "What is the difference between cilia and flagella?",
            quizAnswer = "Cilia are usually short and numerous; flagella are usually longer and fewer, although both have related internal structures in eukaryotes.",
            sourceFile = "Biology_Workbook1_Part3_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 29,
            sourceTopic = "Centrioles",
            appTitles = listOf("Centrosome"),
            explanation = "Centrioles help organise the spindle fibres that separate chromosomes during cell division. They are found mainly in animal cells and ensure that each new cell receives the correct number of chromosomes. Although tiny, centrioles play an important role in healthy growth and reproduction. Errors during this process can lead to abnormal cells.",
            keyPoints = listOf("Cell division", "Spindle fibres", "Animal cells"),
            realLifeExample = "A traffic controller directs vehicles safely at a busy junction.",
            verifiedFact = "Most higher plant cells do not have centrioles.",
            quizQuestion = "What do centrioles organise during cell division?",
            quizAnswer = "Microtubules of the mitotic spindle, through the centrosome in many animal cells.",
            sourceFile = "Biology_Workbook1_Part3_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 30,
            sourceTopic = "Cell Junctions",
            appTitles = listOf("Cell Junctions"),
            explanation = "In multicellular organisms, neighbouring cells are connected by specialised structures called cell junctions. These junctions hold cells together, prevent leakage and allow communication between cells. Skin cells stay tightly connected to protect the body, while heart muscle cells communicate rapidly so the heart beats in a coordinated way. Cell junctions are essential for healthy tissues and organs.",
            keyPoints = listOf("Connection", "Communication", "Tissues"),
            realLifeExample = "Bridges connect different parts of a city.",
            verifiedFact = "Different tissues have different types of cell junctions.",
            quizQuestion = "Why are cell junctions important?",
            quizAnswer = "They attach cells, seal spaces, anchor tissues, or permit communication.",
            sourceFile = "Biology_Workbook1_Part3_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 31,
            sourceTopic = "Cell Size",
            appTitles = listOf("Cell Size"),
            explanation = "Although cells are microscopic, they are not all the same size. Most animal and plant cells measure between 10 and 100 micrometres, while some nerve cells can be more than a metre long. Cell size depends on its function. Smaller cells exchange materials more efficiently because they have a larger surface area compared to their volume. This allows oxygen, nutrients and waste to move quickly. If a cell becomes too large, it cannot obtain enough materials fast enough, so it usually divides.",
            keyPoints = listOf("Microscopic", "Surface area", "Volume"),
            realLifeExample = "Red blood cells are small so oxygen can move quickly.",
            verifiedFact = "The largest single cell is the ostrich egg.",
            quizQuestion = "Why do cells remain small?",
            quizAnswer = "Small cells generally have a larger surface-area-to-volume ratio and shorter internal transport distances.",
            sourceFile = "Biology_Workbook1_Part4_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 32,
            sourceTopic = "Cell Shape",
            appTitles = listOf("Cell Shape"),
            explanation = "Cells have different shapes because each shape suits a particular job. Red blood cells are disc-shaped to carry oxygen, nerve cells are long to transmit signals, and muscle cells are elongated to contract. Plant cells are usually rectangular because of their rigid cell wall. A cell's shape improves its efficiency.",
            keyPoints = listOf("Different jobs", "Adaptation", "Structure"),
            realLifeExample = "A spoon and a fork have different shapes for different uses.",
            verifiedFact = "More than 200 specialised cell types exist in humans.",
            quizQuestion = "Why do nerve cells have long extensions?",
            quizAnswer = "Their long processes allow signals to travel and connect with distant cells.",
            sourceFile = "Biology_Workbook1_Part4_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 33,
            sourceTopic = "Surface Area to Volume Ratio",
            appTitles = listOf("Surface Area to Volume Ratio"),
            explanation = "As a cell grows, its volume increases faster than its surface area. This makes it harder for materials to enter and leave. Cells solve this problem by remaining small, dividing, or developing folds that increase surface area. This principle explains why villi in the intestine and root hairs in plants are very thin and numerous.",
            keyPoints = listOf("Exchange efficiency", "Cell growth"),
            realLifeExample = "A thin sponge absorbs water faster than a solid block.",
            verifiedFact = "Root hair cells greatly increase absorption.",
            quizQuestion = "Why is surface area important?",
            quizAnswer = "Surface area divided by volume.",
            sourceFile = "Biology_Workbook1_Part4_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 34,
            sourceTopic = "Cell Transport",
            appTitles = listOf("Cell Transport"),
            explanation = "Cells constantly move substances in and out through the cell membrane. Transport may occur without energy through diffusion and osmosis or with energy through active transport. This movement supplies nutrients, removes waste and keeps the internal environment stable. Without transport, cells could not survive.",
            keyPoints = listOf("Diffusion", "Osmosis", "Active transport"),
            realLifeExample = "A railway station moves people in and out efficiently.",
            verifiedFact = "Transport maintains homeostasis.",
            quizQuestion = "Name three methods of cell transport.",
            quizAnswer = "It supplies nutrients, removes wastes, maintains gradients, and supports cell signalling and homeostasis.",
            sourceFile = "Biology_Workbook1_Part4_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 35,
            sourceTopic = "Homeostasis",
            appTitles = listOf("Homeostasis"),
            explanation = "Living cells work best under stable internal conditions. Homeostasis is the process of maintaining a constant internal environment despite changes outside. Cells regulate temperature, water, salts and pH to keep enzymes working properly. Humans sweat when hot and shiver when cold as part of homeostasis.",
            keyPoints = listOf("Stable conditions", "Balance"),
            realLifeExample = "An air conditioner keeps a room comfortable.",
            verifiedFact = "Homeostasis operates every second.",
            quizQuestion = "What is homeostasis?",
            quizAnswer = "Maintenance of relatively stable internal conditions despite external or internal change.",
            sourceFile = "Biology_Workbook1_Part4_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 36,
            sourceTopic = "Cell Respiration",
            appTitles = listOf("Cellular Respiration"),
            explanation = "Cells need energy every moment. During cellular respiration, glucose is broken down to release energy stored as ATP. Most respiration takes place in mitochondria using oxygen. This energy powers movement, growth, repair and active transport. Plants and animals both carry out cellular respiration.",
            keyPoints = listOf("ATP", "Glucose", "Oxygen"),
            realLifeExample = "Charging a phone battery provides power for later use.",
            verifiedFact = "Respiration occurs day and night.",
            quizQuestion = "Where does most respiration occur?",
            quizAnswer = "Mainly ATP, produced by transferring energy from nutrients through metabolic reactions.",
            sourceFile = "Biology_Workbook1_Part4_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 37,
            sourceTopic = "ATP",
            appTitles = listOf("ATP"),
            explanation = "ATP, or adenosine triphosphate, is called the energy currency of the cell. Whenever a cell needs energy, ATP releases it by breaking one phosphate bond. This quick energy supply powers muscle movement, nerve signals, protein production and many other activities. Cells continuously make and use ATP.",
            keyPoints = listOf("Energy currency", "Phosphate"),
            realLifeExample = "A rechargeable battery stores energy.",
            verifiedFact = "One cell uses millions of ATP molecules every second.",
            quizQuestion = "Why is ATP called the energy currency?",
            quizAnswer = "It is the cell's immediate, transferable energy-coupling molecule.",
            sourceFile = "Biology_Workbook1_Part4_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 38,
            sourceTopic = "Enzymes",
            appTitles = listOf("Enzymes"),
            explanation = "Enzymes are proteins that speed up chemical reactions without being used up. They help digest food, copy DNA and produce energy. Each enzyme works only with a specific substance called its substrate. Temperature and pH affect enzyme activity, which is why extremely high fever can be dangerous.",
            keyPoints = listOf("Catalyst", "Substrate", "Protein"),
            realLifeExample = "Scissors cut paper faster than tearing by hand.",
            verifiedFact = "Saliva contains digestive enzymes.",
            quizQuestion = "What is the function of enzymes?",
            quizAnswer = "They lower activation energy and speed reactions without being consumed overall.",
            sourceFile = "Biology_Workbook1_Part4_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 39,
            sourceTopic = "Biomolecules",
            appTitles = listOf("Biomolecules"),
            explanation = "Living organisms are made from four major biomolecules: carbohydrates, proteins, lipids and nucleic acids. Each has a special role. Carbohydrates provide energy, proteins build and repair tissues, lipids store energy and form membranes, while nucleic acids carry genetic information. Together they support every life process.",
            keyPoints = listOf("Carbohydrates", "Proteins", "Lipids", "Nucleic acids"),
            realLifeExample = "A balanced meal contains several biomolecules.",
            verifiedFact = "Water is essential but not a biomolecule.",
            quizQuestion = "Name the four major biomolecules.",
            quizAnswer = "Carbohydrates, lipids, proteins, and nucleic acids.",
            sourceFile = "Biology_Workbook1_Part4_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 40,
            sourceTopic = "Cell as a Factory",
            appTitles = listOf("Cell as a Factory"),
            explanation = "A useful way to understand cells is to compare them with a factory. The nucleus is the manager, ribosomes are workers, the endoplasmic reticulum transports materials, the Golgi apparatus packs products, mitochondria provide power and lysosomes handle waste. This comparison helps students remember the function of each organelle while understanding how they work together.",
            keyPoints = listOf("Analogy", "Organelles"),
            realLifeExample = "A factory has different departments with different jobs.",
            verifiedFact = "No single organelle can keep the cell alive alone.",
            quizQuestion = "Which organelle acts like the factory manager?",
            quizAnswer = "The nucleus in the simplified factory analogy, although cell control is distributed and no organelle works alone.",
            sourceFile = "Biology_Workbook1_Part4_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 41,
            sourceTopic = "Microscope",
            appTitles = listOf("Microscopy"),
            explanation = "Many cell structures are too small to be seen with our eyes. A microscope magnifies tiny objects so scientists and students can study cells in detail. A simple microscope has one lens, while a compound microscope uses two or more lenses to provide higher magnification. Modern electron microscopes can reveal organelles that are impossible to see with ordinary microscopes. Learning to use a microscope correctly is an important laboratory skill in biology.",
            keyPoints = listOf("Magnification", "Compound microscope", "Observation"),
            realLifeExample = "A magnifying glass enlarges a small insect, while a microscope enlarges a cell.",
            verifiedFact = "Electron microscopes can magnify objects millions of times.",
            quizQuestion = "Why do we use a microscope?",
            quizAnswer = "To resolve and study structures too small for the unaided eye.",
            sourceFile = "Biology_Workbook1_Part5_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 42,
            sourceTopic = "Magnification",
            appTitles = listOf("Magnification"),
            explanation = "Magnification tells us how many times bigger an image appears compared to the real object. Scientists use magnification to observe tiny structures such as cells and bacteria. If a cell is magnified 400×, it appears 400 times larger than its actual size. Understanding magnification helps students interpret microscope images correctly.",
            keyPoints = listOf("Image size", "Actual size", "Scale"),
            realLifeExample = "Zooming in on a mobile phone camera makes objects appear larger.",
            verifiedFact = "Higher magnification usually shows a smaller field of view.",
            quizQuestion = "What does 400× magnification mean?",
            quizAnswer = "The image appears 400 times the object's actual linear size.",
            sourceFile = "Biology_Workbook1_Part5_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 43,
            sourceTopic = "Staining Cells",
            appTitles = listOf("Staining"),
            explanation = "Many cells are transparent and difficult to observe. Scientists use special stains such as iodine or methylene blue to colour different parts of a cell. Staining increases contrast, making structures like the nucleus easier to identify. Different stains are used for different tissues depending on what scientists want to study.",
            keyPoints = listOf("Iodine", "Methylene blue", "Contrast"),
            realLifeExample = "Colouring a pencil sketch makes important details easier to see.",
            verifiedFact = "Onion cells are often stained with iodine in school laboratories.",
            quizQuestion = "Why are stains used when observing cells?",
            quizAnswer = "To increase contrast or label particular structures.",
            sourceFile = "Biology_Workbook1_Part5_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 44,
            sourceTopic = "Onion Peel Experiment",
            appTitles = listOf("Onion Peel Experiment"),
            explanation = "The onion peel experiment is one of the first practical activities in biology. A thin peel from the onion is placed on a slide, stained with iodine and observed under a microscope. Students can clearly identify the cell wall, cytoplasm, nucleus and large vacuole. This experiment introduces the basic structure of plant cells.",
            keyPoints = listOf("Practical", "Plant cell", "Microscope"),
            realLifeExample = "Like looking at bricks in a wall, each onion cell can be seen separately.",
            verifiedFact = "Onion peel is transparent, making it ideal for classroom observation.",
            quizQuestion = "Which structures can be seen in an onion peel cell?",
            quizAnswer = "Cell wall, cytoplasm, nucleus when stained, and a large vacuole region; chloroplasts are normally absent in onion bulb epidermis.",
            sourceFile = "Biology_Workbook1_Part5_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 45,
            sourceTopic = "Cheek Cell Experiment",
            appTitles = listOf("Cheek Cell Experiment"),
            explanation = "Human cheek cells are easy to collect by gently scraping the inside of the cheek. After staining with methylene blue, students can observe the cell membrane, cytoplasm and nucleus under a microscope. Unlike plant cells, cheek cells do not have a cell wall or chloroplasts. This simple activity helps compare plant and animal cells.",
            keyPoints = listOf("Animal cell", "Methylene blue", "Comparison"),
            realLifeExample = "Doctors also collect cheek cells for some DNA tests.",
            verifiedFact = "Cheek cells are replaced regularly.",
            quizQuestion = "Which structure is absent in cheek cells but present in plant cells?",
            quizAnswer = "A cell wall.",
            sourceFile = "Biology_Workbook1_Part5_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 46,
            sourceTopic = "Comparing Plant and Animal Cells",
            appTitles = listOf("Comparing Plant and Animal Cells"),
            explanation = "Plant and animal cells share many organelles, including the nucleus, mitochondria and cell membrane. However, plant cells also have a cell wall, chloroplasts and a large central vacuole. Animal cells are usually more flexible and have smaller vacuoles. Comparing both cells helps students understand how organisms are adapted to different lifestyles.",
            keyPoints = listOf("Similarities", "Differences", "Organelles"),
            realLifeExample = "A house and an apartment both provide shelter but have different designs.",
            verifiedFact = "Both plant and animal cells are eukaryotic.",
            quizQuestion = "Name one organelle found only in plant cells.",
            quizAnswer = "A cell wall is characteristic of plant cells; chloroplasts occur only in photosynthetic plant cells and algae.",
            sourceFile = "Biology_Workbook1_Part5_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 47,
            sourceTopic = "Specialised Cells",
            appTitles = listOf("Specialised Cells"),
            explanation = "Not all cells perform the same task. Red blood cells transport oxygen, nerve cells carry electrical signals, muscle cells help movement and root hair cells absorb water. Their shapes and structures are adapted for these specific functions. Specialisation makes multicellular organisms more efficient.",
            keyPoints = listOf("Adaptation", "Function", "Cell types"),
            realLifeExample = "Different workers in a company have different responsibilities.",
            verifiedFact = "The human body has more than 200 specialised cell types.",
            quizQuestion = "Why are cells specialised?",
            quizAnswer = "Specialisation allows cells to perform particular jobs efficiently in a multicellular organism.",
            sourceFile = "Biology_Workbook1_Part5_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 48,
            sourceTopic = "Levels of Organisation",
            appTitles = listOf("Levels of Organization"),
            explanation = "Cells work together to form tissues. Tissues combine to form organs, organs form organ systems and organ systems make a complete organism. This organisation allows different parts of the body to perform specialised functions while working together. Understanding these levels helps explain how the body is built.",
            keyPoints = listOf("Cells", "Tissues", "Organs", "Systems"),
            realLifeExample = "Individual musicians form an orchestra that plays together.",
            verifiedFact = "The heart is an organ made mainly of muscle tissue.",
            quizQuestion = "What comes after tissues?",
            quizAnswer = "An organ.",
            sourceFile = "Biology_Workbook1_Part5_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 49,
            sourceTopic = "Unicellular and Multicellular Organisms",
            appTitles = listOf("Unicellular and Multicellular Organisms"),
            explanation = "Some living organisms consist of only one cell, while others are made of millions or trillions of cells. Amoeba and bacteria are unicellular organisms because a single cell performs every life function. Humans, trees and dogs are multicellular organisms with many specialised cells working together. Both types are living, but their organisation is different.",
            keyPoints = listOf("One cell", "Many cells", "Organisation"),
            realLifeExample = "A single worker can run a small shop, while a large company needs many employees.",
            verifiedFact = "The largest organism on Earth is multicellular.",
            quizQuestion = "Give one example of a unicellular organism.",
            quizAnswer = "A bacterium or an Amoeba.",
            sourceFile = "Biology_Workbook1_Part5_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 50,
            sourceTopic = "Review of Cell Biology",
            appTitles = listOf("Review of Cell Biology"),
            explanation = "Cell biology explains how the smallest living units support all life on Earth. By understanding cells, organelles, transport, division and specialisation, students build the foundation for advanced topics such as genetics, physiology and biotechnology. Revising diagrams, practical activities and real-life examples makes learning easier than memorising definitions alone.",
            keyPoints = listOf("Revision", "Foundation", "Organelles"),
            realLifeExample = "Building a strong foundation makes a house stable.",
            verifiedFact = "Every living organism is connected through the study of cells.",
            quizQuestion = "Why is cell biology considered the foundation of biology?",
            quizAnswer = "Because cell structure and activity explain growth, transport, inheritance, physiology, and disease.",
            sourceFile = "Biology_Workbook1_Part5_Quality.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 51,
            sourceTopic = "Genes",
            appTitles = listOf("Genes"),
            explanation = "Have you ever wondered why children resemble their parents? The answer lies in genes. Genes are small sections of DNA that carry instructions for building and running a living organism. Each gene controls one or more characteristics such as eye colour, blood group, or the production of important proteins. Humans have about 20,000 genes working together. Some genes are active all the time, while others are switched on only when needed. Changes in genes can sometimes lead to inherited disorders or useful variations.",
            keyPoints = listOf("DNA segments", "Inheritance", "Traits"),
            realLifeExample = "Family members often share similar features because they inherit genes.",
            verifiedFact = "Humans share more than 99% of their genes with each other.",
            quizQuestion = "What is a gene?",
            quizAnswer = "A DNA sequence that produces a functional RNA or helps specify a functional product.",
            sourceFile = "Biology_Workbook1_Part6_Topics51_60.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 52,
            sourceTopic = "Genetic Code",
            appTitles = listOf("Genetic Code"),
            explanation = "Every cell follows a special biological language called the genetic code. DNA uses four chemical bases—A, T, G and C—to store information. Three bases together form a codon, and each codon represents one amino acid. By reading these codons in the correct order, cells build proteins needed for life. The genetic code is almost universal, meaning the same code works in nearly all living organisms.",
            keyPoints = listOf("Codons", "DNA bases", "Amino acids"),
            realLifeExample = "Computer code gives instructions to software just as genetic code gives instructions to cells.",
            verifiedFact = "The same genetic code is used by bacteria, plants and humans.",
            quizQuestion = "What is a codon?",
            quizAnswer = "A three-nucleotide sequence in mRNA that specifies an amino acid or a stop signal during translation.",
            sourceFile = "Biology_Workbook1_Part6_Topics51_60.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 53,
            sourceTopic = "DNA Replication",
            appTitles = listOf("DNA Replication"),
            explanation = "Before a cell divides, it must make an exact copy of its DNA. This process is called DNA replication. The two DNA strands separate, and each strand acts as a template to build a new matching strand. The result is two identical DNA molecules, ensuring each new cell receives the same genetic information. Accurate replication is essential for growth, repair and reproduction.",
            keyPoints = listOf("Copying DNA", "Cell division"),
            realLifeExample = "Photocopying an important document creates an identical backup.",
            verifiedFact = "Your DNA is copied billions of times during your lifetime.",
            quizQuestion = "Why does DNA replicate?",
            quizAnswer = "To provide each daughter cell with a complete copy of the genome before cell division.",
            sourceFile = "Biology_Workbook1_Part6_Topics51_60.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 54,
            sourceTopic = "Protein Synthesis",
            appTitles = listOf("Protein Synthesis"),
            explanation = "Proteins are made through a process called protein synthesis. First, DNA provides instructions to RNA. The RNA carries these instructions to ribosomes, where amino acids are joined together to form a protein. Different proteins perform different jobs, including building muscles, producing enzymes and carrying oxygen. Without protein synthesis, cells could not grow or repair themselves.",
            keyPoints = listOf("DNA", "RNA", "Ribosomes"),
            realLifeExample = "A recipe is followed step by step to prepare a meal.",
            verifiedFact = "Insulin is a protein produced by cells.",
            quizQuestion = "Where are proteins assembled?",
            quizAnswer = "On ribosomes.",
            sourceFile = "Biology_Workbook1_Part6_Topics51_60.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 55,
            sourceTopic = "Mutation",
            appTitles = listOf("Mutation"),
            explanation = "Sometimes a small change occurs in the DNA sequence. This change is called a mutation. Mutations may happen naturally or because of radiation or certain chemicals. Some mutations have no effect, some are harmful and a few are beneficial. Over many generations, useful mutations contribute to evolution by creating new variations.",
            keyPoints = listOf("DNA change", "Variation"),
            realLifeExample = "A typing mistake can change the meaning of a sentence.",
            verifiedFact = "Most mutations are repaired by the cell.",
            quizQuestion = "What is a mutation?",
            quizAnswer = "A heritable change in a DNA sequence, or more broadly a stable change in genetic material.",
            sourceFile = "Biology_Workbook1_Part6_Topics51_60.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 56,
            sourceTopic = "Cell Growth",
            appTitles = listOf("Cell Growth"),
            explanation = "Cells grow by taking in nutrients, producing proteins and increasing in size. Once a cell becomes large enough, it usually divides into two new cells. Growth allows babies to become adults and helps plants produce new leaves and roots. Healthy growth depends on proper nutrition and normal cell division.",
            keyPoints = listOf("Growth", "Nutrients", "Division"),
            realLifeExample = "A sapling grows into a tree through continuous cell growth.",
            verifiedFact = "Some cells stop growing after reaching maturity.",
            quizQuestion = "How do cells grow?",
            quizAnswer = "By synthesising cellular material and increasing biomass; cell enlargement and cell division are distinct processes.",
            sourceFile = "Biology_Workbook1_Part6_Topics51_60.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 57,
            sourceTopic = "Cell Repair",
            appTitles = listOf("Cell Repair"),
            explanation = "Every day, millions of cells in the human body are damaged and replaced. Skin cells heal cuts, while bone cells repair fractures. Cell repair depends on healthy cell division and protein production. Good nutrition, sleep and proper medical care help the body repair tissues more effectively.",
            keyPoints = listOf("Healing", "Replacement"),
            realLifeExample = "A cracked road is repaired to make it usable again.",
            verifiedFact = "Your skin is constantly replacing old cells.",
            quizQuestion = "Why is cell repair important?",
            quizAnswer = "It restores tissue structure and function after normal wear or injury.",
            sourceFile = "Biology_Workbook1_Part6_Topics51_60.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 58,
            sourceTopic = "Cancer Cells",
            appTitles = listOf("Cancer Cells"),
            explanation = "Normally, cells divide only when needed. Cancer begins when some cells lose control and divide continuously. These abnormal cells form lumps called tumours and may spread to other parts of the body. Early detection and treatment improve the chances of recovery. Researchers continue to study cancer to develop better medicines.",
            keyPoints = listOf("Uncontrolled division", "Tumours"),
            realLifeExample = "Weeds spreading through a garden can crowd out healthy plants.",
            verifiedFact = "Not all tumours are cancerous.",
            quizQuestion = "What causes cancer cells to grow rapidly?",
            quizAnswer = "Accumulated genetic and epigenetic changes can disrupt controls on division, survival, and tissue behaviour.",
            sourceFile = "Biology_Workbook1_Part6_Topics51_60.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 59,
            sourceTopic = "Normal vs Cancer Cells",
            appTitles = listOf("Normal vs Cancer Cells"),
            explanation = "Normal cells follow the rules of the cell cycle. They divide when needed and stop when their job is complete. Cancer cells ignore these signals and continue dividing. They often have unusual shapes and can invade nearby tissues. Comparing both cell types helps scientists understand disease and develop new treatments.",
            keyPoints = listOf("Controlled vs uncontrolled growth"),
            realLifeExample = "Following traffic signals prevents accidents.",
            verifiedFact = "Cancer cells may spread through blood or lymph.",
            quizQuestion = "What is one difference between normal and cancer cells?",
            quizAnswer = "Normal cells usually obey growth controls; cancer cells can evade those controls and may invade other tissues.",
            sourceFile = "Biology_Workbook1_Part6_Topics51_60.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 60,
            sourceTopic = "Importance of Cell Biology",
            appTitles = listOf("Importance of Cell Biology"),
            explanation = "Cell biology explains how life works at its most basic level. It helps doctors understand diseases, farmers improve crops and scientists develop new medicines. Knowledge of cells is also used in biotechnology, genetics and forensic science. Learning cell biology gives students a strong foundation for understanding all other branches of biology.",
            keyPoints = listOf("Foundation", "Medicine", "Biotechnology"),
            realLifeExample = "Understanding bricks helps you understand how a building is made.",
            verifiedFact = "Many Nobel Prizes have been awarded for discoveries in cell biology.",
            quizQuestion = "Why is cell biology important?",
            quizAnswer = "It connects cellular mechanisms with health, agriculture, biotechnology, and nearly every other field of biology.",
            sourceFile = "Biology_Workbook1_Part6_Topics51_60.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 61,
            sourceTopic = "Cell Signalling",
            appTitles = listOf("Cell Communication"),
            explanation = "Imagine a school where no one can communicate. Confusion would quickly spread. Cells also need to exchange information to work together. Cell signalling is the process by which cells send and receive chemical messages. Hormones, neurotransmitters and other signalling molecules bind to receptors on target cells and trigger specific responses such as growth, movement or protein production. Proper signalling keeps the body healthy, while faulty signalling can lead to diseases.",
            keyPoints = listOf("Chemical messages", "Coordination", "Receptors"),
            realLifeExample = "A phone call delivers a message from one person to another.",
            verifiedFact = "One hormone molecule can affect millions of cells.",
            quizQuestion = "What is cell signalling?",
            quizAnswer = "The sending, detection, and processing of signals that change cell behaviour.",
            sourceFile = "Biology_Workbook1_Part7_Topics61_70.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 62,
            sourceTopic = "Cell Receptors",
            appTitles = listOf("Cell Receptors"),
            explanation = "Cells do not respond to every chemical around them. They have special receptor proteins that recognise only certain signalling molecules. When the correct molecule binds, the receptor changes shape and starts a response inside the cell. This lock-and-key relationship ensures accurate communication.",
            keyPoints = listOf("Receptors", "Lock and key", "Specificity"),
            realLifeExample = "A house key opens only the correct lock.",
            verifiedFact = "Insulin works by binding to insulin receptors.",
            quizQuestion = "What is the function of a receptor?",
            quizAnswer = "It detects a particular signal and initiates or alters a response in the cell.",
            sourceFile = "Biology_Workbook1_Part7_Topics61_70.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 63,
            sourceTopic = "Transport Proteins",
            appTitles = listOf("Transport Proteins"),
            explanation = "Many important substances cannot pass directly through the cell membrane. Transport proteins act as channels or carriers, helping glucose, ions and other molecules cross the membrane. Some transport proteins work without energy, while others require ATP for active transport.",
            keyPoints = listOf("Channels", "Carriers", "Membrane"),
            realLifeExample = "An elevator helps people move between floors.",
            verifiedFact = "Aquaporins are transport proteins that move water.",
            quizQuestion = "Why are transport proteins needed?",
            quizAnswer = "They provide selective routes for polar molecules and ions that cannot readily cross the lipid bilayer.",
            sourceFile = "Biology_Workbook1_Part7_Topics61_70.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 64,
            sourceTopic = "Cytoskeleton",
            appTitles = listOf("Cytoskeleton"),
            explanation = "Although cells are tiny, they have an internal framework called the cytoskeleton. It is made of protein fibres that give the cell its shape, help organelles stay in place and allow movement inside the cell. During cell division, the cytoskeleton also helps separate chromosomes.",
            keyPoints = listOf("Support", "Shape", "Protein fibres"),
            realLifeExample = "Steel beams support a tall building.",
            verifiedFact = "The cytoskeleton is constantly changing as the cell moves.",
            quizQuestion = "What is the cytoskeleton made of?",
            quizAnswer = "Protein filaments: microfilaments, intermediate filaments, and microtubules.",
            sourceFile = "Biology_Workbook1_Part7_Topics61_70.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 65,
            sourceTopic = "Extracellular Matrix",
            appTitles = listOf("Extracellular Matrix"),
            explanation = "Animal cells are surrounded by a material called the extracellular matrix. It provides support, helps cells stick together and allows communication between neighbouring cells. Bones, cartilage and skin all depend on a strong extracellular matrix.",
            keyPoints = listOf("Support", "Cell attachment"),
            realLifeExample = "Concrete holds bricks together in a wall.",
            verifiedFact = "Collagen is the most abundant protein in the extracellular matrix.",
            quizQuestion = "Name one function of the extracellular matrix.",
            quizAnswer = "It provides structural support, adhesion, signalling cues, and a medium through which cells organize tissues.",
            sourceFile = "Biology_Workbook1_Part7_Topics61_70.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 66,
            sourceTopic = "Collagen",
            appTitles = listOf("Collagen"),
            explanation = "Collagen is a strong protein found in skin, bones, tendons and ligaments. It gives tissues strength and flexibility. As people grow older, collagen production decreases, which contributes to wrinkles and weaker joints. A healthy diet helps the body produce collagen.",
            keyPoints = listOf("Structural protein", "Skin", "Bones"),
            realLifeExample = "Ropes strengthen a tent.",
            verifiedFact = "Collagen makes up about one-third of the body's protein.",
            quizQuestion = "Where is collagen commonly found?",
            quizAnswer = "In skin, bone, cartilage, tendons, ligaments, and many other extracellular matrices.",
            sourceFile = "Biology_Workbook1_Part7_Topics61_70.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 67,
            sourceTopic = "Cell Adhesion",
            appTitles = listOf("Cell Adhesion"),
            explanation = "Cells must stay attached to form tissues and organs. Cell adhesion molecules act like tiny connectors that hold neighbouring cells together. They also help cells recognise one another and communicate. Poor cell adhesion can contribute to diseases and cancer spread.",
            keyPoints = listOf("Attachment", "Tissues", "Recognition"),
            realLifeExample = "Bricks stay together because of cement.",
            verifiedFact = "White blood cells temporarily reduce adhesion to move into infected tissues.",
            quizQuestion = "Why is cell adhesion important?",
            quizAnswer = "It holds tissues together and also influences cell recognition, signalling, movement, and survival.",
            sourceFile = "Biology_Workbook1_Part7_Topics61_70.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 68,
            sourceTopic = "Gap Junctions",
            appTitles = listOf("Gap Junctions"),
            explanation = "Gap junctions are tiny channels connecting neighbouring animal cells. They allow small molecules and electrical signals to pass directly from one cell to another. Heart muscle cells use gap junctions so they contract together in a regular rhythm.",
            keyPoints = listOf("Communication", "Channels", "Heart"),
            realLifeExample = "A tunnel connects two nearby buildings.",
            verifiedFact = "Gap junctions help coordinate heartbeat.",
            quizQuestion = "Where are gap junctions especially important?",
            quizAnswer = "They are especially important in electrically coordinated tissues such as cardiac muscle.",
            sourceFile = "Biology_Workbook1_Part7_Topics61_70.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 69,
            sourceTopic = "Plasmodesmata",
            appTitles = listOf("Plasmodesmata"),
            explanation = "Unlike animal cells, plant cells communicate through plasmodesmata. These microscopic channels pass through the cell wall and connect the cytoplasm of neighbouring cells. Water, nutrients and signalling molecules move through these channels, helping plant tissues function as one unit.",
            keyPoints = listOf("Plant communication", "Cell wall", "Channels"),
            realLifeExample = "Small doorways connect neighbouring rooms.",
            verifiedFact = "Thousands of plasmodesmata may connect two plant cells.",
            quizQuestion = "What are plasmodesmata?",
            quizAnswer = "Membrane-lined channels through plant cell walls that connect the cytoplasm of neighbouring cells.",
            sourceFile = "Biology_Workbook1_Part7_Topics61_70.xlsx"
        ),
        WorkbookLessonSeed(
            sourceId = 70,
            sourceTopic = "Applications of Cell Biology",
            appTitles = listOf("Applications of Cell Biology"),
            explanation = "Cell biology is used in medicine, agriculture, biotechnology and environmental science. Scientists grow cells in laboratories to develop vaccines, test medicines and study diseases. Farmers use cell culture to produce disease-resistant plants, while forensic experts use cellular and DNA evidence to solve crimes. Understanding cells has improved healthcare and modern technology.",
            keyPoints = listOf("Medicine", "Biotechnology", "Agriculture"),
            realLifeExample = "Doctors test medicines on cultured cells before human trials.",
            verifiedFact = "Cell culture is widely used to develop vaccines.",
            quizQuestion = "Give one application of cell biology.",
            quizAnswer = "Examples include disease diagnosis, cell culture, drug testing, vaccines, tissue engineering, crop propagation, and forensic analysis.",
            sourceFile = "Biology_Workbook1_Part7_Topics61_70.xlsx"
        )
    )

    private val byAppTitle: Map<String, WorkbookLessonSeed> =
        buildMap {
            records.forEach { record ->
                record.appTitles.forEach { title -> putIfAbsent(title, record) }
            }
        }

    fun sourceRecords(): List<WorkbookLessonSeed> = records

    fun phases(title: String): List<LessonPhase>? {
        val record = byAppTitle[title] ?: return null
        val examPoints = record.keyPoints.joinToString("; ")
        return listOf(
            LessonPhase(
                title = "1 · Learn the concept",
                explanation = record.explanation,
                example = "Example from the workbook: ${record.realLifeExample}",
                didYouKnow = record.verifiedFact
            ),
            LessonPhase(
                title = "2 · Why it matters",
                explanation =
                    "Key points from this lesson: $examPoints. Use these points when you explain the topic.",
                example =
                    "Example from the workbook: ${record.realLifeExample}",
                didYouKnow = record.verifiedFact
            ),
            LessonPhase(
                title = "3 · Think like a biologist",
                explanation =
                    "Did you know? ${record.verifiedFact} Connect this fact with the detailed explanation above.",
                example =
                    "Example: Test your understanding with this question: ${record.quizQuestion}",
                didYouKnow = record.verifiedFact
            )
        )
    }

    fun learningContent(title: String, phases: List<LessonPhase>): LessonLearningContent? {
        val record = byAppTitle[title] ?: return null
        val examPoints =
            (record.keyPoints.take(3) + List((3 - record.keyPoints.size).coerceAtLeast(0)) {
                "Connect the definition to its biological function"
            }).take(3)
        return LessonLearningContent(
            detailedExplanation = record.explanation,
            easyWayToLearn = listOf(
                "READ: Identify the structure or process described in the opening paragraph.",
                "VISUALISE: Picture this example — ${record.realLifeExample}",
                "CONNECT: Link each key point to a cause, mechanism, and result.",
                "RECALL: Close the lesson and answer the quick check without looking."
            ),
            realLifeExamples = listOf(
                record.realLifeExample,
                "Did you know? ${record.verifiedFact}",
                "Quick check: ${record.quizQuestion}"
            ),
            importantPoints = examPoints.mapIndexed { index, point ->
                if (index == 0) "Definition and foundation: $point"
                else "Exam point ${index + 1}: $point"
            } + listOf(
                "Example to remember: ${record.realLifeExample}",
                "Important fact: ${record.verifiedFact}"
            ),
            commonMistake =
                "Do not repeat the analogy as if it were the scientific explanation. Use it to remember the idea, then write the real mechanism.",
            quickCheckQuestion = record.quizQuestion,
            quickCheckAnswer = record.quizAnswer
        )
    }
}
