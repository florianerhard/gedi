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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

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
public class TestWebSocketHandler {

	
	@OnWebSocketConnect
    public void onConnect(Session user) throws Exception {
		
		DynamicObject header = DynamicObject.fromMap(
				"msg","init"
				);

		send(user,header);
		
    }

    @OnWebSocketError
    public void methodName(Session session, Throwable error) {
    	error.printStackTrace();
    }
    
    @OnWebSocketClose
    public void onClose(Session user, int statusCode, String reason) {
    	System.out.println(user.getRemoteAddress()+" closed: "+reason+ "("+statusCode+")");
    }

    @OnWebSocketMessage
    public void onMessage(Session user, String message) {
    	
    	System.out.println(message);
    	
    	try {
			send(user,DynamicObject.fromMap(
					"msg","immediate",
					"id", msg.getAndIncrement()));
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		new Thread(()-> {
	    	try {
	    		for (int i=0; i<5; i++) {
		    		Thread.sleep(10);
					send(user,DynamicObject.fromMap(
							"msg","delayed",
							"id", msg.getAndIncrement()));
	    		}
	    	} catch (Exception e1) {
				e1.printStackTrace();
			}
		}).start();
		

    }

    private void send(Session user, DynamicObject header) throws IOException {
    	send(user,header,null);
    }
    AtomicInteger msg = new  AtomicInteger(0);
    private synchronized void send(Session user, DynamicObject header, Consumer2<BinaryWriter> data) throws IOException {
    	System.out.println(header.toJson());
    	BinaryBlob blob = new BinaryBlob();
		blob.putString(header.toJson());
		if (data!=null)
			data.accept(blob);
        blob.finish(false);
        user.getRemote().sendBytes(blob.getByteBuffer());
    }
    
}
