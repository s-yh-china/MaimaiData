package com.paperpig.maimaidata.network.vpn.dns;

import java.nio.ByteBuffer;

public class Question {
    public String Domain;
    public short Type;
    public short Class;

    private int offset;
    private int length;

    public static Question FromBytes(ByteBuffer buffer) {
        Question q = new Question();
        q.offset = buffer.arrayOffset() + buffer.position();
        q.Domain = DnsPacket.ReadDomain(buffer, buffer.arrayOffset());
        q.Type = buffer.getShort();
        q.Class = buffer.getShort();
        q.length = buffer.arrayOffset() + buffer.position() - q.offset;
        return q;
    }

    public int Offset() {
        return offset;
    }

    public int Length() {
        return length;
    }
}
