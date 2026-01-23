package me.dodo.readingnotes.repository;

import me.dodo.readingnotes.domain.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    // 필요하면 여기에 커스텀 쿼리도 작성

    Optional<Book> findByIsbn13(String isbn13);
    boolean existsByIsbn13(String isbn13);

    // ISBN이 없을 때 임시 중복 방지용
    Optional<Book> findFirstByTitleAndAuthor(String title, String author);

    // 삭제되지 않은 책 목록 조회 (검색 + 페이징)
    @Query("SELECT b FROM Book b WHERE b.deletedAt IS NULL " +
            "AND (:keyword IS NULL OR " +
            "     LOWER(b.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "     LOWER(b.author) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "     LOWER(b.publisher) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "     b.isbn13 LIKE CONCAT('%', CAST(:keyword AS string), '%'))")
    Page<Book> findAllByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 관리자용도 동일하게 수정
    @Query("SELECT b FROM Book b " +
            "WHERE (:keyword IS NULL OR " +
            "       LOWER(b.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "       LOWER(b.author) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "       LOWER(b.publisher) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "       b.isbn13 LIKE CONCAT('%', CAST(:keyword AS string), '%')) " +
            "AND (:includeDeleted = true OR b.deletedAt IS NULL)")
    Page<Book> findAllForAdmin(@Param("keyword") String keyword,
                               @Param("includeDeleted") Boolean includeDeleted,
                               Pageable pageable);
}