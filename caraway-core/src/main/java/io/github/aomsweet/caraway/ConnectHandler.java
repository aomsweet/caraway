package io.github.aomsweet.caraway;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.logging.InternalLogger;

import java.net.InetSocketAddress;

/**
 * @author aomsweet
 */
public abstract class ConnectHandler<Q> extends ChannelInboundHandlerAdapter {

    protected final InternalLogger logger;
    protected final CarawayServer caraway;

    public ConnectHandler(CarawayServer caraway, InternalLogger logger) {
        this.logger = logger;
        this.caraway = caraway;
    }

    protected void doConnectServer(ChannelHandlerContext ctx, Channel clientChannel, Q request) {
        try {
            InetSocketAddress serverAddress = getServerAddress(request);
            ServerConnector connector = caraway.getConnector();
            ChannelFuture channelFuture = connector.channel(serverAddress, ctx);
            channelFuture.addListener(future -> {
                if (future.isSuccess()) {
                    Channel serverChannel = channelFuture.channel();
                    if (clientChannel.isActive()) {
                        connected(ctx, clientChannel, serverChannel, request);
                    } else {
                        ChannelUtils.closeOnFlush(serverChannel);
                    }
                } else {
                    logger.error("Unable to establish a remote connection.", future.cause());
                    this.failConnect(ctx, clientChannel, request);
                }
            });
        } catch (ResolveServerAddressException e) {
            logger.error("Unable to get remote address.", e);
            release(ctx.channel(), null);
        }
    }

    protected abstract void connected(ChannelHandlerContext ctx, Channel clientChannel, Channel serverChannel, Q request);

    protected abstract void failConnect(ChannelHandlerContext ctx, Channel clientChannel, Q request);

    protected abstract InetSocketAddress getServerAddress(Q request) throws ResolveServerAddressException;

    public boolean relayDucking(Channel clientChannel, Channel serverChannel) {
        if (clientChannel.isActive()) {
            if (serverChannel.isActive()) {
                clientChannel.pipeline().addLast(new ClientRelayHandler(serverChannel));
                serverChannel.pipeline().addLast(new ServerRelayHandler(clientChannel));

                System.err.println("clientChannel channel: " + clientChannel.pipeline());
                System.err.println("serverChannel channel: " + serverChannel.pipeline());
                return true;
            } else {
                ChannelUtils.closeOnFlush(clientChannel);
            }
        } else {
            ChannelUtils.closeOnFlush(serverChannel);
        }
        return false;
    }

    public void release(Channel clientChannel, Channel serverChannel) {
        if (clientChannel != null) {
            ChannelUtils.closeOnFlush(clientChannel);
        }
        if (serverChannel != null) {
            ChannelUtils.closeOnFlush(serverChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        logger.error(cause.getMessage(), cause);
    }
}
