package im.bigs.pg.external.pg

import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.domain.payment.PaymentStatus
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.random.Random

/**
 * 간단한 PG: 금액 및 카드번호 기반 조건부 승인.
 * - 1000만원 이상: 실패
 * - 카드 마지막 4자리가 "0000": 실패
 * - 그 외: 성공
 */
@Component
class SimplePgClient : PgClientOutPort {
    override fun supports(partnerId: Long): Boolean = partnerId % 2L == 0L

    override fun approve(request: PgApproveRequest): PgApproveResult {
        // 실패 조건 체크
        if (request.amount >= BigDecimal("10000000")) { // 1000만원
            return PgApproveResult(
                approvalCode = "FAIL-AMOUNT",
                approvedAt = LocalDateTime.now(ZoneOffset.UTC),
                status = PaymentStatus.CANCELED
            )
        }

        if (request.cardLast4 == "0000") {
            return PgApproveResult(
                approvalCode = "FAIL-CARD",
                approvedAt = LocalDateTime.now(ZoneOffset.UTC),
                status = PaymentStatus.CANCELED
            )
        }

        // 성공
        val randomDigits = Random.nextInt(100000, 999999)
        return PgApproveResult(
            approvalCode = "SIMPLE-$randomDigits",
            approvedAt = LocalDateTime.now(ZoneOffset.UTC),
            status = PaymentStatus.APPROVED
        )
    }
}
