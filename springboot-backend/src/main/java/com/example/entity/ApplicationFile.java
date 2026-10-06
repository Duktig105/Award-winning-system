package com.example.entity;

import java.time.LocalDateTime;

/**
 * 申请文件实体类
 * 对应数据库表：application_file
 */
public class ApplicationFile {
    private Integer fileId;              // 文件ID（主键）
    private Integer applicationId;       // 申请ID
    private String fileName;             // 文件名
    private String filePath;             // 文件路径
    private String fileType;             // 文件类型
    private Integer fileSize;            // 文件大小（字节）
    private LocalDateTime uploadTime;    // 上传时间

    // Getter and Setter
    public Integer getFileId() {
        return fileId;
    }

    public void setFileId(Integer fileId) {
        this.fileId = fileId;
    }

    public Integer getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Integer applicationId) {
        this.applicationId = applicationId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }
}
