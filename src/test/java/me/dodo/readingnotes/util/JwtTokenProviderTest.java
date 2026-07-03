package me.dodo.readingnotes.util;

import jakarta.servlet.http.HttpServletRequest;
import me.dodo.readingnotes.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtTokenProviderTest {

    // HMAC-SHA256 키는 최소 256비트(32바이트) 필요. 아래는 32바이트 문자열의 BASE64.
    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        // @Value 주입 대신 리플렉션으로 secret 설정 후 @PostConstruct 로직 수동 실행
        ReflectionTestUtils.setField(provider, "secret", SECRET);
        provider.init();
    }

    private User user() {
        User u = new User();
        u.setId(100L);
        u.setEmail("reader@readory.app");
        u.setRole("USER");
        return u;
    }

    @Test
    @DisplayName("정상 발급된 access token은 유효성 검사를 통과한다")
    void validToken() {
        String token = provider.createAccessToken(user());

        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("access token에서 userId 클레임을 추출한다")
    void extractsUserId() {
        String token = provider.createAccessToken(user());

        assertThat(provider.getUserIdFromToken(token)).isEqualTo(100L);
    }

    @Test
    @DisplayName("access token에서 role 클레임을 추출한다")
    void extractsRole() {
        String token = provider.createAccessToken(user());

        assertThat(provider.getRoleFromToken(token)).isEqualTo("USER");
    }

    @Test
    @DisplayName("위·변조된 토큰(서명 불일치)은 유효성 검사를 통과하지 못한다")
    void rejectsTamperedToken() {
        String token = provider.createAccessToken(user());
        // 서명부(마지막 세그먼트)의 한 글자를 다른 글자로 바꿔 서명을 깨뜨린다
        char last = token.charAt(token.length() - 1);
        char replacement = (last == 'A') ? 'B' : 'A';
        String tampered = token.substring(0, token.length() - 1) + replacement;

        assertThat(provider.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("형식이 깨진 문자열은 유효성 검사를 통과하지 못한다")
    void rejectsMalformedToken() {
        assertThat(provider.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰은 유효성 검사를 통과하지 못한다")
    void rejectsTokenSignedWithDifferentKey() {
        JwtTokenProvider other = new JwtTokenProvider();
        ReflectionTestUtils.setField(other, "secret",
                "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA="); // 다른 32바이트 키
        other.init();
        String foreignToken = other.createAccessToken(user());

        assertThat(provider.validateToken(foreignToken)).isFalse();
    }

    @Test
    @DisplayName("새로 발급한 토큰의 남은 시간은 0보다 크다")
    void remainingSecondsPositive() {
        String token = provider.createAccessToken(user());

        assertThat(provider.getRemainingSeconds(token)).isGreaterThan(0);
    }

    @Test
    @DisplayName("Authorization 헤더의 Bearer 토큰을 추출한다")
    void extractTokenFromBearerHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer abc.def.ghi");

        assertThat(JwtTokenProvider.extractToken(request)).isEqualTo("abc.def.ghi");
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 예외를 던진다")
    void extractTokenThrowsWhenHeaderMissing() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);

        assertThatThrownBy(() -> JwtTokenProvider.extractToken(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Bearer 타입이 아니면 예외를 던진다")
    void extractTokenThrowsWhenNotBearer() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Basic abc");

        assertThatThrownBy(() -> JwtTokenProvider.extractToken(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Bearer 뒤 토큰이 비어 있거나 \"null\"이면 예외를 던진다")
    void extractTokenThrowsWhenBlankOrNullLiteral() {
        HttpServletRequest blank = mock(HttpServletRequest.class);
        when(blank.getHeader("Authorization")).thenReturn("Bearer    ");
        assertThatThrownBy(() -> JwtTokenProvider.extractToken(blank))
                .isInstanceOf(IllegalArgumentException.class);

        HttpServletRequest nullLiteral = mock(HttpServletRequest.class);
        when(nullLiteral.getHeader("Authorization")).thenReturn("Bearer null");
        assertThatThrownBy(() -> JwtTokenProvider.extractToken(nullLiteral))
                .isInstanceOf(IllegalArgumentException.class);
    }
}