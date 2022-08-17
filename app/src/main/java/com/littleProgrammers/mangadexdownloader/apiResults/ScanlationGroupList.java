package com.littleProgrammers.mangadexdownloader.apiResults;

public class ScanlationGroupList {
    String result;
    String response;
    ScanlationGroup[] data;
    int limit;
    int offset;
    int total;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public ScanlationGroup[] getData() {
        return data;
    }

    public void setData(ScanlationGroup[] data) {
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
