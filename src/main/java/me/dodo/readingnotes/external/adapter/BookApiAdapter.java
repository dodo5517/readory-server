package me.dodo.readingnotes.external.adapter;

import me.dodo.readingnotes.dto.book.BookCandidate;

import java.util.List;

public interface BookApiAdapter<T> {
    // API 응답 객체를 BookCandidate 리스트로 변환
    List<BookCandidate> adapt(T apiResponse);

    // 검색 소스 식별자 반환
    String getSource();
}