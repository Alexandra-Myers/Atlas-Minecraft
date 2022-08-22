package net.atlas.minecraft.common.networking.listener;


import net.atlas.minecraft.server.networking.connection.ClientConnection;

public interface PacketListener {

    void onDisconnected(String reason);

    ClientConnection getConnection();
}
