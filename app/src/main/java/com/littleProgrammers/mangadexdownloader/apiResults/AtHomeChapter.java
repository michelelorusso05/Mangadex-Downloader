package com.littleProgrammers.mangadexdownloader.apiResults;

public class AtHomeChapter {
    String hash;
    String[] data;
    String[] dataSaver;

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String[] getData() {
        return data;
    }

    public void setData(String[] data) {
        this.data = data;
    }

    public String[] getDataSaver() {
        return dataSaver;
    }

    public void setDataSaver(String[] dataSaver) {
        this.dataSaver = dataSaver;
    }
}
