package com.paperpig.maimaidata.network.vpn.dns;

import com.paperpig.maimaidata.network.vpn.tcpip.CommonMethods;

import java.nio.ByteBuffer;

public class DnsHeader {
    static final short offset_ID = 0;
    static final short offset_ResourceCount = 6;
    static final short offset_AResourceCount = 8;
    static final short offset_EResourceCount = 10;
    public short ID;
    public DnsFlags Flags;
    public short QuestionCount;
    public short ResourceCount;
    public short AResourceCount;
    public short EResourceCount;
    public byte[] Data;
    public int Offset;

    public DnsHeader(byte[] data, int offset) {
        this.Offset = offset;
        this.Data = data;
    }

    public static DnsHeader FromBytes(ByteBuffer buffer) {
        DnsHeader header = new DnsHeader(buffer.array(), buffer.arrayOffset() + buffer.position());
        header.ID = buffer.getShort();
        header.Flags = DnsFlags.Parse(buffer.getShort());
        header.QuestionCount = buffer.getShort();
        header.ResourceCount = buffer.getShort();
        header.AResourceCount = buffer.getShort();
        header.EResourceCount = buffer.getShort();
        return header;
    }

    public void setID(short value) {
        CommonMethods.writeShort(Data, Offset + offset_ID, value);
    }

    public void setResourceCount(short value) {
        CommonMethods.writeShort(Data, Offset + offset_ResourceCount, value);
    }

    public void setAResourceCount(short value) {
        CommonMethods.writeShort(Data, Offset + offset_AResourceCount, value);
    }

    public void setEResourceCount(short value) {
        CommonMethods.writeShort(Data, Offset + offset_EResourceCount, value);
    }
}
