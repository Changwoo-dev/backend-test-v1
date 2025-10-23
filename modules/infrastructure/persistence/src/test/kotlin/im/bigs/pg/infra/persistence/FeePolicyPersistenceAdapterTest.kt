package im.bigs.pg.infra.persistence.partner.adapter

import im.bigs.pg.infra.persistence.config.JpaConfig
import im.bigs.pg.infra.persistence.partner.entity.FeePolicyEntity
import im.bigs.pg.infra.persistence.partner.entity.PartnerEntity
import im.bigs.pg.infra.persistence.partner.repository.FeePolicyJpaRepository
import im.bigs.pg.infra.persistence.partner.repository.PartnerJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@DataJpaTest
@ContextConfiguration(classes = [JpaConfig::class])
class FeePolicyPersistenceAdapterTest @Autowired constructor(
    private val partnerRepo: PartnerJpaRepository,
    private val feePolicyRepo: FeePolicyJpaRepository,
) {
    private lateinit var adapter: FeePolicyPersistenceAdapter
    private var partnerId: Long = 0L

    @BeforeEach
    fun setup() {
        adapter = FeePolicyPersistenceAdapter(feePolicyRepo)

        // 테스트용 파트너 생성
        val partner = partnerRepo.save(
            PartnerEntity(code = "TEST", name = "Test Partner", active = true)
        )
        partnerId = partner.id!!
    }

    @Test
    @DisplayName("여러 정책 중 가장 최근 유효한 정책을 반환해야 한다")
    fun `여러 정책 중 가장 최근 유효한 정책을 반환해야 한다`() {
        // given: 3개의 정책 저장 (과거 → 현재)
        feePolicyRepo.save(
            FeePolicyEntity(
                partnerId = partnerId,
                effectiveFrom = Instant.parse("2020-01-01T00:00:00Z"),
                percentage = BigDecimal("0.0100"),
                fixedFee = BigDecimal.ZERO
            )
        )
        feePolicyRepo.save(
            FeePolicyEntity(
                partnerId = partnerId,
                effectiveFrom = Instant.parse("2023-01-01T00:00:00Z"),
                percentage = BigDecimal("0.0200"),
                fixedFee = BigDecimal("50")
            )
        )
        feePolicyRepo.save(
            FeePolicyEntity(
                partnerId = partnerId,
                effectiveFrom = Instant.parse("2024-01-01T00:00:00Z"),
                percentage = BigDecimal("0.0300"),
                fixedFee = BigDecimal("100")
            )
        )

        // when: 2024-06-01 시점의 정책 조회
        val at = LocalDateTime.of(2024, 6, 1, 0, 0)
        val policy = adapter.findEffectivePolicy(partnerId, at)

        // then: 가장 최근 정책(2024-01-01)이 선택되어야 함
        assertNotNull(policy)
        assertEquals(BigDecimal("0.0300"), policy.percentage)
        assertEquals(BigDecimal("100"), policy.fixedFee)
    }

    @Test
    @DisplayName("미래 정책은 제외되어야 한다")
    fun `미래 정책은 제외되어야 한다`() {
        // given: 과거 정책 1개, 미래 정책 1개
        feePolicyRepo.save(
            FeePolicyEntity(
                partnerId = partnerId,
                effectiveFrom = Instant.parse("2020-01-01T00:00:00Z"),
                percentage = BigDecimal("0.0100"),
                fixedFee = BigDecimal.ZERO
            )
        )
        feePolicyRepo.save(
            FeePolicyEntity(
                partnerId = partnerId,
                effectiveFrom = Instant.parse("2030-01-01T00:00:00Z"), // 미래
                percentage = BigDecimal("0.0500"),
                fixedFee = BigDecimal("200")
            )
        )

        // when: 2024-06-01 시점의 정책 조회
        val at = LocalDateTime.of(2024, 6, 1, 0, 0)
        val policy = adapter.findEffectivePolicy(partnerId, at)

        // then: 과거 정책만 선택되어야 함
        assertNotNull(policy)
        assertEquals(BigDecimal("0.0100"), policy.percentage)
        assertEquals(BigDecimal.ZERO, policy.fixedFee)
    }

    @Test
    @DisplayName("유효한 정책이 없으면 null을 반환해야 한다")
    fun `유효한 정책이 없으면 null을 반환해야 한다`() {
        // given: 미래 정책만 존재
        feePolicyRepo.save(
            FeePolicyEntity(
                partnerId = partnerId,
                effectiveFrom = Instant.parse("2030-01-01T00:00:00Z"),
                percentage = BigDecimal("0.0300"),
                fixedFee = BigDecimal("100")
            )
        )

        // when: 2024-06-01 시점의 정책 조회
        val at = LocalDateTime.of(2024, 6, 1, 0, 0)
        val policy = adapter.findEffectivePolicy(partnerId, at)

        // then: null 반환
        assertNull(policy)
    }

    @Test
    @DisplayName("effective_from이 조회 시점과 같을 때도 선택되어야 한다")
    fun `effective_from이 조회 시점과 같을 때도 선택되어야 한다`() {
        // given
        val effectiveTime = Instant.parse("2024-01-01T00:00:00Z")
        feePolicyRepo.save(
            FeePolicyEntity(
                partnerId = partnerId,
                effectiveFrom = effectiveTime,
                percentage = BigDecimal("0.0300"),
                fixedFee = BigDecimal("100")
            )
        )

        // when: 정확히 같은 시점으로 조회
        val at = LocalDateTime.ofInstant(effectiveTime, ZoneOffset.UTC)
        val policy = adapter.findEffectivePolicy(partnerId, at)

        // then: 정책이 선택되어야 함 (<=)
        assertNotNull(policy)
        assertEquals(BigDecimal("0.0300"), policy.percentage)
    }
}