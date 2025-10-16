package com.paperpig.maimaidata.network.vpn.dns;

public class DnsFlags {
    public boolean QR; // 1 bits
    public int OpCode; // 4 bits
    public boolean AA; // 1 bits
    public boolean TC; // 1 bits
    public boolean RD; // 1 bits
    public boolean RA; // 1 bits
    public int Zero; // 3 bits
    public int Rcode; // 4 bits

    public static DnsFlags Parse(short value) {
        int m_Flags = value & 0xFFFF;
        DnsFlags flags = new DnsFlags();
        flags.QR = ((m_Flags >> 7) & 0x01) == 1;
        flags.OpCode = (m_Flags >> 3) & 0x0F;
        flags.AA = ((m_Flags >> 2) & 0x01) == 1;
        flags.TC = ((m_Flags >> 1) & 0x01) == 1;
        flags.RD = (m_Flags & 0x01) == 1;
        flags.RA = (m_Flags >> 15) == 1;
        flags.Zero = (m_Flags >> 12) & 0x07;
        flags.Rcode = ((m_Flags >> 8) & 0xF);
        return flags;
    }
}
