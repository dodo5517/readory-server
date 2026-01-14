package me.dodo.readingnotes.repository;

import me.dodo.readingnotes.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // JPA가 알아서 쿼리 생성해줌.
    boolean existsByEmail(String email); // SELECT COUNT(*) FROM user WHERE email = ?
    boolean existsByUsername(String username); // SELECT COUNT(*) FROM user WHERE username = ?

    Optional<User> findByEmail(String email);

    Optional<User> findByApiKey(String apiKey);

    void delete(User user);

    @Query("""
        select u
        from User u
        where
            (:keyword is null or :keyword = '' or
                lower(u.username) like lower(concat('%', :keyword, '%')) or
                lower(u.email) like lower(concat('%', :keyword, '%'))
            )
            and (:provider is null or :provider = '' or u.provider = :provider)
            and (:role is null or :role = '' or u.role = :role)
    """)
    Page<User> searchUsers(
            @Param("keyword") String keyword,
            @Param("provider") String provider,
            @Param("role") String role,
            Pageable pageable
    );
}