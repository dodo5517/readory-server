package me.dodo.readingnotes.repository;

import me.dodo.readingnotes.domain.BookComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookCommentRepository extends JpaRepository<BookComment, Long> {

    Optional<BookComment> findByUser_IdAndBook_Id(Long userId, Long bookId);

//    boolean existsByUser_IdAndBook_Id(Long userId, Long bookId);

    // 책 목록 조회 시 코멘트 일괄 조회용 (book lazy 로딩 방지)
//    @Query("select c from BookComment c join fetch c.book where c.user.id = :userId and c.book.id in :bookIds")
//    List<BookComment> findAllByUser_IdAndBook_IdIn(@Param("userId") Long userId, @Param("bookIds") List<Long> bookIds);
}