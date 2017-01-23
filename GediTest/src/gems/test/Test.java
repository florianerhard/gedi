package gems.test;


import gedi.app.Gedi;
import gedi.core.reference.Chromosome;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.SwingGenoVisViewer;
import gedi.gui.genovis.VisualizationTrack;
import gedi.oml.OmlNode;
import gedi.oml.OmlNodeExecutor;
import gedi.oml.OmlReader;
import gedi.oml.petrinet.Pipeline;
import gedi.oml.remote.Pipeline.Receiver;
import gedi.oml.remote.Pipeline.ReceiverGroup;
import gedi.remote.RemoteConnections;
import gedi.remote.RetryRemoteConnection;
import gedi.remote.RetryRemoteConnection.RemoteRetryEvent;
import gedi.remote.RetryRemoteConnection.RemoteRetryEventType;
import gedi.util.StringUtils;
import gedi.util.math.stat.RandomNumbers;
import gedi.util.nashorn.JS;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;

import javax.script.ScriptException;
import javax.swing.JFrame;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import org.apache.commons.math3.special.Gamma;

public class Test {

	
	
	public static void main(String[] args) throws Exception {
		
		JS js = new JS();
		js.putVariable("a", 1);
		js.putVariable("b", 1);
		
		js.eval("var c = 2;");
		js.eval("var d = 2;");
		js.eval("var a = 2;");
		
		js.putVariable("c", 1);
		
		System.out.println(StringUtils.toString(js.getVariables(true).keySet()));
		System.out.println("a="+js.getVariable("a"));
		System.out.println("b="+js.getVariable("b"));
		System.out.println("c="+js.getVariable("c"));
		System.out.println("d="+js.getVariable("d"));
		
		System.out.println("a="+js.eval("a"));
		System.out.println("b="+js.eval("b"));
		System.out.println("c="+js.eval("c"));
		System.out.println("d="+js.eval("d"));
		
	}
	
	private static void rn(){
		
		double[] t = new RandomNumbers().getUnif(100_000_000, 10, 5000).toDoubleArray();
		
		long s = System.nanoTime();
		for (double x : t) cern.jet.stat.Gamma.logGamma(x);
		System.out.println(StringUtils.getHumanReadableTimespanNano(System.nanoTime()-s));
		
		
		s = System.nanoTime();
		for (double x : t) Gamma.logGamma(x);
		System.out.println(StringUtils.getHumanReadableTimespanNano(System.nanoTime()-s));
		
		
	}
	
	
	private static void compress() throws IOException {
			
		byte[] b = new byte[36412];
		byte[] c = new byte[12109];
		LZ4Compressor comp = LZ4Factory.fastestInstance().fastCompressor();
		LZ4FastDecompressor decomp = LZ4Factory.fastestInstance().fastDecompressor();
		
		FileInputStream is = new FileInputStream("/home/users/erhard/test/lz4/bug.bin");
		is.read(b, 0, b.length);
		is.close();
		
		int l = comp.compress(b, 0, b.length, c, 0);
		decomp.decompress(c, 0,b, 0, l);
		
//		 EI.seq(0,100).parallelized(new MapMult2()).print();

		
	}
//	private static class MapMult2 implements Function<ExtendedIterator<Integer>,ExtendedIterator<Integer>> {
//		Mult2 m2 = new Mult2();
//		@Override
//		public ExtendedIterator<Integer> apply(ExtendedIterator<Integer> t) {
//			return t.map(m2);
//		}
//		
//	}
//	private static class Mult2 implements Function<Integer, Integer> {
//
//		@Override
//		public Integer apply(Integer t) {
//			try {
//				Thread.sleep(50);
//			} catch (InterruptedException e) {
//				Thread.currentThread().interrupt();
//			}
//			return t*2;
//		}
//		
//	}
//	
//	
//	
//	private static void showViewerOml() throws SQLException, IOException, ScriptException {
//		
//		Chromosome chr = Chromosome.obtain("chr1");
//		ArrayGenomicRegion region = GenomicRegion.parse("76,535,176-76,547,530");
//		region = GenomicRegion.parse("71546600-71547200");
//		
//		OmlNode root = new OmlReader().parse(new File("data/tracks.oml"));
//		Pipeline pipeline = new OmlNodeExecutor().execute(root);
//		
//		SwingGenoVisViewer viewer = new SwingGenoVisViewer(pipeline.getPetriNet());
//		for (VisualizationTrack track : pipeline.getTracks())
//			viewer.addTrack(track);
//		
//		JFrame frame = new JFrame();
//		frame.setPreferredSize(new Dimension(1024, 800));
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		
//		frame.getContentPane().setLayout(new BorderLayout());
//		frame.getContentPane().add(viewer, BorderLayout.CENTER);
//		
//		frame.pack();
//		frame.setVisible(true);
//		viewer.setLocation(chr,region);
//		
//		
//	}
//	
	private static void showViewerRemoteOml() throws SQLException, IOException, ScriptException, URISyntaxException {
		
		Gedi.startup(true);
		
		Chromosome chr = Chromosome.obtain("chr1");
		ArrayGenomicRegion region = GenomicRegion.parse("76,535,176-76,547,530");
		region = GenomicRegion.parse("71546600-71547200");
		
		OmlNode localRoot = new OmlReader().parse(new File("data/tracks_netty_localhost.oml"));
		OmlNode remoteRoot = new OmlReader().parse(new File("data/tracks_netty_hpclient13.oml"));
		
		Pipeline pipeline = new OmlNodeExecutor().execute(localRoot);
		
		SwingGenoVisViewer viewer = new SwingGenoVisViewer(pipeline.getPetriNet());
		
		
		ReceiverGroup rg = new ReceiverGroup(remoteRoot,pipeline.getObjects(Receiver.class));
		RetryRemoteConnection retrier = RemoteConnections.getInstance().connectRetry(new URI("tracks://hpclient13"), rg, 5000,true);
		retrier.addListener(new RetryRemoteConnection.RetryListener() {

			@Override
			public void remoteRetryEvent(RemoteRetryEvent event) {
				if (event.getType()==RemoteRetryEventType.CONNECTED)
					viewer.refreshLocation();
				System.out.println("Connection: "+event.getType());
			}
			
		});
		
		for (VisualizationTrack track : pipeline.getTracks())
			viewer.addTrack(track);
		viewer.getDataManager().addListener(rg);
		
		JFrame frame = new JFrame();
		frame.setPreferredSize(new Dimension(1024, 800));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(viewer, BorderLayout.CENTER);
		
		frame.pack();
		frame.setVisible(true);
		viewer.setLocation(chr,region);
		
		
	}

	
}
