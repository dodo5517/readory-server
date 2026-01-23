package me.dodo.readingnotes.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import me.dodo.readingnotes.dto.auth.ApiKeyResponse;
import me.dodo.readingnotes.dto.common.MaskedApiKeyResponse;
import me.dodo.readingnotes.dto.user.UpdatePasswordRequest;
import me.dodo.readingnotes.dto.user.UpdateUsernameRequest;
import me.dodo.readingnotes.dto.user.UserRequest;
import me.dodo.readingnotes.dto.user.UserResponse;
import me.dodo.readingnotes.service.S3Service;
import me.dodo.readingnotes.service.UserService;
import me.dodo.readingnotes.domain.User;
import me.dodo.readingnotes.util.JwtTokenProvider;
import org.slf4j.Logger; // java.util.logging.Logger 보다 세부 설정 가능.
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final S3Service s3Service;
    private final JwtTokenProvider jwtTokenProvider;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    public UserController(UserService userService,
                          S3Service s3Service,
                          JwtTokenProvider jwtTokenProvider) {
            this.userService = userService; // controller에 service 의존성 주입
            this.s3Service = s3Service;
            this.jwtTokenProvider = jwtTokenProvider;
    }

    // post(일반 회원가입)
    @PostMapping
    public UserResponse registerUser(@RequestBody @Valid UserRequest request){ // @Valid는 유효성 검사를 해줌.
        log.debug("회원가입 요청(request): {}", request.toString());

        User savedUser = userService.registerUser(request.toEntity());
        log.debug("user: {}", savedUser.toString());

        return new UserResponse(savedUser);
    }

    // 로그인한 유저의 정보 조회
    @GetMapping("/me")
    public UserResponse getMe(HttpServletRequest request){
        // JwtAuthFilter가 심어준 값 사용
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        User user = userService.findUserById(userId);
        return new UserResponse(user);
    }

    // 유저 프로필 사진 업로드
    @PostMapping("/me/profile-image")
    public ResponseEntity<String> uploadProfileImage(@RequestParam("image") MultipartFile image,
                                                     HttpServletRequest httpRequest) throws Exception {
        // JwtAuthFilter가 심어준 값 사용
        Long userId = (Long) httpRequest.getAttribute("USER_ID");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        // 기존 이미지 삭제
        userService.deleteProfileImage(userId);

        // 새 이미지 업로드
        String fileName = "user-" + userId + "_" + UUID.randomUUID();
        String imageUrl = s3Service.uploadProfileImage(image, fileName);

        userService.updateProfileImage(userId, imageUrl); // DB에 URL 저장

        return ResponseEntity.ok(imageUrl);
    }

    // 유저 프로필 사진 삭제
    @DeleteMapping("/me/profile-image")
    public ResponseEntity<Void> deleteProfileImage(HttpServletRequest httpRequest) {
        // JwtAuthFilter가 심어준 값 사용
        Long userId = (Long) httpRequest.getAttribute("USER_ID");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        userService.deleteProfileImage(userId);
        userService.updateProfileImage(userId, null); // DB에서 URL 제거
        return ResponseEntity.noContent().build();
    }
    
    
    // 유저 이름 수정
    @PatchMapping("/me/username")
    public ResponseEntity<Void> updateUsername(@RequestBody @Valid UpdateUsernameRequest request,
                                               HttpServletRequest httpRequest){
        // JwtAuthFilter가 심어준 값 사용
        Long userId = (Long) httpRequest.getAttribute("USER_ID");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        // 유저 이름 수정
        Boolean result = userService.updateUsername(userId, request.getNewUsername());

        // 수정 확인
        log.debug("updateUsername: {}", result);

        // 상태코드만 반환 (204)
        return ResponseEntity.noContent().build();
    }

    // 유저 비밀번호 수정
    @PatchMapping("/me/password")
    public ResponseEntity<Void> updatePassword(@RequestBody @Valid UpdatePasswordRequest request,
                                               HttpServletRequest httpRequest){
        // JwtAuthFilter가 심어준 값 사용
        Long userId = (Long) httpRequest.getAttribute("USER_ID");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        // 유저 비밀번호 수정
        Boolean result = userService.updatePassword(userId, request.getCurrentPassword(), request.getNewPassword());

        // 수정 확인
        log.debug("updatePassword: {}", result);

        // 상태코드만 반환 (204)
        return ResponseEntity.noContent().build();
    }

    // api_key 재발급
    @PostMapping("/api-key/reissue")
    public ResponseEntity<MaskedApiKeyResponse> reissueApiKey(HttpServletRequest request) {
        log.debug("API Key 재발급 요청");

        // JwtAuthFilter가 심어준 값 사용
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        String maskedApiKey = userService.reissueApiKey(userId);
        return ResponseEntity.ok(new MaskedApiKeyResponse("API Key가 새로 발급되었습니다.", maskedApiKey));
    }

    // api_key 전체(마스킹 안 된) 조회
    @GetMapping("/api-key")
    public ResponseEntity<ApiKeyResponse> getApiKey(HttpServletRequest request) {
        log.debug("API Key 전체 조회 요청");

        // JwtAuthFilter가 심어준 값 사용
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        String apiKey = userService.getRawApiKey(userId);
        return ResponseEntity.ok(new ApiKeyResponse("API Key 조회 성공", apiKey));
    }

    // 유저 탈퇴
    @DeleteMapping("/delete")
    public Boolean deleteUser(HttpServletRequest request){
        // JwtAuthFilter가 심어준 값 사용
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        return userService.deleteUserById(userId);
    }

}
