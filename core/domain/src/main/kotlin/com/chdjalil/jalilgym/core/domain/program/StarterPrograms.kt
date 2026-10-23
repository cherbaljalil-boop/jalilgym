package com.chdjalil.jalilgym.core.domain.program

import com.chdjalil.jalilgym.core.model.StarterProgram

/** Ids of the bundled program templates (assets/seed/programs.json). */
object ProgramTemplates {
    const val FULL_BODY_3 = "tpl-full-body-3"
    const val PPL_3 = "tpl-ppl-3"
    const val PPL_6 = "tpl-ppl-6"
}

/** Template behind a starter choice; null for [StarterProgram.CUSTOM] (start from scratch). */
val StarterProgram.templateId: String?
    get() = when (this) {
        StarterProgram.FULL_BODY_3 -> ProgramTemplates.FULL_BODY_3
        StarterProgram.PPL_3 -> ProgramTemplates.PPL_3
        StarterProgram.PPL_6 -> ProgramTemplates.PPL_6
        StarterProgram.CUSTOM -> null
    }

/** Next index in a rotation of [dayCount] days. */
fun nextRotationIndex(current: Int, dayCount: Int): Int = if (dayCount <= 0) 0 else (current + 1).mod(dayCount)
