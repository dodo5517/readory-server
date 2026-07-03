package me.dodo.readingnotes.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenHasherTest {

    @Test
    @DisplayName("같은 입력은 항상 같은 해시를 반환한다 (결정성)")
    void sameInputProducesSameHash() {
        String hash1 = TokenHasher.sha256Hex("refresh-token-value");
        String hash2 = TokenHasher.sha256Hex("refresh-token-value");

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("SHA-256 해시는 64자리 16진수 문자열이다")
    void hashIs64HexChars() {
        String hash = TokenHasher.sha256Hex("anything");

        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("알려진 입력에 대해 기대한 SHA-256 값을 반환한다")
    void knownVector() {
        // "hello"의 표준 SHA-256 (소문자 hex)
        assertThat(TokenHasher.sha256Hex("hello"))
                .isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }

    @Test
    @DisplayName("서로 다른 입력은 다른 해시를 반환한다")
    void differentInputsProduceDifferentHashes() {
        assertThat(TokenHasher.sha256Hex("token-a"))
                .isNotEqualTo(TokenHasher.sha256Hex("token-b"));
    }

    @Test
    @DisplayName("빈 문자열도 정상적으로 해싱한다")
    void hashesEmptyString() {
        assertThat(TokenHasher.sha256Hex(""))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }
}