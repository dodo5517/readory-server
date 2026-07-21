package me.dodo.readingnotes.scheduler;

import me.dodo.readingnotes.domain.Book;
import me.dodo.readingnotes.dto.book.BookCandidate;
import me.dodo.readingnotes.external.client.KakaoBookClient;
import me.dodo.readingnotes.repository.BookRepository;
import me.dodo.readingnotes.service.BookRefreshService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookRefreshSchedulerTest {

    @Mock
    BookRepository bookRepository;

    @Mock
    KakaoBookClient kakaoBookClient;

    @Mock
    BookRefreshService bookRefreshService;

    @InjectMocks
    BookRefreshScheduler scheduler;

    private Book bookWith(long id, String isbn13) {
        Book b = Book.createFrom("t" + id, "a" + id, "출판사",
                "10", isbn13, "http://cover", LocalDate.of(2001, 1, 1));
        ReflectionTestUtils.setField(b, "id", id);
        return b;
    }

    private BookCandidate candidateWith(String isbn13) {
        BookCandidate c = new BookCandidate();
        c.setIsbn13(isbn13);
        c.setTitle("fresh");
        return c;
    }

    @Test
    @DisplayName("stale 도서를 ISBN으로 재조회해 각각 반영한다")
    void refreshStaleBooks_appliesEachResult() {
        Book b1 = bookWith(1L, "9781111111111");
        Book b2 = bookWith(2L, "9782222222222");
        when(bookRepository.findStaleBooks(any(), any())).thenReturn(List.of(b1, b2));
        when(kakaoBookClient.searchByIsbn("9781111111111")).thenReturn(List.of(candidateWith("9781111111111")));
        when(kakaoBookClient.searchByIsbn("9782222222222")).thenReturn(List.of(candidateWith("9782222222222")));

        scheduler.refreshStaleBooks();

        verify(bookRefreshService).applyRefresh(eq(1L), any(BookCandidate.class));
        verify(bookRefreshService).applyRefresh(eq(2L), any(BookCandidate.class));
    }

    @Test
    @DisplayName("Kakao 결과가 없으면 갱신하지 않고 넘어간다")
    void refreshStaleBooks_skipsWhenNoResult() {
        Book b1 = bookWith(1L, "9781111111111");
        when(bookRepository.findStaleBooks(any(), any())).thenReturn(List.of(b1));
        when(kakaoBookClient.searchByIsbn("9781111111111")).thenReturn(List.of()); // 결과 없음

        scheduler.refreshStaleBooks();

        verify(bookRefreshService, never()).applyRefresh(anyLong(), any());
    }

    @Test
    @DisplayName("한 건이 예외여도 나머지 도서는 계속 처리한다")
    void refreshStaleBooks_continuesAfterException() {
        Book b1 = bookWith(1L, "9781111111111");
        Book b2 = bookWith(2L, "9782222222222");
        when(bookRepository.findStaleBooks(any(), any())).thenReturn(List.of(b1, b2));
        when(kakaoBookClient.searchByIsbn("9781111111111")).thenThrow(new RuntimeException("API 오류"));
        when(kakaoBookClient.searchByIsbn("9782222222222")).thenReturn(List.of(candidateWith("9782222222222")));

        scheduler.refreshStaleBooks();

        verify(bookRefreshService, never()).applyRefresh(eq(1L), any());
        verify(bookRefreshService).applyRefresh(eq(2L), any(BookCandidate.class));
    }

    @Test
    @DisplayName("갱신 대상이 없으면 외부 호출도 하지 않는다")
    void refreshStaleBooks_noopWhenEmpty() {
        when(bookRepository.findStaleBooks(any(), any())).thenReturn(List.of());

        scheduler.refreshStaleBooks();

        verifyNoInteractions(kakaoBookClient);
        verifyNoInteractions(bookRefreshService);
    }
}
