package com.cy.helmet.factorytest;

import android.os.Environment;

import com.cy.helmet.sensor.GSensor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class FactoryTest {
    private static FactoryTest mInstance;
    private static final String FACTORY_TEST_NAME = "factoryTest.txt";
    private static final String FACTORY_PATH = Environment.getExternalStorageDirectory().getPath()
            + File.separator + "Helmet" + File.separator + "config";
    public static final int PSENSOR_MODE_AND = 1;
    public static final int PSENSOR_MODE_OR = 2;
    public static int mPsensorMode = PSENSOR_MODE_OR;
    public static int mGsensorCalValue = GSensor.DEFAULT_GSENSOR_VALUE;//0-31;

    public static synchronized FactoryTest getInstance() {
        if (mInstance == null) {
            synchronized (FactoryTest.class) {
                if (mInstance == null) {
                    mInstance = new FactoryTest();
                }
            }
        }
        return mInstance;
    }

    public void readFactoryConfig() {
        File fileDir = new File(FACTORY_PATH);
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }

        File file = new File(FACTORY_PATH + File.separator + FACTORY_TEST_NAME);
        if (file.exists()) {
            FileInputStream fin = null;
            BufferedReader reader = null;
            String firstLine = null;
            String secondLine = null;
            try {
                fin = new FileInputStream(file);
                reader = new BufferedReader(new InputStreamReader(fin));
                firstLine = reader.readLine();
                secondLine = reader.readLine();
                FactoryUtil.e(
                        "bef------->firstLine = " + firstLine
                                + "\nsecondLine = " + secondLine);
                if (firstLine != null && secondLine != null) {
                    firstLine = extractVlidCharacters(firstLine);
                    secondLine = extractVlidCharacters(secondLine);
                    try {
                        mPsensorMode = Integer.valueOf(firstLine);
                        mGsensorCalValue = Integer.valueOf(secondLine);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                FactoryUtil.e(
                        "aft------->firstLine = " + firstLine
                                + "\nsecondLine = " + secondLine);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            FactoryUtil.e("------->file not exists " + file.getAbsolutePath());
        }
    }

    private String extractVlidCharacters(String source) {
        if (source == null)
            return null;
        if (!source.contains(":"))
            return null;

        String tag = source.substring(source.indexOf(":") + 1);

        return tag;
    }
}
