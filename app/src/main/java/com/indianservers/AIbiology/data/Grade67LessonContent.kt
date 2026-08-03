package com.indianservers.AIbiology.data

/**
 * Fully authored NCERT Curiosity biology lessons for Grades 6 and 7.
 *
 * These lessons deliberately do not use the catalogue's advanced fallback text.
 * Every concept has a simple explanation, a useful connection, a safe extension,
 * and three concrete examples written for middle-school learners.
 */
internal object Grade67LessonContent {
    private data class Seed(
        val core: String,
        val connection: String,
        val advanced: String,
        val examples: List<String>
    )

    private fun seed(
        core: String,
        connection: String,
        advanced: String,
        firstExample: String,
        secondExample: String,
        thirdExample: String
    ) = Seed(core, connection, advanced, listOf(firstExample, secondExample, thirdExample))

    fun phases(areaNumber: Int, title: String): List<LessonPhase>? {
        val seed = when (areaNumber) {
            45 -> grade6[title]
            46 -> grade7[title]
            else -> null
        } ?: return null
        return listOf(
            LessonPhase(
                title = "1 · Learn the idea",
                explanation = seed.core,
                example = "Example: ${seed.examples[0]}",
                didYouKnow = seed.connection
            ),
            LessonPhase(
                title = "2 · Connect it",
                explanation = seed.connection,
                example = "Example: ${seed.examples[1]}",
                didYouKnow = "A good answer explains the observation and the reason for it."
            ),
            LessonPhase(
                title = "3 · Think deeper",
                explanation = seed.advanced,
                example = "Example: ${seed.examples[2]}",
                didYouKnow = "Use evidence from an observation or fair test before drawing a conclusion."
            )
        )
    }

    private val grade6 = mapOf(
        "Diversity Around Us" to seed(
            "Many kinds of plants and animals live around us. This variety is called biodiversity. Living things differ in size, shape, colour, food, movement and the places where they live.",
            "We group living things by shared features so that they are easier to observe, compare and study. A useful group is based on more than one clear feature.",
            "Members of one group can still be different. Classification is a tool made by people; it should change when new observations give better evidence.",
            "A school garden may contain grasses, shrubs, trees, ants, butterflies and birds.",
            "A bat and a pigeon can both fly, but a bat has hair and feeds its young with milk, while a pigeon has feathers.",
            "Make a biodiversity list for two nearby places and compare which place has more kinds of organisms."
        ),
        "Herbs, Shrubs and Trees" to seed(
            "Plants can be grouped by their height, stem and branching pattern. Herbs are usually small with soft green stems. Shrubs have several hard, woody stems near the ground. Trees are tall and usually have one thick woody trunk.",
            "These groups describe growth form, not the name of a plant family. Looking at the stem and where branches begin is more reliable than looking only at height.",
            "Some plants do not fit perfectly because growth depends on species, age and conditions. State the observed features when you classify an unfamiliar plant.",
            "Mint is a herb, rose is a shrub and mango is a tree.",
            "A young tree may be short, but its single woody main stem shows that it is not an herb.",
            "Observe three local plants and record stem texture, number of main stems and branching height."
        ),
        "Leaf Venation and Root Types" to seed(
            "Venation is the pattern of veins in a leaf. Reticulate venation forms a branching network and is commonly linked with a taproot. Parallel venation has veins running side by side and is commonly linked with fibrous roots.",
            "A taproot has one main root with smaller side roots. A fibrous root system has many roots of similar size growing from the stem base. Roots anchor plants and absorb water and minerals.",
            "The venation-root link is a useful pattern, not a rule without exceptions. Confirm a plant with several features instead of pulling out protected plants just to see the roots.",
            "Hibiscus usually has reticulate venation and a taproot.",
            "Wheat usually has parallel venation and fibrous roots.",
            "Predict the root type from a fallen leaf, then check a safe example such as a weed with teacher guidance."
        ),
        "Monocot and Dicot Plants" to seed(
            "A monocot seed has one cotyledon, while a dicot seed has two. Cotyledons are seed leaves that store or help supply food to the young plant.",
            "Monocots commonly show parallel leaf venation and fibrous roots. Dicots commonly show reticulate venation and a taproot. These connected features help us identify flowering plants.",
            "Scientists use flower parts, pollen and internal stem structure too. At this level, use seed leaves, venation and roots as a simple evidence set.",
            "Maize and wheat are monocots; bean and pea are dicots.",
            "A soaked bean can be opened gently to see its two cotyledons.",
            "Compare a grass leaf with a bean leaf and record at least two differences."
        ),
        "Grouping Animals" to seed(
            "Animals can be grouped using observable features such as body covering, number of legs, movement, food and habitat. One feature alone may place unrelated animals together.",
            "Choose features that answer a clear question. For example, grouping by habitat helps study where animals live, while grouping by body covering helps compare their bodies.",
            "Scientists classify animals using body structure, development and DNA evidence. Everyday groups such as 'flying animals' are useful for observation but do not always show close relationship.",
            "Fish, dolphin and whale all live in water, but fish have gills while dolphins and whales breathe with lungs.",
            "Cow and pigeon both move on land, but they differ in body covering and reproduction.",
            "Create two different grouping rules for cow, bat, pigeon, fish, lizard and butterfly."
        ),
        "Habitats and Adaptations" to seed(
            "A habitat is the place where an organism lives and gets food, water, air, shelter and suitable conditions. An adaptation is a feature that helps an organism survive and reproduce in its habitat.",
            "Body shape, covering, feet, roots and behaviour may be adaptations. Adaptations develop in populations over many generations; an individual does not grow a needed feature just by wishing for it.",
            "A feature can involve a trade-off. Webbed feet help a duck push water but are not built for gripping branches like the feet of many perching birds.",
            "A duck's webbed feet act like paddles in water.",
            "A mountain goat has strong hooves and a body suited to steep, cold habitats.",
            "Compare a cactus with a broad-leaved plant and explain how each manages water."
        ),
        "Biodiversity and Conservation" to seed(
            "Biodiversity includes the variety of living organisms in an area. Conserving biodiversity means protecting organisms and the habitats and relationships they need.",
            "Cutting forests can remove food and shelter, expose soil, disturb water cycles and reduce populations. Planting native species and protecting habitats can help.",
            "Saving one species may require protecting a whole food web. Conservation decisions use surveys, local knowledge and repeated monitoring.",
            "A pond supports plants, insects, frogs, fish and visiting birds.",
            "Project Tiger protects tigers as well as large areas used by many other species.",
            "Prepare a class biodiversity register and repeat the survey in another season."
        ),
        "Food Diversity and Traditions" to seed(
            "People eat different foods because crops, climate, culture, seasons and local resources differ. Traditional meals often combine cereals, pulses, vegetables, fruits and other locally available foods.",
            "No single food supplies every nutrient in the required amount. Food traditions can be studied respectfully while also checking hygiene, variety and nutritional balance.",
            "Cooking can improve taste, safety and digestion, but too much heating or processing can reduce some nutrients. Healthy choices consider both culture and evidence.",
            "Rice with dal combines a cereal and a pulse.",
            "Fermented foods such as idli develop a different texture and flavour from the batter.",
            "Compare a traditional local meal with a packaged snack for variety, fibre and processing."
        ),
        "Nutrients in Food" to seed(
            "Nutrients are useful substances in food. Carbohydrates and fats mainly provide energy; proteins support growth and repair; vitamins and minerals help the body work properly. Fibre and water are also essential.",
            "Different foods contain different mixtures of nutrients. The amount needed varies with age, activity, health and growth.",
            "A food can contain a nutrient without being the best source of it. Read claims carefully and compare the whole food, not one attractive word on a label.",
            "Rice and potato are rich in carbohydrates; pulses and eggs provide protein.",
            "Nuts and oils contain fats, while fruits and vegetables supply several vitamins, minerals and fibre.",
            "Read three food labels and compare serving size, protein, sugar, fat and fibre."
        ),
        "Food Tests" to seed(
            "Simple tests can show whether some food components are present. Iodine turns blue-black with starch. A translucent oily patch suggests fat. The school protein test gives a violet colour when protein is present.",
            "Use a small sample and include a known positive and a known negative when possible. A colour change must be compared with the starting colour.",
            "A negative result means the test did not detect that component under those conditions. It does not prove that the food contains no nutrients.",
            "Potato or rice gives a blue-black colour with iodine because it contains starch.",
            "Oil leaves a translucent patch on paper that remains after drying.",
            "Sugar does not turn blue-black with iodine because sugar is a carbohydrate but not starch."
        ),
        "Balanced Diet" to seed(
            "A balanced diet supplies suitable amounts of carbohydrates, proteins, fats, vitamins, minerals, fibre and water. It includes variety rather than depending on one food.",
            "Balance depends on the person. A growing child and a very active person may need different quantities, but both need variety and safe food.",
            "A healthy pattern also considers excess salt, free sugar and highly processed foods. Occasional foods do not decide health; repeated habits matter more.",
            "A meal of roti, dal, vegetables, curd and fruit includes several food groups.",
            "Only noodles and biscuits may provide energy but too little fibre and micronutrient variety.",
            "Design an affordable one-day meal plan using foods commonly available in your area."
        ),
        "Deficiency Diseases" to seed(
            "A deficiency disease can develop when the body lacks a nutrient for a long time. Vitamin A deficiency can affect dim-light vision; iron deficiency can cause anaemia; iodine deficiency can cause goitre; vitamin D or calcium shortage can weaken bones.",
            "Symptoms can have more than one cause, so a trained health professional should diagnose illness. A varied diet helps prevention, but supplements should not be taken carelessly.",
            "Nutrients work together. Vitamin D helps the body absorb calcium, which is why both may be considered when supporting bone health.",
            "Carrots and leafy vegetables provide substances the body can use for vitamin A.",
            "Iodised salt helps provide iodine in small safe amounts.",
            "Explain why a doctor may give vitamin D together with calcium after a fracture."
        ),
        "Millets and Healthy Grains" to seed(
            "Millets are grains such as ragi, bajra and jowar. They can provide carbohydrates, fibre and useful minerals, and many grow well in relatively dry conditions.",
            "Millets add variety to meals but cannot alone meet every nutritional need. Combine them with pulses, vegetables, fruits and other appropriate foods.",
            "A crop's value includes nutrition, water need, local climate, storage, cost and farmer knowledge. No grain is automatically best for every person and place.",
            "Ragi can be eaten with sambar or pulses to make the meal more varied.",
            "Bajra is grown in several dry regions because it can tolerate limited water better than some crops.",
            "Compare the nutrition label of a millet product with its ingredient list and added sugar."
        ),
        "Food Miles" to seed(
            "Food miles describe how far food travels from where it is produced to where it is eaten. Transport, cooling, storage and packaging all use resources.",
            "Local seasonal food can sometimes need less transport and storage, but distance alone does not show the complete environmental effect.",
            "A fair comparison considers farming method, waste, transport type, refrigeration, packaging and nutrition as well as kilometres.",
            "A mango grown nearby in season may travel fewer kilometres than an imported fruit.",
            "Milk needs safe cooling during transport even when the distance is short.",
            "Trace one food from farm to plate and list every stage where energy or packaging is used."
        ),
        "Living and Non-living Things" to seed(
            "Living organisms carry out life processes: they need nutrition, respire, grow, excrete, respond to stimuli and reproduce as a kind. Living things also have a life span.",
            "Movement alone does not prove life. A car moves but does not perform life processes, while a plant is living even though it usually stays rooted in one place.",
            "Seeds may appear inactive but are living and can germinate in suitable conditions. A wooden log came from a living tree, but the log as a whole no longer carries out life processes.",
            "A sunflower shoot bends as it grows toward light.",
            "A stone neither grows from within nor respires or reproduces.",
            "Compare a dry seed, a toy car and a seedling using several characteristics of life."
        ),
        "Seed Germination" to seed(
            "Germination is the beginning of growth of a seed into a seedling. A viable seed generally needs water, oxygen and a suitable temperature. Some seeds also respond to light or darkness.",
            "Water activates the seed, oxygen supports respiration and a suitable temperature allows enzymes to work. The young plant first uses food stored in the seed.",
            "Too much water can fill air spaces and reduce oxygen. This explains why waterlogged seeds may fail even though water is necessary.",
            "A soaked bean swells and its young root emerges first.",
            "Dry grains are stored away from moisture to reduce germination and spoilage.",
            "Test temperature with equal seeds, equal water and only one changed condition."
        ),
        "Growth and Movement in Plants" to seed(
            "Plants grow by making new cells and increasing in size. Roots generally grow downward, while shoots generally grow upward and often bend toward light.",
            "Plant movement is usually slow because it happens through growth or changes in water pressure rather than muscles and legs.",
            "Roots respond to gravity and water, while shoots respond to light and gravity. A response may help the plant reach a resource.",
            "A seedling near a window bends toward the light.",
            "The root of a germinating seed turns downward even if the seed lies sideways.",
            "Rotate a safely grown seedling and record how root and shoot direction change over several days."
        ),
        "Plant Life Cycle" to seed(
            "A flowering plant life cycle can include seed, germination, seedling, mature plant, flowers, fruits and new seeds. The new seeds can begin the next generation.",
            "Flowers take part in reproduction. After pollination and fertilisation, parts of a flower can develop into a fruit containing seeds.",
            "Life-cycle length differs widely. Some plants complete it in one season, while many trees live and reproduce for years.",
            "A bean seed germinates, grows leaves, forms flowers and later produces pods with seeds.",
            "A mango fruit protects seeds that may grow into new mango plants.",
            "Create a dated picture record of one fast-growing plant from seed to flowering."
        ),
        "Mosquito and Frog Life Cycles" to seed(
            "A mosquito develops through egg, larva, pupa and adult stages. A frog develops from eggs into a tadpole, then a froglet and an adult. Large body changes during development are called metamorphosis.",
            "Mosquito larvae and frog tadpoles live in water. Adult mosquitoes are flying insects, while adult frogs can live on land and in water.",
            "Life-cycle knowledge helps conservation and health. Removing standing water can reduce mosquito breeding, while clean ponds are important habitats for frogs and many other organisms.",
            "A mosquito pupa does not feed but changes into the adult form.",
            "A tadpole has a tail and gills; an adult frog develops legs and uses lungs and skin.",
            "Compare the stages and habitats of a butterfly, mosquito and frog without handling wild animals."
        ),
        "Natural Resources" to seed(
            "Natural resources are useful materials and conditions obtained from nature, including air, water, soil, sunlight, forests, rocks, minerals and fuels.",
            "People depend on natural resources for food, shelter, energy, transport and products. A machine is useful, but it is made by people from natural materials and is not itself a natural resource.",
            "Resources are connected. A forest influences soil, water, air, climate and biodiversity, so using one resource can affect several others.",
            "Cotton comes from a plant and iron comes from mineral ores.",
            "Sunlight supports photosynthesis and can also produce electricity in solar panels.",
            "Trace a pencil, notebook or bicycle back to the natural resources used to make it."
        ),
        "Air and Water as Resources" to seed(
            "Air supplies oxygen for respiration and carbon dioxide for photosynthesis. Water is needed by organisms, farms, homes and industries and also provides aquatic habitats.",
            "Air and water are renewed by natural cycles, but clean usable supplies can still become scarce or polluted. Renewable does not mean unlimited.",
            "Protecting a resource includes both quantity and quality. Saving water does not help fully if the remaining water is contaminated.",
            "Wind moves sailboats and can turn turbines.",
            "Rainwater harvesting stores water for later use or helps recharge groundwater.",
            "Plan how a school can reduce leaking taps and keep wastewater away from clean-water sources."
        ),
        "Forests, Soil and Minerals" to seed(
            "Forests contain trees and many other organisms. Soil supports plants and contains mineral particles, water, air and living matter. Rocks contain minerals used for buildings, metals and many products.",
            "Plant roots hold soil, fallen matter helps build soil and forest cover slows rainwater flow. Large-scale tree cutting can increase erosion and habitat loss.",
            "Soil forms very slowly compared with the speed at which erosion can remove it. Mining provides necessary materials but can disturb land and water.",
            "Earthworms and microbes help mix and break down material in soil.",
            "Granite is used in construction, while iron minerals provide metal for tools.",
            "Compare soil under thick vegetation with exposed soil after rain."
        ),
        "Renewable and Non-renewable Resources" to seed(
            "Renewable resources can be naturally replaced on a human timescale when managed well. Non-renewable resources form so slowly that a used supply is not quickly replaced.",
            "Sunlight and wind are renewable. Coal, petroleum and natural gas are fossil fuels and are non-renewable. Forests are renewable only if regrowth matches or exceeds use.",
            "The label depends on rate. Groundwater can be renewed, but pumping it faster than recharge makes the local supply decline.",
            "A solar panel uses incoming sunlight, while a petrol scooter uses fuel made from petroleum.",
            "Coal formed over millions of years, so burned coal cannot be replaced during a human lifetime.",
            "Classify water, forests and soil carefully and explain the conditions needed for each to remain renewable."
        ),
        "Conserving Natural Resources" to seed(
            "Conservation means using resources carefully, preventing waste and pollution, and protecting nature so that resources remain available.",
            "Useful actions include reducing unnecessary use, reusing safe items, recycling materials, saving water and energy, and protecting native vegetation.",
            "The best action is often to avoid waste before it is created. Recycling still needs collection, transport and energy.",
            "Repairing a leaking tap saves treated water every day.",
            "Using both sides of paper can reduce demand for new paper.",
            "Conduct a one-week school resource audit, then choose one measurable change and check whether it works."
        )
    )

    private val grade7 = mapOf(
        "Adolescence and Puberty" to seed(
            "Adolescence is the transition from childhood to adulthood, usually from about 10 to 19 years. Puberty is the stage during which body changes lead toward reproductive maturity.",
            "The timing and speed of puberty vary naturally. Changes do not begin at exactly the same age for everyone, so comparisons and teasing are harmful.",
            "Puberty is one part of adolescence. Adolescence also includes emotional, social and thinking changes that continue as a young person matures.",
            "A growth spurt may make height increase quickly during adolescence.",
            "Two healthy classmates of the same age may begin puberty at different times.",
            "Make a respectful distinction between adolescence, puberty and adulthood."
        ),
        "Physical Changes During Adolescence" to seed(
            "During puberty, height and body shape change, hair grows in the armpits and pubic region, sweat and oil glands become more active, and reproductive organs mature.",
            "Some changes are common to all adolescents. Others, called secondary sexual characteristics, differ typically between males and females but are not the reproductive organs themselves.",
            "Bodies vary, and one feature does not define a person's health, identity or worth. Seek a trusted adult or health professional when a change causes concern.",
            "The voice often deepens more noticeably in boys as the voice box grows.",
            "Pimples can occur in adolescents of any sex because skin glands become more active.",
            "Sort example changes into common changes and changes more typical of boys or girls."
        ),
        "Menstruation" to seed(
            "Menstruation is the periodic shedding of the uterine lining through the vagina when pregnancy has not begun. It starts after puberty; cycle length and bleeding pattern can vary.",
            "Menstruation is a normal biological process, not an illness or impurity. Clean absorbent products, washing, safe disposal, nutrition and support help menstrual health.",
            "A commonly described cycle is about 28 days, but healthy cycles are not identical. Severe pain, very heavy bleeding or major concern should be discussed with a trusted adult and health professional.",
            "The first menstrual period is called menarche.",
            "A student can attend school and normal activities during menstruation with suitable care.",
            "Explain why a fixed 28-day cycle should not be treated as a rule for every person."
        ),
        "Emotional Changes in Adolescence" to seed(
            "Adolescents may experience stronger or changing emotions, greater self-awareness, new interests and a growing wish for independence. These changes are influenced by the brain, hormones and life experiences.",
            "Talking with trusted people, sleeping well, using healthy ways to manage stress and asking for help are signs of strength.",
            "A difficult feeling is not the same as harmful behaviour. If sadness, fear or anger feels overwhelming or continues, support from a responsible adult or mental-health professional is important.",
            "Feeling excited and nervous about a new school activity can happen at the same time.",
            "Pausing and speaking calmly can prevent an argument from growing.",
            "Prepare a list of trusted people and safe services a young person can contact for help."
        ),
        "Healthy Adolescence" to seed(
            "Healthy adolescence includes varied food, enough sleep, regular physical activity, personal hygiene, supportive relationships and time for study and rest.",
            "Growth raises nutritional needs. Protein supports growth and repair, iron supports healthy blood, calcium and vitamin D support bones, and varied foods supply many micronutrients.",
            "Health advice should be realistic and safe. Extreme diets, unsafe supplements and punishing exercise can harm a growing body.",
            "Washing sweaty skin and changing clean clothes helps personal hygiene.",
            "Outdoor play, sport, dance or walking can provide regular activity.",
            "Design a balanced daily routine that includes sleep, meals, movement, study and relaxation."
        ),
        "Hormones During Puberty" to seed(
            "Hormones are chemical messengers made in the body and carried to target organs. During puberty, hormones help coordinate growth and reproductive development.",
            "The pituitary gland signals other endocrine glands. Testes mainly produce testosterone, while ovaries mainly produce oestrogen and progesterone; all people have several hormones in different amounts.",
            "Hormones act only on cells with suitable receptors. Body changes result from interacting hormones, genes, nutrition, health and environment.",
            "Hormones help cause the growth spurt of puberty.",
            "Iodine is needed to make thyroid hormones, while the normal growing voice box is not the same as an enlarged thyroid.",
            "Draw a simple message pathway: gland, hormone in blood, target organ and response."
        ),
        "Avoiding Addictive Substances" to seed(
            "Tobacco, alcohol and misused drugs can harm the developing body and brain. Repeated use can lead to dependence, making it difficult to stop despite harm.",
            "Advertisements, curiosity or peer pressure do not make a substance safe. A clear refusal, leaving the situation and contacting a trusted adult are protective choices.",
            "Addiction is a health condition, not a reason to shame someone. Prevention, medical care, counselling and social support can help.",
            "Smoking damages lungs and also exposes nearby people to harmful smoke.",
            "A student can say, 'No, I do not use that,' and move toward trusted friends or adults.",
            "Practise a short refusal plan for a situation involving peer pressure."
        ),
        "Life Processes" to seed(
            "Life processes are activities that keep organisms alive, including nutrition, respiration, transport, excretion and regulation. Reproduction continues a species but is not required for one individual's immediate survival.",
            "In a multicellular animal, organ systems cooperate. Digested nutrients and oxygen reach cells through transport, and wastes are carried away.",
            "A failure in one process can affect others. For example, poor gas exchange reduces oxygen available for cellular respiration.",
            "The digestive system makes nutrients available and the circulatory system distributes them.",
            "The respiratory and circulatory systems work together to supply oxygen.",
            "Make a flow diagram linking food, oxygen, cells, energy and waste removal."
        ),
        "Human Digestive System" to seed(
            "The digestive system includes the alimentary canal—mouth, oesophagus, stomach, small intestine, large intestine and anus—and associated organs such as salivary glands, liver and pancreas.",
            "Food is ingested, moved, mechanically broken, chemically digested, absorbed and finally egested if undigested. Each organ has a different role.",
            "Digestion is not simply 'food dissolving.' Enzymes break large food molecules into smaller soluble molecules that can be absorbed.",
            "Teeth and the tongue break and mix food in the mouth.",
            "Muscular movements push swallowed food through the oesophagus.",
            "Trace a piece of food in the correct order from mouth to anus."
        ),
        "Digestion and Absorption" to seed(
            "Digestion breaks large food molecules into smaller soluble molecules. Absorption is the movement of digested nutrients through the intestinal wall into blood or lymph.",
            "Most nutrient absorption occurs in the small intestine. Its folded lining has many villi, which provide a large surface area. The large intestine absorbs much of the remaining water and some salts.",
            "Digestion, absorption and assimilation are different. Assimilation is the use of absorbed nutrients by cells for energy, growth, repair or storage.",
            "Salivary amylase begins breaking starch in the mouth.",
            "Glucose and amino acids pass through the small-intestine lining after digestion.",
            "Compare iodine results for plain starch and starch left with saliva under suitable conditions."
        ),
        "Digestion in Other Animals" to seed(
            "Animals digest food in different ways. Ruminants such as cows swallow partly chewed grass, later return it to the mouth as cud and chew it again. Amoeba surrounds food with its cell membrane and forms a food vacuole.",
            "A ruminant's digestive system and microbes help break down plant material. Amoeba has no mouth or stomach, yet it can take in and digest food inside one cell.",
            "Different feeding structures suit different diets and body plans. Similar life needs can be met by very different biological structures.",
            "A cow may rest while chewing cud that has returned from its stomach.",
            "An Amoeba forms temporary projections around a food particle.",
            "Compare where digestion occurs in a human, a ruminant and an Amoeba."
        ),
        "Breathing and Respiration" to seed(
            "Breathing is the physical movement of air into and out of lungs. Cellular respiration is the chemical process in cells that releases usable energy from food.",
            "In aerobic respiration, glucose reacts with oxygen and produces carbon dioxide, water and energy. Breathing supplies oxygen and removes much of the carbon dioxide, but it is not itself respiration.",
            "Breathing rate often rises during exercise because active cells need faster oxygen delivery and carbon-dioxide removal.",
            "The chest expands during inhalation and becomes smaller during exhalation.",
            "Muscle cells release energy through respiration while a person is running.",
            "Explain why 'we inhale oxygen' is less accurate than 'we inhale air rich in oxygen.'"
        ),
        "Human Respiratory System" to seed(
            "Air normally enters through the nostrils, passes through nasal passages, the windpipe and branching air tubes, and reaches the lungs. The rib cage and diaphragm help ventilation.",
            "During inhalation, the diaphragm contracts and moves downward while the chest volume increases. During exhalation, it relaxes and chest volume decreases.",
            "Nasal hairs and mucus trap some particles, but they cannot make polluted air safe. Smoking damages airways and gas-exchange surfaces.",
            "Sneezing helps remove irritating particles from nasal passages.",
            "The windpipe carries air toward the two lungs.",
            "Use a model to connect diaphragm movement, chest volume and air movement without claiming that the model copies every detail."
        ),
        "Alveoli and Gas Exchange" to seed(
            "Alveoli are tiny air sacs in the lungs where gases are exchanged. Oxygen moves from alveolar air into blood, while carbon dioxide moves from blood into the alveoli.",
            "Many alveoli give a large surface area. Their thin, moist walls and nearby capillaries allow rapid diffusion.",
            "Gas movement depends on concentration differences maintained by ventilation and blood flow. Damaged or fluid-filled alveoli reduce effective exchange.",
            "Blood arriving at lung capillaries has less oxygen than fresh alveolar air.",
            "Exhaled air contains more carbon dioxide than inhaled air and can turn lime water milky faster.",
            "Explain how one alveolar feature supports fast gas exchange."
        ),
        "Circulatory System" to seed(
            "The circulatory system consists mainly of the heart, blood and blood vessels. The heart pumps blood, which carries oxygen, nutrients, hormones and wastes around the body.",
            "Arteries carry blood away from the heart, veins carry blood toward it and tiny capillaries allow exchange with tissues. Direction, not oxygen content, defines an artery or vein.",
            "Circulation links every organ system. Nutrients absorbed by the intestine and oxygen absorbed by the lungs are transported to cells.",
            "The pulse is linked to pressure waves produced when the heart pumps.",
            "Blood carries carbon dioxide from tissues toward the lungs.",
            "Draw a simple route from intestine and lungs to a working muscle and back."
        ),
        "Breathing in Other Animals" to seed(
            "Animals use gas-exchange structures suited to their bodies and habitats. Fish use gills, insects use branching air tubes, earthworms exchange gases through moist skin, and adult frogs use lungs and skin.",
            "A surface for gas exchange is usually thin, moist and has enough area. Water or air must move past it to maintain gas differences.",
            "One animal may change structures during its life cycle. Tadpoles use gills, while adult frogs use lungs on land and skin especially in water.",
            "Fish gills take oxygen dissolved in water.",
            "An earthworm's skin must remain moist for effective gas exchange.",
            "Compare fish gills and human lungs for medium, location and movement of oxygen."
        ),
        "Photosynthesis" to seed(
            "Photosynthesis is the process by which green plants use light energy to make glucose from carbon dioxide and water. Oxygen is released. Chlorophyll helps capture light energy.",
            "Photosynthesis stores light energy in food. Plants use some glucose in respiration and use or store the rest to build other materials.",
            "Plants do not get food from soil. Roots absorb water and minerals, while most food is made in green parts using carbon dioxide from air.",
            "A potato stores starch made from glucose produced by photosynthesis.",
            "Aquatic plants can release visible oxygen bubbles in bright light.",
            "Use an iodine test after destarching a plant to test whether light is needed for starch formation."
        ),
        "Leaves and Stomata" to seed(
            "Leaves are well suited for photosynthesis because many are broad and thin and contain chlorophyll. Stomata are tiny pores that allow carbon dioxide, oxygen and water vapour to move between leaf and air.",
            "Broad leaves capture light, thin tissues shorten gas movement distance and veins bring water and carry away sugars. Guard cells help control stomatal opening.",
            "Opening stomata supports carbon-dioxide entry but also allows water loss. Plants balance photosynthesis with water conservation.",
            "A leaf peel viewed with a microscope can show stomata.",
            "Carbon dioxide enters a leaf through stomata during photosynthesis.",
            "Compare the likely stomatal behaviour of a well-watered plant and a plant losing too much water."
        ),
        "Requirements for Photosynthesis" to seed(
            "Photosynthesis requires light, chlorophyll, carbon dioxide and water. It produces glucose and oxygen. The simple word equation is carbon dioxide plus water, using light and chlorophyll, gives glucose plus oxygen.",
            "A fair test changes one requirement while keeping other important conditions similar. Starch in a leaf can be used as evidence that photosynthesis occurred.",
            "The iodine test detects starch, not photosynthesis directly. A conclusion is valid only when the leaf was destarched first and suitable controls were used.",
            "A covered part of a destarched leaf receives no light and does not turn blue-black after the starch test.",
            "A plant kept without carbon dioxide does not form starch even when it receives light.",
            "Design four conditions to separate the effects of light and carbon dioxide."
        ),
        "Xylem Transport" to seed(
            "Xylem carries water and dissolved minerals mainly upward from roots to stems and leaves. Xylem vessels form long tubes through the plant.",
            "Root hairs absorb water from soil. Water loss through stomata helps pull a continuous column of water upward through xylem.",
            "Transport rate changes with temperature, wind, humidity and leaf area because these affect water loss. Other forces also help water enter and move through a plant.",
            "A white flower placed in coloured water can develop coloured veins or petals.",
            "Celery in coloured water shows the path of water through xylem.",
            "Compare equal shoots in warm and cool conditions while controlling light, airflow and water supply."
        ),
        "Phloem Transport" to seed(
            "Phloem carries dissolved sugars and other organic substances from sources, such as photosynthesising leaves, to places that use or store them.",
            "Growing roots, fruits, seeds and young leaves can receive food through phloem. Movement can be upward or downward depending on source and need.",
            "Xylem and phloem are different: xylem mainly transports water and minerals upward, while phloem transports food between sources and sinks.",
            "Sugar made in a leaf can move to a growing fruit.",
            "Stored food in a seed can support a young shoot before its leaves are fully active.",
            "Predict how removing a complete ring of bark containing phloem would affect food movement."
        ),
        "Respiration in Plants" to seed(
            "Plants respire all the time. In aerobic respiration, cells use oxygen to break down glucose, releasing carbon dioxide, water and usable energy for growth, repair and transport.",
            "Photosynthesis and respiration are different. Photosynthesis stores energy and requires light in green cells; respiration releases usable energy and occurs in living plant cells day and night.",
            "During daylight, a green plant may photosynthesise faster than it respires, so its net gas exchange can show carbon-dioxide uptake and oxygen release.",
            "Germinating seeds release carbon dioxide that turns lime water milky.",
            "Roots need oxygen in soil for respiration, so waterlogged soil can harm many plants.",
            "Compare photosynthesis and respiration by location, timing, inputs, outputs and energy change."
        )
    )
}
