-- =========================
-- Table: users
-- =========================
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    email       VARCHAR(100) NOT NULL,
    password    VARCHAR(255),
    provider    VARCHAR(20)  NOT NULL,
    provider_id VARCHAR(100),
    api_key     VARCHAR(100) NOT NULL,
    profile_image_url VARCHAR(500),
    user_status VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT uq_users_api_key  UNIQUE (api_key),

    CONSTRAINT ck_users_user_status
    CHECK (user_status IN ('ACTIVE', 'BLOCKED', 'SUSPENDED'))
    );

-- 보통 로그인/조회에서 자주 쓰는 인덱스 (UNIQUE면 자동 인덱스가 생기지만 명시 유지해도 무방)
-- CREATE UNIQUE INDEX IF NOT EXISTS uq_users_username ON users(username);
-- CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email ON users(email);
-- CREATE UNIQUE INDEX IF NOT EXISTS uq_users_api_key ON users(api_key);

-- provider/provider_id로 사용자 찾는 패턴이 있다면 유용(소셜 로그인 조회용)
CREATE INDEX IF NOT EXISTS idx_users_provider_provider_id
    ON users (provider, provider_id);

-- updated_at 자동 갱신이 필요하면(권장) 트리거가 필요합니다.
-- 아래는 선택 사항: updated_at을 UPDATE 때마다 now()로 갱신
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_users_set_updated_at ON users;
CREATE TRIGGER trg_users_set_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

-- =========================
-- Table: refresh_token
-- =========================
CREATE TABLE IF NOT EXISTS refresh_token (
                                             id          BIGSERIAL PRIMARY KEY,

                                             user_id     BIGINT NOT NULL,
                                             device_info VARCHAR(255) NOT NULL,

    token       VARCHAR(512) NOT NULL,
    expiry_date TIMESTAMP NOT NULL,

    created_at  TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_refresh_token_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE
    );

-- upsert conflict 대상 (user_id, device_info)
CREATE UNIQUE INDEX IF NOT EXISTS uq_refresh_token_user_device
    ON refresh_token (user_id, device_info);

-- token 전역 유니크 (엔티티에 unique=true라 반영)
CREATE UNIQUE INDEX IF NOT EXISTS uq_refresh_token_token
    ON refresh_token (token);

-- (선택) user_id로 토큰 조회가 잦으면 도움
CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id
    ON refresh_token (user_id);

-- =========================
-- Table: books
-- =========================
CREATE TABLE IF NOT EXISTS books (
                                     id              BIGSERIAL PRIMARY KEY,

                                     title           VARCHAR(255) NOT NULL,
    author          VARCHAR(255) NOT NULL,
    publisher       VARCHAR(255),

    isbn10          VARCHAR(10),
    isbn13          VARCHAR(13),

    published_date  DATE,

    cover_url       VARCHAR(512),

    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMP,

    CONSTRAINT uq_isbn13 UNIQUE (isbn13)
    );

-- (선택) 조회 성능용 인덱스들: 실제 쿼리 패턴에 따라 조정
CREATE INDEX IF NOT EXISTS idx_books_title
    ON books (title);

CREATE INDEX IF NOT EXISTS idx_books_author
    ON books (author);

-- (선택) 소프트 삭제(deleted_at IS NULL) 조건으로 자주 조회하면 부분 인덱스가 유용
CREATE INDEX IF NOT EXISTS idx_books_not_deleted
    ON books (id)
    WHERE deleted_at IS NULL;

-- updated_at 자동 갱신 트리거 (users에서 만든 함수 재사용 가능)
-- 이미 set_updated_at() 함수를 schema.sql 앞쪽에 만들어뒀다면 아래 트리거만 있으면 됩니다.
DROP TRIGGER IF EXISTS trg_books_set_updated_at ON books;
CREATE TRIGGER trg_books_set_updated_at
    BEFORE UPDATE ON books
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

-- =========================
-- Table: reading_records
-- =========================
CREATE TABLE IF NOT EXISTS reading_records (
    id            BIGSERIAL PRIMARY KEY,

    book_id       BIGINT,
    user_id       BIGINT NOT NULL,

    sentence      VARCHAR(1000),
    comment       VARCHAR(1000),

    raw_title     VARCHAR(255),
    raw_author    VARCHAR(255),

    match_status  VARCHAR(32) NOT NULL DEFAULT 'PENDING',

    recorded_at   TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL,
    matched_at    TIMESTAMP,

    CONSTRAINT fk_reading_records_book
    FOREIGN KEY (book_id)
    REFERENCES books(id)
    ON DELETE SET NULL,

    CONSTRAINT fk_reading_records_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE,

    CONSTRAINT ck_reading_records_match_status
    CHECK (match_status IN (
           'PENDING',
           'RESOLVED_AUTO',
           'RESOLVED_MANUAL',
           'NO_CANDIDATE',
           'MULTIPLE_CANDIDATES'
                           ))
    );

-- 엔티티에 선언한 인덱스 반영
CREATE INDEX IF NOT EXISTS idx_rr_user_recorded
    ON reading_records (user_id, recorded_at);

CREATE INDEX IF NOT EXISTS idx_record_user_book_at_id
    ON reading_records (user_id, book_id, recorded_at, id);

-- (선택) 매칭 상태/시간 기반 조회가 잦으면 도움
CREATE INDEX IF NOT EXISTS idx_rr_match_status
    ON reading_records (match_status);

CREATE INDEX IF NOT EXISTS idx_rr_user_match_status_recorded
    ON reading_records (user_id, match_status, recorded_at);

CREATE INDEX IF NOT EXISTS idx_rr_matched_at
    ON reading_records (matched_at);

-- =========================
-- Table: book_source_link
-- =========================
CREATE TABLE IF NOT EXISTS book_source_link (
    id           BIGSERIAL PRIMARY KEY,

    book_id      BIGINT NOT NULL,

    source       VARCHAR(20) NOT NULL,
    external_id  VARCHAR(512),

    isbn10       VARCHAR(10),
    isbn13       VARCHAR(13),

    synced_at    TIMESTAMP,

    meta_json    TEXT,

    CONSTRAINT fk_bsl_book
    FOREIGN KEY (book_id)
    REFERENCES books(id)
    ON DELETE CASCADE,

    CONSTRAINT uq_book_source UNIQUE (book_id, source),
    CONSTRAINT uq_source_external UNIQUE (source, external_id)
    );

-- 엔티티 인덱스 반영
CREATE INDEX IF NOT EXISTS idx_bsl_isbn13
    ON book_source_link (isbn13);

-- (선택) 아래 인덱스는 실제 조회 패턴에 따라 유용할 수 있음
CREATE INDEX IF NOT EXISTS idx_bsl_book_id
    ON book_source_link (book_id);

CREATE INDEX IF NOT EXISTS idx_bsl_source
    ON book_source_link (source);

-- =========================
-- Table: user_auth_logs
-- =========================
CREATE TABLE IF NOT EXISTS user_auth_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT,

    event_type  VARCHAR(50) NOT NULL,
    result      VARCHAR(20) NOT NULL,

    fail_reason VARCHAR(50),

    ip_address  VARCHAR(45),
    user_agent  VARCHAR(255),

    identifier  VARCHAR(255),
    provider    VARCHAR(20),

    created_at  TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_user_auth_logs_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE SET NULL,

    CONSTRAINT ck_user_auth_logs_event_type
    CHECK (event_type IN (
           'LOGIN',
           'LOGIN_FAIL',
           'LOGOUT_CURRENT_DEVICE',
           'LOGOUT_ALL_DEVICES'
                         )),

    CONSTRAINT ck_user_auth_logs_result
    CHECK (result IN ('SUCCESS', 'FAIL'))
    );

-- 유저별 인증 이력 조회
CREATE INDEX IF NOT EXISTS idx_user_auth_logs_user_created
    ON user_auth_logs (user_id, created_at DESC);

-- 로그인 실패 추적/보안 분석용
CREATE INDEX IF NOT EXISTS idx_user_auth_logs_fail
    ON user_auth_logs (result, created_at DESC)
    WHERE result = 'FAIL';

-- 특정 identifier(email, social id) 기준 추적
CREATE INDEX IF NOT EXISTS idx_user_auth_logs_identifier
    ON user_auth_logs (identifier);

-- IP 기준 보안 분석
CREATE INDEX IF NOT EXISTS idx_user_auth_logs_ip
    ON user_auth_logs (ip_address);
