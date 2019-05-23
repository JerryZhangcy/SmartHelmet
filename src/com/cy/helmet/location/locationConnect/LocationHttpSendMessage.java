package com.cy.helmet.location.locationConnect;

import org.json.JSONObject;

public class LocationHttpSendMessage {
    public static final int TYPE_CACHING = 0;//缓存数据
    public static final int TYPE_NORMAL = 1;//单条发送数据
    public static final int TYPE_MUL = 2;//多条数据
    public JSONObject message;
    private byte[] mSendBytes;
    public int mMsg_type = TYPE_NORMAL;

    public LocationHttpSendMessage(JSONObject msg, int type) {
        if (msg != null) {
            message = msg;
            mSendBytes = message.toString().getBytes();
            mMsg_type = type;
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
        return message == null ? null : message.toString();
    }
}
