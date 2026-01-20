package me.dodo.readingnotes.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import me.dodo.readingnotes.domain.ApiLog;
import me.dodo.readingnotes.domain.User;
import me.dodo.readingnotes.domain.UserAuthLog;
import me.dodo.readingnotes.dto.admin.BookDetailResponse;
import me.dodo.readingnotes.dto.admin.BookListResponse;
import me.dodo.readingnotes.dto.auth.ApiKeyResponse;
import me.dodo.readingnotes.dto.common.MaskedApiKeyResponse;
import me.dodo.readingnotes.dto.log.ApiLogDetailResponse;
import me.dodo.readingnotes.dto.log.ApiLogListResponse;
import me.dodo.readingnotes.dto.log.AuthLogDetailResponse;
import me.dodo.readingnotes.dto.log.AuthLogListResponse;
import me.dodo.readingnotes.dto.user.*;
import me.dodo.readingnotes.dto.admin.AdminPageUserResponse;
import me.dodo.readingnotes.service.*;
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
    private UserService userService;
    private S3Service s3Service;
    private LogService logService;
    private BookService bookService;

    public AdminController(UserService userService,
                           S3Service s3Service, AuthService authService,
                           LogService logService,
                           BookService bookService) {
        this.userService = userService;
        this.s3Service = s3Service;
        this.authService = authService;
        this.logService = logService;
        this.bookService = bookService;
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
        // adminId 추출
        Long adminId = (Long) request.getAttribute("USER_ID");
        if (adminId == null) {
            throw new IllegalArgumentException("userId가 없습니다.");
        }

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        // 페이지 조회 + 검색
        return userService.findAllUsers(keyword, provider, role, pageable);
    }

    // 특정 유저 조희
    @GetMapping("/users/{id}")
    public AdminPageUserResponse getUser(@PathVariable Long id,
                                         HttpServletRequest request){
        // adminId 추출
        Long adminId = (Long) request.getAttribute("USER_ID");
        if (adminId == null) {
            throw new IllegalArgumentException("userId가 없습니다.");
        }

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
                                               HttpServletRequest servletRequest){
        // adminId 추출
        Long adminId = (Long) servletRequest.getAttribute("USER_ID");
        if (adminId == null) {
            throw new IllegalArgumentException("userId가 없습니다.");
        }

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
                                               HttpServletRequest servletRequest){
        // adminId 추출
        Long adminId = (Long) servletRequest.getAttribute("USER_ID");
        if (adminId == null) {
            throw new IllegalArgumentException("userId가 없습니다.");
        }

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
                                                     HttpServletRequest request) throws Exception {
        // adminId 추출
        Long adminId = (Long) request.getAttribute("USER_ID");
        if (adminId == null) {
            throw new IllegalArgumentException("userId가 없습니다.");
        }

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
                                                   HttpServletRequest request) {
        // adminId 추출
        Long adminId = (Long) request.getAttribute("USER_ID");
        if (adminId == null) {
            throw new IllegalArgumentException("userId가 없습니다.");
        }
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
        // adminId 추출
        Long adminId = (Long) request.getAttribute("USER_ID");
        if (adminId == null) {
            throw new IllegalArgumentException("userId가 없습니다.");
        }

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        String maskedApiKey = userService.reissueApiKey(id);
        return ResponseEntity.ok(new MaskedApiKeyResponse("API Key가 새로 발급되었습니다.", maskedApiKey));
    }

    // 특정 유저의 api_key 전체(마스킹 안 된) 조회
    @GetMapping("/users/{id}/api-key")
    public ResponseEntity<ApiKeyResponse> getApiKey(@PathVariable Long id,
                                                    HttpServletRequest request) {
        // adminId 추출
        Long adminId = (Long) request.getAttribute("USER_ID");
        if (adminId == null) {
            throw new IllegalArgumentException("userId가 없습니다.");
        }

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        String apiKey = userService.getRawApiKey(id);
        return ResponseEntity.ok(new ApiKeyResponse("API Key 조회 성공", apiKey));
    }

    // 유저 탈퇴
    @DeleteMapping("/users/{id}/delete")
    public Boolean deleteUser(@PathVariable Long id,
                              HttpServletRequest request){
        // adminId 추출
        Long adminId = (Long) request.getAttribute("USER_ID");
        if (adminId == null) {
            throw new IllegalArgumentException("userId가 없습니다.");
        }

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        return userService.deleteUserById(id);
    }
    
    // 특정 유저 상태 수정
    @PostMapping("/users/{id}/status")
    public ResponseEntity<Void> changeUserStatus(@PathVariable Long id,
                                                 @RequestBody ChangeUserStatusRequest request,
                                                 HttpServletRequest servletRequest){
        // adminId 추출
        Long adminId = (Long) servletRequest.getAttribute("USER_ID");
        if (adminId == null) {
            throw new IllegalArgumentException("userId가 없습니다.");
        }

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
                                               HttpServletRequest request){
        // adminId 추출
        Long adminId = (Long) request.getAttribute("USER_ID");
        if (adminId == null) {
            throw new IllegalArgumentException("userId가 없습니다.");
        }
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
                                           HttpServletRequest request){
        // adminId 추출
        Long adminId = (Long) request.getAttribute("USER_ID");
        if (adminId == null) {
            throw new IllegalArgumentException("userId가 없습니다.");
        }

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
    public Page<AuthLogListResponse> getAuthLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserAuthLog.AuthEventType type,
            @RequestParam(required = false) UserAuthLog.AuthResult result,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request
    ) {
        // adminId 추출
        Long adminId = (Long) request.getAttribute("USER_ID");
        if (adminId == null) {
            throw new IllegalArgumentException("userId가 없습니다.");
        }

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        // 전체 로그 조회
        return logService.findAuthLogs(keyword, type, result, pageable);
    }

    // 특정 로그 조회
    @GetMapping("/auth/logs/{id}")
    public AuthLogDetailResponse getLogDetail(@PathVariable Long id,
                                              HttpServletRequest request){
        // adminId 추출
        Long adminId = (Long) request.getAttribute("USER_ID");
        if (adminId == null) {
            throw new IllegalArgumentException("userId가 없습니다.");
        }
        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        // 특정 로그 조회
        return logService.findAuthLog(id);
    }

    // ##############################
    // API 로그
    // ##############################

    // 전체 API 로그 조회
    @GetMapping("/api/logs")
    public Page<ApiLogListResponse> getApiLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ApiLog.Result result,
            @RequestParam(required = false) Integer statusCode,
            @RequestParam(required = false) String method,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request
    ) {
        // adminId 추출
        Long adminId = (Long) request.getAttribute("USER_ID");
        if (adminId == null) throw new IllegalArgumentException("userId가 없습니다.");

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        return logService.findApiLogs(keyword, result, statusCode, method, pageable);
    }

    // 특정 API 로그 조회
    @GetMapping("/api/logs/{id}")
    public ApiLogDetailResponse getApiLogDetail(@PathVariable Long id,
                                                HttpServletRequest request) {
        // adminId 추출
        Long adminId = (Long) request.getAttribute("USER_ID");
        if (adminId == null) throw new IllegalArgumentException("userId가 없습니다.");

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        return logService.findApiLog(id);
    }


    // ##############################
    // 책 관리
    // ##############################

    // 전체 책 목록 조회 (기본 = 10개)
    @GetMapping("/books")
    public Page<BookListResponse> getAllBooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "false") Boolean includeDeleted,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {
        // adminId 추출
        Long adminId = (Long) request.getAttribute("USER_ID");
        if (adminId == null) {
            throw new IllegalArgumentException("userId가 없습니다.");
        }

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        // 페이지 조회 + 검색
        return bookService.findAllBooksForAdmin(keyword, includeDeleted, pageable);
    }

    // 특정 책 상세 조회
    @GetMapping("/books/{id}")
    public BookDetailResponse getBook(@PathVariable Long id,
                                      HttpServletRequest request) {
        // adminId 추출
        Long adminId = (Long) request.getAttribute("USER_ID");
        if (adminId == null) {
            throw new IllegalArgumentException("userId가 없습니다.");
        }

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        // 책 정보 조회
        return bookService.findBookById(id);
    }

    // 책 소프트 삭제 (deletedAt 설정)
    @DeleteMapping("/books/{id}")
    public ResponseEntity<Void> softDeleteBook(@PathVariable Long id,
                                               HttpServletRequest request) {
        // adminId 추출
        Long adminId = (Long) request.getAttribute("USER_ID");
        if (adminId == null) {
            throw new IllegalArgumentException("userId가 없습니다.");
        }

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        // 소프트 삭제
        bookService.softDeleteBook(id);

        return ResponseEntity.noContent().build();
    }

    // 책 영구 삭제 (DB에서 완전 삭제)
    @DeleteMapping("/books/{id}/permanent")
    public ResponseEntity<Void> hardDeleteBook(@PathVariable Long id,
                                               HttpServletRequest request) {
        // adminId 추출
        Long adminId = (Long) request.getAttribute("USER_ID");
        if (adminId == null) {
            throw new IllegalArgumentException("userId가 없습니다.");
        }

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        // 영구 삭제
        bookService.hardDeleteBook(id);

        return ResponseEntity.noContent().build();
    }

    // 삭제된 책 복구
    @PostMapping("/books/{id}/restore")
    public ResponseEntity<Void> restoreBook(@PathVariable Long id,
                                            HttpServletRequest request) {
        // adminId 추출
        Long adminId = (Long) request.getAttribute("USER_ID");
        if (adminId == null) {
            throw new IllegalArgumentException("userId가 없습니다.");
        }

        // 관리자 권한 있는지 확인
        userService.assertAdmin(adminId);

        // 복구
        bookService.restoreBook(id);

        return ResponseEntity.noContent().build();
    }

}
