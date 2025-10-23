package im.bigs.pg.infra.persistence.payment.entity

import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * DB용 결제 이력 엔티티.
 * - createdAt/Id 조합을 커서 정렬 키로 사용합니다.
 */
@Entity
@Table(name = "payment")
class PaymentEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    var partnerId: Long,
    @Column(nullable = false, precision = 15, scale = 0)
    var amount: BigDecimal,
    @Column(nullable = false, precision = 10, scale = 6)
    var appliedFeeRate: BigDecimal,
    @Column(nullable = false, precision = 15, scale = 0)
    var feeAmount: BigDecimal,
    @Column(nullable = false, precision = 15, scale = 0)
    var netAmount: BigDecimal,
    @Column(length = 8)
    var cardBin: String? = null,
    @Column(length = 4)
    var cardLast4: String? = null,
    @Column(nullable = false, length = 32)
    var approvalCode: String,
    @Column(nullable = false)
    var approvedAt: Instant,
    @Column(nullable = false, length = 20)
    var status: String,
    @Column(nullable = false)
    var createdAt: Instant,
    @Column(nullable = false)
    var updatedAt: Instant,
) {
    fun toDomain(): Payment {
        return Payment(
            id = this.id,
            partnerId = this.partnerId,
            amount = this.amount,
            appliedFeeRate = this.appliedFeeRate,
            feeAmount = this.feeAmount,
            netAmount = this.netAmount,
            cardBin = this.cardBin,
            cardLast4 = this.cardLast4,
            approvalCode = this.approvalCode,
            approvedAt = LocalDateTime.ofInstant(this.approvedAt, ZoneOffset.UTC),
            status = PaymentStatus.valueOf(this.status),
            createdAt = LocalDateTime.ofInstant(this.createdAt, ZoneOffset.UTC),
            updatedAt = LocalDateTime.ofInstant(this.updatedAt, ZoneOffset.UTC)
        )
    }
    companion object {
        fun from(domain: Payment): PaymentEntity {
            return PaymentEntity(
                id = domain.id,
                partnerId = domain.partnerId,
                amount = domain.amount,
                appliedFeeRate = domain.appliedFeeRate,
                feeAmount = domain.feeAmount,
                netAmount = domain.netAmount,
                cardBin = domain.cardBin,
                cardLast4 = domain.cardLast4,
                approvalCode = domain.approvalCode,
                approvedAt = domain.approvedAt.atZone(ZoneOffset.UTC).toInstant(),
                status = domain.status.name,
                createdAt = domain.createdAt.atZone(ZoneOffset.UTC).toInstant(),
                updatedAt = domain.updatedAt.atZone(ZoneOffset.UTC).toInstant()
            )
        }
    }
}
