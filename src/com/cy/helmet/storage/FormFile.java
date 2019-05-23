package com.cy.helmet.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by jiaqing on 2018/1/6.
 */

/**
 * upload file class
 */
public class FormFile {
    private byte[] data;
    private InputStream inStream;
    private File file;

    private String filname;

    private String parameterName;

    private String contentType = "application/octet-stream";

    public FormFile(String filname, byte[] data, String parameterName, String contentType) {
        this.data = data;
        this.filname = filname;
        this.parameterName = parameterName;
        if (contentType != null) {
            this.contentType = contentType;
        }
    }

    public FormFile(String filname, File file, String parameterName) {
        this(filname, file, parameterName, null);
    }

    public FormFile(String filname, File file, String parameterName, String contentType) {
        this.filname = filname;
        this.parameterName = parameterName;
        this.file = file;
        try {
            this.inStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (contentType != null) {
            this.contentType = contentType;
        }
    }

    public File getFile() {
        return file;
    }

    public InputStream getInStream() {
        return inStream;
    }

    public byte[] getData() {
        return data;
    }

    public String getFileName() {
        return filname;
    }

    public void setFileName(String filname) {
        this.filname = filname;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

}

