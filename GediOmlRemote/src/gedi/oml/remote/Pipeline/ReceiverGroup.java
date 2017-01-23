package gedi.oml.remote.Pipeline;

import gedi.core.data.mapper.GenomicRegionDataMapper;
import gedi.core.data.mapper.GenomicRegionDataMappingJob;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.oml.OmlNode;
import gedi.util.gui.PixelBasepairMapper;
import gedi.util.job.ExecutionContext;
import gedi.util.job.Transition;
import gedi.util.job.schedule.PetriNetEvent;
import gedi.util.job.schedule.PetriNetListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReceiverGroup extends ChannelInboundHandlerAdapter implements Consumer<SocketChannel>, PetriNetListener {
	private static final Logger log = Logger.getLogger( ReceiverGroup.class.getName() );

	private Receiver[] receivers;
	private OmlNode remoteRoot;
	private ChannelHandlerContext ctx;
//	private LinkedBlockingQueue<ChannelHandlerContext> ctxgetter = new LinkedBlockingQueue<ChannelHandlerContext>(1);

	private MutableReferenceGenomicRegion<PixelBasepairMapper> location = new MutableReferenceGenomicRegion<PixelBasepairMapper>();
	
	public ReceiverGroup(OmlNode remoteRoot, Collection<Receiver> receivers) {
		this.remoteRoot = remoteRoot;
		this.receivers = receivers.toArray(new Receiver[0]);
	}

	@Override
	public void accept(SocketChannel ch) {
		ChannelPipeline pl = ch.pipeline();
		pl.addLast(this);
		for (Receiver r : receivers)
			pl.addLast(r);
	}
	
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		
		ctx.writeAndFlush(remoteRoot.toOml());
		synchronized (this) {
			this.ctx = ctx;
			for (Receiver r : receivers)
				r.setDisabled(false);
		}
//		ctxgetter.offer(ctx);
		super.channelActive(ctx);
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		synchronized (this) {
			this.ctx = null;
			for (Receiver r : receivers)
				r.setDisabled(true);
		}
		super.channelInactive(ctx);
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		
		RemotePipelineData data = (RemotePipelineData) msg;
		
		boolean release = false;
		try {
			synchronized (location) {
				boolean answerCorrect = data.getData().getReference().equals(location.getReference())  && data.getData().getRegion().equals(location.getRegion());
				if (answerCorrect)
					ctx.fireChannelRead(msg);
				else release = true;
					// noone is interested in this answer anymore (i.e. the corresponding thread has been interrupted already)
			}
		} finally {
			if (release)
				 ReferenceCountUtil.release(msg);
		}
		
	}
	
	@Override
	public void petriNetExecutionStarted(PetriNetEvent event) {
		ExecutionContext context = event.getContext();
		
		synchronized (this) {
			if (ctx==null)
//				try {
//					ctx = ctxgetter.take();
//				} catch (InterruptedException e) {
//					return;
//				}	
				return;
		}
		
		// first write disabled receiver ids
		StringBuilder sb = new StringBuilder();
		Set<Transition> dis = context.getDisabledTransitions();
		for (Transition t : context.getPetrNet().getTransitions()) {
			GenomicRegionDataMapper mapper = ((GenomicRegionDataMappingJob)t.getJob()).getMapper();
			if (mapper instanceof Receiver && !dis.contains(t)) {
				Receiver r = (Receiver) mapper;
				if (sb.length()>0) sb.append(",");
				sb.append(r.getId());
			}
		}
		ctx.write("E:"+sb.toString());
		
		// and then the location
		synchronized (location) {
			location.set(
					context.getContext(GenomicRegionDataMappingJob.REFERENCE),
					context.getContext(GenomicRegionDataMappingJob.REGION),
					((PixelLocationMapping) context.getContext(GenomicRegionDataMappingJob.PIXELMAPPING)).getXmapper()
					);
		}
		ctx.writeAndFlush(new RemotePipelineData<PixelBasepairMapper>(context.getContext(ExecutionContext.UID), location));


	}

	@Override
	public void petriNetExecutionFinished(PetriNetEvent event) {
		
	}

	@Override
	public void petriNetExecutionCancelled(PetriNetEvent event) {
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		log.log(Level.SEVERE, "ReceiverGroup caught exception!", cause);
	}
	
}
