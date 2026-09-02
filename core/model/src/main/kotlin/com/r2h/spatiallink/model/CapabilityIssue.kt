package com.r2h.spatiallink.model

enum class CapabilityIssueCategory {
    SECURITY,
    SERVICE,
    FRAMEWORK,
    CALLBACK,
    STATUS,
}

enum class CapabilityIssueCode(val category: CapabilityIssueCategory) {
    SECURITY_FAILURE(CapabilityIssueCategory.SECURITY),
    SERVICE_UNAVAILABLE(CapabilityIssueCategory.SERVICE),
    UNEXPECTED_FAILURE(CapabilityIssueCategory.FRAMEWORK),
    CALLBACK_FAILURE(CapabilityIssueCategory.CALLBACK),
    UNKNOWN_STATUS(CapabilityIssueCategory.STATUS),
}

data class CapabilityIssue(
    val subsystem: CapabilitySubsystem,
    val code: CapabilityIssueCode,
    val detail: String? = null,
) {
    val category: CapabilityIssueCategory
        get() = code.category
}
