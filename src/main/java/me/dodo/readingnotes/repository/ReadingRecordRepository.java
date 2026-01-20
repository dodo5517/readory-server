package me.dodo.readingnotes.repository;

import me.dodo.readingnotes.domain.ReadingRecord;
import me.dodo.readingnotes.dto.book.BookWithLastRecordResponse;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReadingRecordRepository extends JpaRepository<ReadingRecord, Long> {
    // 필요하면 여기에 커스텀 쿼리도 작성

    // 해당 유저의 모든 기록을 페이지 단위로 가져옴
    // Page라서 전체 개수(totalElements), 전체 페이지 수(totalPages), 현재 페이지 번호(pageNumber) 포함함.
    // N+1 줄이기 위해서 book을 EntityGraph로 로딩함.
    @EntityGraph(attributePaths = "book")
    Optional<ReadingRecord> findByIdAndUserId(Long recordId, Long userId);

    // 해당 유저의 모든 기록 불러오기(제목/작가 or 문장/코멘트 로 검색)
    // 1) 제목/작가 검색
    @Query(
            value = """
        select rr
        from ReadingRecord rr
        left join fetch rr.book b
        where rr.user.id = :userId
          and (
                :q is null or :q = ''
             or lower(b.title)  like lower(concat('%', :q, '%'))
             or lower(b.author) like lower(concat('%', :q, '%'))
          )
          order by rr.recordedAt desc, rr.id desc
        """,
        countQuery = """
        select count(rr)
        from ReadingRecord rr
        join rr.book b
        where rr.user.id = :userId
          and (
                :q is null or :q = ''
             or lower(b.title)  like lower(concat('%', :q, '%'))
             or lower(b.author) like lower(concat('%', :q, '%'))
          )
        """
    )
    Page<ReadingRecord> findMyRecordsByBook(@Param("userId") Long userId,
                                            @Param("q") String q,
                                            Pageable pageable);
    // 2) 문장/코멘트로 검색
    @Query(
            value = """
        select rr
        from ReadingRecord rr
        left join fetch rr.book b
        where rr.user.id = :userId
          and (
                :q is null or :q = ''
             or lower(rr.sentence) like lower(concat('%', :q, '%'))
             or lower(rr.comment)  like lower(concat('%', :q, '%'))
          )
          order by rr.recordedAt desc, rr.id desc
        """,
            countQuery = """
        select count(rr)
        from ReadingRecord rr
        where rr.user.id = :userId
          and (
                :q is null or :q = ''
             or lower(rr.sentence) like lower(concat('%', :q, '%'))
             or lower(rr.comment)  like lower(concat('%', :q, '%'))
          )
        """
    )
    Page<ReadingRecord> findMyRecordsByText(@Param("userId") Long userId,
                                            @Param("q") String q,
                                            Pageable pageable);

    // 해당 유저의 특정 책 찾기 (postgreSql은 밑처럼 하면 문제 생겨서 cursor 유무로 분기함.)
    // sqlite
    @Query("""
        select r
          from ReadingRecord r
         where r.user.id = :userId
           and r.book.id = :bookId
           and ( 
                :cursorAt is null
                 or r.recordedAt < :cursorAt
                 or (r.recordedAt = :cursorAt and r.id < :cursorId) )
         order by r.recordedAt desc, r.id desc
    """)
    List<ReadingRecord> findSliceByUserAndBookWithCursor(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId,
            @Param("cursorAt") LocalDateTime cursorAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
    // postgreSQL
    @Query("""
        select r from ReadingRecord r
        where r.user.id = :userId
          and r.book.id = :bookId
        order by r.recordedAt desc, r.id desc
        """)
    List<ReadingRecord> findSliceFirstPage(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId,
            Pageable pageable
    );
    @Query("""
        select r from ReadingRecord r
        where r.user.id = :userId
          and r.book.id = :bookId
          and (r.recordedAt < :cursorAt or (r.recordedAt = :cursorAt and r.id < :cursorId))
        order by r.recordedAt desc, r.id desc
        """)
    List<ReadingRecord> findSliceNextPage(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId,
            @Param("cursorAt") LocalDateTime cursorAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );


    // 기간 계산(해당 유저의 해당 책 기록 중 가장 과거/가장 최근)
    @Query("""
        select min(r.recordedAt)
          from ReadingRecord r
         where r.user.id = :userId
           and r.book.id = :bookId
    """)
    LocalDateTime findMinRecordedAtByUserAndBook(Long userId, Long bookId);
    @Query("""
        select max(r.recordedAt)
          from ReadingRecord r
         where r.user.id = :userId
           and r.book.id = :bookId
    """)
    LocalDateTime findMaxRecordedAtByUserAndBook(Long userId, Long bookId);

    // 해당 유저의 기록 중 최신 N개만 가져옴,  count 쿼리 없음.
    // 페이지네이션 필요 없으니 굳이 Page 안 쓰고 List로 반환
    @Query("""
            select rr from ReadingRecord rr
            left join fetch rr.book b
            where rr.user.id = :userId
            order by rr.recordedAt desc
            """)
    List<ReadingRecord> findLatestByUser(@Param("userId") Long userId, Pageable pageable);

    // 해당 유저가 기록한 책 중에서 매칭이 끝난 책만 가져옴.
    // 최근 기록순
    @Query("""
        select new me.dodo.readingnotes.dto.book.BookWithLastRecordResponse(
            b.id, b.title, b.author, b.isbn10, b.isbn13, b.coverUrl, max(r.recordedAt)
        )
        from ReadingRecord r join r.book b
        where r.user.id = :userId
          and r.matchStatus in (me.dodo.readingnotes.domain.ReadingRecord.MatchStatus.RESOLVED_AUTO,
                                me.dodo.readingnotes.domain.ReadingRecord.MatchStatus.RESOLVED_MANUAL)
          and (:q is null or :q = ''
               or lower(b.title) like lower(concat('%', :q, '%'))
               or lower(b.author) like lower(concat('%', :q, '%')))
        group by b.id, b.title, b.author, b.isbn10, b.isbn13, b.coverUrl
        order by max(r.recordedAt) desc
        """)
    Page<BookWithLastRecordResponse> findConfirmedBooksByRecent(Long userId, String q, Pageable pageable);
    // 제목순
    @Query("""
        select new me.dodo.readingnotes.dto.book.BookWithLastRecordResponse(
            b.id, b.title, b.author, b.isbn10, b.isbn13, b.coverUrl, max(r.recordedAt)
        )
        from ReadingRecord r join r.book b
        where r.user.id = :userId
          and r.matchStatus in (me.dodo.readingnotes.domain.ReadingRecord.MatchStatus.RESOLVED_AUTO,
                                me.dodo.readingnotes.domain.ReadingRecord.MatchStatus.RESOLVED_MANUAL)
          and (:q is null or :q = ''
               or lower(b.title) like lower(concat('%', :q, '%'))
               or lower(b.author) like lower(concat('%', :q, '%')))
        group by b.id, b.title, b.author, b.isbn10, b.isbn13, b.coverUrl
        order by b.title asc
        """)
    Page<BookWithLastRecordResponse> findConfirmedBooksByTitle(Long userId, String q, Pageable pageable);

    // Day 목록
    // sqlite
//    @Query("""
//        select
//           function('strftime', '%Y-%m-%d', r.recordedAt) as day,
//           count(r) as cnt
//        from ReadingRecord r
//        where r.user.id = :userId
//          and r.recordedAt >= :start and r.recordedAt < :end
//        group by function('strftime', '%Y-%m-%d', r.recordedAt)
//        order by function('strftime', '%Y-%m-%d', r.recordedAt) asc
//        """)
    // postgreSQL
    @Query("""
        select
           function('date', r.recordedAt) as day,
           count(r) as cnt
        from ReadingRecord r
        where r.user.id = :userId
          and r.recordedAt >= :start and r.recordedAt < :end
        group by function('date', r.recordedAt)
        order by function('date', r.recordedAt) asc
    """)
    List<DayCountRow> countByDayInRange(@Param("userId") Long userId,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    // 하루 기록 보기/월 전체 기록 보기
    @Query("""
      select rr
      from ReadingRecord rr
      where rr.user.id = :userId
        and rr.recordedAt >= :start
        and rr.recordedAt <  :end
        and (
              :q is null or :q = ''
           or lower(rr.sentence) like lower(concat('%', :q, '%'))
           or lower(rr.comment)  like lower(concat('%', :q, '%'))
        )
      """)
    Page<ReadingRecord> findRecordsInRange(@Param("userId") Long userId,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end,
                                           @Param("q") String q,
                                           Pageable pageable);

    // ##############################
    // 관리자 전용 메서드
    // ##############################

    // 관리자용: 전체 기록 조회 (검색 + 필터링)
    @Query("SELECT r FROM ReadingRecord r " +
            "JOIN FETCH r.user " +
            "LEFT JOIN FETCH r.book " +
            "WHERE (:keyword IS NULL OR " +
            "       LOWER(r.rawTitle) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "       LOWER(r.rawAuthor) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "       LOWER(r.sentence) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "       LOWER(r.user.username) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) " +
            "AND (:matchStatus IS NULL OR r.matchStatus = :matchStatus) " +
            "AND (:userId IS NULL OR r.user.id = :userId)")
    Page<ReadingRecord> findAllForAdmin(@Param("keyword") String keyword,
                                        @Param("matchStatus") ReadingRecord.MatchStatus matchStatus,
                                        @Param("userId") Long userId,
                                        Pageable pageable);

    // 관리자용: ID로 기록 조회 (연관 엔티티 함께)
    @Query("SELECT r FROM ReadingRecord r " +
            "JOIN FETCH r.user " +
            "LEFT JOIN FETCH r.book " +
            "WHERE r.id = :id")
    Optional<ReadingRecord> findByIdForAdmin(@Param("id") Long id);
}