package me.dodo.readingnotes.service;

import me.dodo.readingnotes.domain.Book;
import me.dodo.readingnotes.dto.admin.BookDetailResponse;
import me.dodo.readingnotes.dto.admin.BookListResponse;
import me.dodo.readingnotes.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class BookService {

    private final BookRepository bookRepository;

    @Autowired
    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    // 관리자용 책 목록 조회 (검색 + 삭제된 책 포함 여부)
    public Page<BookListResponse> findAllBooksForAdmin(String keyword, Boolean includeDeleted, Pageable pageable) {
        boolean includeDeletedValue = includeDeleted != null && includeDeleted;
        String kw = normalize(keyword);
        return bookRepository.findAllForAdmin(kw, includeDeletedValue, pageable)
                .map(BookListResponse::new);
    }

    // 관리자용 책 상세 조회
    public BookDetailResponse findBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 책을 찾을 수 없습니다. id=" + id));
        return new BookDetailResponse(book);
    }

    // 책 소프트 삭제
    @Transactional
    public void softDeleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 책을 찾을 수 없습니다. id=" + id));

        if (book.getDeletedAt() != null) {
            throw new IllegalStateException("이미 삭제된 책입니다. id=" + id);
        }

        book.setDeletedAt(LocalDateTime.now());
    }

    // 책 영구 삭제 (관리자 전용)
    @Transactional
    public void hardDeleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new IllegalArgumentException("해당 책을 찾을 수 없습니다. id=" + id);
        }
        bookRepository.deleteById(id);
    }

    // 삭제된 책 복구
    @Transactional
    public void restoreBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 책을 찾을 수 없습니다. id=" + id));

        if (book.getDeletedAt() == null) {
            throw new IllegalStateException("삭제되지 않은 책입니다. id=" + id);
        }

        book.setDeletedAt(null);
    }
    private String normalize(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
