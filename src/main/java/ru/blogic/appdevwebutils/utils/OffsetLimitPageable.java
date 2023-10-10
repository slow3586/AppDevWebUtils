package ru.blogic.appdevwebutils.utils;

import lombok.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Value
public class OffsetLimitPageable implements Pageable {
    int offset;
    int limit;

    @Override
    public int getPageNumber() {
        return 0;
    }

    @Override
    public int getPageSize() {
        return 0;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Pageable next() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Pageable previousOrFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Pageable first() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Pageable withPage(int pageNumber) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasPrevious() {
        throw new UnsupportedOperationException();
    }
}
