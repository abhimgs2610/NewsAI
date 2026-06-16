package com.newsai.news_ai_backend.pagination;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class OffsetBasedPageRequest implements Pageable {

	private final int limit;
	private final long offset;
	private final Sort sort;

	public OffsetBasedPageRequest(int limit, long offset) {
		this(limit, offset, Sort.unsorted());
	}

	public OffsetBasedPageRequest(int limit, long offset, Sort sort) {
		if (limit < 1) {
			throw new IllegalArgumentException("limit must be greater than zero.");
		}
		if (offset < 0) {
			throw new IllegalArgumentException("offset must not be negative.");
		}
		this.limit = limit;
		this.offset = offset;
		this.sort = sort == null ? Sort.unsorted() : sort;
	}

	@Override
	public int getPageNumber() {
		return (int) (offset / limit);
	}

	@Override
	public int getPageSize() {
		return limit;
	}

	@Override
	public long getOffset() {
		return offset;
	}

	@Override
	public Sort getSort() {
		return sort;
	}

	@Override
	public Pageable next() {
		return new OffsetBasedPageRequest(limit, offset + limit, sort);
	}

	@Override
	public Pageable previousOrFirst() {
		return hasPrevious() ? new OffsetBasedPageRequest(limit, Math.max(offset - limit, 0), sort) : first();
	}

	@Override
	public Pageable first() {
		return new OffsetBasedPageRequest(limit, 0, sort);
	}

	@Override
	public Pageable withPage(int pageNumber) {
		if (pageNumber < 0) {
			throw new IllegalArgumentException("pageNumber must not be negative.");
		}
		return new OffsetBasedPageRequest(limit, (long) pageNumber * limit, sort);
	}

	@Override
	public boolean hasPrevious() {
		return offset > 0;
	}
}