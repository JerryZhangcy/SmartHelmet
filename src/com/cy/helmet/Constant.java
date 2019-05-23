package com.cy.helmet;

/**
 * Created by yaojiaqing on 2017/12/30.
 */

public class Constant {

    public static final long MAX_SEND_PROTO_SIZE = 32 * 1024 * 1024;

    //upload file url suffix
    public static final String UPLOAD_FILE_URL_SUFFIX = "/helmet/uploadfile";

    //sos send url path
    public static final String SOS_SERVER_URL_PATH = "/helmet_call/sos/";

    //active address
    public static final String ACTIVE_SERVER_ADDRESS = "http://helmet.chaoyingtec.com/as/activate";

    //decode parameter
    public static final int MAX_RECV_MESSAGE_BYTE = 8 * 1024 * 1024;
    public static final int LENGTH_FIELD_INDEX = 1;
    public static final int LENGTH_FIELD_SIZE = 4;
    public static final long TIME_INTERVAL = 60 * 1000;

    //file type
    public static final int UPLOAD_IS_SYNC = 1;
    public static final int UPLOAD_IS_ASYNC = 2;

    //voice
    public static final int PLAY_HINT_WHEN_TAKE_PHOTO = 1;
    public static final int PLAY_HINT_WHEN_RECORD = 1;
    public static final int FORCE_TO_TAKE_VIDEO = 1;

    //talk
    public static final int REQUEST_TALK_TIMEOUT_SEC = 3;

    //media
    public static final int MEDIA_VIDEO = 1;
    public static final int MEDIA_PHOTO = 2;

    // connect
    public static final byte MSG_PREFIX_CODE = 0x50;
    public static final int MSG_PREFIX_LEN = 5;
    public static final int MAX_RECV_MSG_LEN = 8 * 1024 * 1024;
    public static final int RESULT_OK = 1;

    //video
    public static final int DEFAULT_VIDEO_SIZE = 32;

    //storage
    public static final int STORAGE_TYPE_INTERNAL = 1;
    public static final int STORAGE_TYPE_EXTERNAL = 2;


    //network
    public static final int NETWORK_TYPE_WIFI = 1;
    public static final int NETWORK_TYPE_MOBILE = 2;

    public static final int NETWORK_STATUS_WEAK = 1;
    public static final int NETWORK_STATUS_NORMAL = 2;
    public static final int NETWORK_STATUS_STRONG = 3;

    //file upload
    public static final int FILE_UPLOAD_NOT_EXIST = 0;
    public static final int FILE_UPLOAD_ASYNC_RESP = 1;
    public static final int FILE_UPLOAD_FAILED = 2;

    //network choose
    public static final int NET_CHOOSE_WIFI_FIRST = 1;
    public static final int NET_CHOOSE_FORCE_WIFI = 2;
    public static final int NET_CHOOSE_FORCE_MOBILE = 3;

   //add by Jerry activation wifi for default
   public static final String ACTIVATION_WIFI_SSID = "maozi";
   public static final String ACTIVATION_WIFI_PWD = "12345678";   
}
