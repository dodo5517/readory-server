package me.dodo.readingnotes.service;

import me.dodo.readingnotes.domain.Book;
import me.dodo.readingnotes.dto.book.BookCandidate;
import me.dodo.readingnotes.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookRefreshServiceTest {

    @Mock
    BookRepository bookRepo;

    @InjectMocks
    BookRefreshService bookRefreshService;

    private Book book;
    private LocalDateTime oldUpdatedAt;

    @BeforeEach
    void setUp() {
        book = Book.createFrom("옛 제목", "옛 저자", "옛 출판사",
                "1111111111", "9781111111111", "http://old/cover.jpg", LocalDate.of(2000, 1, 1));
        ReflectionTestUtils.setField(book, "id", 1L);
        // updatedAt을 과거로 밀어 markRefreshed 효과를 검증할 수 있게 함
        oldUpdatedAt = LocalDateTime.now().minusDays(200);
        ReflectionTestUtils.setField(book, "updatedAt", oldUpdatedAt);
    }

    private BookCandidate candidate(String isbn13) {
        BookCandidate c = new BookCandidate();
        c.setSource("KAKAO");
        c.setTitle("새 제목");
        c.setAuthor("새 저자");
        c.setPublisher("새 출판사");
        c.setIsbn10("2222222222");
        c.setIsbn13(isbn13);
        c.setThumbnailUrl("http://new/cover.jpg");
        c.setPublishedDate(LocalDate.of(2020, 5, 5));
        return c;
    }

    @Test
    @DisplayName("ISBN13이 같으면 최신값으로 갱신하고 재확인 시각을 갱신한다")
    void applyRefresh_updatesWhenIsbnMatches() {
        when(bookRepo.findById(1L)).thenReturn(Optional.of(book));

        bookRefreshService.applyRefresh(1L, candidate("9781111111111"));

        assertThat(book.getTitle()).isEqualTo("새 제목");
        assertThat(book.getAuthor()).isEqualTo("새 저자");
        assertThat(book.getPublisher()).isEqualTo("새 출판사");
        assertThat(book.getIsbn10()).isEqualTo("2222222222");
        assertThat(book.getIsbn13()).isEqualTo("9781111111111"); // 조회 키는 그대로 유지
        assertThat(book.getCoverUrl()).isEqualTo("http://new/cover.jpg");
        assertThat(book.getPublishedDate()).isEqualTo(LocalDate.of(2020, 5, 5));
        assertThat(book.getUpdatedAt()).isAfter(oldUpdatedAt); // markRefreshed 반영
    }

    @Test
    @DisplayName("ISBN13이 다르면 다른 도서이므로 반영하지 않는다")
    void applyRefresh_skipsWhenIsbnDiffers() {
        when(bookRepo.findById(1L)).thenReturn(Optional.of(book));

        bookRefreshService.applyRefresh(1L, candidate("9789999999999"));

        assertThat(book.getTitle()).isEqualTo("옛 제목");
        assertThat(book.getUpdatedAt()).isEqualTo(oldUpdatedAt); // 갱신되지 않음
    }

    @Test
    @DisplayName("재조회 결과가 null이면 반영하지 않는다")
    void applyRefresh_skipsWhenFreshIsNull() {
        when(bookRepo.findById(1L)).thenReturn(Optional.of(book));

        bookRefreshService.applyRefresh(1L, null);

        assertThat(book.getTitle()).isEqualTo("옛 제목");
        assertThat(book.getUpdatedAt()).isEqualTo(oldUpdatedAt);
    }

    @Test
    @DisplayName("응답의 빈 필드는 기존 값을 유지한다(coalesce)")
    void applyRefresh_coalescesBlankFields() {
        when(bookRepo.findById(1L)).thenReturn(Optional.of(book));
        BookCandidate sparse = candidate("9781111111111");
        sparse.setAuthor("");          // blank -> 유지
        sparse.setPublisher(null);     // null  -> 유지
        sparse.setThumbnailUrl("   "); // blank -> 유지
        sparse.setIsbn10(null);        // null  -> 유지
        sparse.setPublishedDate(null); // null  -> 유지

        bookRefreshService.applyRefresh(1L, sparse);

        assertThat(book.getTitle()).isEqualTo("새 제목");            // 값 있는 필드는 갱신
        assertThat(book.getAuthor()).isEqualTo("옛 저자");           // 빈 값은 기존 유지
        assertThat(book.getPublisher()).isEqualTo("옛 출판사");
        assertThat(book.getCoverUrl()).isEqualTo("http://old/cover.jpg");
        assertThat(book.getIsbn10()).isEqualTo("1111111111");
        assertThat(book.getPublishedDate()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(book.getUpdatedAt()).isAfter(oldUpdatedAt);
    }

    @Test
    @DisplayName("도서를 찾지 못하면 아무 일도 하지 않는다")
    void applyRefresh_noopWhenBookMissing() {
        when(bookRepo.findById(99L)).thenReturn(Optional.empty());

        // 예외 없이 통과하면 성공
        bookRefreshService.applyRefresh(99L, candidate("9781111111111"));
    }
}
