package gedi.gui.genovis.tracks;

import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.gui.genovis.VisualizationTrack;
import gedi.gui.genovis.VisualizationTrackAdapter;
import gedi.gui.genovis.VisualizationTrackPickInfo;
import gedi.gui.genovis.VisualizationTrackPickInfo.TrackEventType;
import gedi.gui.genovis.style.StyleObject;
import gedi.gui.genovis.tracks.boxrenderer.BoxRenderer;
import gedi.gui.genovis.tracks.selection.RegionSelectionModel;
import gedi.util.FunctorUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.functions.TriFunction;
import gedi.util.gui.PixelBasepairMapper.PixelBasePairRange;
import gedi.util.math.stat.RandomNumbers;
import gedi.util.mutable.MutableInteger;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;


@GenomicRegionDataMapping(fromType=IntervalTree.class)
public class PackRegionTrack<D> extends VisualizationTrackAdapter<IntervalTree<GenomicRegion,D>,ReferenceGenomicRegion<D>> {

	public enum PackMode {
		Pixel, Basepair
	}
	
	protected ToIntFunction<D> categorizer = d->0;
	protected BoxRenderer<D> boxRenderer = new BoxRenderer<D>();
	protected int hspace = 1;
	protected int vspace = 1;
	protected int maxTracks = 500;
	protected double simpleBasePairsPerPixel;
	
	private RegionSelectionModel selection = new RegionSelectionModel();
	
	private boolean topDown = false;

	private PackMode mode = PackMode.Basepair;
	
	public void setBoxRenderer(BoxRenderer<D> boxRenderer) {
		this.boxRenderer = boxRenderer;
		if (this.viewer!=null)
			this.viewer.repaint();
	}
	
	public RegionSelectionModel getSelection() {
		return selection;
	}
	
	
	public void setSelection(RegionSelectionModel selection) {
		this.selection = selection;
	}
	
	public void setSelection(PackRegionTrack<?> track) {
		this.selection = track.selection;
	}
	
	public void addViewDoubleClick() {
		addListener(pi->{
			if (pi==null || pi.getData()==null || pi.getData().getRegion()==null) return;
			int e = (int) (pi.getData().getRegion().getTotalLength()*0.05);
			viewer.setLocation(pi.getData().getReference().toStrandIndependent(), pi.getData().getRegion().extendFront(e).extendBack(e));
		}, VisualizationTrackPickInfo.TrackEventType.DoubleClicked);
	}
	
	public void addToolTip() {
		
		
		addListener(pi->{
			if (pi.getType()==TrackEventType.EnteredObject && pi.getData()!=null && pi.getData().getData()!=null) {
				String text = pi.getData().getData().toString();
				text = text.replace("\t", " ");
				text = "<html>"+text.replace("\n", "<br>")+"</html>";
				viewer.showToolTip(text);
			} else {
				viewer.showToolTip(null);
			}
		}, VisualizationTrackPickInfo.TrackEventType.EnteredObject, VisualizationTrackPickInfo.TrackEventType.ExitedObject);
	}
	
	@SuppressWarnings("rawtypes")
	public void addSelect() {
		addListener(pi->{
			if (pi.getData()!=null) {
				selection.toggle(new ImmutableReferenceGenomicRegion<PackRegionTrack>(pi.getReference(), pi.getData().getRegion(), PackRegionTrack.this));
				viewer.repaint(true);
			}
		}, VisualizationTrackPickInfo.TrackEventType.Clicked);
	}
	
	@SuppressWarnings("rawtypes")
	public void addCopyToStorage(MemoryIntervalTreeStorage<D> storage,boolean unique) {
		selection.setMaxSelected(1);
		addSelect();
		selection.addListener(rsm->{
			ImmutableReferenceGenomicRegion<? extends VisualizationTrack> rgr = rsm.getElement();
			Strand strand = fixedStrand!=null?fixedStrand:rgr.getReference().getStrand();
			if (rsm.isSelected())
				storage.add(new ImmutableReferenceGenomicRegion<D>(rgr.getReference().toStrand(strand), rgr.getRegion(), getData(rgr.getReference()).getData().get(rgr.getRegion())));
			else
				storage.remove(rgr.getReference().toStrand(strand), rgr.getRegion());
			viewer.reload();
		});
	}
	
	@Override
	public GenomicRegion[] getSmartRegions() {
		
		GenomicRegion[] re = new GenomicRegion[viewer.getReference().length];
		for (int i=0; i<viewer.getReference().length; i++) {
			MutableReferenceGenomicRegion<IntervalTree<GenomicRegion, D>> d = getData(viewer.getReference()[i]);
			if (d==null) return null;
			re[i] = d.getData().toGenomicRegion(r->r).intersect(viewer.getRegion()[i]);
		}
		
		return re;
	}

	public enum PackDirection  {
		Forward,Reverse,Random
	}
	
	private PackDirection packdirection = PackDirection.Forward;
	
	public void setCategorizer(ToIntFunction<D> categorizer) {
		this.categorizer = categorizer;
	}
	
	public void setPackdirection(PackDirection packdirection) {
		this.packdirection = packdirection;
	}
	
	public void setVspace(int vspace) {
		this.vspace = vspace;
		if (this.viewer!=null)
			this.viewer.repaint();
	}
	
	public void setHspace(int hspace) {
		this.hspace = hspace;
		if (this.viewer!=null)
			this.viewer.repaint();
	}
	
	public void setTopDown(boolean topDown) {
		this.topDown = topDown;
	}
	
	public void setBottomUp(boolean buttomUp) {
		this.topDown = !buttomUp;
	}
	
	public void setMode(PackMode mode) {
		this.mode = mode;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public PackRegionTrack() {
		super((Class)IntervalTree.class);
		this.minHeight = this.prefHeight = this.maxHeight = 15;
		this.minPixelPerBasePair=0.0001;
		simpleBasePairsPerPixel = 0.00003; // i.e. never
	}
	

//	@Override
//	protected SubSequenceExtractor<IntervalTree<GenomicRegion,D>> createExtractor() {
//		return new IntervalTreeSubExtractor<GenomicRegion, D>();
//	}
	
	@Override
	protected gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<IntervalTree<GenomicRegion, D>> renderBackground(
			gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<IntervalTree<GenomicRegion, D>> context) {
		context.g2.setPaint(getBackground());
		context.g2.fill(bounds);
		return context;
	}
	
	protected static final int MAX_HEIGHT = 0;
	
	@Override
	protected TrackRenderContext<IntervalTree<GenomicRegion, D>> renderBegin(
			HashMap<ReferenceSequence, MutableReferenceGenomicRegion<Void>> current,
			HashMap<ReferenceSequence, MutableReferenceGenomicRegion<IntervalTree<GenomicRegion, D>>> data,
			TrackRenderContext<IntervalTree<GenomicRegion, D>> context) {
		context.putValue(MAX_HEIGHT, 0.0);
		return context;
	}
	
	private HashMap<GenomicRegion,Double> orderMap;
	private RandomNumbers rnd = new RandomNumbers();
	
	@Override
	public TrackRenderContext<IntervalTree<GenomicRegion,D>> renderTrack(TrackRenderContext<IntervalTree<GenomicRegion,D>> context) {
		IntArrayList tracks = new IntArrayList();
		MutableInteger truncated = new MutableInteger(0);
		
		processBoxes(tracks,truncated, context.reference, context.regionToRender, context.data, (r,y,h)->{
			StyleObject selStyle = selection.getStyle(new ImmutableReferenceGenomicRegion(context.reference,r, PackRegionTrack.this));
			TriFunction<ReferenceSequence, GenomicRegion, D, Paint> ob = boxRenderer.border;
			TriFunction<ReferenceSequence, GenomicRegion, D, Stroke> os = boxRenderer.borderStroke;
			if (selStyle!=null) {
				boxRenderer.border = (ref,reg,x)->selStyle.getColor();
				boxRenderer.borderStroke = (ref,reg,x)->BoxRenderer.SELECTION;
			}
			GenomicRegion reg = boxRenderer.renderBox(context.g2, viewer.getLocationMapper(), context.reference, fixedStrand!=null?fixedStrand:context.reference.getStrand(),r, context.data.get(r), getBounds().getX(), y, h);
			boxRenderer.border = ob;
			boxRenderer.borderStroke = os;
			return reg;
		});
		
		if (truncated.N!=0) {
			double y = topDown?getBounds().getMaxY()-2:getBounds().getMinY();
			context.g2.setPaint(Color.red);
			context.g2.fill(new Rectangle2D.Double(getBounds().getMinX(),y,getBounds().getWidth(),2));
			context.g2.draw(new Rectangle2D.Double(getBounds().getMinX(),y,getBounds().getWidth(),2));
		}
		
		double height = Math.max(0, tracks.size());
		context.putValue(MAX_HEIGHT, Math.max((Double) context.get(MAX_HEIGHT),height));

		return context;
	}
	
	@Override
	public boolean isEmptyData(IntervalTree<GenomicRegion, D> data) {
		return data.isEmpty();
	}
	
	public void pick(VisualizationTrackPickInfo<ReferenceGenomicRegion<D>> info) {
		if (boxRenderer.forceLabel) return; // TODO!
		
		IntArrayList tracks = new IntArrayList();
		MutableInteger truncated = new MutableInteger(0);
		MutableReferenceGenomicRegion<D> re = new MutableReferenceGenomicRegion<D>();
		re.setReference(info.getReference());
		MutableReferenceGenomicRegion<IntervalTree<GenomicRegion, D>> d = getData(info.getReference());
		if (d==null) return;
		IntervalTree<GenomicRegion, D> data = d.getData();
		
		processBoxes(tracks,truncated, info.getReference(), getCurrent(info.getReference()).getRegion(), data, 
				(r,y,h)->{
					if (info.getPixelY()>=y && info.getPixelY()<y+h && r.contains(info.getBp())) 
						re.setRegion(r).setData(data.get(r));
					return r;
				});
		
		if (re.getRegion()!=null)
			info.setData(re);
	}

	private void processBoxes(IntArrayList tracks, MutableInteger truncated,
			ReferenceSequence reference, GenomicRegion regionToRender,
			IntervalTree<GenomicRegion, D> data, BoxProcessor processor) {
		boolean simpleMode = viewer.getLocationMapper().getPixelsPerBasePair()<simpleBasePairsPerPixel;
		int vspace = simpleMode?0:this.vspace;
		
		PixelBasePairRange[] ranges = viewer.getLocationMapper().getRanges(reference);
		PixelBasePairRange range = ranges.length==1?ranges[0]:null;
		
		
		
		Iterator<GenomicRegion> it;
		
		switch (packdirection) {
		case Reverse: it= data.keySet().descendingIterator(); break;
		case Random: 
			it = randomize(data);
			break;
		case Forward: 
		default:it= data.keySet().iterator(); break;
		}
		
		Consumer<GenomicRegion> renderIt = r->{
			if (!regionToRender.intersects(r)) return;
			
			double h = simpleMode?1:boxRenderer.prefHeight(reference, regionToRender, data.get(r));
			
			int start = vspace;
			for (int i=0; i<tracks.size(); i++) {
				
				double bp = range!=null?viewer.getLocationMapper().bpToPixel(range,reference, r.getStart()):viewer.getLocationMapper().bpToPixel(ranges,reference, r.getStart());
				int startComp = mode==PackMode.Basepair?r.getStart():(int)Math.ceil(bp);
				
				if (tracks.getInt(i)+hspace>=startComp)
					start = i+1;
				else if (i-start==h)
					break;
			}

			if (start>maxTracks) {
				truncated.N = 1;
				return;
			}
			
			if (start+h>maxTracks) {
				truncated.N = 1;
				h = maxTracks-start;
			}
			
			double y = topDown?getBounds().getY()+start:getBounds().getMaxY()-h-start;
			
			GenomicRegion reg = processor.processBox(r,y,h);
			if (!reg.isEmpty()) {
				double bp = range!=null?viewer.getLocationMapper().bpToPixel(range,reference, reg.getStop()):viewer.getLocationMapper().bpToPixel(ranges,reference, reg.getStop());
				int right = mode==PackMode.Basepair?reg.getStop():(int)Math.ceil(bp);
					
				for (int track=start; track<start+h+vspace; track++)
					tracks.set(track, right);
			}
		};
		
		if (categorizer==null)
			while (it.hasNext())
				renderIt.accept(it.next());
		else{
			ArrayList<ArrayList<GenomicRegion>> categories = new ArrayList<ArrayList<GenomicRegion>>();
			while (it.hasNext()) {
				GenomicRegion r = it.next();
				D d = data.get(r);
				int c = categorizer.applyAsInt(d);
				if (c>=0) {
					while (categories.size()<=c) categories.add(new ArrayList<GenomicRegion>());
					categories.get(c).add(r);
				}
			}
			for (ArrayList<GenomicRegion> l : categories){
				for (GenomicRegion r : l)
					renderIt.accept(r);
				for (int track=0; track<tracks.size(); track++)
					tracks.set(track, regionToRender.getEnd());
			}
		}
	}

	private interface BoxProcessor {
		GenomicRegion processBox(GenomicRegion r, double y, double h);
	}
	
	
	private Iterator<GenomicRegion> randomize(
			IntervalTree<GenomicRegion, D> data) {
		GenomicRegion[] a = data.keySet().toArray(new GenomicRegion[0]);
		if (orderMap==null) orderMap = new HashMap<GenomicRegion, Double>();
		else orderMap.keySet().retainAll(data.keySet());
		for (GenomicRegion x : a) if (!orderMap.containsKey(x)) orderMap.put(x,rnd.getUnif());
		Arrays.sort(a, (x,y)->Double.compare(orderMap.get(x), orderMap.get(y)));
		return FunctorUtils.arrayIterator(a);
	}

	@Override
	public TrackRenderContext<IntervalTree<GenomicRegion, D>> renderEnd(
			TrackRenderContext<IntervalTree<GenomicRegion, D>> context) {
		double height = context.get(MAX_HEIGHT);
		if (height!=bounds.getHeight()) {
			this.minHeight = this.prefHeight = this.maxHeight = height;
			this.viewer.relayout();
		}
		return context;
	}


	
	
}
