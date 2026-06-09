package me.dodo.readingnotes.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import me.dodo.readingnotes.domain.ApiLog;
import me.dodo.readingnotes.domain.ReadingRecord;
import me.dodo.readingnotes.domain.UserAuthLog;
import me.dodo.readingnotes.dto.admin.*;
import me.dodo.readingnotes.dto.auth.ApiKeyResponse;
import me.dodo.readingnotes.dto.common.ApiResponse;
import me.dodo.readingnotes.dto.common.MaskedApiKeyResponse;
import me.dodo.readingnotes.dto.common.PageResponse;
import me.dodo.readingnotes.dto.log.ApiLogDetailResponse;
import me.dodo.readingnotes.dto.log.ApiLogListResponse;
import me.dodo.readingnotes.dto.log.AuthLogDetailResponse;
import me.dodo.readingnotes.dto.log.AuthLogListResponse;
import me.dodo.readingnotes.dto.notice.NoticeResponse;
import me.dodo.readingnotes.dto.notice.NoticeUpdateRequest;
import me.dodo.readingnotes.dto.user.*;
import me.dodo.readingnotes.exception.AuthException;
import me.dodo.readingnotes.service.*;
import me.dodo.readingnotes.util.CookieUtil;
import me.dodo.readingnotes.util.ImageResizer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AuthService authService;
    private UserService userService;
    private S3Service s3Service;
    private LogService logService;
    private BookService bookService;
    private ReadingRecordService readingRecordService;
    private ImageResizer imageResizer;
    private NoticeService noticeService;

    public AdminController(UserService userService,
                           S3Service s3Service, AuthService authService,
                           LogService logService,
                           BookService bookService,
                           ReadingRecordService readingRecordService,
                           ImageResizer imageResizer,
                           NoticeService noticeService) {
        this.userService = userService;
        this.s3Service = s3Service;
        this.authService = authService;
        this.logService = logService;
        this.bookService = bookService;
        this.readingRecordService = readingRecordService;
        this.imageResizer = imageResizer;
        this.noticeService = noticeService;
    }

    // ##############################
    // 유저
    // ##############################

    @GetMapping("/users")
    public ApiResponse<PageResponse<AdminPageUserResponse>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String role,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        return ApiResponse.success(PageResponse.from(userService.findAllUsers(keyword, provider, role, pageable)));
    }

    @GetMapping("/users/{id}")
    public ApiResponse<AdminPageUserResponse> getUser(@PathVariable Long id,
                                                       HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        return ApiResponse.success(new AdminPageUserResponse(userService.findUserById(id)));
    }

    @PatchMapping("/users/{id}/username")
    public ApiResponse<Void> updateUsername(@PathVariable Long id,
                                             @RequestBody @Valid UpdateUsernameRequest request,
                                             HttpServletRequest servletRequest) {
        Long adminId = extractAdminId(servletRequest);
        userService.assertAdmin(adminId);
        userService.updateUsername(id, request.getNewUsername());
        return ApiResponse.success("사용자명이 수정되었습니다.");
    }

    @PatchMapping("/users/{id}/password")
    public ApiResponse<Void> updatePassword(@PathVariable Long id,
                                             @RequestBody @Valid UpdatePasswordAdminRequest request,
                                             HttpServletRequest servletRequest) {
        Long adminId = extractAdminId(servletRequest);
        userService.assertAdmin(adminId);
        userService.updatePasswordAdmin(id, request.getNewPassword());
        return ApiResponse.success("비밀번호가 수정되었습니다.");
    }

    @PostMapping("/users/{id}/reset")
    public ResponseEntity<ApiResponse<String>> userReset(@PathVariable Long id,
                                                          HttpServletRequest request,
                                                          HttpServletResponse httpResponse) throws Exception {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        authService.logoutAllDevices(id);
        ResponseCookie deleteCookie = CookieUtil.deleteRefreshTokenCookie();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
        String newPw = userService.reset(id);
        return ResponseEntity.ok(ApiResponse.success("인증이 초기화되었습니다.", newPw));
    }

    @PostMapping("/users/{id}/profile-image")
    public ApiResponse<String> uploadProfileImage(@PathVariable Long id,
                                                   @RequestParam("image") MultipartFile image,
                                                   HttpServletRequest request) throws Exception {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        userService.deleteProfileImage(id);
        byte[] resizedImage = imageResizer.resizeImageKeepRatio(image);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        String fileName = "user-" + id + "_" + timestamp;
        String imageUrl = s3Service.uploadProfileImage(resizedImage, fileName, image.getContentType());
        userService.updateProfileImage(id, imageUrl);
        return ApiResponse.success(null, imageUrl);
    }

    @DeleteMapping("/users/{id}/profile-image")
    public ApiResponse<Void> deleteProfileImage(@PathVariable Long id,
                                                 HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        userService.deleteProfileImage(id);
        userService.updateProfileImage(id, null);
        return ApiResponse.success("프로필 사진이 삭제되었습니다.");
    }

    @PostMapping("/users/{id}/api-key/reissue")
    public ApiResponse<MaskedApiKeyResponse> reissueApiKey(@PathVariable Long id,
                                                            HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        String maskedApiKey = userService.reissueApiKey(id);
        return ApiResponse.success("API Key가 새로 발급되었습니다.",
                new MaskedApiKeyResponse("API Key가 새로 발급되었습니다.", maskedApiKey));
    }

    @GetMapping("/users/{id}/api-key")
    public ApiResponse<ApiKeyResponse> getApiKey(@PathVariable Long id,
                                                  HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        String apiKey = userService.getRawApiKey(id);
        return ApiResponse.success("API Key 조회 성공",
                new ApiKeyResponse("API Key 조회 성공", apiKey));
    }

    @DeleteMapping("/users/{id}/delete")
    public ApiResponse<Boolean> deleteUser(@PathVariable Long id,
                                            HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        return ApiResponse.success(userService.deleteUserById(id));
    }

    @PostMapping("/users/{id}/status")
    public ApiResponse<Void> changeUserStatus(@PathVariable Long id,
                                               @RequestBody ChangeUserStatusRequest request,
                                               HttpServletRequest servletRequest) {
        Long adminId = extractAdminId(servletRequest);
        userService.assertAdmin(adminId);
        userService.changeUserStatus(id, request.getStatus());
        return ApiResponse.success("상태가 변경되었습니다.");
    }

    @PostMapping("/users/{id}/role")
    public ApiResponse<Void> changeUserRole(@PathVariable Long id,
                                             @RequestBody String role,
                                             HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        userService.changeUserRole(id, role);
        return ApiResponse.success("권한이 변경되었습니다.");
    }

    @PostMapping("/users/{id}/logout")
    public ApiResponse<Void> logoutUser(@PathVariable Long id,
                                         HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        authService.logoutAllDevices(id);
        return ApiResponse.success("모든 기기에서 로그아웃 되었습니다.");
    }

    // ##############################
    // 유저 인증 로그
    // ##############################

    @GetMapping("/auth/logs")
    public ApiResponse<PageResponse<AuthLogListResponse>> getAuthLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserAuthLog.AuthEventType type,
            @RequestParam(required = false) UserAuthLog.AuthResult result,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        return ApiResponse.success(PageResponse.from(logService.findAuthLogs(keyword, type, result, pageable)));
    }

    @GetMapping("/auth/logs/{id}")
    public ApiResponse<AuthLogDetailResponse> getLogDetail(@PathVariable Long id,
                                                            HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        return ApiResponse.success(logService.findAuthLog(id));
    }

    // ##############################
    // API 로그
    // ##############################

    @GetMapping("/api/logs")
    public ApiResponse<PageResponse<ApiLogListResponse>> getApiLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ApiLog.Result result,
            @RequestParam(required = false) Integer statusCode,
            @RequestParam(required = false) String method,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        return ApiResponse.success(PageResponse.from(logService.findApiLogs(keyword, result, statusCode, method, pageable)));
    }

    @GetMapping("/api/logs/{id}")
    public ApiResponse<ApiLogDetailResponse> getApiLogDetail(@PathVariable Long id,
                                                              HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        return ApiResponse.success(logService.findApiLog(id));
    }

    // ##############################
    // 책 관리
    // ##############################

    @GetMapping("/books")
    public ApiResponse<PageResponse<BookListResponse>> getAllBooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "false") Boolean includeDeleted,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        return ApiResponse.success(PageResponse.from(bookService.findAllBooksForAdmin(keyword, includeDeleted, pageable)));
    }

    @GetMapping("/books/{id}")
    public ApiResponse<BookDetailResponse> getBook(@PathVariable Long id,
                                                    HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        return ApiResponse.success(bookService.findBookById(id));
    }

    @DeleteMapping("/books/{id}")
    public ApiResponse<Void> softDeleteBook(@PathVariable Long id,
                                             HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        bookService.softDeleteBook(id);
        return ApiResponse.success("책이 삭제되었습니다.");
    }

    @DeleteMapping("/books/{id}/permanent")
    public ApiResponse<Void> hardDeleteBook(@PathVariable Long id,
                                             HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        bookService.hardDeleteBook(id);
        return ApiResponse.success("책이 영구 삭제되었습니다.");
    }

    @PostMapping("/books/{id}/restore")
    public ApiResponse<Void> restoreBook(@PathVariable Long id,
                                          HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        bookService.restoreBook(id);
        return ApiResponse.success("책이 복구되었습니다.");
    }

    @GetMapping("/records/stats/books")
    public ApiResponse<AdminBookStatsResponse> getBookStats(HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        return ApiResponse.success(bookService.getBookStatsForAdmin());
    }

    // ##############################
    // 독서 기록 관리
    // ##############################

    @GetMapping("/records")
    public ApiResponse<PageResponse<AdminRecordListResponse>> getRecordsByUser(
            @RequestParam Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ReadingRecord.MatchStatus matchStatus,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        return ApiResponse.success(PageResponse.from(
                readingRecordService.findRecordsByUserForAdmin(keyword, matchStatus, userId, pageable)));
    }

    @GetMapping("/records/stats")
    public ApiResponse<AdminRecordStatsResponse> getStats(HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        return ApiResponse.success(readingRecordService.getStatsForAdmin());
    }

    @GetMapping("/records/user-activity")
    public ApiResponse<PageResponse<AdminUserActivityResponse>> getUserActivity(
            @PageableDefault(size = 20, sort = "lastRecordedAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        return ApiResponse.success(PageResponse.from(readingRecordService.findUserActivityForAdmin(pageable)));
    }

    @GetMapping("/records/{id}")
    public ApiResponse<AdminRecordDetailResponse> getRecord(@PathVariable Long id,
                                                             HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        return ApiResponse.success(readingRecordService.findRecordByIdForAdmin(id));
    }

    @PatchMapping("/records/{id}")
    public ApiResponse<AdminRecordDetailResponse> updateRecord(
            @PathVariable Long id,
            @RequestBody AdminRecordUpdateRequest updateRequest,
            HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        return ApiResponse.success(readingRecordService.updateRecordForAdmin(id, updateRequest));
    }

    @DeleteMapping("/records/{id}")
    public ApiResponse<Void> deleteRecord(@PathVariable Long id,
                                           HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        readingRecordService.deleteRecordForAdmin(id);
        return ApiResponse.success("기록이 삭제되었습니다.");
    }

    @PostMapping("/records/clean-sentences")
    public ApiResponse<Map<String, Integer>> cleanAllSentences(HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        return ApiResponse.success(readingRecordService.cleanAllSentences());
    }

    // ##############################
    // 공지 관리
    // ##############################

    @GetMapping("/notices")
    public ApiResponse<List<NoticeResponse>> getAllNotices(HttpServletRequest request) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        return ApiResponse.success(noticeService.getAllNotices());
    }

    @PostMapping("/notice")
    public ApiResponse<NoticeResponse> createNotice(HttpServletRequest request,
                                                     @RequestBody NoticeUpdateRequest body) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        return ApiResponse.success(noticeService.createNotice(body));
    }

    @PatchMapping("/notice/{id}")
    public ApiResponse<NoticeResponse> updateNotice(HttpServletRequest request,
                                                     @PathVariable Long id,
                                                     @RequestBody NoticeUpdateRequest body) {
        Long adminId = extractAdminId(request);
        userService.assertAdmin(adminId);
        return ApiResponse.success(noticeService.updateNotice(id, body));
    }

    private Long extractAdminId(HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("USER_ID");
        if (adminId == null) {
            throw new AuthException("인증이 필요합니다.");
        }
        return adminId;
    }
}
