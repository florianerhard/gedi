/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */

package gedi.oml.remote;

import gedi.app.Gedi;
import gedi.gui.genovis.TracksDataManager;
import gedi.oml.remote.Pipeline.RemotePipelineData;
import gedi.oml.remote.Pipeline.Sender;
import gedi.oml.remote.Pipeline.TrackProtocol;
import gedi.remote.Protocol;
import gedi.remote.RemoteConnections;
import gedi.util.StringUtils;
import gedi.util.functions.EI;
import gedi.util.gui.PixelBasepairMapper;
import gedi.util.oml.OmlNodeExecutor;
import gedi.util.oml.OmlReader;
import gedi.util.oml.petrinet.Pipeline;
import gedi.util.policy.GediPolicy;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;

import java.io.FilePermission;
import java.io.IOException;
import java.lang.reflect.ReflectPermission;
import java.net.SocketPermission;
import java.util.HashMap;
import java.util.PropertyPermission;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TracksServer extends ChannelInboundHandlerAdapter {

	private static final Logger log = Logger.getLogger( TracksServer.class.getName() );

	
	public static void main(String[] args) throws IOException {
		Gedi.startup(true);
		
		InternalLoggerFactory.setDefaultFactory(new JdkLoggerFactory());
		
		String[] files = EI.wrap(args).skip(1).toArray(String.class);
		
		new GediPolicy()
			.addDefaults()
			.addPermission(new RuntimePermission("accessClassInPackage.sun.misc"))
			.addPermission(new RuntimePermission("accessDeclaredMembers"))
			.addPermission(new ReflectPermission("suppressAccessChecks"))
			.addPermission(new SocketPermission("localhost", "listen,resolve"))
			.addPermission(new PropertyPermission("*", "read"))
			.addPermission(new SocketPermission("*", "resolve,accept"))
			.addAllPermissions(Bootstrap.class, TracksServer.class, Protocol.class)
			.addFileReadPermissions(files)
			.addPermission(new FilePermission("/proc/sys/net/core/somaxconn","read"))
		.setup();
		
		RemoteConnections.getInstance().serveSync(new TrackProtocol(), ch->{
			ChannelPipeline pl = ch.pipeline();
			pl.addLast(new TracksServer());
		});
		
	}


	private TracksDataManager dataManager;
	
	private HashMap<String,Sender> idToSender = new HashMap<String, Sender>();
	
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		boolean release = false;
		
		try {
			
			if (msg instanceof String) {
				if (((String) msg).startsWith("E:")) {
					for (Sender s : idToSender.values())
						s.setDisabled(true);
					for (String disabled : StringUtils.split(((String) msg).substring(2), ','))
						idToSender.get(disabled).setDisabled(false);
					
				} else {
				
					// pipeline oml
					Pipeline pipeline = new OmlNodeExecutor().execute(new OmlReader().parse((String)msg));
					log.log(Level.INFO, pipeline.getPetriNet().getTransitions().size()+" transitions found!");
					dataManager = new TracksDataManager(pipeline.getPetriNet());
					for (Sender sender : pipeline.getObjects(Sender.class)) {
						sender.setChannelHandlerContext(ctx);
						idToSender.put(sender.getInput(), sender);
					}
					release = true;
				}
			}
			else if (msg instanceof RemotePipelineData) {
				RemotePipelineData data = (RemotePipelineData) msg;
				dataManager.setLocation((PixelBasepairMapper)data.getData().getData(), data.getData().getReference(), data.getData().getRegion(), (x)->{});
				release = true;
			}
			else {
				ctx.fireChannelRead(msg);
				release = false;
			}
			
			
        	
        } finally {
            if (release) {
                ReferenceCountUtil.release(msg);
            }
        }
	}
	
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		log.log(Level.SEVERE, "Server caught exception!", cause);
	}
	
}
