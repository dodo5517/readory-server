-- ─────────────────────────────────────────────────────────────────────────────
-- SQLite init (동시성/무결성/성능 기본값)
-- ─────────────────────────────────────────────────────────────────────────────
PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL;
PRAGMA busy_timeout = 5000;

-- ISO-8601 문자열로 날짜/시간 저장 (예: 2025-09-03T15:10:00)
-- Hibernate가 LocalDate/LocalDateTime ↔ TEXT 변환을 처리하므로 TEXT 사용

-- ─────────────────────────────────────────────────────────────────────────────
-- USERS
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
                                     id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                                     username            TEXT    NOT NULL UNIQUE,                 -- length=50 (참고)
                                     email               TEXT    NOT NULL UNIQUE,                 -- length=100 (참고)
                                     password            TEXT,                                    -- length=255 (참고)
                                     provider            TEXT    NOT NULL,                        -- length=20 (참고)
                                     provider_id         TEXT,                                    -- length=100 (참고)
                                     api_key             TEXT    NOT NULL UNIQUE,                 -- length=100 (참고)
                                     profile_image_url   TEXT,                                    -- length=500 (참고)
                                     user_status         TEXT    NOT NULL DEFAULT 'ACTIVE' CHECK (user_status IN ('ACTIVE','BLOCKED','SUSPENDED')),
    role                TEXT    NOT NULL DEFAULT 'USER',         -- length=20 (참고)
    created_at TEXT NOT NULL DEFAULT (substr(strftime('%Y-%m-%dT%H:%M:%f','now'),1,23)),
    updated_at TEXT NOT NULL DEFAULT (substr(strftime('%Y-%m-%dT%H:%M:%f','now'),1,23))
    );

-- 자주 조회되는 컬럼에 보조 인덱스가 필요하면 여기에 추가 가능
-- CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- ─────────────────────────────────────────────────────────────────────────────
-- BOOKS
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS books (
                                     id              INTEGER PRIMARY KEY AUTOINCREMENT,
                                     title           TEXT    NOT NULL,            -- length=255 (참고)
                                     author          TEXT    NOT NULL,            -- length=255 (참고)
                                     publisher       TEXT,                        -- length=255 (참고)
                                     isbn10          TEXT,                        -- length=10  (참고)
                                     isbn13          TEXT UNIQUE,                 -- length=13  (참고)  @UniqueConstraint
                                     published_date  TEXT,                        -- LocalDate -> TEXT(YYYY-MM-DD)
                                     cover_url       TEXT,                        -- length=512 (참고)
                                     created_at      TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (substr(strftime('%Y-%m-%dT%H:%M:%f','now'),1,23)),
    deleted_at TEXT
    );

-- ─────────────────────────────────────────────────────────────────────────────
-- REFRESH_TOKEN
--  - (user_id, device_info) UNIQUE
--  - token UNIQUE
--  - user 삭제 시 토큰 자동 삭제 (ON DELETE CASCADE)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refresh_token (
                                             id           INTEGER PRIMARY KEY AUTOINCREMENT,
                                             user_id      INTEGER NOT NULL,
                                             token        TEXT    NOT NULL UNIQUE,          -- length=512 (참고)
                                             device_info  TEXT,                              -- length=255 (참고)
                                             expiry_date  TEXT    NOT NULL,                  -- LocalDateTime -> TEXT
                                             created_at TEXT NOT NULL DEFAULT (substr(strftime('%Y-%m-%dT%H:%M:%f','now'),1,23)),
    CONSTRAINT fk_rt_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
    );

CREATE UNIQUE INDEX IF NOT EXISTS uq_refresh_token_user_device
    ON refresh_token(user_id, device_info);

-- ─────────────────────────────────────────────────────────────────────────────
-- READING_RECORDS
--  - user 삭제 시 기록 자동 삭제 (ON DELETE CASCADE)
--  - book은 nullable
--  - 인덱스: (user_id, recorded_at), (user_id, book_id, recorded_at, id)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reading_records (
                                               id           INTEGER PRIMARY KEY AUTOINCREMENT,
                                               book_id      INTEGER,
                                               user_id      INTEGER NOT NULL,
                                               sentence     TEXT,                   -- length=1000 (참고)
                                               comment      TEXT,                   -- length=1000 (참고)
                                               raw_title    TEXT,                   -- length=255 (참고)
                                               raw_author   TEXT,                   -- length=255 (참고)
                                               match_status TEXT NOT NULL DEFAULT 'PENDING'
                                               CHECK (match_status IN ('PENDING','RESOLVED_AUTO','RESOLVED_MANUAL','NO_CANDIDATE','MULTIPLE_CANDIDATES')),
    recorded_at  TEXT NOT NULL,
    updated_at   TEXT NOT NULL,
    matched_at   TEXT,
    CONSTRAINT fk_rr_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE,
    CONSTRAINT fk_rr_book
    FOREIGN KEY (book_id) REFERENCES books(id)
    ON DELETE NO ACTION           -- 북 삭제 정책은 상황에 따라 CASCADE/SET NULL 고려
    );

CREATE INDEX IF NOT EXISTS idx_rr_user_recorded
    ON reading_records(user_id, recorded_at);

CREATE INDEX IF NOT EXISTS idx_record_user_book_at_id
    ON reading_records(user_id, book_id, recorded_at, id);

-- ─────────────────────────────────────────────────────────────────────────────
-- BOOK_SOURCE_LINK
--  - UNIQUE (book_id, source)
--  - UNIQUE (source, external_id)
--  - INDEX (isbn13)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS book_source_link (
                                                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                                                book_id      INTEGER NOT NULL,
                                                source       TEXT    NOT NULL,         -- length=20 (참고)  ex) KAKAO/NAVER/GOOGLE
                                                external_id  TEXT,                      -- length=512 (참고)
                                                isbn10       TEXT,                      -- length=10  (참고)
                                                isbn13       TEXT,                      -- length=13  (참고)
                                                synced_at    TEXT,                      -- LocalDateTime -> TEXT
                                                meta_json    TEXT,                      -- TEXT(긴 JSON)
                                                CONSTRAINT fk_bsl_book
                                                FOREIGN KEY (book_id) REFERENCES books(id)
    ON DELETE CASCADE
    );

CREATE UNIQUE INDEX IF NOT EXISTS uq_book_source
    ON book_source_link(book_id, source);

CREATE UNIQUE INDEX IF NOT EXISTS uq_source_external
    ON book_source_link(source, external_id);

CREATE INDEX IF NOT EXISTS idx_bsl_isbn13
    ON book_source_link(isbn13);
-- ─────────────────────────────────────────────────────────────────────────────
-- USER_AUTH_LOGS
-- - INDEX (user_id)
-- - INDEX (occurred_at)
-- - INDEX (event_type)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_auth_logs (
                                              id           INTEGER PRIMARY KEY AUTOINCREMENT,
                                              user_id      INTEGER,
                                              event_type   TEXT NOT NULL, -- LOGIN, LOGIN_FAIL, LOGOUT_CURRENT_DEVICE, LOGOUT_ALL_DEVICES
                                              result       TEXT NOT NULL, -- SUCCESS, FAIL
                                              fail_reason  TEXT, -- length=50
                                              ip_address   TEXT NOT NULL, -- length=45
                                              user_agent   TEXT, -- length=255
                                              identifier   TEXT, -- length=255
                                              provider   TEXT, -- length=20 ex) LOCAL, GOOGLE, KAKAO, NAVER
                                              created_at  TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY(user_id) REFERENCES users(id)
    );

CREATE INDEX IF NOT EXISTS idx_auth_logs_user_id
    ON user_auth_logs(user_id);

CREATE INDEX IF NOT EXISTS idx_auth_logs_created_at
    ON user_auth_logs(created_at);

CREATE INDEX IF NOT EXISTS idx_auth_logs_event_type
    ON user_auth_logs(event_type);