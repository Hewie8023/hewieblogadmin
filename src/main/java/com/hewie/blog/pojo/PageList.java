package com.hewie.blog.pojo;

import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;

public class PageList<T> implements Serializable {
    //分页
    //当前页码

    private long currentPage;
    //总数量
    private long totalCount;
    //每页数量
    private long pageSize;
    //总页数
    private long totalPage;
    //是否是第一页
    private boolean isFirst;
    //是否是最后一页
    private boolean isLast;
    //数据
    private List<T> contents;

    public PageList() {
    }

    public PageList(long currentPage, long totalCount, long pageSize) {
        this.currentPage = currentPage;
        this.totalCount = totalCount;
        this.pageSize = pageSize;
        //计算总页数
        if (totalCount > 0) {
            this.totalPage = (long) ((totalCount / (pageSize * 1.0f)) + 0.9f);
        } else {
            this.totalPage = 1;
        }
        //第一页，最后一页        //第一页为0，最后一页为总的页码
        //        this.isFirst = this.currentPage == 1;
        //        this.isLast = this.currentPage == this.totalPage;
        this.isFirst = this.currentPage == 1;
        this.isLast = this.currentPage == totalPage;
    }

    public long getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(long currentPage) {
        this.currentPage = currentPage;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(long totalPage) {
        this.totalPage = totalPage;
    }

    public boolean isFirst() {
        return isFirst;
    }

    public void setFirst(boolean first) {
        isFirst = first;
    }

    public boolean isLast() {
        return isLast;
    }

    public void setLast(boolean last) {
        isLast = last;
    }

    public List<T> getContents() {
        return contents;
    }

    public void setContents(List<T> contents) {
        this.contents = contents;
    }

    public void parsePage(Page<T> all) {
        setContents(all.getContent());
        setFirst(all.isFirst());
        setLast(all.isLast());
        setCurrentPage(all.getNumber() + 1);
        setTotalCount(all.getTotalElements());
        setTotalPage(all.getTotalPages());
        setPageSize(all.getSize());
    }
}
