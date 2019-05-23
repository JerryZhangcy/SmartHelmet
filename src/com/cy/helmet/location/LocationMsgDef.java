package com.cy.helmet.location;

/**
 * Created by zhangchongyang on 18-1-3.
 */

public class LocationMsgDef {

    public static final byte GPS_CONFIRM = 0;
    public static final byte GPS_DATA = 1;
    public static final byte GPS_PARAMETER_SET = 2;
    public static final byte GPS_PARAMETER_GET = 3;
    public static final byte GPS_STATE = 4;
    public static final byte GPS_SOS = 5;
    public static final byte GPS_SAME = 6;
    public static final byte GPS_INFO = 7;
    public static final byte GPS_ERR = 8;
    public static final byte GPS_LBS = 9;
    public static final byte GPS_CACHING = 10;


    /**
     * GPS_CONFIRM(消息类型t=0),连接上的时候的注册包（n=server_id）
     * 若此次连接为短连接，则在消息内容中的服务器ID后面加一个英文字符的冒号
     */
    public static class Common {
        public static final String COMMON_T = "t";
        public static final String COMMON_N = "n";
        public static final String COMMON_E = "e";
        private byte t; //表示消息类型
        private String n; //表示消息的具体内容
        private String e;//GPS模块的唯一序列号（IMEI）

        public void setT(byte t) {
            this.t = t;
        }

        public byte getT() {
            return this.t;
        }

        public void setE(String e) {
            this.e = e;
        }

        public String getE() {
            return this.e;
        }
    }

    /**
     * GPS_DATA(消息类型t=1): 定时上传的GPS位置数据,包含定位方式(基站or卫星)
     */
    public static class GpsData {
        public static final String GPSDATA_S = "s";
        public static final String GPSDATA_X = "x";
        public static final String GPSDATA_Y = "y";
        public static final String GPSDATA_TIME = "time";
        public static final String GPSDATA_G = "g";
        public static final String GPSDATA_XS = "xs";
        public static final String GPSDATA_YS = "ys";
        public static final String GPSDATA_ZS = "zs";

        private int s;//1表示GPS定位 -1表示定位失败
        private String x;//表示纬度，格式：**.******小数点后6位，前2位，不足用0补齐,失败传0
        private String y;//表示经度，格式：***.******小数点后6位，前2位，不足用0补齐，失败传0
        private long time;//缓存的GPS数据需要包含时间，unix time
        private int g;//GPS_Num
        private String xs;//定位时重力传感器X轴方向数值
        private String ys;//定位时重力传感器Y轴方向数值
        private String zs;//定位时重力传感器Z轴方向数值

        public int getS() {
            return s;
        }

        public void setS(int s) {
            this.s = s;
        }

        public String getX() {
            return x;
        }

        public void setX(String x) {
            this.x = x;
        }

        public String getY() {
            return y;
        }

        public void setY(String y) {
            this.y = y;
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }

        public int getG() {
            return g;
        }

        public void setG(int g) {
            this.g = g;
        }

        public String getXs() {
            return xs;
        }

        public void setXs(String xs) {
            this.xs = xs;
        }

        public String getYs() {
            return ys;
        }

        public void setYs(String ys) {
            this.ys = ys;
        }

        public String getZs() {
            return zs;
        }

        public void setZs(String zs) {
            this.zs = zs;
        }
    }

    /**
     * GPS_PARAMETER_SET(消息类型t=2): GPS可配置参数,包括服务器IP(多个)、端口和
     * 定位数据上传频率、状态上传频率等可配置更改的信息
     * <p>
     * GPS_PARAMETER_GET(消息类型t=3): 用于响应查询GPS可配置参数,包括服务器IP(多
     * 个)、端口和定位数据上传频率、状态上传频率等可配置更改的信息
     */
    public static class GpsParameter {
        public static final String GPSPARAMETER_A = "a";
        public static final String GPSPARAMETER_I = "i";
        public static final String GPSPARAMETER_P = "p";
        public static final String GPSPARAMETER_D = "d";
        public static final String GPSPARAMETER_F = "f";
        @Deprecated
        public static final String GPSPARAMETER_B = "b";
        private int a;//消息ID,来自应该响应的服务器包
        private String i;//服务器IP,此项一旦修改,GPS终端应该在返回服务器响应包后,立即连接到新的IP上去
        private int p;//服务器Port,此项一旦修改,GPS终端应该在返回服务器响应包后,立即连接到新的Port上去
        private int d;//定位数据上传频率,单位:秒/次
        private int f;//状态包上传频率,单位:秒/次
        @Deprecated
        private int b;//非工作模式下定位数据包上传频率,单位:秒/次 默认60秒/次

        public int getA() {
            return a;
        }

        public void setA(int a) {
            this.a = a;
        }

        public String getI() {
            return i;
        }

        public void setI(String i) {
            this.i = i;
        }

        public int getP() {
            return p;
        }

        public void setP(int p) {
            this.p = p;
        }

        public int getD() {
            return d;
        }

        public void setD(int d) {
            this.d = d;
        }

        public int getF() {
            return f;
        }

        public void setF(int f) {
            this.f = f;
        }

        @Deprecated
        public int getB() {
            return b;
        }

        @Deprecated
        public void setB(int b) {
            this.b = b;
        }
    }

    /**
     * GPS_STATE(消息类型t=4): 定时上报的GPS状态消息,包括信号强度,电量等信息
     * 包含字段，非工作状态下不上传此信息
     */
    public static class GpsState {
        public static final String GPSSTATE_C = "c";
        public static final String GPSSTATE_G = "g";
        public static final String GPSSTATE_B = "b";
        private int c;//GSM_CSQ
        private int g;//GPS_Num
        private int b;//当前电量百分比,取值范围:0-100

        public int getC() {
            return c;
        }

        public void setC(int c) {
            this.c = c;
        }

        public int getG() {
            return g;
        }

        public void setG(int g) {
            this.g = g;
        }

        public int getB() {
            return b;
        }

        public void setB(int b) {
            this.b = b;
        }
    }

    /**
     * GPS_SOS(消息类型t=5): GPS模块主动触发的报警信息,为一个短的字符串,0x53=S表
     * 示SOS主动报警(按钮);0x4C=L表示低电量报警
     * <p>
     * GPS_SAME(消息类型t=6): 消息内容为:“D”,表示此次定位数据与上次一样
     */
    public static class GpsSosSame {
        public static final String GPSSOSSAME_m = "m";
        private String m;//0x53=S表示SOS主动报警(按钮);0x4C=L表示低电量报警

        public String getM() {
            return m;
        }

        public void setM(String m) {
            this.m = m;
        }
    }

    /**
     * GPS_INFO(消息类型t=7): 包括GPS模块的固件信息,软件版本号等,一般仅供查询时返
     * 回,不可更改的信息
     */
    public static class GpsInfo {
        public static final String GPSINFO_A = "a";
        public static final String GPSINFO_P = "p";
        public static final String GPSINFO_Q = "q";
        public static final String GPSINFO_M = "m";
        private int a;//消息ID,来自应该响应的服务器包
        private String p;//硬件ID 共用手机的IMEI
        private String q;//软件版本 此项留空""
        private String m;//固件版本

        public int getA() {
            return a;
        }

        public void setA(int a) {
            this.a = a;
        }

        public String getP() {
            return p;
        }

        public void setP(String p) {
            this.p = p;
        }

        public String getQ() {
            return q;
        }

        public void setQ(String q) {
            this.q = q;
        }

        public String getM() {
            return m;
        }

        public void setM(String m) {
            this.m = m;
        }
    }

    /**
     * GPS_ERR(消息类型t=8): 当模块自身出现错误或者当下发的参数或者查询消息错误、无法
     * 解析的时候,需要返回GPS返回一个ERR包过来,两种情况的稍有不同,详细见以下。
     */
    public static class GpsError {
        public static final byte UNKNOW_ERROR = 0;
        public static final byte RESOLVE_ERROR = 1;
        public static final byte UNKNOW_TYPE_ERROR = 2;
        public static final byte IERR = 3;

        public static final String GPSERROR_A = "a";
        public static final String GPSERROR_P = "p";
        private int a;//消息ID,来自应该响应的服务器包;若主动上报,则a值应该置为0
        /**
         * 0 :UnknownErr 未知错误
         * 1 :ResolveMsgErr 解析服务器下发参数失败
         * 2 :UnknownMsgTypeErr 收到未定义类型消息
         * 3 :IERR 内部错误,这里一般指解析完参数后,自身内部逻辑处理出错导致无法正确响应
         */
        private byte p;//错误消息码,具体待约定

        public int getA() {
            return a;
        }

        public void setA(int a) {
            this.a = a;
        }

        public byte getP() {
            return p;
        }

        public void setP(byte p) {
            this.p = p;
        }
    }

    /**
     * GPS_LBS(消息类型t=9): 当模块无法使用GPS定位但是能接收到基站信号时,模块延续定
     * 位频率上传当前基站数据
     */
    public static class GpsLbs {
        public static final String GPSLBS_M = "m";
        public static final String GPSLBS_N = "n";
        public static final String GPSLBS_L = "l";
        public static final String GPSLBS_C = "c";
        public static final String GPSDATA_XS = "xs";
        public static final String GPSDATA_YS = "ys";
        public static final String GPSDATA_ZS = "zs";

        private int m;//MCC: 国家代码：中国代码 460
        private int n;//MNC，移动设备网络代码（Mobile Network Code，MNC），中国移动 = 00，中国联通 = 01, 中国电信 = 03 05 11
        private int l;//LAC，Location Area Code，位置区域码；
        private int c;//CID，Cell Identity，基站编号，是个16位的数据（范围是0到65535）
        private String xs;//定位时重力传感器X轴方向数值
        private String ys;//定位时重力传感器Y轴方向数值
        private String zs;//定位时重力传感器Z轴方向数值

        public int getM() {
            return m;
        }

        public void setM(int m) {
            this.m = m;
        }

        public int getN() {
            return n;
        }

        public void setN(int n) {
            this.n = n;
        }

        public int getL() {
            return l;
        }

        public void setL(int l) {
            this.l = l;
        }

        public int getC() {
            return c;
        }

        public void setC(int c) {
            this.c = c;
        }

        public String getXs() {
            return xs;
        }

        public void setXs(String xs) {
            this.xs = xs;
        }

        public String getYs() {
            return ys;
        }

        public void setYs(String ys) {
            this.ys = ys;
        }

        public String getZs() {
            return zs;
        }

        public void setZs(String zs) {
            this.zs = zs;
        }
    }
}
