package me.dodo.readingnotes.service;

import me.dodo.readingnotes.domain.Book;
import me.dodo.readingnotes.domain.User;
import me.dodo.readingnotes.domain.UserBookPin;
import me.dodo.readingnotes.repository.BookRepository;
import me.dodo.readingnotes.repository.UserBookPinRepository;
import me.dodo.readingnotes.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookPinService {

    private final UserBookPinRepository pinRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public BookPinService(UserBookPinRepository pinRepository,
                          UserRepository userRepository,
                          BookRepository bookRepository) {
        this.pinRepository = pinRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    // 책 고정
    @Transactional
    public void pin(Long userId, Long bookId) {
        if (pinRepository.existsByUser_IdAndBook_Id(userId, bookId)) return;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 책입니다."));

        pinRepository.save(new UserBookPin(user, book));
    }

    // 책 고정 해제
    @Transactional
    public void unpin(Long userId, Long bookId) {
        pinRepository.deleteByUser_IdAndBook_Id(userId, bookId);
    }
}