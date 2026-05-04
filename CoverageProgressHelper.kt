package com.nutripulse.app.ui.ration

data class CoverageSegmentState(
    val label: String,
    val fraction: Double,
    val color: Int
)

object CoverageProgressHelper {

    private data class SegmentDef(val label: String, val start: Double, val end: Double, val color: Int)

    private val segments = listOf(
        SegmentDef("0-60", 0.0, 60.0, 0xFFFF5A5A.toInt()),
        SegmentDef("60-85", 60.0, 85.0, 0xFFFFC400.toInt()),
        SegmentDef("85-100", 85.0, 100.0, 0xFF00FF88.toInt()),
        SegmentDef("100+", 100.0, 120.0, 0xFF53A7FF.toInt())
    )

    fun build(coveragePct: Double): List<CoverageSegmentState> {
        val value = coveragePct.coerceIn(0.0, 120.0)
        return segments.map { seg ->
            val span = (seg.end - seg.start).coerceAtLeast(1.0)
            val filled = (value - seg.start).coerceIn(0.0, span)
            CoverageSegmentState(
                label = seg.label,
                fraction = (filled / span).coerceIn(0.0, 1.0),
                color = seg.color
            )
        }
    }

    fun statusLabel(coveragePct: Double): String = when {
        coveragePct >= 100.0 -> "Karşılama: TAM / ÜSTÜ"
        coveragePct >= 85.0 -> "Karşılama: YETERLİ"
        coveragePct >= 60.0 -> "Karşılama: SINIRDA"
        else -> "Karşılama: DÜŞÜK"
    }
}
