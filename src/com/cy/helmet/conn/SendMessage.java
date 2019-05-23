package com.cy.helmet.conn;

import com.cy.helmet.Constant;
import com.cy.helmet.core.protocol.HelmetServer;
import com.cy.helmet.util.Util;

import java.nio.ByteBuffer;

/**
 * Created by yaojiaqing on 2018/2/3.
 */

public class SendMessage {

    private int mLength;
    private byte[] mSendBytes;
    public HelmetServer.H2SMessage mMsg;

    public SendMessage(HelmetServer.H2SMessage msg) {
        if (msg != null) {
            mMsg = msg;
            byte[] protoByteArray = msg.toByteArray();
            mLength = protoByteArray.length;
            mSendBytes = new byte[mLength + Constant.MSG_PREFIX_LEN];
            byte[] lengthByteArray = Util.intToByteArray(protoByteArray.length);
            ByteBuffer.wrap(mSendBytes).put(Constant.MSG_PREFIX_CODE).put(lengthByteArray).put(protoByteArray);
        }
    }

    public byte[] toBytes() {
        return mSendBytes;
    }

    public boolean isValid() {
        return (mSendBytes != null && mSendBytes.length != 0);
    }

    @Override
    public String toString() {
        return mMsg == null ? "null" : mMsg.toString();
    }
}
