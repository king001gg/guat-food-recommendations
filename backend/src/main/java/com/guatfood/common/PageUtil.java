package com.guatfood.common;

import java.util.ArrayList;
import java.util.List;

/**
 * 内存分页工具
 */
public class PageUtil {

    private PageUtil() {}

    public static <T> PageResult<T> paginate(List<T> all, int page, int size) {
        int s = Math.min(Math.max(size, 1), 100);
        int p = Math.max(page, 1);
        int total = all.size();
        int from = Math.max(0, (p - 1) * s);
        int to = Math.min(total, from + s);
        List<T> slice = from < total ? new ArrayList<>(all.subList(from, to)) : new ArrayList<>();
        return PageResult.of(total, p, s, slice);
    }
}
