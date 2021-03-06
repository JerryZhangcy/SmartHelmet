// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: MessageId.proto

package com.cy.helmet.core.protocol;

public final class MessageId {
  private MessageId() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public enum MsgId
      implements com.google.protobuf.ProtocolMessageEnum {
    H2S_MessageId_Start(0, 100000),
    H2S_MessageId_Register(1, 100001),
    H2S_MessageId_Heart_Beat(2, 100002),
    H2S_MessageId_Take_Photo(3, 100003),
    H2S_MessageId_Send_Voice(4, 100004),
    H2S_MessageId_Req_Talk(5, 100005),
    H2S_MessageId_Sos(6, 100007),
    H2S_MessageId_List_VedioFile_Resp(7, 100008),
    H2S_MessageId_List_PhotoFile_Resp(8, 100009),
    H2S_MessageId_Get_File_Resp(9, 100010),
    H2S_MessageId_Play_Voice_Resp(10, 100011),
    H2S_MessageId_Get_DeviceInfo_Resp(11, 100012),
    H2S_MessageId_Update_Software_Resp(12, 100013),
    H2S_MessageId_Version_Check(13, 100014),
    H2S_MessageId_Start_Video_Resp(14, 100015),
    H2S_MessageId_Start_Video(15, 100016),
    H2S_MessageId_Start_Video_Live_Resp(16, 100017),
    H2S_MessageId_Stop_Video(17, 100018),
    H2S_MessageId_Stop_Video_Resp(18, 100019),
    H2S_MessageId_Del_File_Resp(19, 100020),
    H2S_MessageId_Fall_Detection(20, 100021),
    H2S_MessageId_Take_Photo_Resp(21, 100022),
    H2S_MessageId_update_status(22, 100023),
    H2S_MessageId_HelmetCfg_Resp(23, 100024),
    H2S_MessageId_Stop_Video_Live_Resp(24, 100025),
    H2S_MessageId_NetTypeChoose_Resp(25, 100026),
    H2S_MessageId_Refresh_Helmet_Voice(26, 100027),
    H2S_MessageId_End(27, 199999),
    S2H_MessageId_Start(28, 200000),
    S2H_MessageId_Register_Resp(29, 200001),
    S2H_MessageId_Update_Software(30, 200007),
    S2H_MessageId_Send_Voice(31, 200008),
    S2H_MessageId_Take_Photo(32, 200009),
    S2H_MessageId_List_VedioFile(33, 200010),
    S2H_MessageId_List_PhotoFile(34, 200011),
    S2H_MessageId_Del_File(35, 200012),
    S2H_MessageId_Get_File(36, 200013),
    S2H_MessageId_Heart_Beat_Resp(37, 200014),
    S2H_MessageId_Req_Talk_Resp(38, 200015),
    S2H_MessageId_Start_Video(39, 200017),
    S2H_MessageId_Unknow_Device(40, 200018),
    S2H_MessageId_Version_Check_Resp(41, 200019),
    S2H_MessageId_Send_Voice_Resp(42, 200020),
    S2H_MessageId_Start_Video_Live(43, 200021),
    S2H_MessageId_Stop_Video(44, 200022),
    S2H_MessageId_Device_Cfg(45, 200023),
    S2H_MessageId_Response_Fall(46, 200024),
    S2H_MessageId_update_status_Resp(47, 200025),
    S2H_MessageId_Stop_Video_Live(48, 200026),
    S2H_MessageId_Sos_Resp(49, 200027),
    S2H_MessageId_Fall_Detection_Resp(50, 200028),
    S2H_MessageId_NetTypeChoose(51, 200029),
    S2H_MessageId_End(52, 299999),
    ;
    
    public static final int H2S_MessageId_Start_VALUE = 100000;
    public static final int H2S_MessageId_Register_VALUE = 100001;
    public static final int H2S_MessageId_Heart_Beat_VALUE = 100002;
    public static final int H2S_MessageId_Take_Photo_VALUE = 100003;
    public static final int H2S_MessageId_Send_Voice_VALUE = 100004;
    public static final int H2S_MessageId_Req_Talk_VALUE = 100005;
    public static final int H2S_MessageId_Sos_VALUE = 100007;
    public static final int H2S_MessageId_List_VedioFile_Resp_VALUE = 100008;
    public static final int H2S_MessageId_List_PhotoFile_Resp_VALUE = 100009;
    public static final int H2S_MessageId_Get_File_Resp_VALUE = 100010;
    public static final int H2S_MessageId_Play_Voice_Resp_VALUE = 100011;
    public static final int H2S_MessageId_Get_DeviceInfo_Resp_VALUE = 100012;
    public static final int H2S_MessageId_Update_Software_Resp_VALUE = 100013;
    public static final int H2S_MessageId_Version_Check_VALUE = 100014;
    public static final int H2S_MessageId_Start_Video_Resp_VALUE = 100015;
    public static final int H2S_MessageId_Start_Video_VALUE = 100016;
    public static final int H2S_MessageId_Start_Video_Live_Resp_VALUE = 100017;
    public static final int H2S_MessageId_Stop_Video_VALUE = 100018;
    public static final int H2S_MessageId_Stop_Video_Resp_VALUE = 100019;
    public static final int H2S_MessageId_Del_File_Resp_VALUE = 100020;
    public static final int H2S_MessageId_Fall_Detection_VALUE = 100021;
    public static final int H2S_MessageId_Take_Photo_Resp_VALUE = 100022;
    public static final int H2S_MessageId_update_status_VALUE = 100023;
    public static final int H2S_MessageId_HelmetCfg_Resp_VALUE = 100024;
    public static final int H2S_MessageId_Stop_Video_Live_Resp_VALUE = 100025;
    public static final int H2S_MessageId_NetTypeChoose_Resp_VALUE = 100026;
    public static final int H2S_MessageId_Refresh_Helmet_Voice_VALUE = 100027;
    public static final int H2S_MessageId_End_VALUE = 199999;
    public static final int S2H_MessageId_Start_VALUE = 200000;
    public static final int S2H_MessageId_Register_Resp_VALUE = 200001;
    public static final int S2H_MessageId_Update_Software_VALUE = 200007;
    public static final int S2H_MessageId_Send_Voice_VALUE = 200008;
    public static final int S2H_MessageId_Take_Photo_VALUE = 200009;
    public static final int S2H_MessageId_List_VedioFile_VALUE = 200010;
    public static final int S2H_MessageId_List_PhotoFile_VALUE = 200011;
    public static final int S2H_MessageId_Del_File_VALUE = 200012;
    public static final int S2H_MessageId_Get_File_VALUE = 200013;
    public static final int S2H_MessageId_Heart_Beat_Resp_VALUE = 200014;
    public static final int S2H_MessageId_Req_Talk_Resp_VALUE = 200015;
    public static final int S2H_MessageId_Start_Video_VALUE = 200017;
    public static final int S2H_MessageId_Unknow_Device_VALUE = 200018;
    public static final int S2H_MessageId_Version_Check_Resp_VALUE = 200019;
    public static final int S2H_MessageId_Send_Voice_Resp_VALUE = 200020;
    public static final int S2H_MessageId_Start_Video_Live_VALUE = 200021;
    public static final int S2H_MessageId_Stop_Video_VALUE = 200022;
    public static final int S2H_MessageId_Device_Cfg_VALUE = 200023;
    public static final int S2H_MessageId_Response_Fall_VALUE = 200024;
    public static final int S2H_MessageId_update_status_Resp_VALUE = 200025;
    public static final int S2H_MessageId_Stop_Video_Live_VALUE = 200026;
    public static final int S2H_MessageId_Sos_Resp_VALUE = 200027;
    public static final int S2H_MessageId_Fall_Detection_Resp_VALUE = 200028;
    public static final int S2H_MessageId_NetTypeChoose_VALUE = 200029;
    public static final int S2H_MessageId_End_VALUE = 299999;
    
    
    public final int getNumber() { return value; }
    
    public static MsgId valueOf(int value) {
      switch (value) {
        case 100000: return H2S_MessageId_Start;
        case 100001: return H2S_MessageId_Register;
        case 100002: return H2S_MessageId_Heart_Beat;
        case 100003: return H2S_MessageId_Take_Photo;
        case 100004: return H2S_MessageId_Send_Voice;
        case 100005: return H2S_MessageId_Req_Talk;
        case 100007: return H2S_MessageId_Sos;
        case 100008: return H2S_MessageId_List_VedioFile_Resp;
        case 100009: return H2S_MessageId_List_PhotoFile_Resp;
        case 100010: return H2S_MessageId_Get_File_Resp;
        case 100011: return H2S_MessageId_Play_Voice_Resp;
        case 100012: return H2S_MessageId_Get_DeviceInfo_Resp;
        case 100013: return H2S_MessageId_Update_Software_Resp;
        case 100014: return H2S_MessageId_Version_Check;
        case 100015: return H2S_MessageId_Start_Video_Resp;
        case 100016: return H2S_MessageId_Start_Video;
        case 100017: return H2S_MessageId_Start_Video_Live_Resp;
        case 100018: return H2S_MessageId_Stop_Video;
        case 100019: return H2S_MessageId_Stop_Video_Resp;
        case 100020: return H2S_MessageId_Del_File_Resp;
        case 100021: return H2S_MessageId_Fall_Detection;
        case 100022: return H2S_MessageId_Take_Photo_Resp;
        case 100023: return H2S_MessageId_update_status;
        case 100024: return H2S_MessageId_HelmetCfg_Resp;
        case 100025: return H2S_MessageId_Stop_Video_Live_Resp;
        case 100026: return H2S_MessageId_NetTypeChoose_Resp;
        case 100027: return H2S_MessageId_Refresh_Helmet_Voice;
        case 199999: return H2S_MessageId_End;
        case 200000: return S2H_MessageId_Start;
        case 200001: return S2H_MessageId_Register_Resp;
        case 200007: return S2H_MessageId_Update_Software;
        case 200008: return S2H_MessageId_Send_Voice;
        case 200009: return S2H_MessageId_Take_Photo;
        case 200010: return S2H_MessageId_List_VedioFile;
        case 200011: return S2H_MessageId_List_PhotoFile;
        case 200012: return S2H_MessageId_Del_File;
        case 200013: return S2H_MessageId_Get_File;
        case 200014: return S2H_MessageId_Heart_Beat_Resp;
        case 200015: return S2H_MessageId_Req_Talk_Resp;
        case 200017: return S2H_MessageId_Start_Video;
        case 200018: return S2H_MessageId_Unknow_Device;
        case 200019: return S2H_MessageId_Version_Check_Resp;
        case 200020: return S2H_MessageId_Send_Voice_Resp;
        case 200021: return S2H_MessageId_Start_Video_Live;
        case 200022: return S2H_MessageId_Stop_Video;
        case 200023: return S2H_MessageId_Device_Cfg;
        case 200024: return S2H_MessageId_Response_Fall;
        case 200025: return S2H_MessageId_update_status_Resp;
        case 200026: return S2H_MessageId_Stop_Video_Live;
        case 200027: return S2H_MessageId_Sos_Resp;
        case 200028: return S2H_MessageId_Fall_Detection_Resp;
        case 200029: return S2H_MessageId_NetTypeChoose;
        case 299999: return S2H_MessageId_End;
        default: return null;
      }
    }
    
    public static com.google.protobuf.Internal.EnumLiteMap<MsgId>
        internalGetValueMap() {
      return internalValueMap;
    }
    private static com.google.protobuf.Internal.EnumLiteMap<MsgId>
        internalValueMap =
          new com.google.protobuf.Internal.EnumLiteMap<MsgId>() {
            public MsgId findValueByNumber(int number) {
              return MsgId.valueOf(number);
            }
          };
    
    public final com.google.protobuf.Descriptors.EnumValueDescriptor
        getValueDescriptor() {
      return getDescriptor().getValues().get(index);
    }
    public final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptorForType() {
      return getDescriptor();
    }
    public static final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptor() {
      return com.cy.helmet.core.protocol.MessageId.getDescriptor().getEnumTypes().get(0);
    }
    
    private static final MsgId[] VALUES = {
      H2S_MessageId_Start, H2S_MessageId_Register, H2S_MessageId_Heart_Beat, H2S_MessageId_Take_Photo, H2S_MessageId_Send_Voice, H2S_MessageId_Req_Talk, H2S_MessageId_Sos, H2S_MessageId_List_VedioFile_Resp, H2S_MessageId_List_PhotoFile_Resp, H2S_MessageId_Get_File_Resp, H2S_MessageId_Play_Voice_Resp, H2S_MessageId_Get_DeviceInfo_Resp, H2S_MessageId_Update_Software_Resp, H2S_MessageId_Version_Check, H2S_MessageId_Start_Video_Resp, H2S_MessageId_Start_Video, H2S_MessageId_Start_Video_Live_Resp, H2S_MessageId_Stop_Video, H2S_MessageId_Stop_Video_Resp, H2S_MessageId_Del_File_Resp, H2S_MessageId_Fall_Detection, H2S_MessageId_Take_Photo_Resp, H2S_MessageId_update_status, H2S_MessageId_HelmetCfg_Resp, H2S_MessageId_Stop_Video_Live_Resp, H2S_MessageId_NetTypeChoose_Resp, H2S_MessageId_Refresh_Helmet_Voice, H2S_MessageId_End, S2H_MessageId_Start, S2H_MessageId_Register_Resp, S2H_MessageId_Update_Software, S2H_MessageId_Send_Voice, S2H_MessageId_Take_Photo, S2H_MessageId_List_VedioFile, S2H_MessageId_List_PhotoFile, S2H_MessageId_Del_File, S2H_MessageId_Get_File, S2H_MessageId_Heart_Beat_Resp, S2H_MessageId_Req_Talk_Resp, S2H_MessageId_Start_Video, S2H_MessageId_Unknow_Device, S2H_MessageId_Version_Check_Resp, S2H_MessageId_Send_Voice_Resp, S2H_MessageId_Start_Video_Live, S2H_MessageId_Stop_Video, S2H_MessageId_Device_Cfg, S2H_MessageId_Response_Fall, S2H_MessageId_update_status_Resp, S2H_MessageId_Stop_Video_Live, S2H_MessageId_Sos_Resp, S2H_MessageId_Fall_Detection_Resp, S2H_MessageId_NetTypeChoose, S2H_MessageId_End, 
    };
    
    public static MsgId valueOf(
        com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
      if (desc.getType() != getDescriptor()) {
        throw new java.lang.IllegalArgumentException(
          "EnumValueDescriptor is not for this type.");
      }
      return VALUES[desc.getIndex()];
    }
    
    private final int index;
    private final int value;
    
    private MsgId(int index, int value) {
      this.index = index;
      this.value = value;
    }
    
    // @@protoc_insertion_point(enum_scope:com.cy.helmet.core.protocol.MsgId)
  }
  
  
  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\017MessageId.proto\022\033com.cy.helmet.core.pr" +
      "otocol*\277\016\n\005MsgId\022\031\n\023H2S_MessageId_Start\020" +
      "\240\215\006\022\034\n\026H2S_MessageId_Register\020\241\215\006\022\036\n\030H2S" +
      "_MessageId_Heart_Beat\020\242\215\006\022\036\n\030H2S_Message" +
      "Id_Take_Photo\020\243\215\006\022\036\n\030H2S_MessageId_Send_" +
      "Voice\020\244\215\006\022\034\n\026H2S_MessageId_Req_Talk\020\245\215\006\022" +
      "\027\n\021H2S_MessageId_Sos\020\247\215\006\022\'\n!H2S_MessageI" +
      "d_List_VedioFile_Resp\020\250\215\006\022\'\n!H2S_Message" +
      "Id_List_PhotoFile_Resp\020\251\215\006\022!\n\033H2S_Messag" +
      "eId_Get_File_Resp\020\252\215\006\022#\n\035H2S_MessageId_P",
      "lay_Voice_Resp\020\253\215\006\022\'\n!H2S_MessageId_Get_" +
      "DeviceInfo_Resp\020\254\215\006\022(\n\"H2S_MessageId_Upd" +
      "ate_Software_Resp\020\255\215\006\022!\n\033H2S_MessageId_V" +
      "ersion_Check\020\256\215\006\022$\n\036H2S_MessageId_Start_" +
      "Video_Resp\020\257\215\006\022\037\n\031H2S_MessageId_Start_Vi" +
      "deo\020\260\215\006\022)\n#H2S_MessageId_Start_Video_Liv" +
      "e_Resp\020\261\215\006\022\036\n\030H2S_MessageId_Stop_Video\020\262" +
      "\215\006\022#\n\035H2S_MessageId_Stop_Video_Resp\020\263\215\006\022" +
      "!\n\033H2S_MessageId_Del_File_Resp\020\264\215\006\022\"\n\034H2" +
      "S_MessageId_Fall_Detection\020\265\215\006\022#\n\035H2S_Me",
      "ssageId_Take_Photo_Resp\020\266\215\006\022!\n\033H2S_Messa" +
      "geId_update_status\020\267\215\006\022\"\n\034H2S_MessageId_" +
      "HelmetCfg_Resp\020\270\215\006\022(\n\"H2S_MessageId_Stop" +
      "_Video_Live_Resp\020\271\215\006\022&\n H2S_MessageId_Ne" +
      "tTypeChoose_Resp\020\272\215\006\022(\n\"H2S_MessageId_Re" +
      "fresh_Helmet_Voice\020\273\215\006\022\027\n\021H2S_MessageId_" +
      "End\020\277\232\014\022\031\n\023S2H_MessageId_Start\020\300\232\014\022!\n\033S2" +
      "H_MessageId_Register_Resp\020\301\232\014\022#\n\035S2H_Mes" +
      "sageId_Update_Software\020\307\232\014\022\036\n\030S2H_Messag" +
      "eId_Send_Voice\020\310\232\014\022\036\n\030S2H_MessageId_Take",
      "_Photo\020\311\232\014\022\"\n\034S2H_MessageId_List_VedioFi" +
      "le\020\312\232\014\022\"\n\034S2H_MessageId_List_PhotoFile\020\313" +
      "\232\014\022\034\n\026S2H_MessageId_Del_File\020\314\232\014\022\034\n\026S2H_" +
      "MessageId_Get_File\020\315\232\014\022#\n\035S2H_MessageId_" +
      "Heart_Beat_Resp\020\316\232\014\022!\n\033S2H_MessageId_Req" +
      "_Talk_Resp\020\317\232\014\022\037\n\031S2H_MessageId_Start_Vi" +
      "deo\020\321\232\014\022!\n\033S2H_MessageId_Unknow_Device\020\322" +
      "\232\014\022&\n S2H_MessageId_Version_Check_Resp\020\323" +
      "\232\014\022#\n\035S2H_MessageId_Send_Voice_Resp\020\324\232\014\022" +
      "$\n\036S2H_MessageId_Start_Video_Live\020\325\232\014\022\036\n",
      "\030S2H_MessageId_Stop_Video\020\326\232\014\022\036\n\030S2H_Mes" +
      "sageId_Device_Cfg\020\327\232\014\022!\n\033S2H_MessageId_R" +
      "esponse_Fall\020\330\232\014\022&\n S2H_MessageId_update" +
      "_status_Resp\020\331\232\014\022#\n\035S2H_MessageId_Stop_V" +
      "ideo_Live\020\332\232\014\022\034\n\026S2H_MessageId_Sos_Resp\020" +
      "\333\232\014\022\'\n!S2H_MessageId_Fall_Detection_Resp" +
      "\020\334\232\014\022!\n\033S2H_MessageId_NetTypeChoose\020\335\232\014\022" +
      "\027\n\021S2H_MessageId_End\020\337\247\022"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
      new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
        public com.google.protobuf.ExtensionRegistry assignDescriptors(
            com.google.protobuf.Descriptors.FileDescriptor root) {
          descriptor = root;
          return null;
        }
      };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
  }
  
  // @@protoc_insertion_point(outer_class_scope)
}
