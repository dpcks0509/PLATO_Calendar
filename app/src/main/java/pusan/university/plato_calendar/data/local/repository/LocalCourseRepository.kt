package pusan.university.plato_calendar.data.local.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import pusan.university.plato_calendar.domain.repository.CourseRepository
import pusan.university.plato_calendar.presentation.common.extension.formatCourseCode
import javax.inject.Inject

private const val UNKNOWN_COURSE_NAME = "알 수 없는 교과목"

class LocalCourseRepository
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val json: Json,
    ) : CourseRepository {
        private val courses: Map<String, String> by lazy {
            val jsonString =
                context.assets
                    .open("courses.json")
                    .bufferedReader()
                    .use { it.readText() }

            json.decodeFromString<Map<String, String>>(jsonString)
        }

        override fun getCourseName(courseCode: String): String =
            courses.entries
                .find { course ->
                    course.key.formatCourseCode() == courseCode
                }?.value ?: UNKNOWN_COURSE_NAME

        override fun getCourseCode(courseName: String): String =
            courses.entries
                .find { course ->
                    course.value == courseName
                }?.key
                ?.formatCourseCode()
                .orEmpty()
    }
