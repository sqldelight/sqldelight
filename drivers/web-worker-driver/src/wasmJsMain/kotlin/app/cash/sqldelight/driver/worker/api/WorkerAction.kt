package app.cash.sqldelight.driver.worker.api

@Suppress("ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_SUPERTYPES_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING")
internal actual sealed external interface WorkerAction : JsAny

@Suppress("NOTHING_TO_INLINE", "FunctionName", "RedundantSuppression")
internal actual inline fun WorkerAction(value: String) = value.toJsString().unsafeCast<WorkerAction>()
