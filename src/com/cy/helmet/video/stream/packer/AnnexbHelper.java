package com.cy.helmet.video.stream.packer;

import android.media.MediaCodec;
import android.util.Log;

import com.cy.helmet.video.constant.SopCastConstant;
import com.cy.helmet.video.utils.SopCastLog;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class AnnexbHelper {

    // Coded slice of a non-IDR picture slice_layer_without_partitioning_rbsp( )
    public final static int NonIDR = 1;
    // Coded slice of an IDR picture slice_layer_without_partitioning_rbsp( )
    public final static int IDR = 5;
    // Supplemental enhancement information (SEI) sei_rbsp( )
    public final static int SEI = 6;
    // Sequence parameter set seq_parameter_set_rbsp( )
    public final static int SPS = 7;
    // Picture parameter set pic_parameter_set_rbsp( )
    public final static int PPS = 8;
    // Access unit delimiter access_unit_delimiter_rbsp( )
    public final static int AccessUnitDelimiter = 9;

    private AnnexbNaluListener mListener;

    private static byte[] mPps;
    private static byte[] mSps;
    private boolean mUploadPpsSps = true;
    private static int mAnnexbHelperNum = 0;

    /**
     * the search result for annexb.
     */
    class AnnexbSearch {
        public int startCode = 0;
        public boolean match = false;
    }

    public static synchronized AnnexbHelper newInstance() {
        mAnnexbHelperNum++;
        return new AnnexbHelper();
    }

    private AnnexbHelper() {

    }

    public void stop() {
        mListener = null;
        mUploadPpsSps = true;
        synchronized (AnnexbHelper.class) {
            mAnnexbHelperNum--;
            if (mAnnexbHelperNum <= 0) {
                mPps = null;
                mSps = null;
            }
        }
    }

    public interface AnnexbNaluListener {
        void onSpsPps(byte[] sps, byte[] pps);

        void onVideo(byte[] data, boolean isKeyFrame, byte[] sps, byte[] pps);
    }

    public void setAnnexbNaluListener(AnnexbNaluListener listener) {
        mListener = listener;
    }

    /**
     * 将硬编得到的视频数据进行处理生成每一帧视频数据，然后传给flv打包器
     *
     * @param bb 硬编后的数据buffer
     * @param bi 硬编的BufferInfo
     */
    public void analyseVideoData(ByteBuffer bb, MediaCodec.BufferInfo bi) {

        Log.e("YJQ", "analyseVideoData start==============");

        long startAnalyse = System.currentTimeMillis();

        bb.position(bi.offset);
        bb.limit(bi.offset + bi.size);

        ArrayList<byte[]> frames = new ArrayList<>();
        boolean isKeyFrame = false;

        Log.e("YJQ", "bbPosition: " + bb.position());
        Log.e("YJQ", "bbSize: " + (bi.offset + bi.size - bb.position()));

        while (bb.position() < bi.offset + bi.size) {
            //fetch nal data
            long startFetchNal = System.currentTimeMillis();
            byte[] frame = annexbDemux(bb, bi);
            if (frame == null) {
                SopCastLog.e(SopCastConstant.TAG, "annexb not match.");
                break;
            }

            long fetchNalEnd = System.currentTimeMillis();
            Log.e("YJQ", "fetchNal: " + (fetchNalEnd - startFetchNal));

            // ignore the nalu type aud(9)
            if (isAccessUnitDelimiter(frame)) {
                continue;
            }

            // for pps
            if (isPps(frame)) {
                mPps = frame;
                continue;
            }

            // for sps
            if (isSps(frame)) {
                mSps = frame;
                continue;
            }

            // for IDR frame
            if (isKeyFrame(frame)) {
                isKeyFrame = true;
            } else {
                isKeyFrame = false;
            }

            byte[] naluHeader = buildNaluHeader(frame.length);
            frames.add(naluHeader);
            frames.add(frame);

            Log.e("YJQ", "mergeFrame: " + (System.currentTimeMillis() - fetchNalEnd));
        }

        if (mPps != null && mSps != null && mListener != null && mUploadPpsSps) {
            if (mListener != null) {
                long startSpsPps = System.currentTimeMillis();
                mListener.onSpsPps(mSps, mPps);
                Log.e("YJQ", "onSpsPpsTime: " + (System.currentTimeMillis() - startSpsPps));
            }
            mUploadPpsSps = false;
        }

        long mergeByteStart = System.currentTimeMillis();

        if (frames.size() == 0 || mListener == null) {
            return;
        }

        int size = 0;
        for (int i = 0; i < frames.size(); i++) {
            byte[] frame = frames.get(i);
            size += frame.length;
        }

        byte[] data = new byte[size];
        int currentSize = 0;
        for (int i = 0; i < frames.size(); i++) {
            byte[] frame = frames.get(i);
            System.arraycopy(frame, 0, data, currentSize, frame.length);
            currentSize += frame.length;
        }

        long mergeByteEnd = System.currentTimeMillis();
        Log.e("YJQ", "mergeBytes: " + (mergeByteEnd - mergeByteStart));

        if (mListener != null) {
            mListener.onVideo(data, isKeyFrame, mSps, mPps);
        }

        Log.e("YJQ", "writeData: " + (System.currentTimeMillis() - mergeByteEnd));

        Log.e("YJQ", "analyseVideoData finish==============");
    }

    /**
     * 从硬编出来的数据取出一帧nal
     *
     * @param bb
     * @param bi
     * @return
     */
    private byte[] annexbDemux(ByteBuffer bb, MediaCodec.BufferInfo bi) {

        long annexStart = System.currentTimeMillis();
        long annexFirst = -1;

        int index = bb.position();
        if (index >= bi.offset + bi.size - 3) {
            return null;
        }

        ByteBuffer frameBuffer = null;
        int frameStartPos = -1;
        boolean needFurtherTraverse = false;

        boolean findFirstPos = false;
        boolean findSecondPos = false;

        while (index < bi.offset + bi.size - 3) {
            if (!findFirstPos) {
                // not match.
                if (bb.get(index) != 0x00 || bb.get(index + 1) != 0x00) {
                    break;
                }

                // match N[00] 00 00 01, where N>=0
                if (bb.get(index + 2) == 0x01) {
                    findFirstPos = true;
                    bb.position(index + 3);

                    frameBuffer = bb.slice();
                    frameStartPos = bb.position();
                    index = frameStartPos;

                    annexFirst = System.currentTimeMillis();
                    Log.e("YJQ", "annex_first: " + (annexFirst - annexStart));

                    if (index < bi.offset + bi.size - 3) {
                        needFurtherTraverse = ((bb.get(bb.position()) & 0x1f) == SPS);
                    }

                    if (needFurtherTraverse) {
                        continue;
                    } else {
                        break;
                    }
                }
                index++;
            } else if (!findSecondPos) {
                if ((bb.get(index) == 0x00) && (bb.get(index + 1) == 0x00) && (bb.get(index + 2) == 0x01)) {
                    findSecondPos = true;
                    break;
                }
                bb.get();
                index = bb.position();
            }
        }

        Log.e("YJQ", "annex_second: " + (System.currentTimeMillis() - annexFirst));

        if (!findFirstPos) {
            return null;
        }

        int size;
        if (needFurtherTraverse) {
            size = bb.position() - frameStartPos;
            if (!findSecondPos) {
                Log.e("YJQ", "annex_second: " + false);
                size += 3;
            }
        } else {
            bb.position(bi.offset + bi.size);
            size = bb.position() - frameStartPos;
        }

        byte[] frameBytes = new byte[size];
        frameBuffer.get(frameBytes);

        long annexAll = System.currentTimeMillis() - annexStart;
        Log.e("YJQ", "annex_all: " + annexAll);

        return frameBytes;
    }

    /**
     * 从硬编出来的byteBuffer中查找nal
     *
     * @param as
     * @param bb
     * @param bi
     */
    private void avcStartWithAnnexb(AnnexbSearch as, ByteBuffer bb, MediaCodec.BufferInfo bi) {
        as.match = false;
        as.startCode = 0;
        int pos = bb.position();
        while (pos < bi.offset + bi.size - 3) {
            // not match.
            if (bb.get(pos) != 0x00 || bb.get(pos + 1) != 0x00) {
                break;
            }

            // match N[00] 00 00 01, where N>=0
            if (bb.get(pos + 2) == 0x01) {
                as.match = true;
                as.startCode = pos + 3 - bb.position();
                break;
            }
            pos++;
        }
    }

    private byte[] buildNaluHeader(int length) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(length);
        return buffer.array();
    }


    private boolean isSps(byte[] frame) {
        if (frame.length < 1) {
            return false;
        }
        // 5bits, 7.3.1 NAL unit syntax,
        // H.264-AVC-ISO_IEC_14496-10.pdf, page 44.
        //  7: SPS, 8: PPS, 5: I Frame, 1: P Frame
        int nal_unit_type = (frame[0] & 0x1f);
        return nal_unit_type == SPS;
    }

    private boolean isPps(byte[] frame) {
        if (frame.length < 1) {
            return false;
        }
        // 5bits, 7.3.1 NAL unit syntax,
        // H.264-AVC-ISO_IEC_14496-10.pdf, page 44.
        //  7: SPS, 8: PPS, 5: I Frame, 1: P Frame
        int nal_unit_type = (frame[0] & 0x1f);
        return nal_unit_type == PPS;
    }

    private boolean isKeyFrame(byte[] frame) {
        if (frame.length < 1) {
            return false;
        }
        // 5bits, 7.3.1 NAL unit syntax,
        // H.264-AVC-ISO_IEC_14496-10.pdf, page 44.
        //  7: SPS, 8: PPS, 5: I Frame, 1: P Frame
        int nal_unit_type = (frame[0] & 0x1f);
        return nal_unit_type == IDR;
    }

    private static boolean isAccessUnitDelimiter(byte[] frame) {
        if (frame.length < 1) {
            return false;
        }
        // 5bits, 7.3.1 NAL unit syntax,
        // H.264-AVC-ISO_IEC_14496-10.pdf, page 44.
        //  7: SPS, 8: PPS, 5: I Frame, 1: P Frame
        int nal_unit_type = (frame[0] & 0x1f);
        return nal_unit_type == AccessUnitDelimiter;
    }
}
