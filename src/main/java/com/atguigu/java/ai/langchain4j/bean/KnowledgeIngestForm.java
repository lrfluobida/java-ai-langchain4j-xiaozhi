package com.atguigu.java.ai.langchain4j.bean;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeIngestForm {

    // 需要导入的文件名列表
    private List<String> fileNames = new ArrayList<>();

    public List<String> getFileNames() {
        return fileNames;
    }

    public void setFileNames(List<String> fileNames) {
        this.fileNames = fileNames;
    }
}
