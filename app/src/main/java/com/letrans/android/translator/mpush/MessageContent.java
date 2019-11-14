package com.letrans.android.translator.mpush;

public class MessageContent {
    String content;
    int nid;
    String ticker;
    String title;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getNid() {
        return nid;
    }

    public void setNid(int nid) {
        this.nid = nid;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return "MessageContent{" +
                "content='" + content + '\'' +
                ", nid=" + nid +
                ", ticker='" + ticker + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
