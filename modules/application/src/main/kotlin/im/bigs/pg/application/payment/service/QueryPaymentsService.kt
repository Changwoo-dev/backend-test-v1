package im.bigs.pg.application.payment.service

import im.bigs.pg.application.payment.port.`in`.QueryFilter
import im.bigs.pg.application.payment.port.`in`.QueryPaymentsUseCase
import im.bigs.pg.application.payment.port.`in`.QueryResult
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64

/**
 * 결제 이력 조회 유스케이스 구현체.
 * - 커서 토큰은 createdAt/id를 안전하게 인코딩해 전달/복원합니다.
 * - 통계는 조회 조건과 동일한 집합을 대상으로 계산됩니다.
 */
@Service
class QueryPaymentsService(
    private val paymentOutPort: PaymentOutPort
) : QueryPaymentsUseCase {
    /**
     * 필터를 기반으로 결제 내역을 조회합니다.
     *
     * 현재 구현은 과제용 목업으로, 빈 결과를 반환합니다.
     * 지원자는 커서 기반 페이지네이션과 통계 집계를 완성하세요.
     *
     * @param filter 파트너/상태/기간/커서/페이지 크기
     * @return 조회 결과(목록/통계/커서)
     */
    override fun query(filter: QueryFilter): QueryResult {
        val from = filter.from?.atZone(ZoneOffset.UTC)?.toInstant()
        val to = filter.to?.atZone(ZoneOffset.UTC)?.toInstant()
        val cursor = decodeCursor(filter.cursor)
        val items = paymentOutPort.findPayments(
            partnerId = filter.partnerId,
            status = filter.status,
            from = from,
            to = to,
            cursorCreatedAt = cursor.first,
            cursorId = cursor.second,
            limit = filter.limit
        )

        val summary = paymentOutPort.findPaymentSummary(
            partnerId = filter.partnerId,
            status = filter.status,
            from = from,
            to = to
        )

        val hasNext = items.size >= filter.limit
        val last = items.lastOrNull()
        val nextCursor = encodeCursor(last?.createdAt, last?.id)

        return QueryResult(
            items = items,
            summary = summary,
            nextCursor = nextCursor,
            hasNext = hasNext
        )
    }

    private fun encodeCursor(createdAt: LocalDateTime?, id: Long?): String? {
        if (createdAt == null || id == null) return null
        val epochMillis = createdAt.toInstant(ZoneOffset.UTC).toEpochMilli()
        val raw = "$epochMillis:$id"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray())
    }

    private fun decodeCursor(cursor: String?): Pair<Instant?, Long?> {
        if (cursor.isNullOrBlank()) return null to null
        return try {
            val raw = String(Base64.getUrlDecoder().decode(cursor))
            val parts = raw.split(":")
            val ts = parts[0].toLong()
            val id = parts[1].toLong()
            Instant.ofEpochMilli(ts) to id
        } catch (e: Exception) {
            null to null
        }
    }
}
