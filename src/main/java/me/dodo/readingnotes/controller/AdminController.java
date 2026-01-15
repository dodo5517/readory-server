package me.dodo.readingnotes.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import me.dodo.readingnotes.domain.User;
import me.dodo.readingnotes.domain.UserAuthLog;
import me.dodo.readingnotes.dto.auth.ApiKeyResponse;
import me.dodo.readingnotes.dto.common.MaskedApiKeyResponse;
import me.dodo.readingnotes.dto.log.LogDetailResponse;
import me.dodo.readingnotes.dto.log.LogListResponse;
import me.dodo.readingnotes.dto.user.*;
import me.dodo.readingnotes.dto.admin.AdminPageUserResponse;
import me.dodo.readingnotes.service.AuthService;
import me.dodo.readingnotes.service.LogService;
import me.dodo.readingnotes.service.S3Service;
import me.dodo.readingnotes.service.UserService;
import me.dodo.readingnotes.util.JwtTokenProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AuthService authService;
    private JwtTokenProvider jwtTokenProvider;
    private UserService userService;
    private S3Service s3Service;
    private LogService logService;

    public AdminController(JwtTokenProvider jwtTokenProvider,
                           UserService userService,
                           S3Service s3Service, AuthService authService,
                           LogService logService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
        this.s3Service = s3Service;
        this.authService = authService;
        this.logService = logService;
    }

    // ##############################
    // 유저
    // ##############################

    // 전체 유저 목록 조회 (기본 = 10개)
    @GetMapping("/users")
    public Page<AdminPageUserResponse> getAllUsers(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String provider,
                                                   @RequestParam(required = false) String role,
                                                   @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                                   HttpServletRequest request) {
        // 토큰에서 adminId 추출
        String accessToken = jwtTokenProvider.extractToken(request);
        jwtTokenProvider.assertValid(accessToken);
        Long adminId = jwtTokenProvider.getUserIdFromToken(accessToken);

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        // 페이지 조회 + 검색
        return userService.findAllUsers(keyword, provider, role, pageable);
    }

    // 특정 유저 조희
    @GetMapping("/users/{id}")
    public AdminPageUserResponse getUser(@PathVariable Long id,
                                         HttpServletRequest request){
        // 토큰에서 adminId 추출
        String accessToken = jwtTokenProvider.extractToken(request);
        jwtTokenProvider.assertValid(accessToken);
        Long adminId = jwtTokenProvider.getUserIdFromToken(accessToken);

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);
        // 유저 정보 조회
        User user = userService.findUserById(id);
        return new AdminPageUserResponse(user);
    }

    // 특정 유저 이름 수정
    @PatchMapping("/users/{id}/username")
    public ResponseEntity<Void> updateUsername(@PathVariable Long id,
                                               @RequestBody @Valid UpdateUsernameRequest request,
                                               HttpServletRequest httpRequest){
        // 토큰에서 adminId 추출
        String accessToken = jwtTokenProvider.extractToken(httpRequest);
        jwtTokenProvider.assertValid(accessToken);
        Long adminId = jwtTokenProvider.getUserIdFromToken(accessToken);

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        // 유저 이름 수정
        userService.updateUsername(id, request.getNewUsername());

        // 상태코드만 반환 (204)
        return ResponseEntity.noContent().build();
    }

    // 특정 유저 비밀번호 덮어씌우기
    @PatchMapping("/users/{id}/password")
    public ResponseEntity<Void> updatePassword(@PathVariable Long id,
                                               @RequestBody @Valid UpdatePasswordAdminRequest request,
                                               HttpServletRequest httpRequest){
        // 토큰에서 adminId 추출
        String accessToken = jwtTokenProvider.extractToken(httpRequest);
        jwtTokenProvider.assertValid(accessToken);
        Long adminId = jwtTokenProvider.getUserIdFromToken(accessToken);

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        // 유저 비밀번호 수정
        userService.updatePasswordAdmin(id, request.getNewPassword());

        // 상태코드만 반환 (204)
        return ResponseEntity.noContent().build();
    }

    // 특정 유저 프로필 사진 업로드
    @PostMapping("/users/{id}/profile-image")
    public ResponseEntity<String> uploadProfileImage(@PathVariable Long id,
                                                     @RequestParam("image") MultipartFile image,
                                                     HttpServletRequest httpRequest) throws Exception {
        // 토큰에서 adminId 추출
        String accessToken = jwtTokenProvider.extractToken(httpRequest);
        jwtTokenProvider.assertValid(accessToken);
        Long adminId = jwtTokenProvider.getUserIdFromToken(accessToken);

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        // 기존 이미지 삭제
        userService.deleteProfileImage(id);

        // 새 이미지 업로드
        String fileName = "user-" + id + "_" + UUID.randomUUID();
        String imageUrl = s3Service.uploadProfileImage(image, fileName);

        userService.updateProfileImage(id, imageUrl); // DB에 URL 저장

        return ResponseEntity.ok(imageUrl);
    }

    // 특정 유저 프로필 사진 삭제
    @DeleteMapping("/users/{id}/profile-image")
    public ResponseEntity<Void> deleteProfileImage(@PathVariable Long id,
                                                   HttpServletRequest httpRequest) {
        // 토큰에서 adminId 추출
        String accessToken = jwtTokenProvider.extractToken(httpRequest);
        jwtTokenProvider.assertValid(accessToken);
        Long adminId = jwtTokenProvider.getUserIdFromToken(accessToken);

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        // 기존 이미지 삭제
        userService.deleteProfileImage(id);
        userService.updateProfileImage(id, null); // DB에서 URL 제거
        return ResponseEntity.noContent().build();
    }

    // 특정 유저의 api_key 재발급
    @PostMapping("/users/{id}/api-key/reissue")
    public ResponseEntity<MaskedApiKeyResponse> reissueApiKey(@PathVariable Long id,
                                                              HttpServletRequest request) {
        // 토큰에서 adminId 추출
        String accessToken = jwtTokenProvider.extractToken(request);
        jwtTokenProvider.assertValid(accessToken);
        Long adminId = jwtTokenProvider.getUserIdFromToken(accessToken);

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        String maskedApiKey = userService.reissueApiKey(id);
        return ResponseEntity.ok(new MaskedApiKeyResponse("API Key가 새로 발급되었습니다.", maskedApiKey));
    }

    // 특정 유저의 api_key 전체(마스킹 안 된) 조회
    @GetMapping("/users/{id}/api-key")
    public ResponseEntity<ApiKeyResponse> getApiKey(@PathVariable Long id,
                                                    HttpServletRequest request) {
        // 토큰에서 adminId 추출
        String accessToken = jwtTokenProvider.extractToken(request);
        jwtTokenProvider.assertValid(accessToken);
        Long adminId = jwtTokenProvider.getUserIdFromToken(accessToken);

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        String apiKey = userService.getRawApiKey(id);
        return ResponseEntity.ok(new ApiKeyResponse("API Key 조회 성공", apiKey));
    }

    // 유저 탈퇴
    @DeleteMapping("/users/{id}/delete")
    public Boolean deleteUser(@PathVariable Long id,
                              HttpServletRequest request){
        // 토큰에서 adminId 추출
        String accessToken = jwtTokenProvider.extractToken(request);
        jwtTokenProvider.assertValid(accessToken);
        Long adminId = jwtTokenProvider.getUserIdFromToken(accessToken);

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        return userService.deleteUserById(id);
    }
    
    // 특정 유저 상태 수정
    @PostMapping("/users/{id}/status")
    public ResponseEntity<Void> changeUserStatus(@PathVariable Long id,
                                                 @RequestBody ChangeUserStatusRequest request,
                                                 HttpServletRequest servletRequest){
        // 토큰에서 adminId 추출
        String accessToken = jwtTokenProvider.extractToken(servletRequest);
        jwtTokenProvider.assertValid(accessToken);
        Long adminId = jwtTokenProvider.getUserIdFromToken(accessToken);

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        // 상태 수정
        userService.changeUserStatus(id, request.getStatus());

        // 상태코드만 반환 (204)
        return ResponseEntity.noContent().build();
    }

    // 특정 유저 권한(역할) 수정
    @PostMapping("/users/{id}/role")
    public ResponseEntity<Void> changeUserRole(@PathVariable Long id,
                                               @RequestBody String role,
                                               HttpServletRequest servletRequest){
        // 토큰에서 adminId 추출
        String accessToken = jwtTokenProvider.extractToken(servletRequest);
        jwtTokenProvider.assertValid(accessToken);
        Long adminId = jwtTokenProvider.getUserIdFromToken(accessToken);

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        // 권한(역할) 수정
        userService.changeUserRole(id, role);

        // 상태코드만 반환 (204)
        return ResponseEntity.noContent().build();
    }

    // 특정 유저 기기 전체 로그아웃
    @PostMapping("/users/{id}/logout")
    public ResponseEntity<Void> logoutUser(@PathVariable Long id,
                                           HttpServletRequest servletRequest){
        // 토큰에서 adminId 추출
        String accessToken = jwtTokenProvider.extractToken(servletRequest);
        jwtTokenProvider.assertValid(accessToken);
        Long adminId = jwtTokenProvider.getUserIdFromToken(accessToken);

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        // 유저의 모든 기기 로그아웃
        authService.logoutAllDevices(id);

        // 상태코드만 반환 (204)
        return ResponseEntity.noContent().build();
    }

    // ##############################
    // 유저 인증 로그
    // ##############################

    // 전체 로그 조회
    @GetMapping("/auth/logs")
    public Page<LogListResponse> getLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserAuthLog.AuthEventType type,
            @RequestParam(required = false) UserAuthLog.AuthResult result,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request
    ) {
        // 토큰에서 adminId 추출
        String accessToken = jwtTokenProvider.extractToken(request);
        jwtTokenProvider.assertValid(accessToken);
        Long adminId = jwtTokenProvider.getUserIdFromToken(accessToken);

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        // 전체 로그 조회
        return logService.findLogs(keyword, type, result, pageable);
    }

    // 특정 로그 조회
    @GetMapping("/auth/logs/{id}")
    public LogDetailResponse getLogDetail(@PathVariable Long id,
                                          HttpServletRequest request){
        // 토큰에서 adminId 추출
        String accessToken = jwtTokenProvider.extractToken(request);
        jwtTokenProvider.assertValid(accessToken);
        Long adminId = jwtTokenProvider.getUserIdFromToken(accessToken);

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        // 특정 로그 조회
        return logService.findLog(id);
    }
}
