package com.cy.helmet.wifi;

import android.os.Environment;

import com.cy.helmet.location.LocationUtil;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

public class WifiManagerUtil {
    private static final String WIFI_SCAN_RESULT = "wifi_scan_result.txt";
    private static final String WIFI_SCAN_FILE_PATH = "Helmet" + File.separator + "wifiCaching";
    private static Object mObject = new Object();
    private static String mLocalPath = Environment.getExternalStorageDirectory().getPath();
    private static String mFolderPath = mLocalPath + File.separator + WIFI_SCAN_FILE_PATH;

    public static void writeFile(JSONArray array) {
        synchronized (mObject) {
            Writer writer = null;
            FileOutputStream fos = null;
            File file = new File(mFolderPath);
            if (!file.exists()) {
                file.mkdirs();
            }
            File inputFile = new File(mFolderPath + File.separator + WIFI_SCAN_RESULT);
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
                writer.write(array.toString() + "\n");
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

    public static ArrayList<JSONArray> readFile() {
        ArrayList<JSONArray> list = new ArrayList<JSONArray>();
        String firstLine = null;
        synchronized (mObject) {
            BufferedReader reader = null;
            File readFile = new File(mFolderPath + File.separator + WIFI_SCAN_RESULT);
            if (!readFile.exists()) {
                LocationUtil.e("LocationCaching---getFirstLineInfo---->readFile is no exists");
                return null;
            }
            FileInputStream fin = null;
            try {
                fin = new FileInputStream(readFile);
                reader = new BufferedReader(new InputStreamReader(fin));
                while ((firstLine = reader.readLine()) != null) {
                    try {
                        JSONArray array = new JSONArray(firstLine);
                        list.add(array);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
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

    public static void deleteCachingFile() {
        synchronized (mObject) {
            File readFile = new File(mFolderPath + File.separator + WIFI_SCAN_RESULT);
            if (readFile.exists()) {
                readFile.delete();
            }
        }
    }
}
