package com.cy.helmet.storage;

import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import com.cy.helmet.Constant;
import com.cy.helmet.HelmetClient;
import com.cy.helmet.WorkThreadManager;
import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.conn.HelmetMessageSender;
import com.cy.helmet.util.LogUtil;
import com.cy.helmet.util.NetworkPing;
import com.cy.helmet.util.StringUtil;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by yaojiaqing on 2018-02-26.
 */

public class LocalFileManager {

    private Set<String> mUploadingFileName = new HashSet<String>();

    private static LocalFileManager mInstance;

    private static final Executor FILE_UPLOAD_EXECUTOR = initFileUploadExecutor();

    private LocalFileManager() {

    }

    public synchronized static LocalFileManager getInstance() {
        if (mInstance == null) {
            mInstance = new LocalFileManager();
        }

        return mInstance;
    }

    private boolean isFileLocked(String fileName) {
        synchronized (LocalFileManager.class) {
            return mUploadingFileName.contains(fileName);
        }
    }

    private void lockFile(String fileName) {
        synchronized (LocalFileManager.class) {
            mUploadingFileName.add(fileName);
        }
    }

    private void unlockFile(String fileName) {
        synchronized (LocalFileManager.class) {
            mUploadingFileName.remove(fileName);
        }
    }

    private static Executor initFileUploadExecutor() {
        return new ThreadPoolExecutor(3,
                3,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(1));
    }

    public void uploadFile(final String url, final String fileName, final int fileType, final int speedLimit, final boolean isSync) {
        LogUtil.e("upload file>>>" + fileName);

        // forbid upload file if in sleep mode
        if (HelmetClient.isSleepNow()) {
            HelmetMessageSender.sendUploadFileResp(fileName, Constant.FILE_UPLOAD_FAILED);
            return;
        }

        if (StringUtil.isNullEmptyOrSpace(url) || StringUtil.isNullEmptyOrSpace(fileName)) {
            LogUtil.e("Invalid url or file name.");
            HelmetMessageSender.sendUploadFileResp(fileName, Constant.FILE_UPLOAD_FAILED);
            return;
        }

        try {
            FILE_UPLOAD_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        lockFile(fileName);
                        uploadFileImpl(url, fileName, fileType, speedLimit, isSync);
                    } catch (Exception e) {
                    } finally {
                        unlockFile(fileName);
                    }
                }
            });
        } catch (RejectedExecutionException localRejectedExecutionException) {
            WorkThreadManager.executeOnNetWorkThread(new Runnable() {
                @Override
                public void run() {
                    HelmetMessageSender.sendUploadFileResp(fileName, Constant.FILE_UPLOAD_FAILED);
                }
            });

            return;
        }
    }

    public void deleteFileByName(final String fileName) {
        synchronized (LocalFileManager.class) {
            try {
                if (!isFileLocked(fileName)) {
                    deleteFileImpl(fileName);
                } else {
                    HelmetMessageSender.sendDeleteFileResp(fileName, false);
                    return;
                }
            } catch (Exception e) {
            } finally {
            }
        }
    }

    public boolean deleteLocalFile(final File mediaFile) {

        if (mediaFile == null || !mediaFile.exists() || !mediaFile.isFile()) {
            return false;
        }

        String fileName = mediaFile.getName();
        synchronized (LocalFileManager.class) {
            try {
                if (!isFileLocked(fileName)) {
                    return mediaFile.delete();
                } else {
                    return false;
                }
            } catch (Exception e) {
            } finally {
            }

            return false;
        }
    }

    /**
     * upload the given file to the server
     *
     * @param url        the upload server address
     * @param fileName   file name
     * @param fileType   file type
     * @param speedLimit the max speed of uploading, such as 1024kbps, 0 if no speed restrict
     * @param isSync     sync
     */
    private void uploadFileImpl(String url, String fileName, int fileType, int speedLimit, boolean isSync) {
        LogUtil.e("uploadFile: " + fileName);

        boolean uploadSuccess = false;
        OutputStream os = null;
        HttpURLConnection conn = null;
        BufferedReader br = null;
        InputStream is = null;

        String devId = HelmetConfig.get().getDeviceId();
        Uri uri = Uri.parse(url)
                .buildUpon()
                .appendQueryParameter("devId", devId)
                .appendQueryParameter("fileName", fileName)
                .appendQueryParameter("type", fileType + "")
                .build();
        String address = uri.toString();

        File uploadFile = FileUtil.getMediaSubFile(fileName);
        if (uploadFile == null || !uploadFile.exists()) {
            HelmetMessageSender.sendUploadFileResp(fileName, Constant.FILE_UPLOAD_NOT_EXIST);
            return;
        }

        if (!isSync) {
            HelmetMessageSender.sendUploadFileResp(fileName, Constant.FILE_UPLOAD_ASYNC_RESP);
        }

        long fileSize = uploadFile.length();
        FormFile formFile = new FormFile(uploadFile.getName(), uploadFile, "file", null);
        LogUtil.e("fileSize = " + fileSize);

        final String BOUNDARY = "---------------------------7da2137580612";
        final String endline = "--" + BOUNDARY + "--\r\n";
        StringBuilder textEntity = new StringBuilder();

        try {
            conn = (HttpURLConnection) new URL(address).openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setChunkedStreamingMode(0);
            conn.setRequestProperty("helmet-file-size", String.valueOf(fileSize));
            conn.setRequestProperty("Charset", "UTF-8");
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);
            conn.connect();

            os = conn.getOutputStream();
            os.write(textEntity.toString().getBytes("UTF-8"));

            StringBuilder fileEntity = new StringBuilder();
            fileEntity.append("--");
            fileEntity.append(BOUNDARY);
            fileEntity.append("\r\n");
            fileEntity.append("Content-Disposition: form-data;name=\"" + formFile.getParameterName()
                    + "\";filename=\"" + formFile.getFileName() + "\"\r\n");
            fileEntity.append("Content-Type: " + formFile.getContentType() + "\r\n\r\n");

            os.write(fileEntity.toString().getBytes("UTF-8"));

            //input stream
            is = formFile.getInStream();
            if (is != null) {
                byte[] buffer = new byte[4096];
                int len = 0;

                int curPercent = 0;
                long uploadedLen = 0;

                int intervalMs = 0;
                if (speedLimit > 0) {
                    speedLimit = (speedLimit < 80 ? 80 : speedLimit);
                    intervalMs = 32000 / speedLimit;
                }

                // calculate real speed
                long startUpload = SystemClock.elapsedRealtime();
                LogUtil.e("file start upload time = " + startUpload);

                while ((len = formFile.getInStream().read(buffer, 0, 4096)) != -1) {

                    long lastTime = SystemClock.elapsedRealtime();

                    // break file uploading if in sleep mode
                    if (HelmetClient.isSleepNow()) {
                        LogUtil.e("Break file uploading in pre-sleep state.");
                        return;
                    }

                    // calculate real speed
                    uploadedLen += len;
                    os.write(buffer, 0, len);

                    //print percent
                    int tempPercent = (int) (uploadedLen * 100 / fileSize);
                    if (tempPercent - curPercent > 1) {
                        curPercent = tempPercent;
                        LogUtil.e("file upload: " + tempPercent + "%");
                    }

                    if (intervalMs > 0) {
                        try {
                            long finishTime = SystemClock.elapsedRealtime();
                            long useTime = Math.abs(finishTime - lastTime);
                            if (useTime < intervalMs) {
                                Thread.sleep(intervalMs - useTime);
                            }
                        } catch (Exception e) {
                        }
                    }

//                    // calculate real speed
                    //long realUseTime = SystemClock.elapsedRealtime() - startUpload;
                    //double realSpeed = (double) uploadedLen * speedLimit / (double) realUseTime / 1024;
                }
            } else {
                os.write(formFile.getData(), 0, formFile.getData().length);
            }

            os.write("\r\n".getBytes("UTF-8"));
            os.write(endline.getBytes());
            os.flush();

            br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuffer sb = new StringBuffer();
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            String result = sb.toString();
            try {
                JSONObject resultJSON = new JSONObject(result);
                uploadSuccess = (0 == resultJSON.getInt("code"));
            } catch (Exception e) {
            }

        } catch (Exception e) {
            LogUtil.e(e);
            NetworkPing.getInstance().startPing(); //add by Jerry
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            if (conn != null) {
                conn.disconnect();
            }

            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }

        if (uploadSuccess) {
            LogUtil.e("file upload end = " + SystemClock.elapsedRealtime());
            LogUtil.e("upload file success: " + fileName);
        } else {
            LogUtil.e("upload file failed: " + fileName);
            HelmetMessageSender.sendUploadFileResp(fileName, Constant.FILE_UPLOAD_FAILED);
        }
    }

    public boolean deleteFileImpl(String wholeFileName) {

        if (StringUtil.isNullEmptyOrSpace(wholeFileName)) {
            return false;
        }

        String pureFileName = wholeFileName.substring(0, wholeFileName.indexOf("."));
        if (pureFileName.length() != 17) {
            return false;
        }

        boolean delSuccess = false;
        String dirName = wholeFileName.substring(0, 8);

        List<File> mediaDirList = FileUtil.getMediaDirList();
        for (int i = 0; i < mediaDirList.size(); i++) {
            String mediaDir = mediaDirList.get(i).getPath();

            File subDir = FileUtil.getMediaSubDir(dirName, mediaDir);
            if (subDir != null) {
                File subFile = new File(subDir, wholeFileName);
                if (subFile.exists() && !subFile.isDirectory()) {
                    if (subFile != null && subFile.exists()) {
                        delSuccess = subFile.delete();

                        //delete empty directory
                        String[] files = subDir.list();
                        if (files != null && files.length == 0) {
                            subDir.delete();
                        }
                    }
                }
            }
        }

        LogUtil.e("delete: " + wholeFileName + ":" + delSuccess);

        HelmetMessageSender.sendDeleteFileResp(wholeFileName, delSuccess);

        return delSuccess;
    }

    public File getEmptyVideoFile() {
        SimpleDateFormat formater = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String pureFileName = formater.format(new Date());
        String fileName = pureFileName + ".flv";
        String mediaSubDirName = pureFileName.substring(0, 8);

        File mediaSubDir = FileUtil.getOrCreateMediaSubDir(mediaSubDirName);
        if (mediaSubDir != null) {
            return new File(mediaSubDir, fileName);
        }

        return null;
    }

    public File getEmptyPhotoFile(boolean compress) {
        String fileName;
        SimpleDateFormat formater = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String pureFileName = formater.format(new Date());
        if (compress) {
            fileName = pureFileName + ".jpg";
        } else {
            fileName = pureFileName + ".jpeg";
        }
        String mediaSubDirName = pureFileName.substring(0, 8);

        File mediaSubDir = FileUtil.getOrCreateMediaSubDir(mediaSubDirName);
        if (mediaSubDir != null) {
            return new File(mediaSubDir, fileName);
        }

        return null;
    }
}
