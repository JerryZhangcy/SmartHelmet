package com.czt.mp3recorder;

import java.io.File;

/**
 * Created by jiaqing on 2018/1/4.
 */

public interface OnEncodeListener {
    void onEncodeFinish(File file);
}
