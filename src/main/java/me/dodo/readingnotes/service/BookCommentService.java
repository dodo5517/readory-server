package me.dodo.readingnotes.service;

import me.dodo.readingnotes.domain.Book;
import me.dodo.readingnotes.domain.BookComment;
import me.dodo.readingnotes.domain.User;
import me.dodo.readingnotes.dto.book.BookCommentRequest;
import me.dodo.readingnotes.dto.book.BookCommentResponse;
import me.dodo.readingnotes.repository.BookCommentRepository;
import me.dodo.readingnotes.repository.BookRepository;
import me.dodo.readingnotes.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class BookCommentService {

    private final BookCommentRepository bookCommentRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public BookCommentService(BookCommentRepository bookCommentRepository,
                              BookRepository bookRepository,
                              UserRepository userRepository) {
        this.bookCommentRepository = bookCommentRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    // 책 코멘트 조회 (없으면 null)
    @Transactional(readOnly = true)
    public BookCommentResponse getComment(Long userId, Long bookId) {
        return bookCommentRepository.findByUser_IdAndBook_Id(userId, bookId)
                .map(BookCommentResponse::new)
                .orElse(null);
    }

    // 책 코멘트 저장 또는 수정 (upsert)
    @Transactional
    public BookCommentResponse upsertComment(Long userId, Long bookId, BookCommentRequest req) {
        String content = req.getContent();
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("코멘트 내용이 비어있습니다.");
        }

        Optional<BookComment> existing = bookCommentRepository.findByUser_IdAndBook_Id(userId, bookId);

        BookComment comment;
        if (existing.isPresent()) {
            // 수정
            comment = existing.get();
            comment.setContent(content.trim());
            comment.setUpdatedAt(LocalDateTime.now());
        } else {
            // 신규 생성
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자"));
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 책"));

            comment = new BookComment();
            comment.setUser(user);
            comment.setBook(book);
            comment.setContent(content.trim());
        }

        return new BookCommentResponse(bookCommentRepository.save(comment));
    }

    // 책 코멘트 삭제
    @Transactional
    public void deleteComment(Long userId, Long bookId) {
        BookComment comment = bookCommentRepository.findByUser_IdAndBook_Id(userId, bookId)
                .orElseThrow(() -> new IllegalArgumentException("해당 코멘트가 존재하지 않습니다."));
        bookCommentRepository.delete(comment);
    }
}