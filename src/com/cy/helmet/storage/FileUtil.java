package com.cy.helmet.storage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Environment;
import android.util.Log;

import com.cy.helmet.Constant;
import com.cy.helmet.HelmetApplication;
import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.util.LogUtil;
import com.cy.helmet.util.StringUtil;
import com.cy.helmet.util.Util;
import com.cy.helmet.video.controller.PhotoCallback;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jiaqing on 2018/1/3.
 */

public class FileUtil {

    public static final String HELMET_ROOT_DIR = "Helmet";
    public static final String VOICE_DIR_NAME = "voice";
    public static final String MEDIA_DIR_NAME = "media";
    public static final String CONFIG_DIR_NAME = "config";

    public static final String CONFIG_FILE_NAME = "dev_config";

    public static File getAudioDir(String rootdir, String recorddir) {
        File sdFile = Environment.getExternalStorageDirectory();
        File rootDir = new File(sdFile, rootdir);
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }

        File recordDir = new File(rootDir, recorddir);
        if (!recordDir.exists()) {
            recordDir.mkdirs();
        }

        return recordDir;
    }

    public static File getAudioFile(String fileName) {
        return new File(getAudioDir(HELMET_ROOT_DIR, VOICE_DIR_NAME), fileName);
    }

    public static File getWritableStorageDir(int type) {
        File storageRootDir = null;
        StorageBean bean = HelmetStorage.getInstance().
                getWritableStorage(type == Constant.STORAGE_TYPE_EXTERNAL);

        LogUtil.e("writableStorage: " + bean.toString());

        if (bean != null) {
            storageRootDir = new File(bean.getPath());
        }

        return storageRootDir;
    }

    public static List<File> getMediaDirList() {
        List<File> storageRootDir = new ArrayList<>();
        List<StorageBean> storageDetail = HelmetStorage.getInstance().getMountedStorage();
        for (StorageBean bean : storageDetail) {
            if (bean == null) {
                continue;
            }
            if (bean.isRemovable() && !Util.isSystemApp()) {
                continue;
            }
            File mediafile = getMediaDir(new File(bean.getPath()));
            if ((mediafile != null) && mediafile.exists()) {
                storageRootDir.add(mediafile);
            }
        }
        return storageRootDir;
    }


    /**
     * get directory in media dir
     * create directory if not exist
     *
     * @return
     */

    public static File getOrCreateMediaSubDir(String subDirName) {

        File mediaSubDir = null;

        if (!StringUtil.isNullEmptyOrSpace(subDirName)) {
            File storageRootDir = getWritableStorageDir(HelmetConfig.get().firstTfCard);

            LogUtil.e("storageRootDir>>>> " + storageRootDir);

            File rootDir = new File(storageRootDir, HELMET_ROOT_DIR);
            if (!rootDir.exists()) {
                rootDir.mkdir();
            }

            File mediaDir = new File(rootDir, MEDIA_DIR_NAME);
            if (!mediaDir.exists()) {
                mediaDir.mkdir();
            } else if (!mediaDir.isDirectory()) {
                mediaDir.delete();
                mediaDir.mkdir();
            }

            mediaSubDir = new File(mediaDir, subDirName);
            if (!mediaSubDir.exists()) {
                mediaSubDir.mkdir();
            }
        }

        return mediaSubDir;
    }

//    public static StorageBean getDestStorageBean() {
//
//        StorageBean destBean = null;
//
//        int storageType = HelmetConfig.get().firstTfCard;
//        List<StorageBean> storageDetail = StorageUtils.getStorageDetailList();
//
//        for (StorageBean bean : storageDetail) {
//            if (storageType == Constant.STORAGE_TYPE_EXTERNAL) {
//                if (bean.isRemovable()) {
//                    destBean = bean;
//                    break;
//                } else if (destBean == null) {
//                    destBean = bean;
//                }
//            } else if (storageType == Constant.STORAGE_TYPE_INTERNAL) {
//                if (!bean.isRemovable()) {
//                    destBean = bean;
//                    break;
//                } else if (destBean == null) {
//                    destBean = bean;
//                }
//            } else {
//                destBean = bean;
//                break;
//            }
//        }
//
//        return destBean;
//    }

    /**
     * get media directory
     * which save video and photo in each sub directory named with date
     *
     * @return
     */
    public static File getMediaDir(File storageRootDir) {

        File mediaDir = null;
        if (storageRootDir != null &&
                storageRootDir.exists() &&
                storageRootDir.isDirectory()) {

            File rootDir = new File(storageRootDir, HELMET_ROOT_DIR);
            if (!rootDir.exists()) {
                rootDir.mkdir();
            }

            mediaDir = new File(rootDir, MEDIA_DIR_NAME);
            if (!mediaDir.exists()) {
                mediaDir.mkdir();
            } else if (!mediaDir.isDirectory()) {
                mediaDir.delete();
                mediaDir.mkdir();
            }
        }

        return mediaDir;
    }


    /**
     * get media directory
     * which save video and photo in each sub directory named with date
     *
     * @return
     */
    public static File getMediaSubDir(String subDirName, String mediaDir) {
        if (StringUtil.isNullEmptyOrSpace(subDirName)) {
            return null;
        }

        File dateDir = new File(mediaDir, subDirName);
        if (dateDir.exists() && dateDir.isDirectory()) {
            return dateDir;
        }

        return null;
    }

    public static File getMediaSubFile(String fileName) {

        if (StringUtil.isNullEmptyOrSpace(fileName)) {
            return null;
        }

        String pureFileName = fileName.substring(0, fileName.indexOf("."));
        if (pureFileName.length() != 17) {
            return null;
        }

        String dirName = fileName.substring(0, 8);
        List<File> mediaDirList = getMediaDirList();
        for (int i = 0; i < mediaDirList.size(); i++) {
            String mediaDir = mediaDirList.get(i).getPath();
            File subDir = getMediaSubDir(dirName, mediaDir);
            if (subDir != null) {
                File subFile = new File(subDir, fileName);
                if (subFile.exists() && !subFile.isDirectory()) {
                    return subFile;
                }
            }
        }

        return null;
    }

    public static File saveAudioMessage(String uuid, byte[] bytes) {
        LogUtil.e("SaveAudioMessage..........." + uuid);
        File aDir = getAudioDir(HELMET_ROOT_DIR, VOICE_DIR_NAME);
        File receiveAudioFile = new File(aDir, "receive_audio_file.mp3");

        FileOutputStream outputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        try {
            if (receiveAudioFile.exists()) {
                receiveAudioFile.delete();
            }
            receiveAudioFile.createNewFile();
            outputStream = new FileOutputStream(receiveAudioFile);
            bufferedOutputStream = new BufferedOutputStream(outputStream);
            bufferedOutputStream.write(bytes);
            bufferedOutputStream.flush();

            return receiveAudioFile;

        } catch (Exception e) {
            // 打印异常信息
            e.printStackTrace();
        } finally {
            // 关闭创建的流对象
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }

        return null;
    }

    /**
     * return received voice file or null if file not exist
     *
     * @return last
     */
    public static File getReceivedVoiceFile() {
        File aDir = getAudioDir(HELMET_ROOT_DIR, VOICE_DIR_NAME);
        File receiveAudioFile = new File(aDir, "receive_audio_file.mp3");
        if (receiveAudioFile.exists() && receiveAudioFile.isFile()) {
            return receiveAudioFile;
        }

        return null;
    }

    public static boolean savePublicConfig(String content) {

        FileOutputStream fos = null;
        File rootDir = getAudioDir(HELMET_ROOT_DIR, CONFIG_DIR_NAME);
        File configFile = new File(rootDir, CONFIG_FILE_NAME);

        if (configFile.exists()) {
            configFile.delete();
        }

        try {
            configFile.createNewFile();
            fos = new FileOutputStream(configFile);
            fos.write(content.getBytes());
            return true;
        } catch (Exception e) {
            LogUtil.e("txhlog savePublicConfig err=" + e.toString());
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return false;
    }

    public static String readPublicConfig() {

        FileInputStream fis = null;

        File configDir = getAudioDir(HELMET_ROOT_DIR, CONFIG_DIR_NAME);
        File configFile = new File(configDir, CONFIG_FILE_NAME);
        if (!configFile.exists() || !configFile.isFile()) {
            return "";
        }

        try {
            fis = new FileInputStream(configFile);
            byte temp[] = new byte[fis.available()];

            fis.read(temp);
            String result = new String(temp);

            return result;
        } catch (Exception e) {
            LogUtil.e("txhlog readPublicConfig err=" + e.toString());
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return "";
    }

    public static void deletePublicConfig() {
        File configDir = getAudioDir(HELMET_ROOT_DIR, CONFIG_DIR_NAME);
        File configFile = new File(configDir, CONFIG_FILE_NAME);
        if (configFile.exists()) {
            configFile.delete();
        }
    }

    public static void savePrivateConfig(String message) {
        FileOutputStream fos = null;
        Context context = HelmetApplication.mAppContext;

        try {
            fos = context.openFileOutput(CONFIG_FILE_NAME, Context.MODE_PRIVATE);
            byte[] bytes = message.getBytes();
            fos.write(bytes);
        } catch (Exception e) {
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String readPrivateConfig() {

        Context context = HelmetApplication.mAppContext;
        FileInputStream fis = null;

        try {
            fis = context.openFileInput(CONFIG_FILE_NAME);
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);

            return new String(buffer);
        } catch (Exception e) {
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return "";
    }

    public static List<File> getMediaFileList(int mediaType, long startTime, long endTime) {

        List<File> mediaFileList = new ArrayList<File>();

        if (startTime > endTime) {
            return mediaFileList;
        }

        String suffix = "";
        if (mediaType != Constant.MEDIA_VIDEO && mediaType != Constant.MEDIA_PHOTO) {
            return mediaFileList;
        }

        String startDirName = Util.getFormatDate(startTime);
        String endDirName = Util.getFormatDate(endTime);
        String startFileName = Util.getFormatTime(startTime);
        String endFileName = Util.getFormatTime(endTime);

        List<File> mediaDirList = getMediaDirList();
        for (int i = 0; i < mediaDirList.size(); i++) {
            File mediaDir = mediaDirList.get(i);
            if (mediaDir == null) {
                continue;
            }

            File[] dateDirArray = mediaDir.listFiles();
            if (dateDirArray == null || dateDirArray.length == 0) {
                continue;
            }

            if (startDirName.equals(endDirName)) {//same dir
                File dir = FileUtil.getMediaSubDir(startDirName, mediaDir.getPath());
                mediaFileList.addAll(getMediaFileListFromDir(dir, mediaType, startFileName, endFileName));
            } else {
                for (File dir : dateDirArray) {
                    String dirName = dir.getName();
                    int comareStart = dirName.compareTo(startDirName);
                    int compareEnd = dirName.compareTo(endDirName);

                    if (comareStart == 0 || compareEnd == 0) {
                        mediaFileList.addAll(getMediaFileListFromDir(dir, mediaType, startFileName, endFileName));
                    } else if (comareStart > 0 && compareEnd < 0) {
                        mediaFileList.addAll(getMediaFileListFromDir(dir, mediaType, null, null));
                    } else {
                        continue;
                    }
                }
            }
        }

        return mediaFileList;
    }

    private static List<File> getMediaFileListFromDir(File dir, final int mediaType, String startTime, String endTime) {
        List<File> fileList = new ArrayList<File>();

        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return fileList;
        }

        File[] fileArray = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (mediaType == Constant.MEDIA_VIDEO) {
                    return name.endsWith(".flv");
                } else if (mediaType == Constant.MEDIA_PHOTO) {
                    return (name.endsWith(".jpeg") || name.endsWith(".jpg"));
                } else {
                    return false;
                }
            }
        });

        boolean selectAllFile = ((startTime == null) && (endTime == null));
        if (fileArray != null) {
            for (File file : fileArray) {
                String fileName = file.getName();
                fileName = fileName.substring(0, fileName.lastIndexOf("."));
                if (selectAllFile || fileName.compareTo(startTime) >= 0 && fileName.compareTo(endTime) <= 0) {
                    fileList.add(file);
                }
            }
        }

        return fileList;
    }

    public static void saveBitmapFromYuvImage(Bitmap bitmap,
                                              boolean isFront,
                                              boolean compress,
                                              PhotoCallback callback) {
        if (bitmap != null) {
            //rotate bitmap
            Matrix matrix = new Matrix();
            Log.e("YJQ", "isFront: " + isFront);
            if (isFront) {
                matrix.preRotate(180);
            } else {
                matrix.preRotate(-270);
            }

            if (compress) {
                int srcWidth = bitmap.getWidth();
                float scale = ((float) 360 / srcWidth);
                matrix.postScale(scale, scale);
            }

            //add by Jerry 临时修改
            //Bitmap finalBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            //bitmap.recycle();
            saveBitmap(bitmap, compress, callback);
        } else {
            if (callback != null) {
                callback.updatePhotoStatus(false, null);
            }
        }
    }

    public static void saveBitmap(Bitmap bmp, boolean compress, PhotoCallback callback) {

        File photoFile = LocalFileManager.getInstance().getEmptyPhotoFile(compress);
        Log.e("YJQ", "saving Bitmap : " + photoFile.getName());

        long startSave = System.currentTimeMillis();

        FileOutputStream fos = null;
        BufferedOutputStream bos = null;

        try {
            fos = new FileOutputStream(photoFile);
            bos = new BufferedOutputStream(fos);

            long startCompress = System.currentTimeMillis();
            if (compress) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            } else {
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            }

            long finishCompress = System.currentTimeMillis();
            Log.e("YJQ", "compressTime : " + (finishCompress - startCompress));

            bos.flush();

            Log.e("YJQ", "writeFileTime : " + (System.currentTimeMillis() - finishCompress));

        } catch (IOException e) {
            if (callback != null) {
                callback.updatePhotoStatus(false, null);
            }
            LogUtil.e("save photo failed: ");
            LogUtil.e(e);
            return;
        } finally {
            if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
            }

            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        Log.e("YJQ", "savingTime : " + (System.currentTimeMillis() - startSave));

        callback.updatePhotoStatus(true, photoFile);
    }
}
