package com.cy.helmet.storage;

/**
 * Created by jiaqing on 2018/3/24.
 */

import android.content.Context;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.cy.helmet.HelmetApplication;
import com.cy.helmet.util.LogUtil;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jiaqing on 2018/3/24.
 */

public class HelmetStorage {

    private Context mContext;
    private StorageManager mStorageManager;

    private Method getVolumeList;
    private Class<?> storageValumeClazz;
    private Method getPath;
    private Method isRemovable;

    private StorageBean mInternalStorage;
    private StorageBean mExternalStorage;

    private static HelmetStorage mInstance;

    private HelmetStorage() {
        mContext = HelmetApplication.mAppContext;
        mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        try {
            getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            storageValumeClazz = Class.forName("android.os.storage.StorageVolume");
            getPath = storageValumeClazz.getMethod("getPath");
            isRemovable = storageValumeClazz.getMethod("isRemovable");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (getVolumeList == null ||
                    storageValumeClazz == null ||
                    getPath == null ||
                    isRemovable == null) {
                throw new RuntimeException("StorageManager reflection error.");
            }
        }
    }

    public static synchronized HelmetStorage getInstance() {
        if (mInstance == null) {
            mInstance = new HelmetStorage();
        }
        return mInstance;
    }

    public void refreshStorageBeanList() {
        mExternalStorage = null;
        try {
            final Object invokeVolumeList = getVolumeList.invoke(mStorageManager);
            final int length = Array.getLength(invokeVolumeList);
            for (int i = 0; i < length; i++) {
                final Object storageVolume = Array.get(invokeVolumeList, i);
                //得到StorageVolume对象
                final String path = (String) getPath.invoke(storageVolume);
                final boolean removable = (Boolean) isRemovable.invoke(storageVolume);
                if (TextUtils.isEmpty(path) || path.toLowerCase().contains("usb")) {
                    //ignore usb and other storage
                    continue;
                }

                StorageBean storageBean = new StorageBean();
                storageBean.setPath(path);
                storageBean.setRemovable(removable);
                if (!storageBean.isMounted()) {
                    continue;
                }

                if (removable) {
                    mExternalStorage = storageBean;
                } else {
                    mInternalStorage = storageBean;
                }

                if (mExternalStorage != null && mInternalStorage == null) {
                    break;
                }
            }

            return;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return;
    }

    public StorageBean getWritableStorage(boolean tfFirst) {

        LogUtil.e("getWritableStorage_external: " + tfFirst);

        // refresh storage list
        if (mInternalStorage == null ||
                mExternalStorage == null ||
                !mExternalStorage.isMounted()) {
            refreshStorageBeanList();
        }

        boolean isTFValid = (mExternalStorage != null && mExternalStorage.isWritable());
        boolean isNandValid = (mInternalStorage != null && mInternalStorage.isWritable());

        LogUtil.e("isTFValid: " + isTFValid);
        LogUtil.e("isNandValid: " + isNandValid);

        if (!isTFValid && !isNandValid) {
            //clear space
            releaseStorageIfNecessary();
            return mInternalStorage;
        } else if (isTFValid && !isNandValid) {
            return mExternalStorage;
        } else if (!isTFValid && isNandValid) {
            return mInternalStorage;
        } else {
            return tfFirst ? mExternalStorage : mInternalStorage;
        }
    }

    public List<StorageBean> getMountedStorage() {
        List<StorageBean> mountStorage = new ArrayList<StorageBean>();

        // refresh storage list
        if (mInternalStorage == null ||
                mExternalStorage == null ||
                !mExternalStorage.isMounted()) {
            refreshStorageBeanList();
        }

        if (mInternalStorage != null) {
            mountStorage.add(mInternalStorage);
        }

        if (mExternalStorage != null && mExternalStorage.isMounted()) {
            mountStorage.add(mExternalStorage);
        }

        return mountStorage;
    }

    public void releaseStorageIfNecessary() {

        LogUtil.e("releaseStorageIfNecessary...");

        long releaseSize = 0;
        List<File> storageDirList = new ArrayList<File>();
        if (mExternalStorage != null && mInternalStorage.isMounted()) {
            releaseSize += mExternalStorage.getTotalSize();
            storageDirList.add(new File(mExternalStorage.getPath()));
        }

        if (mInternalStorage != null) {
            releaseSize += mInternalStorage.getTotalSize();
            storageDirList.add(new File(mInternalStorage.getPath()));
        }

        StorageScavenger.startClearStorage(storageDirList, (long) (releaseSize * 0.2));
    }

    public StorageBean getExternalStorage() {
        if (mExternalStorage == null || !mExternalStorage.isMounted()) {
            refreshStorageBeanList();
        }
        return mExternalStorage;
    }

    public StorageBean[] getStorageArray() {
        StorageBean[] result = new StorageBean[]{null, null};
        refreshStorageBeanList();
        result[0] = mInternalStorage;
        result[1] = mExternalStorage;
        return result;
    }
}

