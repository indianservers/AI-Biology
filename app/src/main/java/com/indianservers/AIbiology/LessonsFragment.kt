package com.indianservers.AIbiology

import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.indianservers.AIbiology.data.Lesson
import com.indianservers.AIbiology.data.LessonArea
import com.indianservers.AIbiology.data.LessonCatalog
import com.indianservers.AIbiology.data.LessonLevel
import com.indianservers.AIbiology.databinding.FragmentLessonsBinding
import kotlin.math.roundToInt

/**
 * Complete content-driven Lessons flow.
 *
 * All visible controls are functional. Lesson imagery is intentionally omitted
 * until reviewed topic-specific assets are supplied.
 */
class LessonsFragment : Fragment() {
    private var _binding: FragmentLessonsBinding? = null
    private val binding get() = _binding!!

    private enum class Screen { HOME, EXPLORER, OVERVIEW, LESSON_LIST, READER, QUIZ }

    private var screen = Screen.HOME
    private val history = ArrayDeque<Screen>()
    private var selectedLevel: LessonLevel? = null
    private var selectedArea: LessonArea = LessonCatalog.areas.first { it.number == 3 }
    private var selectedLesson: Lesson = LessonCatalog.lessons(selectedArea).first()
    private var readerTab = 0
    private var explorerQuery = ""
    private var quizLessons: List<Lesson> = emptyList()
    private var quizIndex = 0
    private var quizScore = 0
    private var quizSelectedAnswer: String? = null
    private var quizAnswered = false

    private val preferences by lazy {
        requireContext().getSharedPreferences("lesson_progress", 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLessonsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.lessonScreenRoot) { root, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            root.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.lessonScreenRoot)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = goBack()
            }
        )
        render()
    }

    private fun render() {
        binding.lessonScreenRoot.removeAllViews()
        val page = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(NAVY)
        }
        page.addView(topBar(), matchWrap())
        page.addView(
            ScrollView(requireContext()).apply {
                isFillViewport = true
                clipToPadding = false
                overScrollMode = View.OVER_SCROLL_NEVER
                addView(
                    LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(18.dp, 18.dp, 18.dp, 30.dp)
                        when (screen) {
                            Screen.HOME -> renderHome(this)
                            Screen.EXPLORER -> renderExplorer(this)
                            Screen.OVERVIEW -> renderOverview(this)
                            Screen.LESSON_LIST -> renderLessonList(this)
                            Screen.READER -> renderReader(this)
                            Screen.QUIZ -> renderQuiz(this)
                        }
                    }
                )
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        page.addView(bottomNavigation(), matchHeight(66.dp))
        binding.lessonScreenRoot.addView(page, matchMatch())
    }

    private fun topBar(): View = LinearLayout(requireContext()).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(14.dp, 8.dp, 14.dp, 8.dp)
        background = solid(DARK_PANEL)

        addView(
            actionButton(if (screen == Screen.HOME) "Exit" else "Back", compact = true) {
                if (screen == Screen.HOME) {
                    findNavController().popBackStack()
                } else {
                    goBack()
                }
            },
            LinearLayout.LayoutParams(76.dp, 48.dp)
        )
        addView(
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12.dp, 0, 8.dp, 0)
                addView(
                    label(
                        when (screen) {
                            Screen.HOME -> "Lessons"
                            Screen.EXPLORER -> "Topic Explorer"
                            Screen.OVERVIEW -> selectedArea.title
                            Screen.LESSON_LIST -> "Lesson Library"
                            Screen.READER -> selectedLesson.title
                            Screen.QUIZ -> "Quick Practice"
                        },
                        19f,
                        Color.WHITE,
                        true
                    )
                )
                addView(
                    label(
                        when (screen) {
                            Screen.HOME -> "Learn from Grade 6 to postgraduate"
                            Screen.EXPLORER -> "Choose a topic and start learning"
                            Screen.OVERVIEW -> "Chapter ${selectedArea.number} overview"
                            Screen.LESSON_LIST -> "${selectedArea.concepts.size} lessons"
                            Screen.READER -> selectedArea.title
                            Screen.QUIZ -> selectedArea.title
                        },
                        11f,
                        MUTED
                    )
                )
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
    }

    private fun bottomNavigation(): View = LinearLayout(requireContext()).apply {
        gravity = Gravity.CENTER
        setPadding(8.dp, 7.dp, 8.dp, 7.dp)
        background = solid(DARK_PANEL)
        addView(navButton("Home", screen == Screen.HOME) { open(Screen.HOME) }, weighted())
        addView(
            navButton("Topics", screen == Screen.EXPLORER) { open(Screen.EXPLORER) },
            weighted()
        )
        addView(
            navButton("Learn", screen == Screen.READER || screen == Screen.LESSON_LIST) {
                openLesson(continueLesson())
            },
            weighted()
        )
        addView(navButton("Practice", screen == Screen.QUIZ) { startQuiz() }, weighted())
    }

    private fun renderHome(content: LinearLayout) {
        content.addView(label("Hello, Learner", 14f, MINT))
        content.addView(label("Explore Biology", 30f, Color.WHITE, true).top(3))
        content.addView(
            label(
                "Build strong basics, connect ideas, and then go deeper.",
                14f,
                MUTED
            ).top(5)
        )

        content.addView(
            card(accent = PURPLE).apply {
                addView(label("LEARN STEP BY STEP", 12f, PALE_PURPLE, true))
                addView(label("From living things to advanced research", 23f, Color.WHITE, true).top(8))
                addView(
                    label(
                        "${LessonCatalog.areas.size} topics  |  ${LessonCatalog.conceptCount} lessons  |  5 learning levels",
                        13f,
                        Color.rgb(211, 219, 239)
                    ).top(8)
                )
                addView(primaryButton("Explore all topics") { open(Screen.EXPLORER) }.top(16))
            }.top(20)
        )

        sectionTitle(content, "Continue learning", "Your next unfinished lesson")
        val next = continueLesson()
        content.addView(
            clickableCard {
                selectedArea = next.area
                selectedLesson = next
                open(Screen.READER)
            }.apply {
                addView(label(next.title, 18f, Color.WHITE, true))
                addView(label("Chapter ${next.area.number}  |  ${next.area.title}", 12f, MUTED).top(5))
                addView(progressBar(areaProgress(next.area)).top(14))
                addView(
                    label(
                        "${areaProgress(next.area)}% complete - Continue",
                        12f,
                        MINT,
                        true
                    ).top(8)
                )
            }
        )

        sectionTitle(content, "Browse by level", "Choose the depth that suits you")
        LessonLevel.entries.forEach { level ->
            val count = areasFor(level).sumOf { it.concepts.size }
            content.addView(
                outlineButton("${level.label}   |   $count lessons") {
                    selectedLevel = level
                    open(Screen.EXPLORER)
                }.top(8)
            )
        }

        sectionTitle(content, "Popular topics", "Open a chapter overview")
        listOf(3, 9, 17, 14, 21, 10).map { number ->
            LessonCatalog.areas.first { it.number == number }
        }.forEach { area ->
            content.addView(areaRow(area) {
                selectedArea = area
                open(Screen.OVERVIEW)
            })
        }
    }

    private fun renderExplorer(content: LinearLayout) {
        val search = EditText(requireContext()).apply {
            hint = "Search topics or lessons"
            setHintTextColor(MUTED)
            setTextColor(Color.WHITE)
            textSize = 14f
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            background = rounded(SURFACE, 14f, BORDER)
            setPadding(16.dp, 0, 16.dp, 0)
            setText(explorerQuery)
            setSelection(text.length)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    explorerQuery = s?.toString().orEmpty()
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    render()
                    true
                } else false
            }
        }
        content.addView(search, matchHeight(52.dp))
        content.addView(label("FILTER BY LEVEL", 11f, MUTED, true).top(18))
        content.addView(
            HorizontalScrollView(requireContext()).apply {
                isHorizontalScrollBarEnabled = false
                addView(
                    LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        addView(filterButton("All", selectedLevel == null) {
                            selectedLevel = null
                            render()
                        })
                        LessonLevel.entries.forEach { level ->
                            addView(filterButton(level.shortLabel, selectedLevel == level) {
                                selectedLevel = level
                                render()
                            }.start(7))
                        }
                    }
                )
            },
            matchHeight(46.dp).topMargin(7.dp)
        )

        val query = explorerQuery.trim()
        val allowed = if (selectedLevel == null) LessonCatalog.areas else areasFor(selectedLevel!!)
        val matchingAreas = allowed.filter { area ->
            query.isBlank() ||
                area.title.contains(query, true) ||
                area.concepts.any { it.contains(query, true) }
        }
        content.addView(
            label(
                "${matchingAreas.size} topics - ${matchingAreas.sumOf { it.concepts.size }} lessons",
                12f,
                MUTED
            ).top(12)
        )
        matchingAreas.forEach { area ->
            content.addView(areaRow(area) {
                selectedArea = area
                selectedLesson = continueLesson(area)
                open(Screen.OVERVIEW)
            })
        }
        if (matchingAreas.isEmpty()) {
            content.addView(
                label("No topic matches \"$query\".", 15f, MUTED).apply {
                    gravity = Gravity.CENTER
                    setPadding(10.dp, 50.dp, 10.dp, 50.dp)
                }
            )
        }
    }

    private fun renderOverview(content: LinearLayout) {
        val lessons = LessonCatalog.lessons(selectedArea)
        val first = lessons.first()
        content.addView(
            card(accent = BLUE).apply {
                addView(label("CHAPTER ${selectedArea.number}", 12f, MINT, true))
                addView(label(selectedArea.title, 28f, Color.WHITE, true).top(7))
                addView(label(selectedArea.caption, 15f, PALE_PURPLE, true).top(9))
                addView(label(first.phases.first().explanation, 14f, PALE_TEXT).top(12))
                addView(
                    label(
                        "${lessons.size} lessons  |  About ${totalReadMinutes(lessons)} min",
                        13f,
                        PALE_PURPLE,
                        true
                    ).top(14)
                )
            }
        )

        sectionTitle(content, "Your progress", "${completedCount(selectedArea)} of ${lessons.size} completed")
        content.addView(progressBar(areaProgress(selectedArea)))
        content.addView(
            primaryButton(if (areaProgress(selectedArea) == 0) "Start chapter" else "Continue learning") {
                openLesson(continueLesson(selectedArea))
            }.top(16)
        )
        content.addView(outlineButton("View all lessons") { open(Screen.LESSON_LIST) }.top(10))
        content.addView(outlineButton("Start quick practice") { startQuiz() }.top(10))

        sectionTitle(content, "What you will learn", "Real curriculum content")
        selectedArea.concepts.take(6).forEachIndexed { index, title ->
            content.addView(
                label("${index + 1}.  $title", 15f, PALE_TEXT, index == 0).apply {
                    setPadding(14.dp, 13.dp, 14.dp, 13.dp)
                    background = rounded(SURFACE, 12f, BORDER)
                }.top(7)
            )
        }
        if (selectedArea.concepts.size > 6) {
            content.addView(
                outlineButton("See ${selectedArea.concepts.size - 6} more lessons") {
                    open(Screen.LESSON_LIST)
                }.top(10)
            )
        }
    }

    private fun renderLessonList(content: LinearLayout) {
        content.addView(label(selectedArea.title, 27f, Color.WHITE, true))
        content.addView(
            label(
                "${selectedArea.concepts.size} text lessons - simple to advanced",
                13f,
                MUTED
            ).top(5)
        )
        content.addView(progressBar(areaProgress(selectedArea)).top(16))
        var visibleSubtopic: String? = null
        LessonCatalog.lessons(selectedArea).forEachIndexed { index, lesson ->
            if (lesson.subtopic != visibleSubtopic) {
                visibleSubtopic = lesson.subtopic
                content.addView(label(lesson.subtopic, 19f, Color.WHITE, true).top(24))
                content.addView(label(lesson.subtopicCaption, 12f, PALE_PURPLE).top(4).bottom(4))
            }
            val complete = isComplete(lesson)
            content.addView(
                clickableCard {
                    openLesson(lesson)
                }.apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(
                        lessonBadge(lesson, index, complete),
                        LinearLayout.LayoutParams(44.dp, 44.dp)
                    )
                    addView(
                        LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(13.dp, 0, 6.dp, 0)
                            addView(label(lesson.title, 16f, Color.WHITE, true))
                            addView(label(lesson.subtitle, 12f, MUTED).top(4))
                            addView(
                                label(
                                    "${readMinutes(lesson)} min read  |  ${if (complete) "Completed" else "Ready to learn"}",
                                    12f,
                                    if (complete) MINT else MUTED
                                ).top(5)
                            )
                        },
                        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    )
                    addView(label("Open", 12f, PALE_PURPLE, true))
                }
            )
        }
    }

    private fun renderReader(content: LinearLayout) {
        content.addView(label("CHAPTER ${selectedArea.number} - ${selectedArea.title.uppercase()}", 11f, MINT, true))
        content.addView(label(selectedLesson.title, 30f, Color.WHITE, true).top(7))
        content.addView(label(selectedLesson.subtitle, 14f, PALE_PURPLE).top(6))
        content.addView(
            label(
                "${readMinutes(selectedLesson)} min read  |  ${if (isComplete(selectedLesson)) "Completed" else "In progress"}",
                12f,
                MUTED
            ).top(6)
        )

        content.addView(
            HorizontalScrollView(requireContext()).apply {
                isHorizontalScrollBarEnabled = false
                addView(
                    LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        listOf(
                            "Learn",
                            "Why it matters",
                            "Examples",
                            "Advanced",
                            "Exam Focus",
                            "Infographics"
                        ).forEachIndexed { index, title ->
                            addView(filterButton(title, readerTab == index) {
                                readerTab = index
                                render()
                            }.apply {
                                if (index > 0) (layoutParams as? ViewGroup.MarginLayoutParams)?.marginStart = 7.dp
                            })
                        }
                    }
                )
            },
            matchHeight(50.dp).topMargin(18.dp)
        )

        when (readerTab) {
            0 -> readerLearn(content)
            1 -> readerWhy(content)
            2 -> readerExamples(content)
            3 -> readerAdvanced(content)
            4 -> readerExamFocus(content)
            else -> readerInfographics(content)
        }

        content.addView(
            primaryButton(if (isComplete(selectedLesson)) "Completed - mark unfinished" else "Mark lesson complete") {
                setComplete(selectedLesson, !isComplete(selectedLesson))
                render()
            }.top(22)
        )
        content.addView(outlineButton("Practice this chapter") { startQuiz(selectedLesson) }.top(10))
        content.addView(
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(
                    outlineButton("Previous lesson") { moveLesson(-1) },
                    weighted().endMargin(5.dp)
                )
                addView(
                    primaryButton("Next lesson") { moveLesson(1) },
                    weighted().startMargin(5.dp)
                )
            }.top(10),
            matchWrap()
        )
    }

    private fun readerLearn(content: LinearLayout) {
        content.addView(
            card(accent = DEEP_GREEN).apply {
                addView(label("AFTER THIS LESSON, YOU WILL BE ABLE TO", 11f, MINT, true))
                selectedLesson.learningOutcomes.forEach { outcome ->
                    addView(label("•  $outcome", 14f, PALE_TEXT).top(9))
                }
            }.top(14)
        )
        lessonBlock(
            content,
            "START SIMPLE",
            selectedLesson.phases[0].explanation,
            MINT
        )
        lessonMedia(
            content,
            assetPath("inline-01.webp"),
            "Inline lesson image 1",
            "A lesson-specific image will appear here between the explanation and connection."
        )
        lessonBlock(
            content,
            "UNDERSTAND THE CONNECTION",
            selectedLesson.phases[1].explanation,
            PALE_PURPLE
        )
        lessonMedia(
            content,
            assetPath("inline-02.webp"),
            "Inline lesson image 2",
            "A second lesson-specific image will appear here beside the deeper explanation."
        )
        lessonBlock(
            content,
            "EASY WAY TO LEARN",
            selectedLesson.learningContent.easyWayToLearn.joinToString("\n\n"),
            AMBER
        )
    }

    private fun readerWhy(content: LinearLayout) {
        lessonBlock(
            content,
            "WHY THIS LESSON IS IMPORTANT",
            selectedLesson.phases[1].explanation,
            MINT
        )
        lessonMedia(
            content,
            assetPath("inline-03.webp"),
            "Evidence and importance",
            "A concept-specific evidence or importance image will appear here."
        )
        lessonBlock(
            content,
            "USEFUL FACT",
            selectedLesson.phases[1].didYouKnow,
            AMBER
        )
        lessonBlock(
            content,
            "COMMON MISTAKE",
            selectedLesson.learningContent.commonMistake,
            PALE_PURPLE
        )
    }

    private fun readerExamples(content: LinearLayout) {
        selectedLesson.learningContent.realLifeExamples.forEachIndexed { index, example ->
            lessonBlock(content, "EXAMPLE ${index + 1}", example, listOf(MINT, AMBER, PALE_PURPLE)[index])
            if (index < 2) {
                lessonMedia(
                    content,
                    assetPath("inline-${String.format("%02d", index + 4)}.webp"),
                    "Example visual ${index + 1}",
                    "A realistic image supporting this exact example will appear here."
                )
            }
        }
    }

    private fun readerAdvanced(content: LinearLayout) {
        lessonBlock(
            content,
            "GO ADVANCED",
            selectedLesson.phases[2].explanation,
            PALE_PURPLE
        )
        lessonMedia(
            content,
            assetPath("inline-06.webp"),
            "Advanced concept view",
            "A detailed mechanism, comparison, or research-evidence image will appear here."
        )
        lessonBlock(
            content,
            "ADVANCED EXAMPLE",
            selectedLesson.phases[2].example,
            MINT
        )
        lessonBlock(
            content,
            "RESEARCH NOTE",
            selectedLesson.phases[2].didYouKnow,
            AMBER
        )
    }

    private fun readerExamFocus(content: LinearLayout) {
        content.addView(
            card(accent = AMBER).apply {
                addView(label("IMPORTANT POINTS TO REMEMBER", 11f, AMBER, true))
                addView(
                    label(
                        "Revise these lesson-specific points before an exam.",
                        13f,
                        MUTED
                    ).top(6)
                )
                selectedLesson.learningContent.importantPoints.forEachIndexed { index, point ->
                    addView(
                        label(
                            "${index + 1}.  $point",
                            14f,
                            PALE_TEXT,
                            index < 3
                        ).top(11)
                    )
                }
            }.top(14)
        )
        lessonBlock(
            content,
            "HOW TO WRITE A HIGH-SCORING ANSWER",
            listOf(
                "1. Begin with a clear definition of ${selectedLesson.title}.",
                "2. Explain the main process, structure, or cause-and-effect link.",
                "3. Use the correct biological terms from the lesson.",
                "4. Add one accurate example and explain how it supports your answer.",
                "5. For a long answer, include the advanced point and state relevant evidence or limitations."
            ).joinToString("\n\n"),
            MINT
        )
        lessonBlock(
            content,
            "PRACTICE EXAM QUESTION",
            selectedLesson.learningContent.quickCheckQuestion,
            PALE_PURPLE
        )
        lessonBlock(
            content,
            "MODEL ANSWER",
            selectedLesson.learningContent.quickCheckAnswer,
            MINT
        )
        lessonBlock(
            content,
            "AVOID THIS COMMON MISTAKE",
            selectedLesson.learningContent.commonMistake,
            AMBER
        )
    }

    private fun readerInfographics(content: LinearLayout) {
        lessonBlock(
            content,
            "INFOGRAPHICS",
            "Infographics are kept separate from lesson images. They summarize the full concept in one visual and may contain reviewed labels, arrows, comparisons, or a process sequence.",
            MINT
        )
        lessonMedia(
            content,
            assetPath("infographic-01.webp"),
            "Concept infographic",
            "The reviewed infographic for ${selectedLesson.title} will appear here."
        )
    }

    private fun assetPath(fileName: String): String =
        "biology/lessons/${selectedArea.id}/${selectedLesson.id.substringAfter('.')}/$fileName"

    private fun lessonMedia(
        content: LinearLayout,
        path: String,
        title: String,
        emptyMessage: String
    ) {
        val bitmap = runCatching {
            requireContext().assets.open(path).use(BitmapFactory::decodeStream)
        }.getOrNull()
        content.addView(
            card().apply {
                addView(label(title.uppercase(), 11f, MINT, true))
                if (bitmap != null) {
                    addView(
                        ImageView(requireContext()).apply {
                            setImageBitmap(bitmap)
                            adjustViewBounds = true
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            contentDescription = "$title for ${selectedLesson.title}"
                        },
                        matchHeight(ViewGroup.LayoutParams.WRAP_CONTENT).topMargin(10.dp)
                    )
                } else {
                    addView(label(emptyMessage, 14f, MUTED).top(9))
                    addView(label("Expected asset: $path", 10f, Color.rgb(91, 118, 143)).top(8))
                }
            }.top(14)
        )
    }

    private fun renderQuiz(content: LinearLayout) {
        if (quizLessons.isEmpty()) prepareQuiz()
        if (quizIndex >= quizLessons.size) {
            renderQuizResult(content)
            return
        }
        val questionLesson = quizLessons[quizIndex]
        val options = quizOptions(questionLesson)
        content.addView(
            label(
                "QUESTION ${quizIndex + 1} OF ${quizLessons.size}",
                12f,
                MINT,
                true
            )
        )
        content.addView(progressBar(((quizIndex.toFloat() / quizLessons.size) * 100).roundToInt()).top(9))
        content.addView(
            label(
                "Which description best explains \"${questionLesson.title}\"?",
                22f,
                Color.WHITE,
                true
            ).top(24)
        )
        options.forEachIndexed { index, option ->
            val isCorrect = option == questionLesson.phases[0].explanation
            val chosen = option == quizSelectedAnswer
            val color = when {
                quizAnswered && isCorrect -> GREEN
                quizAnswered && chosen -> RED
                else -> SURFACE
            }
            content.addView(
                MaterialButton(requireContext()).apply {
                    text = "${('A'.code + index).toChar()}   $option"
                    textSize = 13f
                    isAllCaps = false
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    setTextColor(Color.WHITE)
                    backgroundTintList = ColorStateList.valueOf(color)
                    strokeColor = ColorStateList.valueOf(
                        if (quizAnswered && isCorrect) MINT else BORDER
                    )
                    strokeWidth = 1.dp
                    cornerRadius = 13.dp
                    setPadding(14.dp, 8.dp, 14.dp, 8.dp)
                    isEnabled = !quizAnswered
                    setOnClickListener {
                        quizSelectedAnswer = option
                        quizAnswered = true
                        if (isCorrect) quizScore++
                        render()
                    }
                },
                matchHeight(ViewGroup.LayoutParams.WRAP_CONTENT).topMargin(10.dp)
            )
        }
        if (quizAnswered) {
            val correct = quizSelectedAnswer == questionLesson.phases[0].explanation
            lessonBlock(
                content,
                if (correct) "CORRECT" else "LEARN FROM THIS",
                questionLesson.phases[0].explanation,
                if (correct) MINT else AMBER
            )
            content.addView(
                primaryButton(if (quizIndex == quizLessons.lastIndex) "See result" else "Next question") {
                    quizIndex++
                    quizAnswered = false
                    quizSelectedAnswer = null
                    render()
                }.top(14)
            )
        }
    }

    private fun renderQuizResult(content: LinearLayout) {
        val percent = if (quizLessons.isEmpty()) 0 else quizScore * 100 / quizLessons.size
        content.addView(label("PRACTICE COMPLETE", 12f, MINT, true))
        content.addView(label("$quizScore / ${quizLessons.size}", 48f, Color.WHITE, true).top(10))
        content.addView(
            label(
                when {
                    percent >= 80 -> "Strong work. You understand this chapter well."
                    percent >= 60 -> "Good progress. Review the missed ideas once more."
                    else -> "Keep learning. Read the simple explanations and try again."
                },
                17f,
                PALE_TEXT
            ).top(8)
        )
        content.addView(progressBar(percent).top(22))
        content.addView(primaryButton("Try again") { startQuiz() }.top(22))
        content.addView(outlineButton("Review lessons") { open(Screen.LESSON_LIST) }.top(10))
        content.addView(outlineButton("Chapter overview") { open(Screen.OVERVIEW) }.top(10))
    }

    private fun sectionTitle(content: LinearLayout, title: String, subtitle: String) {
        content.addView(label(title, 19f, Color.WHITE, true).top(24))
        content.addView(label(subtitle, 12f, MUTED).top(3).bottom(10))
    }

    private fun lessonBlock(
        content: LinearLayout,
        title: String,
        body: String,
        accent: Int
    ) {
        content.addView(
            card().apply {
                addView(label(title, 11f, accent, true))
                addView(label(body.removePrefix("Example: ").trim(), 15f, PALE_TEXT).top(9))
            }.top(14)
        )
    }

    private fun areaRow(area: LessonArea, action: () -> Unit): View =
        clickableCard(action).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(
                topicBadge(area),
                LinearLayout.LayoutParams(48.dp, 48.dp)
            )
            addView(
                LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(13.dp, 0, 5.dp, 0)
                    addView(label(area.title, 16f, Color.WHITE, true))
                    addView(label(area.caption, 11f, PALE_PURPLE).top(4))
                    addView(
                        label(
                            "${area.concepts.size} lessons  |  ${area.startingLevel.label}",
                            11f,
                            MUTED
                        ).top(4)
                    )
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
            addView(label("View", 12f, PALE_PURPLE, true))
        }

    private fun topicBadge(area: LessonArea): View {
        val resource = if (area.number <= 36) {
            resources.getIdentifier(
                "lesson_topic_${area.number.toString().padStart(2, '0')}",
                "drawable",
                requireContext().packageName
            )
        } else {
            0
        }
        return if (resource != 0) {
            ImageView(requireContext()).apply {
                setImageResource(resource)
                scaleType = ImageView.ScaleType.FIT_CENTER
                contentDescription = "${area.title} category icon"
                setPadding(2.dp, 2.dp, 2.dp, 2.dp)
            }
        } else {
            label(area.number.toString().padStart(2, '0'), 15f, MINT, true).apply {
                gravity = Gravity.CENTER
                background = solid(DEEP_GREEN, 14f)
            }
        }
    }

    private fun lessonBadge(lesson: Lesson, index: Int, complete: Boolean): View {
        val direct = lesson.title.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
        val alias = when (direct) {
            "viruses" -> "virus"
            "fungal_diversity",
            "fungal_classification",
            "pathogenic_fungi",
            "industrial_fungi" -> "fungi"
            "dna_structure", "rna" -> "dna_and_rna"
            "stem_cells" ->
                if (lesson.area.number == 19) "development_stem_cells"
                else "biotech_stem_cells"
            "histology" -> "histology_slides"
            else -> direct
        }
        val resource = resources.getIdentifier(
            "lesson_subtopic_$alias",
            "drawable",
            requireContext().packageName
        )
        return if (resource != 0) {
            ImageView(requireContext()).apply {
                setImageResource(resource)
                scaleType = ImageView.ScaleType.FIT_CENTER
                contentDescription = "${lesson.title} lesson icon"
                background = solid(if (complete) DEEP_GREEN else Color.TRANSPARENT, 10f)
                setPadding(3.dp, 3.dp, 3.dp, 3.dp)
            }
        } else {
            label((index + 1).toString(), 16f, if (complete) NAVY else MINT, true).apply {
                gravity = Gravity.CENTER
                background = solid(if (complete) MINT else DEEP_GREEN, 12f)
            }
        }
    }

    private fun card(accent: Int? = null): LinearLayout =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18.dp, 18.dp, 18.dp, 18.dp)
            background = if (accent == null) {
                rounded(SURFACE, 18f, BORDER)
            } else {
                GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(accent, SURFACE, DARK_PANEL)
                ).apply { cornerRadius = 18.dp.toFloat() }
            }
        }

    private fun clickableCard(action: () -> Unit): LinearLayout =
        card().apply {
            isClickable = true
            isFocusable = true
            foreground = requireContext().getDrawable(android.R.drawable.list_selector_background)
            setOnClickListener { action() }
            layoutParams = matchWrap().topMargin(9.dp)
        }

    private fun progressBar(progress: Int): View =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            background = solid(Color.rgb(39, 55, 79), 4f)
            addView(
                Space(requireContext()).apply { background = solid(MINT, 4f) },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, progress.coerceIn(0, 100).toFloat())
            )
            addView(
                Space(requireContext()),
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (100 - progress.coerceIn(0, 100)).toFloat())
            )
        }.apply { layoutParams = matchHeight(7.dp) }

    private fun label(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView = TextView(requireContext()).apply {
        text = value
        textSize = size
        setTextColor(color)
        setLineSpacing(2.dp.toFloat(), 1f)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun primaryButton(title: String, action: () -> Unit): MaterialButton =
        MaterialButton(requireContext()).apply {
            text = title
            textSize = 14f
            isAllCaps = false
            setTextColor(Color.rgb(3, 27, 26))
            backgroundTintList = ColorStateList.valueOf(MINT)
            cornerRadius = 13.dp
            minHeight = 50.dp
            setOnClickListener { action() }
        }

    private fun outlineButton(title: String, action: () -> Unit): MaterialButton =
        MaterialButton(requireContext()).apply {
            text = title
            textSize = 14f
            isAllCaps = false
            setTextColor(PALE_TEXT)
            backgroundTintList = ColorStateList.valueOf(SURFACE)
            strokeColor = ColorStateList.valueOf(BORDER)
            strokeWidth = 1.dp
            cornerRadius = 13.dp
            minHeight = 50.dp
            setOnClickListener { action() }
        }

    private fun actionButton(title: String, compact: Boolean, action: () -> Unit): MaterialButton =
        outlineButton(title, action).apply {
            if (compact) {
                minWidth = 0
                setPadding(8.dp, 0, 8.dp, 0)
            }
        }

    private fun navButton(title: String, active: Boolean, action: () -> Unit): MaterialButton =
        MaterialButton(requireContext()).apply {
            text = title
            textSize = 12f
            isAllCaps = false
            minWidth = 0
            setPadding(4.dp, 0, 4.dp, 0)
            setTextColor(if (active) MINT else MUTED)
            backgroundTintList = ColorStateList.valueOf(
                if (active) DEEP_GREEN else Color.TRANSPARENT
            )
            cornerRadius = 12.dp
            setOnClickListener { action() }
        }

    private fun filterButton(title: String, active: Boolean, action: () -> Unit): MaterialButton =
        MaterialButton(requireContext()).apply {
            text = title
            textSize = 12f
            isAllCaps = false
            minWidth = 66.dp
            minHeight = 40.dp
            setPadding(13.dp, 0, 13.dp, 0)
            setTextColor(if (active) Color.rgb(3, 27, 26) else PALE_TEXT)
            backgroundTintList = ColorStateList.valueOf(if (active) MINT else SURFACE)
            strokeColor = ColorStateList.valueOf(if (active) MINT else BORDER)
            strokeWidth = 1.dp
            cornerRadius = 12.dp
            setOnClickListener { action() }
        }

    private fun open(target: Screen) {
        if (target == screen) {
            render()
            return
        }
        history.addLast(screen)
        screen = target
        render()
    }

    private fun openLesson(lesson: Lesson) {
        selectedArea = lesson.area
        selectedLesson = lesson
        readerTab = 0
        open(Screen.READER)
    }

    private fun goBack() {
        if (history.isNotEmpty()) {
            screen = history.removeLast()
            render()
        } else {
            findNavController().popBackStack()
        }
    }

    private fun moveLesson(offset: Int) {
        val lessons = LessonCatalog.lessons(selectedArea)
        val current = lessons.indexOfFirst { it.id == selectedLesson.id }.coerceAtLeast(0)
        val target = (current + offset).coerceIn(0, lessons.lastIndex)
        selectedLesson = lessons[target]
        readerTab = 0
        render()
    }

    private fun startQuiz(focus: Lesson? = null) {
        if (focus != null) selectedLesson = focus
        prepareQuiz()
        open(Screen.QUIZ)
    }

    private fun prepareQuiz() {
        val all = LessonCatalog.lessons(selectedArea)
        val start = all.indexOfFirst { it.id == selectedLesson.id }.coerceAtLeast(0)
        quizLessons = (all.drop(start) + all.take(start)).take(5)
        quizIndex = 0
        quizScore = 0
        quizSelectedAnswer = null
        quizAnswered = false
    }

    private fun quizOptions(correct: Lesson): List<String> {
        val pool = LessonCatalog.lessons(selectedArea)
            .filter { it.id != correct.id }
            .sortedBy { "${correct.id}:${it.id}".hashCode() }
            .take(3)
            .map { it.phases[0].explanation }
        return (pool + correct.phases[0].explanation)
            .distinct()
            .sortedBy { "${correct.id}:$it".hashCode() }
    }

    private fun continueLesson(area: LessonArea = selectedArea): Lesson =
        LessonCatalog.lessons(area).firstOrNull { !isComplete(it) }
            ?: LessonCatalog.lessons(area).first()

    private fun completedCount(area: LessonArea): Int =
        LessonCatalog.lessons(area).count(::isComplete)

    private fun areaProgress(area: LessonArea): Int {
        val lessons = LessonCatalog.lessons(area)
        return if (lessons.isEmpty()) 0 else completedCount(area) * 100 / lessons.size
    }

    private fun isComplete(lesson: Lesson): Boolean =
        preferences.getBoolean("complete.${lesson.id}", false)

    private fun setComplete(lesson: Lesson, complete: Boolean) {
        preferences.edit().putBoolean("complete.${lesson.id}", complete).apply()
    }

    private fun readMinutes(lesson: Lesson): Int =
        (lesson.learningContent.detailedExplanation.split(Regex("\\s+")).size / 115)
            .coerceAtLeast(6)

    private fun totalReadMinutes(lessons: List<Lesson>): Int = lessons.sumOf(::readMinutes)

    private fun areasFor(level: LessonLevel): List<LessonArea> =
        LessonCatalog.areas.filter { it.startingLevel.ordinal <= level.ordinal }

    private fun solid(color: Int, radius: Float = 0f): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.dp
        }

    private fun rounded(color: Int, radius: Float, stroke: Int): GradientDrawable =
        solid(color, radius).apply { setStroke(1.dp, stroke) }

    private fun matchMatch() = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun matchHeight(height: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        height
    )

    private fun weighted() = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)

    private fun LinearLayout.LayoutParams.topMargin(value: Int) = apply { topMargin = value }
    private fun LinearLayout.LayoutParams.startMargin(value: Int) = apply { marginStart = value }
    private fun LinearLayout.LayoutParams.endMargin(value: Int) = apply { marginEnd = value }
    private fun <T : View> T.top(value: Int) = apply {
        layoutParams = (layoutParams as? LinearLayout.LayoutParams ?: matchWrap()).apply {
            topMargin = value.dp
        }
    }
    private fun <T : View> T.bottom(value: Int) = apply {
        layoutParams = (layoutParams as? LinearLayout.LayoutParams ?: matchWrap()).apply {
            bottomMargin = value.dp
        }
    }
    private fun <T : View> T.start(value: Int) = apply {
        layoutParams = (layoutParams as? LinearLayout.LayoutParams
            ?: LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )).apply { marginStart = value.dp }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).roundToInt()

    private val Float.dp: Float
        get() = this * resources.displayMetrics.density

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        val NAVY = Color.rgb(4, 16, 31)
        val DARK_PANEL = Color.rgb(7, 25, 43)
        val SURFACE = Color.rgb(11, 34, 54)
        val BORDER = Color.rgb(38, 65, 88)
        val MINT = Color.rgb(100, 235, 160)
        val GREEN = Color.rgb(18, 112, 73)
        val DEEP_GREEN = Color.rgb(14, 62, 55)
        val RED = Color.rgb(128, 43, 56)
        val MUTED = Color.rgb(139, 162, 187)
        val PALE_TEXT = Color.rgb(220, 232, 242)
        val PALE_PURPLE = Color.rgb(193, 174, 255)
        val PURPLE = Color.rgb(67, 46, 137)
        val BLUE = Color.rgb(18, 70, 118)
        val AMBER = Color.rgb(255, 191, 111)
    }
}
