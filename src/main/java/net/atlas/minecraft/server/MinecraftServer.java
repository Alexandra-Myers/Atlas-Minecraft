package net.atlas.minecraft.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.atlas.minecraft.common.networking.util.ConnectionSide;
import net.atlas.minecraft.common.registry.Registries;
import net.atlas.minecraft.server.networking.ServerPacketListener;
import net.atlas.minecraft.server.networking.connection.ClientConnection;

public class MinecraftServer {
    private static int port = 35566;
    public static ThreadGroup ServerThreadGroup = new ThreadGroup("ServerThreadGroup");
    public static final Registries registries;

    static {
        try {
            registries = new Registries();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static void main(String[] args) {
        try{
            run();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void run(){
       new Thread(ServerThreadGroup, () -> {
           EventLoopGroup bossGroup = new NioEventLoopGroup();
           EventLoopGroup workerGroup = new NioEventLoopGroup();
           try {
               ServerBootstrap b = new ServerBootstrap();
               b.group(bossGroup, workerGroup)
                       .channel(NioServerSocketChannel.class)
                       .childHandler(new ChannelInitializer<SocketChannel>() {
                           @Override
                           public void initChannel(SocketChannel ch) {
                           }
                       }).option(ChannelOption.SO_BACKLOG, 128)
                       .childOption(ChannelOption.SO_KEEPALIVE, true)
                       .childHandler(new ChannelInitializer<>() {
                           @Override
                           protected void initChannel(Channel channel) {
                               try {
                                   channel.config().setOption(ChannelOption.TCP_NODELAY, true);
                               } catch (ChannelException var4) {
                               }
                               ClientConnection clientConnection =new ClientConnection(ConnectionSide.SERVERBOUND);
                               channel.pipeline().addLast("packet_handler", clientConnection);
                               clientConnection.setPacketListener(new ServerPacketListener(clientConnection));
                           }
                       });

               ChannelFuture f;

               try {
                   f = b.bind(port).sync();
                   f.channel().closeFuture().sync();
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
           } finally {
               workerGroup.shutdownGracefully();
               bossGroup.shutdownGracefully();
           }
       }).start();

       System.err.println("QA");
    }

}
