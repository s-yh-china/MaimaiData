package com.paperpig.maimaidata.network.vpn.dns;

import java.nio.ByteBuffer;

public class DnsPacket {
    public DnsHeader Header;
    public Question[] Questions;
    public Resource[] Resources;
    public Resource[] AResources;
    public Resource[] EResources;

    public int Size;

    public static DnsPacket FromBytes(ByteBuffer buffer) {
        if (buffer.limit() < 12) {
            return null;
        }
        if (buffer.limit() > 512) {
            return null;
        }

        DnsPacket packet = new DnsPacket();
        packet.Size = buffer.limit();
        packet.Header = DnsHeader.FromBytes(buffer);

        if (packet.Header.QuestionCount > 2 || packet.Header.ResourceCount > 50 || packet.Header.AResourceCount > 50 || packet.Header.EResourceCount > 50) {
            return null;
        }

        packet.Questions = new Question[packet.Header.QuestionCount];
        packet.Resources = new Resource[packet.Header.ResourceCount];
        packet.AResources = new Resource[packet.Header.AResourceCount];
        packet.EResources = new Resource[packet.Header.EResourceCount];

        for (int i = 0; i < packet.Questions.length; i++) {
            packet.Questions[i] = Question.FromBytes(buffer);
        }

        for (int i = 0; i < packet.Resources.length; i++) {
            packet.Resources[i] = Resource.FromBytes(buffer);
        }

        for (int i = 0; i < packet.AResources.length; i++) {
            packet.AResources[i] = Resource.FromBytes(buffer);
        }

        for (int i = 0; i < packet.EResources.length; i++) {
            packet.EResources[i] = Resource.FromBytes(buffer);
        }

        return packet;
    }

    public static String ReadDomain(ByteBuffer buffer, int dnsHeaderOffset) {
        StringBuilder sb = new StringBuilder();
        int len = 0;
        while (buffer.hasRemaining() && (len = (buffer.get() & 0xFF)) > 0) {
            if ((len & 0xc0) == 0xc0) {
                int pointer = buffer.get() & 0xFF;
                pointer |= (len & 0x3F) << 8;

                ByteBuffer newBuffer = ByteBuffer.wrap(buffer.array(), dnsHeaderOffset + pointer, dnsHeaderOffset + buffer.limit());
                sb.append(ReadDomain(newBuffer, dnsHeaderOffset));
                return sb.toString();
            } else {
                while (len > 0 && buffer.hasRemaining()) {
                    sb.append((char) (buffer.get() & 0xFF));
                    len--;
                }
                sb.append('.');
            }
        }

        if (len == 0 && sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);//ȥ��ĩβ�ĵ㣨.��
        }
        return sb.toString();
    }
}
