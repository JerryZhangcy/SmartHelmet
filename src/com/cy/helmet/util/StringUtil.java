package com.cy.helmet.util;

import android.text.TextUtils;

/**
 * Created by yaojiaqing on 2018-02-22.
 */

public class StringUtil {

    public static boolean isNullEmptyOrSpace(String str) {
        return (str == null || TextUtils.isEmpty(str.trim()));
    }
}
