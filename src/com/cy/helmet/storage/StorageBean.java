package com.cy.helmet.storage;

import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.os.EnvironmentCompat;

import java.io.File;

public class StorageBean {

    public static final long GB = 1073741824;
    public static final long MB = 1048576;
    public static final int KB = 1024;

    private String path;
    private boolean mounted;
    private boolean removable;
    private long totalSize;
    private long availableSize;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
        this.totalSize = getTotalSize();
        this.availableSize = getAvailableSize();
    }

    public void setRemovable(boolean removable) {
        this.removable = removable;
    }

    public boolean isRemovable() {
        return removable;
    }

    public boolean isMounted() {
        if (!removable) {
            mounted = true;
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mounted = (Environment.MEDIA_MOUNTED.equals(Environment.getStorageState(new File(path))));
            } else {
                mounted = (Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(new File(path))));
            }
        }

        return mounted;
    }

    public boolean isWritable() {

        if (!isMounted()) {
            return false;
        }

        /*
        if (removable && !Util.isSystemApp()) {
            //non-system app has no permission
            // to write external storage
            return false;
        }
        */

        totalSize = getTotalSize();
        availableSize = getAvailableSize();

        boolean isValidTotal = totalSize > 0;
        boolean isValidFree;
        if (removable) {//external
            isValidFree = (availableSize > 52428800L);// > 50MB
        } else {
            isValidFree = (availableSize > 1572864000L);// > 1.5G need decrease cache 0.5GB
        }

        return (isValidTotal && isValidFree);
    }

    public long getTotalSize() {
        try {
            final StatFs statFs = new StatFs(path);
            long blockSize = 0;
            long blockCountLong = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                blockSize = statFs.getBlockSizeLong();
                blockCountLong = statFs.getBlockCountLong();
            } else {
                blockSize = statFs.getBlockSize();
                blockCountLong = statFs.getBlockCount();
            }
            return blockSize * blockCountLong;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public long getAvailableSize() {
        try {
            final StatFs statFs = new StatFs(path);
            long blockSize = 0;
            long availableBlocks = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                blockSize = statFs.getBlockSizeLong();
                availableBlocks = statFs.getAvailableBlocksLong();
            } else {
                blockSize = statFs.getBlockSize();
                availableBlocks = statFs.getAvailableBlocks();
            }
            return availableBlocks * blockSize;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int getFreeSpacePercent() {
        long available = getAvailableSize();
        long total = getTotalSize();
        if (total == 0) {
            return 0;
        }

        return (int) (available * 100 / total);
    }

    public int getAvailableSizeMB() {
        long available = getAvailableSize();
        if (available <= 0) {
            return 0;
        }

        return (int) (available / MB);
    }

    public int getTotalSizeGB() {
        long total = getTotalSize();
        if (total <= 0) {
            return 0;
        }
        return (int) (total / GB);
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();
        builder.append("path: ");
        builder.append(path);
        builder.append("\n");

        builder.append("mounted: ");
        builder.append(mounted);
        builder.append("\n");

        builder.append("removable: ");
        builder.append(removable);
        builder.append("\n");

        builder.append("totalSize: ");
        builder.append(totalSize);
        builder.append("\n");

        builder.append("availableSize: ");
        builder.append(availableSize);
        builder.append("\n");
        builder.append("\n");

        return builder.toString();
    }
}
