package com.chdjalil.jalilgym.core.model

enum class ProgramType { PPL, FULL_BODY, UPPER_LOWER, STRENGTH, HOME, BODYWEIGHT, CUSTOM }

/** See docs/BLUEPRINT.md §3.3.1. */
enum class ProgressionRule { DOUBLE, LINEAR, NONE }

enum class SessionStatus { ACTIVE, FINISHED, DISCARDED }

/** Warm-up sets never count toward PRs, volume or progression. */
enum class SetType { WARMUP, WORKING, DROP, FAILURE }

enum class PrType { EST_1RM, MAX_WEIGHT, MAX_REPS_AT_WEIGHT, MAX_VOLUME }

enum class MediaType { IMAGE, ANIMATED, VIDEO }
