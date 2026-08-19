package com.ddtask.scheduler.util

/** 配置导入失败时 [IllegalArgumentException.message] 使用的错误码。 */
object ConfigImportErrors {
    const val EMPTY_JSON = "empty_json"
    const val INVALID_JSON = "invalid_json"
    const val UNSUPPORTED_VERSION = "unsupported_version"
    const val CLIPBOARD_LABEL = "ddtask_config"
}
