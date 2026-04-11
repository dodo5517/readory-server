package me.dodo.readingnotes.repository;

import me.dodo.readingnotes.domain.UserBookPin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserBookPinRepository extends JpaRepository<UserBookPin, Long> {

    Optional<UserBookPin> findByUser_IdAndBook_Id(Long userId, Long bookId);

    boolean existsByUser_IdAndBook_Id(Long userId, Long bookId);

    void deleteByUser_IdAndBook_Id(Long userId, Long bookId);
}