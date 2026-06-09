package me.dodo.readingnotes.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import me.dodo.readingnotes.dto.auth.ApiKeyResponse;
import me.dodo.readingnotes.dto.common.ApiResponse;
import me.dodo.readingnotes.dto.common.MaskedApiKeyResponse;
import me.dodo.readingnotes.dto.user.UpdatePasswordRequest;
import me.dodo.readingnotes.dto.user.UpdateUsernameRequest;
import me.dodo.readingnotes.dto.user.UserRequest;
import me.dodo.readingnotes.dto.user.UserResponse;
import me.dodo.readingnotes.exception.AuthException;
import me.dodo.readingnotes.service.S3Service;
import me.dodo.readingnotes.service.UserService;
import me.dodo.readingnotes.domain.User;
import me.dodo.readingnotes.util.ImageResizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final S3Service s3Service;
    private final ImageResizer imageResizer;

    public UserController(UserService userService,
                          S3Service s3Service,
                          ImageResizer imageResizer) {
        this.userService = userService;
        this.s3Service = s3Service;
        this.imageResizer = imageResizer;
    }

    // 일반 회원가입
    @PostMapping
    public ApiResponse<UserResponse> registerUser(@RequestBody @Valid UserRequest request) {
        log.debug("회원가입 요청(request): {}", request.toString());
        User savedUser = userService.registerUser(request.toEntity());
        return ApiResponse.success(new UserResponse(savedUser));
    }

    // 로그인한 유저의 정보 조회
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe(HttpServletRequest request) {
        Long userId = resolveUserId(request);
        User user = userService.findUserById(userId);
        return ApiResponse.success(new UserResponse(user));
    }

    // 유저 프로필 사진 업로드
    @PostMapping("/me/profile-image")
    public ApiResponse<String> uploadProfileImage(@RequestParam("image") MultipartFile image,
                                                   HttpServletRequest httpRequest) throws Exception {
        Long userId = resolveUserId(httpRequest);

        userService.deleteProfileImage(userId);

        byte[] resizedImage = imageResizer.resizeImageKeepRatio(image);

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        String fileName = "user-" + userId + "_" + timestamp;

        String imageUrl = s3Service.uploadProfileImage(resizedImage, fileName, image.getContentType());
        userService.updateProfileImage(userId, imageUrl);

        return ApiResponse.success(null, imageUrl);
    }

    // 유저 프로필 사진 삭제
    @DeleteMapping("/me/profile-image")
    public ApiResponse<Void> deleteProfileImage(HttpServletRequest httpRequest) {
        Long userId = resolveUserId(httpRequest);
        userService.deleteProfileImage(userId);
        userService.updateProfileImage(userId, null);
        return ApiResponse.success("프로필 사진이 삭제되었습니다.");
    }

    // 유저 이름 수정
    @PatchMapping("/me/username")
    public ApiResponse<Void> updateUsername(@RequestBody @Valid UpdateUsernameRequest request,
                                             HttpServletRequest httpRequest) {
        Long userId = resolveUserId(httpRequest);
        userService.updateUsername(userId, request.getNewUsername());
        return ApiResponse.success("사용자명이 수정되었습니다.");
    }

    // 유저 비밀번호 수정
    @PatchMapping("/me/password")
    public ApiResponse<Void> updatePassword(@RequestBody @Valid UpdatePasswordRequest request,
                                             HttpServletRequest httpRequest) {
        Long userId = resolveUserId(httpRequest);
        userService.updatePassword(userId, request.getCurrentPassword(), request.getNewPassword());
        return ApiResponse.success("비밀번호가 수정되었습니다.");
    }

    // api_key 재발급
    @PostMapping("/api-key/reissue")
    public ApiResponse<MaskedApiKeyResponse> reissueApiKey(HttpServletRequest request) {
        log.debug("API Key 재발급 요청");
        Long userId = resolveUserId(request);
        String maskedApiKey = userService.reissueApiKey(userId);
        return ApiResponse.success("API Key가 새로 발급되었습니다.",
                new MaskedApiKeyResponse("API Key가 새로 발급되었습니다.", maskedApiKey));
    }

    // api_key 전체(마스킹 안 된) 조회
    @GetMapping("/api-key")
    public ApiResponse<ApiKeyResponse> getApiKey(HttpServletRequest request) {
        log.debug("API Key 전체 조회 요청");
        Long userId = resolveUserId(request);
        String apiKey = userService.getRawApiKey(userId);
        return ApiResponse.success("API Key 조회 성공",
                new ApiKeyResponse("API Key 조회 성공", apiKey));
    }

    // 유저 탈퇴
    @DeleteMapping("/delete")
    public ApiResponse<Boolean> deleteUser(HttpServletRequest request) {
        Long userId = resolveUserId(request);
        return ApiResponse.success(userService.deleteUserById(userId));
    }

    private Long resolveUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) {
            throw new AuthException("인증이 필요합니다.");
        }
        return userId;
    }
}
