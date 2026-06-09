package me.dodo.readingnotes.repository;

import me.dodo.readingnotes.domain.BookSourceLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookSourceLinkRepository extends JpaRepository<BookSourceLink, Long> {
    // 중복 확인
    Optional<BookSourceLink> findBySourceAndExternalId(String source, String externalId);
    boolean existsBySourceAndExternalId(String source, String externalId);

    @Modifying
    @Query("DELETE FROM BookSourceLink l WHERE l.book.id = :bookId")
    void deleteAllByBookId(@Param("bookId") Long bookId);
}
