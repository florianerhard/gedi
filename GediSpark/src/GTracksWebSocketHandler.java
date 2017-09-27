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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import gedi.core.data.mapper.GenomicRegionDataMappingJob;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.gui.gtracks.GTrack;
import gedi.gui.gtracks.GTrack.HeightType;
import gedi.gui.gtracks.rendering.GTracksRenderContext;
import gedi.gui.gtracks.rendering.GTracksRenderRequest;
import gedi.gui.gtracks.rendering.GTracksRenderer;
import gedi.gui.gtracks.rendering.target.CompositeRenderTarget;
import gedi.gui.gtracks.rendering.target.DebugRenderTarget;
import gedi.gui.gtracks.rendering.target.PngRenderTarget;
import gedi.gui.gtracks.rendering.target.SvgRenderTarget;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.Consumer2;
import gedi.util.functions.EI;
import gedi.util.gui.PixelBasepairMapper;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.mutable.MutableMonad;
import gedi.util.orm.BinaryBlob;

@WebSocket
public class GTracksWebSocketHandler {

	private ConcurrentHashMap<Session, String> sessionData = new ConcurrentHashMap<>();
	private int spacer = 4;
	private GTracksServer server;
	
    public GTracksWebSocketHandler(GTracksServer server) {
    	this.server = server;
	}

	@OnWebSocketConnect
    public void onConnect(Session user) throws Exception {
		sessionData.put(user, "");
		DynamicObject header = DynamicObject.fromMap(
				"msg","info",
				"genome",DynamicObject.fromMap("map",server.getGenome(),"order",server.getGenome().keySet().toArray(new String[0]))
				);

		send(user,header);
		
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
    	error.printStackTrace();
    }
    
    @OnWebSocketClose
    public void onClose(Session user, int statusCode, String reason) {
    	sessionData.remove(user);
    	System.out.println(user.getRemoteAddress()+" closed: "+reason+ "("+statusCode+")");
    }

    @OnWebSocketMessage
    public void onMessage(Session user, String message) {
    	DynamicObject req = DynamicObject.parseJson(message);
    	
    	String loc = req.getEntry("location").asString();
    	sessionData.put(user, loc);
    	
    	ImmutableReferenceGenomicRegion<Object> rgr = ImmutableReferenceGenomicRegion.parse(loc);    	
    	double width = req.getEntry("width").asDouble();
    	PixelBasepairMapper xmapper = new PixelBasepairMapper().setIntronFixed(rgr.getReference(),false,rgr.getRegion(),spacer,width);
    	try {
			send(user,DynamicObject.fromMap("msg","viewinfo","ppbp",xmapper.getPixelsPerBasePair(),"location",req.getEntry("location").asString(), "location", loc));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
    	
    	server.getData().setLocation(xmapper,rgr.getReference(),rgr.getRegion(),null,(ctx,place)->{
    		if (!loc.equals(sessionData.get(user)))
    				return; // drop if new request already here!
    		
    		if (ctx.getToken(place) instanceof GTracksRenderer) {
	    		String id = place.getProducer().getJob().getId();
	    		
	    		GTracksRenderer ret = ctx.getToken(place);
//	    		SvgRenderTarget tar = new SvgRenderTarget();
	    		PngRenderTarget tar = new PngRenderTarget();
	    		
	    		GTracksRenderRequest result = ret.render(tar, new GTracksRenderContext(rgr.getReference(), rgr.getRegion(), xmapper));
	    		
	    		int height = (int)Math.ceil(result.getHeight());
	    		
	    		try {
	    			DynamicObject header = DynamicObject.fromMap(
	    					"msg","trackdata",
	    					"format", tar.getFormat(),
	    					"track",id,
	    					"height",height,
	    					"location", loc);
	    			send(user,header,out->tar.writeRaw(out, (int)Math.ceil(width), height));
	                
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}
    		
    	});

    }

    private void send(Session user, DynamicObject header) throws IOException {
    	send(user,header,null);
    }
    private synchronized void send(Session user, DynamicObject header, Consumer2<BinaryWriter> data) throws IOException {
    	BinaryBlob blob = new BinaryBlob();
		blob.putString(header.toJson());
		if (data!=null)
			data.accept(blob);
        blob.finish(false);
        user.getRemote().sendBytes(blob.getByteBuffer());
    }
    
    // TODO: Collect the GTracks first; store old renderers; upon new location: send back the results from the old renderer, start the pipeline (which sends the final results per track)
}
