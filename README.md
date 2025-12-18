# Readory Backend

> **Readory (Reading + Memory)**   
> 아이폰 단축어를 통해 책의 문장과 감상을 웹에 자동으로 기록하는 독서 기록 서비스  
> 사용자는 단축어를 실행하는 것만으로 손쉽게 자신의 독서 노트를 웹에 저장할 수 있습니다.

> **기간:** 2025.07.28 ~ 2025.08.29  
> **개인 프로젝트**  

 **프론트 레파지토리**
- [Readory Front (React)](https://github.com/dodo5517/readory-front)

---

## 화면 미리보기

<table>
  <tr>
    <td><img width="500" src="https://github.com/user-attachments/assets/cbd58e11-bf4d-4a2c-9d74-d1875b7771c7" /></td>
    <td><img width="500" src="https://github.com/user-attachments/assets/05dc03e0-7bb1-4f70-8c75-7e4dbe4bc9b0" /></td>
  </tr>
</table>

---

## 데모

[데모 바로가기](https://readory.vercel.app/)
> ℹ️ 데모 사이트 URL이 최근 변경되었습니다!!

---

## 아이폰 단축어 파일

[Readory 단축어](https://dodo5517.tistory.com/173)

---

## 기술 스택

| 구분 | 사용 기술 |
|------|------------|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.4.4 |
| **Build Tool** | Gradle (Groovy DSL) |
| **Database** | SQLite (개발용, JPA/Hibernate 사용) |
| **Security** | Spring Security, JWT, OAuth2 Client |
| **API Docs** | Springdoc OpenAPI (Swagger UI) |
| **Storage** | AWS S3 (이미지 등 업로드용) |
| **HTTP Client** | WebClient (Spring WebFlux) |

---

## 프로젝트 구조

```
me.dodo.readingnotes
├── config         # Spring 설정 (Security, Swagger, OAuth 등)
├── controller     # REST API 엔드포인트
├── domain         # JPA 엔티티 (DB 모델)
├── dto            # 요청/응답 DTO
├── exception      # 예외 처리 및 전역 핸들러
├── external       # 외부 연동 (S3 등)
├── repository     # JPA Repository
├── service        # 비즈니스 로직 계층
├── util           # 공통 유틸리티
└── ReadingNotesApplication.java  # 진입점
```

---

## 서비스 개요

> **아이폰 단축어 한 번으로, 책의 문장과 감상을 기록하는 웹 독서 일지 플랫폼**

### 사용 흐름
1. 사용자가 문장을 **아이폰 단축어**로 공유
2. 입력창에 책 제목, 작가, 감상 등을 입력  
3. 단축어가 백엔드의 `/records` API로 데이터를 전송  
4. 서버는 내용을 데이터베이스에 저장하고, UI에서 즉시 반영  

이 과정을 통해 독서 중에 떠오른 문장을 손쉽게 웹에 기록할 수 있습니다.

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| **단축어 업로드 API** | iOS 단축어에서 텍스트 데이터를 받아 독서 기록으로 저장 |
| **독서 기록 관리** | 책 제목, 문장, 코멘트, 날짜 기반 CRUD |
| **AWS S3 업로드** | 선택적으로 책 표지나 이미지 업로드 |
| **로그인 / 회원 관리** | 이메일 기반 로그인, 소셜(OAuth2) 로그인 |
| **Swagger 문서** | `/swagger-ui/index.html` 경로에서 API 문서 제공 |

---

## 설계 원칙

| 항목 | 내용 |
|------|------|
| **계층 분리** | Controller, Service, Repository, Domain 명확히 분리 |
| **엔티티 독립성** | DTO를 통해 API 요청/응답과 엔티티를 분리 |
| **트랜잭션 제어** | 비즈니스 로직 단위로 명확히 관리 |
| **확장성 고려** | 이후 태그, 통계, 추천 기능 추가 용이 |
| **데이터 일관성** | DB 무결성 및 예외 처리 통합 관리 (`exception` 패키지) |

---
