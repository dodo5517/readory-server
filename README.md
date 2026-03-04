# Readory Backend

> Readory (Reading + Memory)  
> 아이폰 단축어로 책의 문장과 감상을 웹에 기록하는 독서 기록 서비스

**기간:** 2025.07.28 ~ 현재 (초기 개발 후 지속적 유지보수 중)  
**개인 프로젝트**

프론트엔드 레포지토리: [Readory Front (React)](https://github.com/dodo5517/readory-front)

---

## 화면 미리보기

**Desktop**

<img src="https://github.com/user-attachments/assets/03403272-2886-44d4-b150-debcb011182d" width="32%"> <img src="https://github.com/user-attachments/assets/25cd8593-7297-4148-81f8-3553dd9986f1" width="32%"> <img src="https://github.com/user-attachments/assets/049eb8a9-7933-4bd8-96aa-80a5b79f595c" width="32%">

**Mobile**

<img src="https://github.com/user-attachments/assets/4c785920-43e6-49fa-975f-60c4fa65c862" width="24%"> <img src="https://github.com/user-attachments/assets/fe029ee5-6c64-4be8-8f3b-cfdff7b23399"  width="24%"> <img  src="https://github.com/user-attachments/assets/ad0206f1-d9bc-4851-8abd-afb101a58be1"  width="24%"> <img src="https://github.com/user-attachments/assets/d789f1e8-df31-4c81-a71e-3988beeec154" width="24%">

---

## 데모

[데모 바로가기](https://readory.kimdohyeon.dev/)  
[아이폰 단축어 파일](https://dodo5517.tistory.com/173)

---

## 기술 스택

| 구분 | 사용 기술 |
|------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.4.4 |
| Build | Gradle |
| Database | PostgreSQL (Supabase) |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security, JWT, OAuth2 Client |
| Storage | Supabase Storage (S3 호환 방식) |
| HTTP Client | WebClient (Spring WebFlux) |
| 외부 API | 카카오 책 검색, 네이버 책 검색, 구글 / 카카오 / 네이버 OAuth2 |
| 이미지 처리 | Thumbnailator |
| 문자열 유사도 | Apache Commons Text (JaroWinkler) |
| 인프라 | Raspberry Pi, GitHub Actions |
| API 문서 | Springdoc OpenAPI (Swagger UI) |

---

## 프로젝트 구조

```
me.dodo.readingnotes
├── config          # Security, Swagger, OAuth2, 필터 등 설정
├── controller      # REST API 엔드포인트
├── domain          # JPA 엔티티
├── dto             # 요청/응답 DTO
├── exception       # 전역 예외 처리
├── external        # 외부 API 연동 (카카오, 네이버 어댑터)
├── repository      # JPA Repository
├── service         # 비즈니스 로직
└── util            # 공통 유틸리티
```

---

## 서비스 흐름

아이폰 단축어에서 책 정보 / 문장 / 감상을 입력하면 API Key 인증을 거쳐 서버에 기록이 저장됩니다. 
저장 시 제목과 작가명을 기반으로 책 자동 매칭을 시도하고, 결과는 웹에서 확인 및 수정할 수 있습니다.

```
[아이폰 단축어] → POST /api/records (API Key 인증)
      ↓
[서버] 기록 저장 + 책 자동 매칭 (비동기)
      ↓
[웹] 기록 조회 / 책 연결 확정 / 캘린더 통계
```

---
## 주요 기능

### 독서 기록

- 단축어에서 API Key 인증으로 기록 저장 (`POST /api/records`)
- 웹에서 JWT 인증으로 기록 저장 (`POST /api/records/web`)
- 전체 / 월별 / 일별 / 책별 조회 (페이지네이션, 커서 기반)
- 기록 수정, 단일 삭제, 책 전체 삭제

### 책 매칭

- 기록 저장 시 제목·작가 기반으로 자동 매칭 시도 (비동기)
- JaroWinkler 유사도로 정규화 후 점수 계산, 0.88 이상이면 자동 확정
- 로컬 DB 후보 조회 (`/candidates/local`), 외부 API 후보 조회 (`/candidates/external`)
- 카카오·네이버 어댑터 패턴으로 다중 소스 통합
- 사용자가 직접 책을 선택해 연결하거나 해제 가능

**MatchStatus**

| 상태 | 설명 |
|------|------|
| PENDING | 매칭 시도 전 (기본값) |
| RESOLVED_AUTO | 자동 매칭 완료 |
| RESOLVED_MANUAL | 사용자가 직접 선택하여 확정 |
| NO_CANDIDATE | 후보 없음 |
| MULTIPLE_CANDIDATES | 후보 여러 개, 사용자 선택 필요 |

### 캘린더 & 통계

- 월간 달력: 날짜별 기록 수 조회
- 연간 히트맵: 연도 전체 일별 기록 통계 (`month=0`으로 구분)
- 매칭 완료된 책 목록, 최근 기록일 기준 정렬

### 인증

- JWT: Access Token (응답 body) + Refresh Token (HttpOnly 쿠키)
- OAuth2 소셜 로그인 (구글, 카카오, 네이버)
- API Key: 단축어 전용, 발급·재발급·마스킹 조회 지원
- 현재 기기 로그아웃 / 전체 기기 로그아웃
- Refresh Token 재발급 시 User-Agent 검증

### 회원

- 이메일 회원가입 / 탈퇴
- 닉네임, 비밀번호, 프로필 이미지 변경
- 이미지 업로드 시 비율 유지 리사이징 후 Supabase Storage 저장

### 관리자

- 유저: 목록 조회(검색/필터), 상태·권한·닉네임·비밀번호 변경, 강제 로그아웃
- 책: 목록 조회, 소프트 삭제 / 영구 삭제 / 복구, 통계
- 기록: 특정 유저 기록 조회·수정·삭제, 유저별 활동 현황
- 로그: API 요청 로그, 인증 이벤트 로그 조회 및 필터링

---

## 데이터 모델

```
Users ──< ReadingRecords >── Books
  │
  ├── RefreshToken
  ├── UserAuthLog
  └── ApiLog
```

| 엔티티 | 주요 필드 |
|--------|-----------|
| User | id, username, email, password, provider, apiKey, profileImageUrl, userStatus, role |
| Book | id, title, author, publisher, isbn10, isbn13, coverUrl, deletedAt |
| ReadingRecord | id, userId, bookId, sentence, comment, rawTitle, rawAuthor, matchStatus, recordedAt |
| RefreshToken | id, userId, token, userAgent, expiresAt |
| ApiLog | method, uri, statusCode, responseTime, result |
| UserAuthLog | userId, eventType, result, ip, userAgent |

---

## 인프라 & 배포

main 브랜치에 push하면 GitHub Actions에서 빌드 후 SSH로 Raspberry Pi에 JAR를 업로드하고 systemd 서비스를 재시작합니다.

```
GitHub push (main)
      ↓
GitHub Actions
  - JDK 17 설정
  - ./gradlew bootJar
  - SCP로 JAR 업로드
  - systemctl restart readory
      ↓
Raspberry Pi (자체 서버)
```

사용 프로파일: `prod-dev` → `oauth, supabase, book, dev`

---

## 환경 변수

```env
SPRING_PROFILES_ACTIVE=prod-dev
SERVER_PORT=8080
FRONTEND_URL=http://localhost:3000

# ========================
# Log Setting
# ========================
API_LOG_SUCCESS_ENABLED=
AUTH_LOG_SUCCESS_ENABLED=

# ========================
# Supabase Datasource
# ========================
DB_URL=
DB_USERNAME=
DB_PASSWORD=

# ========================
# Supabase Storage(S3 connection method)
# ========================
SUPABASE_STORAGE_URL=
SUPABASE_STORAGE_PUBLIC_URL=
SUPABASE_STORAGE_REGION=
SUPABASE_STORAGE_ACCESS_KEY=
SUPABASE_STORAGE_SECRET_KEY=
SUPABASE_STORAGE_BUCKET=

# ========================
# Kakao Book Search API
# ========================
EXTERNAL_KAKAO_BOOK_REST_API_KEY=

# ========================
# Google OAuth
# ========================
OAUTH2_GOOGLE_CLIENT_ID=
OAUTH2_GOOGLE_CLIENT_SECRET=
OAUTH2_GOOGLE_REDIRECT_URI=

# ========================
# Naver OAuth
# ========================
OAUTH2_NAVER_CLIENT_ID=
OAUTH2_NAVER_CLIENT_SECRET=
OAUTH2_NAVER_REDIRECT_URI=

# ========================
# Kakao OAuth
# ========================
OAUTH2_KAKAO_CLIENT_ID=
OAUTH2_KAKAO_CLIENT_SECRET=
OAUTH2_KAKAO_REDIRECT_URI=

```

---

## API 문서

```
http://localhost:8080/swagger-ui/index.html
```
