# Readory 서버

## DB 연결
MCP postgres 서버를 사용한다. 별도 설치나 psql 없이 MCP 도구로 직접 쿼리한다.
psql, node, curl 등으로 직접 연결 시도하지 않는다.

## 출처 패턴 분석

트리거: "출처 패턴 분석"

1. DB에서 최근 sentence 100개 조회
   ```sql
   SELECT sentence FROM reading_records ORDER BY recorded_at DESC LIMIT 100;
   ```

2. `src/main/java/me/dodo/readingnotes/util/EbookSourceCleaner.java` 읽기
   - 파일 안의 현재 패턴 목록 확인

3. 조회한 sentence 중 현재 패턴으로 제거되지 않은 출처 문구가 있는지 확인
   - 출처 문구: ebook 앱이 자동으로 붙이는 텍스트 (본문과 무관한 플랫폼 정보, URL 등)
   - 없으면 "출처 문구가 남아있는 문장이 없습니다." 출력 후 종료

4. 있으면 사용자에게 보고
   - 원본 문장 전체
   - 추정 플랫폼
   - 추가/수정할 Java 정규식 패턴 제안

5. 사용자가 승인 후 아래 두 곳 수정
   - `EbookSourceCleaner.java`: 패턴 추가, 상단 주석에 플랫폼명과 예시 원문 추가, `clean()` 에 적용
      - 기존 패턴은 삭제하지 않는다. 출처 형식이 바뀐 경우 구버전 패턴을 남기고 신버전을 추가한다.
      - 이유: sentence_original 기준으로 재정리할 때 구버전 패턴이 없으면 과거 기록에 적용할 수 없다.
   - `EbookSourceCleanerTest.java`: 해당 패턴 테스트 케이스 추가