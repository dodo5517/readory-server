package me.dodo.readingnotes.dto.book;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LinkBookRequest {
    private String source;       // "KAKAO"
    private String externalId;   // 공급자별 ID (카카오는 url/ISBN13 등)
    private String isbn13;       // 가능하면 제공
    private String isbn10;
    private String title;        // 표준화해 저장할 내부 기준값
    private String author;
    private String publisher;
    private String coverUrl;
    private String publishedDate; // yyyy-MM-dd / yyyy-MM / yyyy 중 하나

    public static LinkBookRequest fromCandidate(BookCandidate c) {
        LinkBookRequest r = new LinkBookRequest();
        r.setTitle(c.getTitle());
        r.setAuthor(c.getAuthor());
        r.setPublisher(c.getPublisher());
        r.setIsbn10(c.getIsbn10());
        r.setIsbn13(c.getIsbn13());
        r.setCoverUrl(c.getThumbnailUrl());
        r.setPublishedDate(c.getPublishedDate() != null ? c.getPublishedDate().toString() : null);
        r.setSource(c.getSource());
        r.setExternalId(
                c.getExternalId() != null && !c.getExternalId().isBlank()
                        ? c.getExternalId()
                        : c.getIsbn13()
        );
        return r;
    }
}
