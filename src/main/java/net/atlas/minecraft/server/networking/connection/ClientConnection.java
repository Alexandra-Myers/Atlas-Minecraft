package net.atlas.minecraft.server.networking.connection;

import io.netty.channel.*;
import net.atlas.minecraft.common.networking.listener.PacketListener;
import net.atlas.minecraft.common.networking.packet.Packet;
import net.atlas.minecraft.common.networking.util.ConnectionSide;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClientConnection extends SimpleChannelInboundHandler<Packet<?>> {

    public ConnectionSide side;

    public Channel channel;

    public PacketListener packetListener;

    public ClientConnection(ConnectionSide side) {
        this.side = side;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet<?> msg){
        if (this.channel.isOpen()) {
            handlePacket(msg,this.packetListener);
        }
    }

    private static <T extends PacketListener> void handlePacket(Packet<T> packet, PacketListener listener) {
        packet.apply((T)listener);
    }

    @Override
    public void channelActive(@NotNull ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.channel = ctx.channel();
    }

    public void setPacketListener(PacketListener listener) {
        this.packetListener = listener;
    }

    private void send(Packet<?> packet) {
        ChannelFuture channelFuture = this.channel.writeAndFlush(packet);
        channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }
}
