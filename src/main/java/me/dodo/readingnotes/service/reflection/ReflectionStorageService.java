package me.dodo.readingnotes.service.reflection;

import me.dodo.readingnotes.domain.Book;
import me.dodo.readingnotes.domain.Reflection;
import me.dodo.readingnotes.domain.User;
import me.dodo.readingnotes.dto.reflection.ReflectionResponse;
import me.dodo.readingnotes.dto.reflection.ReflectionSaveRequest;
import me.dodo.readingnotes.repository.BookRepository;
import me.dodo.readingnotes.repository.ReflectionRepository;
import me.dodo.readingnotes.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 완성된 독후감의 저장/조회/수정.
 * 유저+책당 하나(upsert). 마크다운 한 덩어리로 보관.
 */
@Service
public class ReflectionStorageService {

    private final ReflectionRepository reflectionRepo;
    private final BookRepository bookRepo;
    private final UserRepository userRepo;

    public ReflectionStorageService(ReflectionRepository reflectionRepo,
                                    BookRepository bookRepo,
                                    UserRepository userRepo) {
        this.reflectionRepo = reflectionRepo;
        this.bookRepo = bookRepo;
        this.userRepo = userRepo;
    }

    /** 저장 또는 갱신(upsert). 이미 있으면 내용만 교체. */
    @Transactional
    public ReflectionResponse save(Long userId, ReflectionSaveRequest req) {
        Reflection r = reflectionRepo.findByUser_IdAndBook_Id(userId, req.bookId())
                .orElseGet(() -> {
                    Book book = bookRepo.findById(req.bookId())
                            .orElseThrow(() -> new IllegalArgumentException("책을 찾을 수 없습니다."));
                    User user = userRepo.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
                    return Reflection.create(user, book, "", "");
                });
        r.updateTitle(req.title() != null ? req.title() : "");
        r.updateContent(req.content() != null ? req.content() : "");
        Reflection saved = reflectionRepo.save(r);
        return toResponse(saved);
    }

    /** 조회. 없으면 empty. */
    @Transactional(readOnly = true)
    public Optional<ReflectionResponse> get(Long userId, Long bookId) {
        return reflectionRepo.findByUser_IdAndBook_Id(userId, bookId).map(this::toResponse);
    }

    /** 존재 여부(진입 분기용). */
    @Transactional(readOnly = true)
    public boolean exists(Long userId, Long bookId) {
        return reflectionRepo.existsByUser_IdAndBook_Id(userId, bookId);
    }

    @Transactional
    public void delete(Long userId, Long bookId) {
        reflectionRepo.findByUser_IdAndBook_Id(userId, bookId)
                .ifPresent(reflectionRepo::delete);
    }

    private ReflectionResponse toResponse(Reflection r) {
        return new ReflectionResponse(
                r.getId(),
                r.getBook().getId(),
                r.getTitle(),
                r.getContent(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}