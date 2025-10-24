# 구현 가이드

## 실행 방법

### 1. DB 켜기
```bash
   docker-compose up -d
```

### 2. 앱 실행
```bash
   ./gradlew :modules:bootstrap:api-payment-gateway:bootRun
```

### 3. API 문서
http://localhost:8080/swagger-ui.html

---

## 구현내용

### 필수
- 결제 생성 API
- 결제 조회 API (페이지네이션 + 통계)
- 제휴사별 수수료 정책
- 카드번호 마스킹

### 선택
- PostgreSQL로 변경 (H2 → PostgreSQL)
- Swagger 문서
- 추가 PG 연동 (SimplePgClient)

---

## 기술 스택

- Kotlin 1.9.25
- Spring Boot 3.4.1
- PostgreSQL 16
- Gradle 8.14

---

## API 사용법

### 결제 생성
```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "partnerId": 1,
    "amount": 10000,
    "cardBin": "123456",
    "cardLast4": "4242",
    "productName": "테스트"
  }'
```

**응답**
```json
{
  "id": 1,
  "amount": 10000,
  "feeAmount": 400,
  "netAmount": 9600,
  "status": "APPROVED"
}
```

**수수료 계산**
- 10,000원 × 3% = 300원
- 고정 수수료 100원
- 총 수수료 = 400원
- 정산금 = 9,600원

### 결제 조회
```bash
   curl "http://localhost:8080/api/v1/payments?partnerId=1&limit=20"
```

**필터**
- partnerId: 제휴사 ID
- status: APPROVED, CANCELED, PENDING
- from/to: 기간 (ISO-8601)
- cursor: 페이지네이션 커서
- limit: 페이지 크기 (기본 20)

**응답**
```json
{
  "items": [...],
  "summary": {
    "count": 10,
    "totalAmount": 100000,
    "totalNetAmount": 96000
  },
  "nextCursor": "MjAyNS0x...",
  "hasNext": false
}
```

---

## 구조

```
modules/
├── domain/          # 순수 비즈니스 로직 (프레임워크 독립)
├── application/     # 유스케이스
├── infrastructure/  # JPA, DB
├── external/        # PG 연동
└── bootstrap/       # Spring Boot
```

---

## 주요 구현

### 1. 수수료 정책
DB에서 제휴사별 정책을 조회해서 적용함
- effective_from 기준으로 최신 정책 선택
- BigDecimal + HALF_UP 반올림
- 정책 변경 시 코드 수정 불필요

### 2. 페이지네이션
커서 방식 사용 (Offset 아님)
- 정렬: `createdAt DESC, id DESC`
- 커서: Base64로 인코딩
- limit + 1 조회로 hasNext 판단

### 3. 통계
items와 summary가 같은 필터 조건 사용
- 페이지네이션과 상관없이 전체 집계
- COUNT, SUM 쿼리

### 4. PG 선택
partnerId 기준으로 자동 선택
- 홀수 (1, 3, 5...): MockPgClient → 항상 승인
- 짝수 (2, 4, 6...): SimplePgClient → 조건부 승인
  - 1천만원 이상 거절
  - 카드번호 0000 거절

### 5. 보안
카드번호 전체는 저장 안 함
- cardBin (앞 6자리)
- cardLast4 (뒤 4자리)
- 로그에도 출력 금지

---

## 테스트

```bash
    # 전체
    ./gradlew test

# 모듈별
    ./gradlew :modules:domain:test
    ./gradlew :modules:application:test
```

**작성한 테스트**
- FeeCalculatorTest - 수수료 계산
- PaymentServiceTest - 결제 생성
- FeePolicyPersistenceAdapterTest - 정책 조회
- PaymentRepositoryPagingTest - 페이지네이션
- SimplePgClientTest - PG 연동

---

## DB 정보

```
Host: localhost:5432
Database: banana
User: banana
Password: banana1234
```

초기 데이터는 앱 시작 시 자동 생성됩니다.
- Partner 2개 (ID: 1, 2)
- FeePolicy 각 2개씩

---

## PG 테스트

### MockPG (partnerId=1)
```bash
    curl -X POST http://localhost:8080/api/v1/payments \
      -H "Content-Type: application/json" \
      -d '{"partnerId":1,"amount":10000,"cardBin":"123456","cardLast4":"1234","productName":"test"}'
```
→ 항상 승인

### SimplePG (partnerId=2) - 정상
```bash
    curl -X POST http://localhost:8080/api/v1/payments \
      -H "Content-Type: application/json" \
      -d '{"partnerId":2,"amount":10000,"cardBin":"123456","cardLast4":"1234","productName":"test"}'
```
→ 승인

### SimplePG - 금액 초과
```bash
    curl -X POST http://localhost:8080/api/v1/payments \
      -H "Content-Type: application/json" \
      -d '{"partnerId":2,"amount":10000000,"cardBin":"123456","cardLast4":"1234","productName":"test"}'
```
→ 거절 (FAIL-AMOUNT)

### SimplePG - 카드번호
```bash
    curl -X POST http://localhost:8080/api/v1/payments \
      -H "Content-Type: application/json" \
      -d '{"partnerId":2,"amount":10000,"cardBin":"123456","cardLast4":"0000","productName":"test"}'
```
→ 거절 (FAIL-CARD)

---

## 유틸

```bash
    # 코드 스타일 체크
    ./gradlew ktlintCheck
    
    # 자동 수정
    ./gradlew ktlintFormat
    
    # 빌드
    ./gradlew build
    
    # 클린 빌드
    ./gradlew clean build
```

---

## 기타

- 원본 README는 과제 설명이라 수정하지 않았습니다.
- 이 파일은 실제 구현 내용 설명용입니다.

