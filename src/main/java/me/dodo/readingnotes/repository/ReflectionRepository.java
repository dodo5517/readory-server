package me.dodo.readingnotes.repository;

import me.dodo.readingnotes.domain.Reflection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReflectionRepository extends JpaRepository<Reflection, Long> {

    Optional<Reflection> findByUser_IdAndBook_Id(Long userId, Long bookId);

    boolean existsByUser_IdAndBook_Id(Long userId, Long bookId);
}