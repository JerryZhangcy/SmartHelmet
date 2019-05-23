package com.cy.helmet.conn;

import com.cy.helmet.Constant;
import com.cy.helmet.core.protocol.ServerHelmet;
import com.cy.helmet.util.LogUtil;

/**
 * Created by yaojiaqing on 2018/2/3.
 */

public class RecvMessage {
    private int mLength;
    public ServerHelmet.S2HMessage mS2HMessage;

    public RecvMessage(byte[] data) {

        if (data == null || data.length < Constant.MSG_PREFIX_LEN) {
            return;
        }

        if ((int) data[0] != Constant.MSG_PREFIX_CODE) {//0b01010000
            return;
        }

        mLength = (data[1] & 0xff)
                | ((data[2] & 0xff) << 8)
                | ((data[3] & 0xff) << 16)
                | ((data[4] & 0xff) << 24);

        if (mLength > data.length) {
            return;
        }

        byte[] content = new byte[mLength];
        System.arraycopy(data, Constant.MSG_PREFIX_LEN, content, 0, content.length);

        try {
            ServerHelmet.S2HMessage protoMessage = ServerHelmet.S2HMessage.parseFrom(content);
            mS2HMessage = protoMessage;
        } catch (Exception e) {
            LogUtil.e("parse 'S2HMessage' failed...");
            return;
        }
    }

    public boolean isValid() {
        return mS2HMessage != null;
    }
}
