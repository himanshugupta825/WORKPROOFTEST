package com.workproof.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Small pagination container used by every listing page (projects, tasks,
 * submissions, users, activity log).
 *
 * Usage in a servlet:
 *   PagedResult<Task> page = taskDao.search(..., pageNumber, 10);
 *   request.setAttribute("taskPage", page);
 * and in the JSP:
 *   <c:forEach items="${taskPage.items}" var="task"> ... </c:forEach>
 */
public class PagedResult<T> {

    private final List<T> items;
    private final int totalItems;
    private final int page;
    private final int pageSize;

    public PagedResult(List<T> items, int totalItems, int page, int pageSize) {
        this.items = items == null ? new ArrayList<>() : items;
        this.totalItems = Math.max(totalItems, 0);
        this.page = Math.max(page, 1);
        this.pageSize = Math.max(pageSize, 1);
    }

    /** Convenience factory for the "no results" case. */
    public static <T> PagedResult<T> empty(int pageSize) {
        return new PagedResult<>(new ArrayList<>(), 0, 1, pageSize);
    }

    public List<T> getItems() { return items; }
    public int getTotalItems() { return totalItems; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }

    public int getTotalPages() {
        if (totalItems == 0) {
            return 1;
        }
        return (int) Math.ceil(totalItems / (double) pageSize);
    }

    public boolean isHasPrevious() { return page > 1; }
    public boolean isHasNext() { return page < getTotalPages(); }
    public boolean isEmpty() { return items.isEmpty(); }

    public int getFirstItemNumber() {
        return totalItems == 0 ? 0 : (page - 1) * pageSize + 1;
    }

    public int getLastItemNumber() {
        return Math.min(page * pageSize, totalItems);
    }

    /**
     * A window of page numbers for the pagination widget, e.g. 1 ... 4 5 6 ... 12.
     * JSP usage: <c:forEach items="${pageResult.pageNumbers}" var="p">
     */
    public List<Integer> getPageNumbers() {
        List<Integer> numbers = new ArrayList<>();
        int total = getTotalPages();
        int start = Math.max(1, page - 2);
        int end = Math.min(total, start + 4);
        start = Math.max(1, end - 4);
        for (int i = start; i <= end; i++) {
            numbers.add(i);
        }
        return numbers;
    }

    /** Offset used by the SQL LIMIT clause (kept here so tests can verify it). */
    public int getOffset() {
        return (page - 1) * pageSize;
    }
}
