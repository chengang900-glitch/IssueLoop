package com.rnd.app.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 分页入参收口：统一页码下界与每页条数上界，避免 page=0 触发
 * {@code PageRequest.of(-1, ...)} 抛 IllegalArgumentException（表现为 500），
 * 以及 size 无上界导致的一次性大查询。
 */
public final class PageRequests {

    public static final int DEFAULT_MAX_SIZE = 200;

    private PageRequests() {
    }

    public static int page(int page) {
        return Math.max(1, page);
    }

    public static int size(int size) {
        return size(size, DEFAULT_MAX_SIZE);
    }

    public static int size(int size, int maxSize) {
        return Math.min(Math.max(1, size), Math.max(1, maxSize));
    }

    public static Pageable of(int page, int size, Sort sort) {
        return PageRequest.of(page(page) - 1, size(size), sort);
    }

    public static Pageable of(int page, int size, int maxSize, Sort sort) {
        return PageRequest.of(page(page) - 1, size(size, maxSize), sort);
    }
}
