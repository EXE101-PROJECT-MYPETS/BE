package com.exe101.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScrollResponse<T> {

    private List<T> content;
    private int size;
    private Long nextCursor;
    private boolean hasNext;

    public static <T> ScrollResponse<T> of(List<T> content, int size, Long nextCursor, boolean hasNext) {
        return new ScrollResponse<>(content, size, nextCursor, hasNext);
    }
}
