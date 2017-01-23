package gedi.remote;

import gedi.remote.codec.AsciiEncoder;
import gedi.remote.codec.DefaultDecoder;
import gedi.remote.codec.BinaryEncoder;
import gedi.remote.codec.NumberEncoder;
import io.netty.channel.ChannelPipeline;

public interface Protocol {

	String getName();
	void setCodecs(ChannelPipeline pipeline);
	int getDefaultPort();
	
	
	default void addDefaultCodecs(ChannelPipeline pipeline) {
		pipeline.addLast(new BinaryEncoder());
		pipeline.addLast(new NumberEncoder());
		pipeline.addLast(new AsciiEncoder());
		pipeline.addLast(new DefaultDecoder());
	}
	
}
