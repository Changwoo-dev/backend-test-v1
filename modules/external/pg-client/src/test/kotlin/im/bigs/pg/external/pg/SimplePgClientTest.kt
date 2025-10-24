package im.bigs.pg.external.pg

import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.domain.payment.PaymentStatus
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimplePgClientTest {
    private val client = SimplePgClient()

    @Test
    @DisplayName("짝수 partnerId를 지원해야 한다")
    fun `짝수 partnerId를 지원해야 한다`() {
        assertTrue(client.supports(2L))
        assertTrue(client.supports(4L))
        assertTrue(!client.supports(1L))
        assertTrue(!client.supports(3L))
    }

    @Test
    @DisplayName("1000만원 이상이면 승인이 실패해야 한다")
    fun `1000만원 이상이면 승인이 실패해야 한다`() {
        val request = PgApproveRequest(
            partnerId = 2L,
            amount = BigDecimal("10000000"),
            cardBin = "1234",
            cardLast4 = "1234",
            productName = "테스트상품"
        )
        val result = client.approve(request)

        assertEquals(PaymentStatus.CANCELED, result.status)
        assertEquals("FAIL-AMOUNT", result.approvalCode)
    }

    @Test
    @DisplayName("카드 마지막 4자리가 0000이면 승인이 실패해야 한다")
    fun `카드 마지막 4자리가 0000이면 승인이 실패해야 한다`() {
        val request = PgApproveRequest(
            partnerId = 2L,
            amount = BigDecimal("10000"),
            cardBin = "1234",
            cardLast4 = "0000",
            productName = "테스트상품"
        )
        val result = client.approve(request)

        assertEquals(PaymentStatus.CANCELED, result.status)
        assertEquals("FAIL-CARD", result.approvalCode)
    }

    @Test
    @DisplayName("정상 조건이면 승인이 성공해야 한다")
    fun `정상 조건이면 승인이 성공해야 한다`() {
        val request = PgApproveRequest(
            partnerId = 2L,
            amount = BigDecimal("50000"),
            cardBin = "1234",
            cardLast4 = "1234",
            productName = "테스트상품"
        )
        val result = client.approve(request)

        assertEquals(PaymentStatus.APPROVED, result.status)
        assertTrue(result.approvalCode.startsWith("SIMPLE-"))
    }
}