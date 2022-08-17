package com.littleProgrammers.mangadexdownloader.apiResults;

public class ChapterResults {
    String result;
    String repsonse;
    Chapter[] data;
    int limit;
    int offset;
    int total;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getRepsonse() {
        return repsonse;
    }

    public void setRepsonse(String repsonse) {
        this.repsonse = repsonse;
    }

    public Chapter[] getData() {
        return data;
    }

    public void setData(Chapter[] data) {
        this.data = data;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}