package com.paperpig.maimaidata.network.vpn.dns;

import com.paperpig.maimaidata.network.vpn.tcpip.CommonMethods;

public class ResourcePointer {
    static final short offset_Domain = 0;
    static final short offset_Type = 2;
    static final short offset_Class = 4;
    static final int offset_TTL = 6;
    static final short offset_DataLength = 10;
    static final int offset_IP = 12;

    byte[] Data;
    int Offset;

    public ResourcePointer(byte[] data, int offset) {
        this.Data = data;
        this.Offset = offset;
    }

    public void setDomain(short value) {
        CommonMethods.writeShort(Data, Offset + offset_Domain, value);
    }

    public short getType() {
        return CommonMethods.readShort(Data, Offset + offset_Type);
    }

    public void setType(short value) {
        CommonMethods.writeShort(Data, Offset + offset_Type, value);
    }

    public void setClass(short value) {
        CommonMethods.writeShort(Data, Offset + offset_Class, value);
    }

    public void setTTL(int value) {
        CommonMethods.writeInt(Data, Offset + offset_TTL, value);
    }

    public void setDataLength(short value) {
        CommonMethods.writeShort(Data, Offset + offset_DataLength, value);
    }

    public void setIP(int value) {
        CommonMethods.writeInt(Data, Offset + offset_IP, value);
    }
}
