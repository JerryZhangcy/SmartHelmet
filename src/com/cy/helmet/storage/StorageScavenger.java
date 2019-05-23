package com.cy.helmet.storage;

/**
 * Created by yaojiaqing on 2018/1/27.
 */

import android.util.Log;

import com.cy.helmet.util.LogUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class StorageScavenger implements Runnable {

    private static Thread mThread = null;

    public synchronized static void startClearStorage(List<File> storageRootList, long recoverySize) {
        if (mThread == null) {
            mThread = new Thread(new StorageScavenger(storageRootList, recoverySize));
            mThread.start();
        }
    }

    private long mNeedReleaseByte;
    private long mHasReleaseByte = 0;
    private List<File> mRootDirList = new ArrayList<File>();

    private StorageScavenger(List<File> storageRootList, long deleteFileSize) {
        if (storageRootList != null) {
            for (File file : storageRootList) {
                LogUtil.e("release storage: " + file.getAbsolutePath());
            }
            LogUtil.e("release storage size: " + deleteFileSize);
            mRootDirList = storageRootList;
        }

        mNeedReleaseByte = deleteFileSize;
    }

    @Override
    public void run() {
        try {
            if (mRootDirList == null || mNeedReleaseByte <= 0) {
                return;
            }

            List<File> dateDirList = new ArrayList<File>();
            for (File storage : mRootDirList) {
                File mediaDir = FileUtil.getMediaDir(storage);
                if (!mediaDir.exists() || !mediaDir.isDirectory()) {
                    continue;
                }

                File[] mediaSubDirArray = mediaDir.listFiles();
                if (mediaSubDirArray == null || mediaSubDirArray.length == 0) {
                    //empty directory
                    continue;
                }

                dateDirList.addAll(Arrays.asList(mediaSubDirArray));
            }

            //sort dir by create date
            Collections.sort(dateDirList, new Comparator<File>() {
                @Override
                public int compare(File leftFile, File rightFile) {
                    String leftFileName = leftFile.getName();
                    String rightFileName = rightFile.getName();
                    return leftFileName.compareTo(rightFileName);
                }
            });

            for (File dir : dateDirList) {
                LogUtil.e("start release dir: " + dir.getName());
                if (!releaseDirectory(dir)) {
                    LogUtil.e("release storage finished: " + dir.getAbsolutePath());
                    break;
                }
            }
        } catch (Exception e) {

        } finally {
            mThread = null;
        }
    }

    private boolean releaseDirectory(File dateDir) {

        if (dateDir == null || !dateDir.exists()) {
            return mHasReleaseByte <= mNeedReleaseByte;
        }

        File[] dateFileArray = dateDir.listFiles();
        if (dateFileArray == null || dateFileArray.length == 0) {
            dateDir.delete();//delete empty dir
            return mHasReleaseByte <= mNeedReleaseByte;
        }

        // ignore latest directory
        SimpleDateFormat formater = new SimpleDateFormat("yyyyMMdd");
        String curDate = formater.format(new Date());
        if (curDate.equals(dateDir.getName())) {
            LogUtil.e("do not release the files of this day");
            return mHasReleaseByte <= mNeedReleaseByte;
        }

        //sort dir by create date
        List<File> dateFileList = Arrays.asList(dateFileArray);
        Collections.sort(dateFileList, new Comparator<File>() {
            @Override
            public int compare(File leftFile, File rightFile) {
                String leftFileName = leftFile.getName();
                int leftIndex = leftFileName.lastIndexOf(".");
                leftIndex = (leftIndex == -1 ? leftFileName.length() : leftIndex);
                leftFileName = leftFileName.substring(0, leftIndex);

                String rightFileName = rightFile.getName();
                int rightIndex = rightFileName.lastIndexOf(".");
                rightIndex = (rightIndex == -1 ? rightFileName.length() : rightIndex);
                rightFileName = rightFileName.substring(0, rightIndex);

                return leftFileName.compareTo(rightFileName);
            }
        });

        for (File subFile : dateFileList) {
            if (mHasReleaseByte >= mNeedReleaseByte) {
                break;
            }

            long fileSize = subFile.length();
            if (LocalFileManager.getInstance().deleteLocalFile(subFile)) {
                mHasReleaseByte += fileSize;
            }
        }

        //delete empty dir
        String[] dateFileNameArray = dateDir.list();
        if (dateFileNameArray == null || dateFileNameArray.length == 0) {
            dateDir.delete();
        }

        return mHasReleaseByte <= mNeedReleaseByte;
    }
}

