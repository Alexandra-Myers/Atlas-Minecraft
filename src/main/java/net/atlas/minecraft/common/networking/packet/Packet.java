package net.atlas.minecraft.common.networking.packet;

import net.atlas.minecraft.common.networking.listener.PacketListener;
import net.atlas.minecraft.server.networking.util.PacketByteBuf;

import java.nio.ByteBuffer;

public interface Packet<T extends PacketListener> {

    void apply(T listener);

    void write(PacketByteBuf byteBuffer);
}
