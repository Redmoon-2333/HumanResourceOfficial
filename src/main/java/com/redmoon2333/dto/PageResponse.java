package com.redmoon2333.dto;

import java.util.List;

/**
 * 分页响应DTO
 */
public class PageResponse<T> {
    
    private List<T> content;           // 数据内容
    private int pageNum;               // 当前页码
    private int pageSize;              // 每页大小
    private long total;                // 总记录数
    private int pages;                 // 总页数
    private boolean hasNext;           // 是否有下一页
    private boolean hasPrevious;       // 是否有上一页
    
    // 构造函数
    public PageResponse() {}
    
    public PageResponse(List<T> content, int pageNum, int pageSize, long total) {
        this.content = content;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.pages = (int) Math.ceil((double) total / pageSize);
        this.hasNext = pageNum < pages;
        this.hasPrevious = pageNum > 1;
    }
    
    // 静态工厂方法
    public static <T> PageResponse<T> of(List<T> content, int pageNum, int pageSize, long total) {
        return new PageResponse<>(content, pageNum, pageSize, total);
    }
    
    // Getters and Setters
    public List<T> getContent() {
        return content;
    }
    
    public void setContent(List<T> content) {
        this.content = content;
    }
    
    public int getPageNum() {
        return pageNum;
    }
    
    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }
    
    public int getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
    
    public long getTotal() {
        return total;
    }
    
    public void setTotal(long total) {
        this.total = total;
    }
    
    public int getPages() {
        return pages;
    }
    
    public void setPages(int pages) {
        this.pages = pages;
    }
    
    public boolean isHasNext() {
        return hasNext;
    }
    
    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }
    
    public boolean isHasPrevious() {
        return hasPrevious;
    }
    
    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }
    
    @Override
    public String toString() {
        return "PageResponse{" +
                "content=" + content +
                ", pageNum=" + pageNum +
                ", pageSize=" + pageSize +
                ", total=" + total +
                ", pages=" + pages +
                ", hasNext=" + hasNext +
                ", hasPrevious=" + hasPrevious +
                '}';
    }
}