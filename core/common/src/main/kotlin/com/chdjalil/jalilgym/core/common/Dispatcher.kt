package com.chdjalil.jalilgym.core.common

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: JgDispatchers)

enum class JgDispatchers { Default, IO }

/** A [kotlinx.coroutines.CoroutineScope] that lives as long as the application process. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope
