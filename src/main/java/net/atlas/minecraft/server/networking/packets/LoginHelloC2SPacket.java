package net.atlas.minecraft.server.networking.packets;

import net.atlas.minecraft.common.networking.packet.Packet;
import net.atlas.minecraft.server.networking.ServerPacketListener;
import net.atlas.minecraft.server.networking.util.PacketByteBuf;

public class LoginHelloC2SPacket implements Packet<ServerPacketListener> {

    public String msg;

    public LoginHelloC2SPacket(PacketByteBuf packetByteBuf) {
        msg = packetByteBuf.readString();
    }

    @Override
    public void apply(ServerPacketListener listener) {
        listener.onHello(this);
    }

    @Override
    public void write(PacketByteBuf byteBuffer) {
    }
}
