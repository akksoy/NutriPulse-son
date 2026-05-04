package com.nutripulse.app.ui.ration

import com.nutripulse.app.data.model.Feed

/** UI tarafinda secilen yem + kullanicinin girdigi taze miktar (kg/gun). */
data class SelectedFeedInput(
    val feed: Feed,
    var amountKg: Double = 0.0
)
