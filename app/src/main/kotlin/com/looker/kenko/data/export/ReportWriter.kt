/*
 * Copyright (C) 2026 LooKeR & Contributors
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.looker.kenko.data.export

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.looker.kenko.R
import com.looker.kenko.data.model.Session
import com.looker.kenko.data.model.StatsPeriod
import com.looker.kenko.data.model.StatsSummary
import com.looker.kenko.data.model.formatWeight
import com.looker.kenko.data.model.summarize
import com.looker.kenko.data.model.volume
import com.looker.kenko.ui.exercises.exerciseNameRes
import java.io.OutputStream
import kotlinx.datetime.LocalDate

/**
 * Parts of the report a lifter can leave out. The sets table is the long one.
 */
data class ReportSections(
    val summary: Boolean = true,
    val muscles: Boolean = true,
    val exercises: Boolean = true,
    val sets: Boolean = true,
)

/**
 * Turns a period of the journal into a file: a spreadsheet or a printable page.
 *
 * Everything is written in the language of the phone — a report is read by people, not by the app.
 */
class ReportWriter(private val context: Context) {

    fun fileName(period: StatsPeriod, extension: String): String =
        "kenko-${period.from}-${period.to}.$extension"

    /**
     * Semicolons and a BOM: that is the shape Excel opens without asking questions.
     */
    fun writeCsv(
        output: OutputStream,
        sessions: List<Session>,
        period: StatsPeriod,
        sections: ReportSections,
    ) {
        val summary = sessions.summarize(period)
        val text = buildString {
            append('﻿')
            row(context.getString(R.string.app_name), context.getString(R.string.title_report))
            row(
                context.getString(R.string.label_period_word),
                "${context.getString(R.string.label_period_from)} ${period.from}",
                "${context.getString(R.string.label_period_to)} ${period.to}",
            )
            appendLine()

            if (sections.summary) {
                row(context.getString(R.string.label_section_summary).uppercase())
                row(context.getString(R.string.label_sessions_count), summary.sessions.toString())
                row(context.getString(R.string.label_exercises_of_period), summary.exercises.toString())
                row(context.getString(R.string.label_sets_count), summary.sets.toString())
                row(context.getString(R.string.label_reps_count), summary.reps.toString())
                row(context.getString(R.string.label_total_volume), formatWeight(summary.volume))
                row(
                    context.getString(R.string.label_volume_per_session),
                    formatWeight(summary.volumePerSession),
                )
                appendLine()
            }

            if (sections.muscles) {
                row(context.getString(R.string.label_section_muscles).uppercase())
                row(
                    context.getString(R.string.label_muscle_one),
                    context.getString(R.string.label_total_volume),
                    context.getString(R.string.label_sets_own),
                    context.getString(R.string.label_assist_set),
                    context.getString(R.string.label_reps_count),
                    context.getString(R.string.label_exercises_of_period),
                    "%",
                )
                summary.muscles.filter { it.isTouched }.forEach { load ->
                    row(
                        context.getString(load.muscle.stringRes),
                        formatWeight(load.volume),
                        load.directSets.toString(),
                        load.assistSets.toString(),
                        load.reps.toString(),
                        load.exercises.toString(),
                        ((summary.share(load) * 100).toInt()).toString(),
                    )
                }
                appendLine()
            }

            if (sections.exercises) {
                row(context.getString(R.string.label_section_exercises).uppercase())
                row(
                    context.getString(R.string.label_exercise_one),
                    context.getString(R.string.label_muscle_one),
                    context.getString(R.string.label_total_volume),
                    context.getString(R.string.label_sets_count),
                    context.getString(R.string.label_reps_count),
                    context.getString(R.string.label_top_weight),
                    context.getString(R.string.label_estimated_max),
                    context.getString(R.string.label_sessions_count),
                )
                summary.exerciseLoads.forEach { load ->
                    row(
                        exerciseName(load.exercise.name),
                        context.getString(load.exercise.target.stringRes),
                        formatWeight(load.volume),
                        load.sets.toString(),
                        load.reps.toString(),
                        formatWeight(load.topWeight),
                        formatWeight(load.estimatedMax),
                        load.sessions.toString(),
                    )
                }
                appendLine()
            }

            if (sections.sets) {
                row(context.getString(R.string.label_section_sets).uppercase())
                row(
                    context.getString(R.string.label_date),
                    context.getString(R.string.label_exercise_one),
                    context.getString(R.string.label_muscle_one),
                    context.getString(R.string.label_weight_plain),
                    context.getString(R.string.label_reps_count),
                    context.getString(R.string.label_total_volume),
                    context.getString(R.string.label_set_type_drop),
                )
                sessions.filter { it.date in period }
                    .sortedBy { it.date }
                    .forEach { session ->
                        session.sets.forEach { set ->
                            row(
                                session.date.toString(),
                                exerciseName(set.exercise.name),
                                context.getString(set.exercise.target.stringRes),
                                formatWeight(set.weight),
                                set.repsOrDuration.toString(),
                                formatWeight(set.volume),
                                if (set.parentSetId != null) set.dropIndex.toString() else "",
                            )
                        }
                    }
            }
        }
        output.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    /**
     * A printable page in the app's own hand: one line per row, tables split across pages.
     */
    fun writePdf(
        output: OutputStream,
        sessions: List<Session>,
        period: StatsPeriod,
        sections: ReportSections,
    ) {
        val summary = sessions.summarize(period)
        val document = PdfDocument()
        val page = PageWriter(document, context)

        page.title(context.getString(R.string.app_name))
        page.subtitle("${period.from} — ${period.to}")

        if (sections.summary) {
            page.heading(context.getString(R.string.label_section_summary))
            page.line(context.getString(R.string.label_sessions_count), summary.sessions.toString())
            page.line(
                context.getString(R.string.label_exercises_of_period),
                summary.exercises.toString(),
            )
            page.line(context.getString(R.string.label_sets_count), summary.sets.toString())
            page.line(context.getString(R.string.label_reps_count), summary.reps.toString())
            page.line(
                context.getString(R.string.label_total_volume),
                formatWeight(summary.volume),
            )
            page.line(
                context.getString(R.string.label_volume_per_session),
                formatWeight(summary.volumePerSession),
            )
        }

        if (sections.muscles) {
            page.heading(context.getString(R.string.label_section_muscles))
            page.tableHead(MUSCLE_COLUMNS, muscleHeaders())
            summary.muscles.filter { it.isTouched }.forEach { load ->
                page.tableRow(
                    MUSCLE_COLUMNS,
                    listOf(
                        context.getString(load.muscle.stringRes),
                        formatWeight(load.volume),
                        load.directSets.toString(),
                        load.assistSets.toString(),
                        "${(summary.share(load) * 100).toInt()}%",
                    ),
                )
            }
        }

        if (sections.exercises) {
            page.heading(context.getString(R.string.label_section_exercises))
            page.tableHead(EXERCISE_COLUMNS, exerciseHeaders())
            summary.exerciseLoads.forEach { load ->
                page.tableRow(
                    EXERCISE_COLUMNS,
                    listOf(
                        exerciseName(load.exercise.name),
                        formatWeight(load.volume),
                        load.sets.toString(),
                        load.reps.toString(),
                        formatWeight(load.topWeight),
                    ),
                )
            }
        }

        if (sections.sets) {
            page.heading(context.getString(R.string.label_section_sets))
            page.tableHead(SET_COLUMNS, setHeaders())
            sessions.filter { it.date in period }
                .sortedBy { it.date }
                .forEach { session ->
                    session.sets.forEach { set ->
                        page.tableRow(
                            SET_COLUMNS,
                            listOf(
                                shortDate(session.date),
                                exerciseName(set.exercise.name),
                                formatWeight(set.weight),
                                set.repsOrDuration.toString(),
                                formatWeight(set.volume),
                            ),
                        )
                    }
                }
        }

        page.finish()
        output.use { document.writeTo(it) }
        document.close()
    }

    private fun muscleHeaders() = listOf(
        context.getString(R.string.label_muscle_one),
        context.getString(R.string.label_total_volume),
        context.getString(R.string.label_direct_set),
        context.getString(R.string.label_assist_set),
        "%",
    )

    private fun exerciseHeaders() = listOf(
        context.getString(R.string.label_exercise_one),
        context.getString(R.string.label_total_volume),
        context.getString(R.string.label_sets_count),
        context.getString(R.string.label_reps_count),
        context.getString(R.string.label_top_weight),
    )

    private fun setHeaders() = listOf(
        context.getString(R.string.label_date),
        context.getString(R.string.label_exercise_one),
        context.getString(R.string.label_weight_plain),
        context.getString(R.string.label_reps_count),
        context.getString(R.string.label_total_volume),
    )

    private fun exerciseName(name: String): String =
        exerciseNameRes(name)?.let(context::getString) ?: name

    private fun shortDate(date: LocalDate): String =
        "${date.day.toString().padStart(2, '0')}.${date.monthNumber.toString().padStart(2, '0')}"

    private fun StringBuilder.row(vararg cells: String) {
        append(cells.joinToString(";") { cell -> cell.replace(';', ',') })
        appendLine()
    }
}

private val MUSCLE_COLUMNS = listOf(0F, 180F, 290F, 380F, 460F)
private val EXERCISE_COLUMNS = listOf(0F, 210F, 290F, 350F, 420F)
private val SET_COLUMNS = listOf(0F, 70F, 290F, 360F, 430F)

/**
 * Lays text down an A4 page and starts a new one when the ink reaches the bottom margin.
 */
private class PageWriter(
    private val document: PdfDocument,
    context: Context,
) {
    private val width = 595
    private val height = 842
    private val margin = 40F
    private val bottom = height - margin

    private var pageNumber = 1
    private var page: PdfDocument.Page = newPage()
    private var y = margin + 10F

    private val titlePaint = Paint().apply {
        textSize = 24F
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val subtitlePaint = Paint().apply {
        textSize = 12F
        color = 0xFF6B6B6B.toInt()
    }
    private val headingPaint = Paint().apply {
        textSize = 15F
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val headPaint = Paint().apply {
        textSize = 10F
        color = 0xFF6B6B6B.toInt()
    }
    private val bodyPaint = Paint().apply { textSize = 11F }
    private val rulePaint = Paint().apply { color = 0xFFDDDDDD.toInt() }

    private fun newPage(): PdfDocument.Page =
        document.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())

    private fun space(needed: Float) {
        if (y + needed <= bottom) return
        document.finishPage(page)
        pageNumber++
        page = newPage()
        y = margin + 10F
    }

    fun title(text: String) {
        space(30F)
        page.canvas.drawText(text.uppercase(), margin, y, titlePaint)
        y += 26F
    }

    fun subtitle(text: String) {
        space(20F)
        page.canvas.drawText(text, margin, y, subtitlePaint)
        y += 22F
    }

    fun heading(text: String) {
        space(34F)
        y += 8F
        page.canvas.drawText(text.uppercase(), margin, y, headingPaint)
        y += 8F
        page.canvas.drawRect(margin, y, width - margin, y + 0.8F, rulePaint)
        y += 14F
    }

    fun line(label: String, value: String) {
        space(18F)
        page.canvas.drawText(label, margin, y, bodyPaint)
        page.canvas.drawText(value, margin + 300F, y, bodyPaint)
        y += 16F
    }

    fun tableHead(columns: List<Float>, cells: List<String>) {
        space(20F)
        cells.forEachIndexed { index, cell ->
            page.canvas.drawText(cell, margin + columns[index], y, headPaint)
        }
        y += 6F
        page.canvas.drawRect(margin, y, width - margin, y + 0.8F, rulePaint)
        y += 12F
    }

    fun tableRow(columns: List<Float>, cells: List<String>) {
        space(16F)
        cells.forEachIndexed { index, cell ->
            page.canvas.drawText(cell, margin + columns[index], y, bodyPaint)
        }
        y += 14F
    }

    fun finish() {
        document.finishPage(page)
    }
}
