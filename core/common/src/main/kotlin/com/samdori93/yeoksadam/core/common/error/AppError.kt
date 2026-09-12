package com.samdori93.yeoksadam.core.common.error

/**
 * 도메인 전반에서 표면화하는 에러 모델.
 * raw 예외를 UI 까지 던지지 않고 [AppError] 로 매핑한다(CLAUDE.md §4).
 */
sealed class AppError(
    open val message: String? = null,
    open val cause: Throwable? = null,
) {
    /** 네트워크 연결 실패(타임아웃, 오프라인 등). */
    data class Network(override val cause: Throwable? = null) : AppError(cause = cause)

    /** 서버가 비정상 응답(4xx/5xx). */
    data class Server(val code: Int, override val message: String? = null) : AppError(message)

    /** 인증 만료/실패. */
    data object Unauthorized : AppError("인증이 필요합니다.")

    /** 위치/카메라 등 권한 미허용. */
    data class PermissionDenied(val permission: String) : AppError("권한이 필요합니다: $permission")

    /** 위치를 확인할 수 없음(GPS off 등). */
    data object LocationUnavailable : AppError("현재 위치를 확인할 수 없습니다.")

    /** 그 외 알 수 없는 에러. */
    data class Unknown(override val cause: Throwable? = null) : AppError(cause = cause)
}
