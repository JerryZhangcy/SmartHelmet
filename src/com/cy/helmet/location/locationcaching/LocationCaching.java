package com.cy.helmet.location.locationcaching;

import android.os.Environment;

import com.cy.helmet.location.LocationFactory;
import com.cy.helmet.location.LocationManager;
import com.cy.helmet.location.LocationMsgDef;
import com.cy.helmet.location.LocationUtil;
import com.cy.helmet.location.locationConnect.LocationHttpSendMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

/**
 * Created by zhangchongyang on 18-3-10.
 */

public class LocationCaching {
    private static Object mObject = new Object();
    private static String mLocalPath = Environment.getExternalStorageDirectory().getPath();
    private static String mFolderPath = mLocalPath + File.separator + LocationUtil.GPS_CACHING_FILE_PATH;
    private static String mFileName = LocationUtil.GPS_CACHING_FILE_NAME;
    private static String mTmpFileName = LocationUtil.GPS_CACHING_TMP_FILE_NAME;

    public static void writeCaching(String data, final String fileName) {
        synchronized (mObject) {
            Writer writer = null;
            FileOutputStream fos = null;
            File file = new File(mFolderPath);
            if (!file.exists()) {
                file.mkdirs();
            }
            File inputFile = new File(mFolderPath + File.separator + fileName);
            if (!inputFile.exists()) {
                try {
                    inputFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    LocationUtil.e("LocationCaching---writeCaching---->create file failed");
                    return;
                }
            }
            inputFile.setWritable(true);
            inputFile.setReadable(true);

            try {
                fos = new FileOutputStream(inputFile, true);
                writer = new OutputStreamWriter(fos);
                writer.write(data + "\n");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (writer != null)
                        writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    if (fos != null)
                        fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public static String getFirstLineInfo() {
        String firstLine = null;
        synchronized (mObject) {
            BufferedReader reader = null;
            File readFile = new File(mFolderPath + File.separator + mFileName);
            if (!readFile.exists()) {
                LocationUtil.e("LocationCaching---getFirstLineInfo---->readFile is no exists");
                return null;
            }
            FileInputStream fin = null;
            try {
                fin = new FileInputStream(readFile);
                reader = new BufferedReader(new InputStreamReader(fin));
                firstLine = reader.readLine();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (fin != null) {
                    try {
                        fin.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        LocationUtil.e("LocationCaching---getFirstLineInfo---->line = " + firstLine);
        if (firstLine != null) {
            String[] split = firstLine.split("\\|");//like this latitude|longitude|gpsnum|time
            firstLine = split[split.length - 1];
        }
        LocationUtil.e("LocationCaching---getFirstLineInfo---->line = " + firstLine);
        return firstLine;
    }

    public static ArrayList<JSONObject> readFullCaching(final String fileName) {
        ArrayList<JSONObject> list = new ArrayList<JSONObject>();
        String firstLine = null;
        synchronized (mObject) {
            BufferedReader reader = null;
            File readFile = new File(mFolderPath + File.separator + fileName);
            if (!readFile.exists()) {
                LocationUtil.e("LocationCaching---getFirstLineInfo---->readFile is no exists");
                return null;
            }
            FileInputStream fin = null;
            try {
                fin = new FileInputStream(readFile);
                reader = new BufferedReader(new InputStreamReader(fin));
                while ((firstLine = reader.readLine()) != null) {
                    String[] split = firstLine.split("\\|");
                    JSONObject jsonObject = LocationFactory.getInstance().
                            getGpsCachingEle(LocationManager.GPS_PROVIDER,
                                    split[0], split[1], Integer.valueOf(split[2]),
                                    split[3], split[4], split[5], Long.parseLong(split[split.length - 1]));
                    list.add(jsonObject);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (fin != null) {
                    try {
                        fin.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return list;
    }

    public static void deleteCachingFile(final String fileName) {
        synchronized (mObject) {
            File readFile = new File(mFolderPath + File.separator + fileName);
            if (readFile.exists()) {
                readFile.delete();
            }
        }
    }

    public static String readCaching() {
        String firstLine = null;
        synchronized (mObject) {
            BufferedReader reader = null;
            BufferedWriter bw = null;
            FileWriter fw = null;
            FileInputStream fin = null;
            int lineCount = 0;
            int index = 0;
            File readFile = new File(mFolderPath + File.separator + mFileName);
            if (!readFile.exists()) {
                LocationUtil.e("LocationCaching---readCaching---->readFile is no exists");
                return null;
            }
            File tmpFile = new File(mFolderPath + File.separator + mTmpFileName);
            try {
                fw = new FileWriter(tmpFile);
                bw = new BufferedWriter(fw);
                if (!tmpFile.exists()) {
                    tmpFile.createNewFile();
                }

                if (!tmpFile.exists()) {
                    LocationUtil.e("LocationCaching---readCaching---->tmpFile is no exists");
                    return null;
                }

                String line = null;
                fin = new FileInputStream(readFile);
                reader = new BufferedReader(new InputStreamReader(fin));
                while ((line = reader.readLine()) != null) {
                    if (lineCount == 0) {
                        firstLine = line;
                    } else {
                        bw.write(line + "\n");
                        if (index++ == 50) {
                            bw.flush();
                            index = 0;
                        }
                    }
                    lineCount++;
                }
                bw.flush();
                LocationUtil.e("LocationCaching---readCaching---->line = " + firstLine);

                if (readFile.exists()) {
                    readFile.delete();
                }

                if (tmpFile.exists()) {
                    tmpFile.renameTo(readFile);
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    if (bw != null) {
                        bw.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    if (fw != null) {
                        fw.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    if (fin != null) {
                        fin.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return firstLine;
    }

    public static void saveGpsCachingBySendFailed(JSONObject jsonObject, int type) {
        if (jsonObject == null)
            return;
        LocationUtil.e("LocationCaching---saveGpsCachingBySendFailed---->type = " + type
                + " jsonObject = " + jsonObject.toString());
        if (type == LocationHttpSendMessage.TYPE_NORMAL) {
            String x;
            String y;
            int g;
            long time;
            String xs;
            String ys;
            String zs;
            try {
                String n = jsonObject.getString(LocationMsgDef.Common.COMMON_N);
                JSONObject newJsonObject = new JSONObject(n);
                x = newJsonObject.getString(LocationMsgDef.GpsData.GPSDATA_X);
                y = newJsonObject.getString(LocationMsgDef.GpsData.GPSDATA_Y);
                g = newJsonObject.getInt(LocationMsgDef.GpsData.GPSDATA_G);
                time = newJsonObject.getLong(LocationMsgDef.GpsData.GPSDATA_TIME);
                xs = newJsonObject.getString(LocationMsgDef.GpsData.GPSDATA_XS);
                ys = newJsonObject.getString(LocationMsgDef.GpsData.GPSDATA_YS);
                zs = newJsonObject.getString(LocationMsgDef.GpsData.GPSDATA_ZS);
                LocationCaching.writeCaching(y + "|" + x + "|" +
                        g + "|" + xs + "|" + ys + "|" + zs + "|" +
                        String.valueOf(time), LocationUtil.GPS_CACHING_FILE_NAME);
            } catch (JSONException e) {
            }
        } else if (type == LocationHttpSendMessage.TYPE_MUL) {
            try {
                String x;
                String y;
                int g;
                long time;
                String xs;
                String ys;
                String zs;
                JSONArray array = jsonObject.getJSONArray(LocationMsgDef.Common.COMMON_N);
                LocationUtil.e("LocationCaching---saveGpsCachingBySendFailed---->array is null "
                        + (array == null));
                if (array != null && array.length() > 0) {
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject object = array.getJSONObject(i);
                        x = object.getString(LocationMsgDef.GpsData.GPSDATA_X);
                        y = object.getString(LocationMsgDef.GpsData.GPSDATA_Y);
                        g = object.getInt(LocationMsgDef.GpsData.GPSDATA_G);
                        time = object.getLong(LocationMsgDef.GpsData.GPSDATA_TIME);
                        xs = object.getString(LocationMsgDef.GpsData.GPSDATA_XS);
                        ys = object.getString(LocationMsgDef.GpsData.GPSDATA_YS);
                        zs = object.getString(LocationMsgDef.GpsData.GPSDATA_ZS);
                        LocationCaching.writeCaching(y + "|" + x + "|" +
                                g + "|" + xs + "|" + ys + "|" + zs + "|" +
                                String.valueOf(time), LocationUtil.GPS_CACHING_FILE_NAME);
                    }
                    LocationCaching.deleteCachingFile(LocationUtil.GPS_NORMAL_FILE_NAME);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
