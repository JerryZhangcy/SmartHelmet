package com.cy.helmet.video.deviceconfig;//package com.laifeng.sopcastsdk.deviceconfig;
//
///**
// * Created by tangxiaohui on 2018/1/16.
// */
//
//public class SHDeviceCfg {
//
//    private volatile static SHDeviceCfg sConfig;
//
//    private SHDeviceCfg() {
//    }
//
//    public static SHDeviceCfg get() {
//        if (sConfig == null) {
//            synchronized (SHDeviceCfg.class) {
//                if (sConfig == null) {
//                    sConfig = new SHDeviceCfg();
//                }
//            }
//        }
//        return sConfig;
//    }
//
//    private int voiceTalkTimeSec;
//    private int videoFileLength;
//    private int videoResolution;
//    private int videoCodeRate;
//    private int liveResolution;
//    private int liveCodeRate;
//    private int openGps;
//    private int locationCfg;
//    private int gyroscopeSwitch;
//    private float gyroscopeCfgX;
//    private float gyroscopeCfgY;
//    private float gyroscopeCfgZ;
//    private int firstTfCard;
//    private int gpsBusId = -1;
//    private int gpsId;
//
//    public int getVoiceTalkTimeSec() {
//        return voiceTalkTimeSec;
//    }
//
//    public void setVoiceTalkTimeSec(int voiceTalkTimeSec) {
//        this.voiceTalkTimeSec = voiceTalkTimeSec;
//    }
//
//    public int getVideoFileLength() {
//        return videoFileLength;
//    }
//
//    public void setVideoFileLength(int videoFileLength) {
//        this.videoFileLength = videoFileLength;
//    }
//
//    public int[] getVideoResolution() {
//        int[] resolution = getResolution(videoResolution);
//        return resolution;
//    }
//
//    public void setVideoResolution(int videoResolution) {
//        this.videoResolution = videoResolution;
//    }
//
//    public int getVideoCodeRate() {
//        return videoCodeRate;
//    }
//
//    public void setVideoCodeRate(int videoCodeRate) {
//        this.videoCodeRate = videoCodeRate;
//    }
//
//    public int[] getLiveResolution() {
//        int[] resolution = getResolution(liveResolution);
//        return resolution;
//    }
//
//    public void setLiveResolution(int liveResolution) {
//        this.liveResolution = liveResolution;
//    }
//
//    public int getLiveCodeRate() {
//        return liveCodeRate;
//    }
//
//    public void setLiveCodeRate(int liveCodeRate) {
//        this.liveCodeRate = liveCodeRate;
//    }
//
//    public int getOpenGps() {
//        return openGps;
//    }
//
//    public void setOpenGps(int openGps) {
//        this.openGps = openGps;
//    }
//
//    public int getLocationCfg() {
//        return locationCfg;
//    }
//
//    public void setLocationCfg(int locationCfg) {
//        this.locationCfg = locationCfg;
//    }
//
//    public int getGyroscopeSwitch() {
//        return gyroscopeSwitch;
//    }
//
//    public void setGyroscopeSwitch(int gyroscopeSwitch) {
//        this.gyroscopeSwitch = gyroscopeSwitch;
//    }
//
//    public float getGyroscopeCfgX() {
//        return gyroscopeCfgX;
//    }
//
//    public void setGyroscopeCfgX(float gyroscopeCfgX) {
//        this.gyroscopeCfgX = gyroscopeCfgX;
//    }
//
//    public float getGyroscopeCfgY() {
//        return gyroscopeCfgY;
//    }
//
//    public void setGyroscopeCfgY(float gyroscopeCfgY) {
//        this.gyroscopeCfgY = gyroscopeCfgY;
//    }
//
//    public float getGyroscopeCfgZ() {
//        return gyroscopeCfgZ;
//    }
//
//    public void setGyroscopeCfgZ(float gyroscopeCfgZ) {
//        this.gyroscopeCfgZ = gyroscopeCfgZ;
//    }
//
//    public int getFirstTfCard() {
//        return firstTfCard;
//    }
//
//    public void setFirstTfCard(int firstTfCard) {
//        this.firstTfCard = firstTfCard;
//    }
//
//    public int getGpsBusId() {
//        return gpsBusId;
//    }
//
//    public void setGpsBusId(int gpsBusId) {
//        this.gpsBusId = gpsBusId;
//    }
//
//    public int getGpsId() {
//        return gpsId;
//    }
//
//    public void setGpsId(int gpsId) {
//        this.gpsId = gpsId;
//    }
//
//    @Override
//    public String toString() {
//        return "{"
//                + "talkTimeSec=" + voiceTalkTimeSec
//                + ",videoFileLength=" + videoFileLength
//                + ",videoResolution=" + videoResolution
//                + ",videoCodeRate=" + videoCodeRate
//                + ",liveResolution=" + liveResolution
//                + ",liveCodeRate=" + liveCodeRate
//                + ",openGps=" + openGps
//                + ",locationCfg=" + locationCfg
//                + ",gyroscopeSwitch=" + gyroscopeSwitch
//                + ",gyroscopeCfgX=" + gyroscopeCfgX
//                + ",gyroscopeCfgY=" + gyroscopeCfgY
//                + ",gyroscopeCfgZ=" + gyroscopeCfgZ
//                + ",firstTfCard=" + firstTfCard
//                + ",gpsBusId=" + gpsBusId
//                + '}';
//    }
//
//    private int[] getResolution(int liveResolution) {
//        int[] resolution = new int[2];
//        switch (liveResolution) {
//            case 320:
//                resolution[0] = 320;
//                resolution[1] = 480;
//                break;
//            case 480:
//                resolution[0] = 480;
//                resolution[1] = 720;
//                break;
//            case 720:
//                resolution[0] = 720;
//                resolution[1] = 1280;
//                break;
//            case 1080:
//                resolution[0] = 1080;
//                resolution[1] = 1920;
//                break;
//            default:
//                resolution[0] = 480;
//                resolution[1] = 720;
//                break;
//        }
//        return resolution;
//    }
//}
