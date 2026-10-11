package com.workproof.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paging maths. Every list page (projects, tasks, submissions, notifications,
 * admin users, activity log) renders its numbers from here, and the SQL LIMIT
 * clause uses {@link PagedResult#getOffset()}. An off-by-one here would show
 * the wrong rows to the reviewer, so the arithmetic is pinned down.
 */
class PagedResultTest {

    @Test
    @DisplayName("an empty result still reports page 1 of 1 so the widget never breaks")
    void emptyResult() {
        PagedResult<String> page = PagedResult.empty(10);

        assertTrue(page.isEmpty());
        assertEquals(0, page.getTotalItems());
        assertEquals(1, page.getTotalPages());
        assertEquals(0, page.getFirstItemNumber());
        assertEquals(0, page.getLastItemNumber());
        assertEquals(0, page.getOffset());
        assertFalse(page.isHasPrevious());
        assertFalse(page.isHasNext());
        assertEquals(List.of(1), page.getPageNumbers());
    }

    @Test
    @DisplayName("a middle page reports its window and offset correctly")
    void middlePage() {
        // 100 items, 10 per page, we are on page 5 -> items 41..50, offset 40
        PagedResult<String> page = new PagedResult<>(List.of("a", "b"), 100, 5, 10);

        assertEquals(10, page.getTotalPages());
        assertEquals(40, page.getOffset());
        assertEquals(41, page.getFirstItemNumber());
        assertEquals(50, page.getLastItemNumber());
        assertTrue(page.isHasPrevious());
        assertTrue(page.isHasNext());
        assertFalse(page.isEmpty());
    }

    @Test
    @DisplayName("the last page stops at the total and offers no next link")
    void lastPage() {
        // 95 items, page size 10 -> page 10 holds items 91..95 (a partial page)
        PagedResult<String> page = new PagedResult<>(List.of("a"), 95, 10, 10);

        assertEquals(10, page.getTotalPages());
        assertEquals(90, page.getOffset());
        assertEquals(91, page.getFirstItemNumber());
        assertEquals(95, page.getLastItemNumber(), "must not claim 100 items exist");
        assertTrue(page.isHasPrevious());
        assertFalse(page.isHasNext());
    }

    @Test
    @DisplayName("an out-of-range page yields no rows instead of an exception")
    void pageBeyondTheEnd() {
        PagedResult<String> page = new PagedResult<>(List.of(), 25, 99, 10);

        assertTrue(page.isEmpty());
        assertTrue(page.getOffset() > 25, "the query simply returns nothing");
        assertEquals(25, page.getLastItemNumber());
    }

    @Test
    @DisplayName("the pagination widget always shows at most five numbers")
    void pageNumberWindow() {
        assertEquals(List.of(1, 2, 3, 4, 5), new PagedResult<>(List.of(), 100, 1, 10).getPageNumbers());
        assertEquals(List.of(1, 2, 3, 4, 5), new PagedResult<>(List.of(), 100, 3, 10).getPageNumbers());
        assertEquals(List.of(3, 4, 5, 6, 7), new PagedResult<>(List.of(), 100, 5, 10).getPageNumbers());
        assertEquals(List.of(6, 7, 8, 9, 10), new PagedResult<>(List.of(), 100, 10, 10).getPageNumbers());

        for (int page = 1; page <= 10; page++) {
            List<Integer> numbers = new PagedResult<>(List.of(), 100, page, 10).getPageNumbers();
            assertTrue(numbers.size() <= 5, "the widget must stay compact");
            assertTrue(numbers.contains(page), "the current page must always be listed");
        }
    }

    @Test
    @DisplayName("invalid page and page-size values are repaired, not trusted")
    void defensiveDefaults() {
        PagedResult<String> page = new PagedResult<>(null, -5, 0, 0);

        assertEquals(1, page.getPage(), "page 0 and negatives become page 1");
        assertEquals(1, page.getPageSize(), "a zero page size would divide by zero");
        assertEquals(0, page.getTotalItems());
        assertTrue(page.getItems().isEmpty(), "a null item list is replaced by an empty one");
        assertEquals(0, page.getOffset());
    }
}
