package me.dodo.readingnotes.service;

// jakarta.transaction.Transactional 보다 밑에가 spring framework 전용으로 연동 잘 됨.
import me.dodo.readingnotes.dto.admin.AdminPageUserResponse;
import me.dodo.readingnotes.exception.PasswordMismatchException;
import me.dodo.readingnotes.repository.RefreshTokenRepository;
import me.dodo.readingnotes.util.ApiKeyGenerator;
import me.dodo.readingnotes.util.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import me.dodo.readingnotes.domain.User;
import me.dodo.readingnotes.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, RefreshTokenRepository refreshTokenRepository, S3Service s3Service) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.s3Service = s3Service;
    }

    // 유저 회원가입
    @Transactional // 트랜젝션 처리
    public User registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())){
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("이미 사용 중인 이름입니다.");
        }

        // 비밀번호 암호화(해싱)
        String encodedPw = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPw);

        // provider 일반 로그인으로 저장
        user.setProvider("local");

        // api_key
        user.setApiKey(ApiKeyGenerator.generate()); // api_key 생성
        if (user.getApiKey() != null){
//            log.info("api_key:" + user.getApiKey().substring(0,8));
        } else{
            log.warn("api_key가 null임.");
        }

        return userRepository.save(user);
    }

    // api_key 재발급
    @Transactional
    public String reissueApiKey(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        String newApiKey = ApiKeyGenerator.generate(); // 랜덤 키 생성 로직
        user.setApiKey(newApiKey); // apiKey 갱신
        userRepository.save(user);

        return maskApiKey(newApiKey); // 마스킹된 키 반환
    }
    // api_key 마스킹
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 4) return "****";
        int visibleCount = 4;
        int maskCount = apiKey.length() - visibleCount;
        return "*".repeat(maskCount) + apiKey.substring(maskCount);
    }

    // api_key 전체(마스킹 안 된) 조회
    @Transactional(readOnly = true)
    public String getRawApiKey(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        return user.getApiKey();
    }

    // 유저 프로필 이미지 추가/수정
    @Transactional
    public void updateProfileImage(Long id, String imageUrl) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        user.setProfileImageUrl(imageUrl);
        userRepository.save(user);
    }

    // 유저 프로필 이미지 삭제
    @Transactional
    public void deleteProfileImage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        if (user.getProfileImageUrl() != null){
            String key = extractKeyFromUrl(user.getProfileImageUrl());
            s3Service.deleteFile(key);
        }

    }
    private String extractKeyFromUrl(String imageUrl) {
        return imageUrl.substring(imageUrl.lastIndexOf("profiles/"));
    }

    // 유저 이름 수정
    @Transactional
    public boolean updateUsername(Long userId, String newUsername) {
        // 유저 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        log.debug("newUsername: {}", newUsername);

        user.setUsername(newUsername); // 새로운 유저이름 저장
        userRepository.save(user); // DB에 저장

        return true;
    }
    
    // 유저 비밀번호 수정(본인)
    @Transactional
    public boolean updatePassword(Long userId, String currentPassword, String newPassword) {
        // 유저 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        // 기존 비밀번호 비교
        if (!passwordEncoder.matches(currentPassword, user.getPassword())){ // 평문 비교가 아니라 해시 비교
            throw new PasswordMismatchException("기존 비밀번호가 일치하지 않습니다.");
        }

        // 기존 user 정보 가져와서 담기
        User newUser = user;
        // 새로운 비밀번호 해싱 후 저장
        newUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(newUser); // DB에 저장

        return true;
    }

    // 유저 비밀번호 수정(관리자)
    @Transactional
    public boolean updatePasswordAdmin(Long userId, String newPassword) {
        if(newPassword == null){
            throw new IllegalArgumentException("password가 없습니다.");
        }
        // 유저 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        // 기존 user 정보 가져와서 담기
        User newUser = user;
        // 새로운 비밀번호 해싱 후 저장
        newUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(newUser); // DB에 저장

        return true;
    }

    // 전체 유저 조회
    public Page<AdminPageUserResponse> findAllUsers(String keyword, String provider,
                                                    String role, Pageable pageable
                                   ) {
        String kw = normalize(keyword);
        String pv = normalize(provider);
        String rl = normalize(role);

        return userRepository.searchUsers(kw, pv, rl, pageable)
                .map(AdminPageUserResponse::new);
    }

    // ID로 유저 조회
    public User findUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 유저가 없습니다."));
    }

    // 유저 삭제
    public boolean deleteUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(()-> new IllegalArgumentException("해당 ID의 유저가 없습니다."));
        // 삭제
        userRepository.delete(user);

        // 삭제 완료 메시지
        log.info("Deleted:" + user.getEmail());
        return true;
    }

    // 관리자인지 권한 확인
    public void assertAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 유저가 없습니다."));
        if (user.getRole() != "ADMIN") {
            throw new IllegalArgumentException("관리자 권한이 없습니다.");
        }
    }

    // 유저 상태 수정
    @Transactional
    public void changeUserStatus(Long userId, User.UserStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status가 없습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 유저가 없습니다."));

        user.setUserStatus(status);
        userRepository.save(user);
    }

    // 유저 권한(역할) 수정
    @Transactional
    public void changeUserRole(Long userId, String role) {
        System.out.println(role);
        if (role == null) {
            throw new IllegalArgumentException("role이 없습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 유저가 없습니다."));

        switch (role) {
            case "ADMIN":
                user.setRole("ADMIN");
                break;
            case "USER":
                user.setRole("USER");
                break;
            default:
                throw new IllegalArgumentException("존재하지 않는 role입니다.");
        }
        userRepository.save(user);
    }


    private String normalize(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

}
