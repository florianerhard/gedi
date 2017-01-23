package gedi.remote;

import gedi.app.extension.ExtensionContext;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.Workarounds;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.URI;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RemoteConnections {

	private static final Logger log = Logger.getLogger( RemoteConnections.class.getName() );

	private static RemoteConnections instance;

	public static RemoteConnections getInstance() {
		if (instance==null) instance = new RemoteConnections();
		return instance;
	}

	private RemoteConnections(){

	}

	/**
	 * Connects to the given server; the mechanics is as follows:
	 * <br/>
	 * The initChannel consumer is supposed to register channel handlers (called upon channel.registered)
	 * <br/>
	 * Depending on the outcome of the Bootrap.connect method, either the errorHandler or the connectedHandler is called.
	 * <br/>
	 * If the connection is lost, the closedHandler is called.
	 * <br/>
	 * If you want to cancel the connection attempt, invoke cancel on the returned ChannelFuture. If you want to terminate the connection, use the
	 * channel object from the connectedHandler.
	 * <br />
	 * If the channel is unregistered, the added flag is reset for all handlers bound to the channel pipeline (important if you want to reuse
	 * the handlers added by the initChannel consumer).
	 * 
	 * @param uri
	 * @param initChannel
	 * @param errorHandler
	 * @param connectedHandler
	 * @param closedHandler
	 * @return
	 */
	public ChannelFuture connect(URI uri, Consumer<SocketChannel> initChannel, Consumer<Throwable> errorHandler, Consumer<SocketChannel> connectedHandler, Runnable closedHandler) {

		final Protocol protocol = ProtocolExtensionPoint.getInstance().get(ExtensionContext.emptyContext(),uri.getScheme());
		if (protocol==null) throw new RuntimeException("Protocol "+uri.getScheme()+" unknown!");


		EventLoopGroup group = new NioEventLoopGroup();
		Bootstrap b = new Bootstrap();
		b.group(group)
		.channel(NioSocketChannel.class)
		.handler(new ChannelInitializer<SocketChannel>() {
			
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ChannelPipeline pipeline = ch.pipeline();
				protocol.setCodecs(pipeline);
				initChannel.accept(ch);
				
				pipeline.addLast(new ChannelInboundHandlerAdapter() {
					
					@Override
					public void channelActive(ChannelHandlerContext ctx)
							throws Exception {
						pipeline.addLast(new ConfigLoggingHandler(ConfigLoggingHandler.LogLevel.INFO));
						connectedHandler.accept(ch);
						super.channelActive(ctx);
					}
					
					@Override
					public void channelInactive(ChannelHandlerContext ctx)
							throws Exception {
						super.channelInactive(ctx);
						closedHandler.run();
					}
					@Override
					public void channelUnregistered(ChannelHandlerContext ctx)
							throws Exception {
						ctx.pipeline().iterator().forEachRemaining((e)->Workarounds.removeAdded(e.getValue()));
						super.channelUnregistered(ctx);
					}
					
				});
			}
		});

		
		// Make a new connection and wait until closed.
		ChannelFuture f = b.connect(uri.getHost(), uri.getPort()==-1?protocol.getDefaultPort():uri.getPort())
				.addListener(new ChannelFutureListener() {
			
					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
							Throwable cause = future.cause();
							if (cause!=null) {
								log.log(Level.INFO, "Connection failed to server "+uri+": "+cause.getMessage());
								try {
									errorHandler.accept(cause);
								} finally {
									group.shutdownGracefully();
								}
							} else {
								log.log(Level.INFO, "Client connected to server "+uri);
								
								future.channel().closeFuture().addListener(new ChannelFutureListener() {
									
									@Override
									public void operationComplete(ChannelFuture future) throws Exception {
										log.log(Level.INFO, "Connection closed to server "+uri);
										group.shutdownGracefully();
									}
								});
								
							}
						
					}
				});
	
		return f;
	}
	

//	/**
//	 * Blocks!
//	 * @param url
//	 * @param handler
//	 * @return
//	 */
//	public void connectSync(URI uri, final Consumer<SocketChannel> initChannel) {
//
//		final Protocol protocol = ProtocolExtensionPoint.getInstance().getProtocol(uri.getScheme());
//		if (protocol==null) throw new RuntimeException("Protocol "+uri.getScheme()+" unknown!");
//
//
//		EventLoopGroup group = new NioEventLoopGroup();
//		try {
//			Bootstrap b = new Bootstrap();
//			b.group(group)
//			.channel(NioSocketChannel.class)
//			.handler(new ChannelInitializer<SocketChannel>() {
//
//				@Override
//				protected void initChannel(SocketChannel ch) throws Exception {
//					ChannelPipeline pipeline = ch.pipeline();
//					pipeline.addLast(new LoggingHandler(LogLevel.INFO));
//					protocol.setCodecs(pipeline);
//					initChannel.accept(ch);
//				}
//			});
//
//			
//			// Make a new connection and wait until closed.
//			ChannelFuture f = b.connect(uri.getHost(), uri.getPort()==-1?protocol.getDefaultPort():uri.getPort())
//					.addListener(new ChannelFutureListener() {
//				
//						@Override
//						public void operationComplete(ChannelFuture future) throws Exception {
//							System.out.println(future.cause());
//						}
//					})
//			.sync();
//			
//			log.log(Level.INFO, "Client connected to server "+uri);
//			f.channel().closeFuture().sync();
////		} catch (ConnectException e) {
//			
//		} catch (InterruptedException e) {
//			log.log(Level.SEVERE, "Client thread interrupted", e);
//		} finally {
//			group.shutdownGracefully();
//		}
//	}
//	
//	/**
//	 * Returns without blocking!
//	 * @param url
//	 * @param handler
//	 * @return
//	 */
//	public void connectAsync(final URI uri, final Consumer<SocketChannel> initChannel) {
//		new Thread() {
//			public void run() {
//				connectSync(uri, initChannel);
//			}
//			
//		}.start();
//	}
		
	
	public RetryRemoteConnection connectRetry(URI uri, final Consumer<SocketChannel> initChannel, long retryMillisec,  boolean killConnectionOnInterupt) {
		RetryRemoteConnection re = new RetryRemoteConnection(uri,initChannel,retryMillisec,killConnectionOnInterupt);
		new Thread(re).start();
		return re;
	}


	/**
	 * Returns without blocking!
	 * @param url
	 * @param handler
	 * @return
	 */
	public void serveSync(Protocol protocol, final Consumer<SocketChannel> initChannel) {


		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
			.channel(NioServerSocketChannel.class)
			.handler(new ConfigLoggingHandler(ConfigLoggingHandler.LogLevel.INFO))
			.childHandler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast(new ConfigLoggingHandler(ConfigLoggingHandler.LogLevel.INFO));
					protocol.setCodecs(pipeline);
					initChannel.accept(ch);
				}
			});

			ChannelFuture f = b.bind(protocol.getDefaultPort()).sync();
			log.log(Level.INFO, "Server thread started for protocol "+protocol+" on port "+protocol.getDefaultPort());
			
			f.channel().closeFuture().sync();
	
		} catch (InterruptedException e) {
			log.log(Level.SEVERE, "Server thread interrupted", e);
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}

	}


}
