package net.atlas.minecraft.server.networking;

import net.atlas.minecraft.common.networking.listener.PacketListener;
import net.atlas.minecraft.server.networking.connection.ClientConnection;
import net.atlas.minecraft.server.networking.packets.LoginHelloC2SPacket;

public class ServerPacketListener implements PacketListener {

    public ClientConnection connection;

    public ServerPacketListener(ClientConnection connection) {
        this.connection = connection;
    }

    @Override
    public void onDisconnected(String reason) {
    }

    public void onHello(LoginHelloC2SPacket packet) {
        System.err.println(packet.msg);
    }

    @Override
    public ClientConnection getConnection() {
        return connection;
    }
}
