package com.atguigu.java.ai.langchain4j.bean;

public class KnowledgeFileInfo {

    // 文件名
    private String fileName;
    // 文件后缀
    private String extension;
    // 文件大小，单位字节
    private long size;
    // 最后修改时间
    private String lastModified;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return "KnowledgeFileInfo{" +
                "fileName='" + fileName + '\'' +
                ", extension='" + extension + '\'' +
                ", size=" + size +
                ", lastModified='" + lastModified + '\'' +
                '}';
    }
}
