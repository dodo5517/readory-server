package me.dodo.readingnotes.external;

import me.dodo.readingnotes.dto.book.BookCandidate;
import java.util.List;

public interface BookSearchClient {
    List<BookCandidate> search(String rawTitle, String rawAuthor, int limit);
}
