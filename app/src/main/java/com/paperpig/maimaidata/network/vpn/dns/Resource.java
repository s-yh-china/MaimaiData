package com.paperpig.maimaidata.network.vpn.dns;

import java.nio.ByteBuffer;

public class Resource {
    public String Domain;
    public short Type;
    public short Class;
    public int TTL;
    public short DataLength;
    public byte[] Data;

    public static Resource FromBytes(ByteBuffer buffer) {
        Resource r = new Resource();
        r.Domain = DnsPacket.ReadDomain(buffer, buffer.arrayOffset());
        r.Type = buffer.getShort();
        r.Class = buffer.getShort();
        r.TTL = buffer.getInt();
        r.DataLength = buffer.getShort();
        r.Data = new byte[r.DataLength & 0xFFFF];
        buffer.get(r.Data);
        return r;
    }
}
