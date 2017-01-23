package gedi.virology.hsv1annotation;

import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.annotation.NameAnnotation;
import gedi.core.data.mapper.GenomicRegionDataMappingJob;
import gedi.core.data.mapper.StorageSource;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericProvider;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.gui.genovis.SwingGenoVisViewer;
import gedi.gui.genovis.VisualizationTrack;
import gedi.gui.genovis.tracks.ChromosomesTrack;
import gedi.gui.genovis.tracks.PackRegionTrack;
import gedi.gui.genovis.tracks.PositionTrack;
import gedi.gui.genovis.tracks.SpacerTrack;
import gedi.gui.genovis.tracks.boxrenderer.BoxRenderer;
import gedi.gui.renderer.JRenderablePanel;
import gedi.lfc.foldchange.MAPLog2FoldChange;
import gedi.riboseq.cleavage.RiboModel;
import gedi.riboseq.inference.codon.Codon;
import gedi.riboseq.inference.codon.CodonInference;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.PaintUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.datastructure.tree.redblacktree.IntervalTreeSet;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.gui.ColorPalettes;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.job.PetriNet;
import gedi.util.job.Place;
import gedi.util.job.Transition;
import gedi.util.mutable.MutableInteger;
import gedi.util.mutable.MutableTriple;
import gedi.util.r.R;
import gedi.util.r.RConnect;
import gedi.util.r.SvgRenderer;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.virology.hsv1annotation.kinetics.KineticProvider;
import gedi.virology.hsv1annotation.kinetics.ReadoutKinetics;
import gedi.virology.hsv1annotation.kinetics.TssKinetics;
import gedi.virology.hsv1annotation.kinetics.TtsKinetics;
import gedi.virology.hsv1annotation.kinetics.provider.ExonBinProvider;
import gedi.virology.hsv1annotation.kinetics.provider.FromStorageProvider;
import gedi.virology.hsv1annotation.kinetics.provider.KineticRegionProvider;
import gedi.virology.hsv1annotation.kinetics.provider.ReadoutProvider;
import gedi.virology.hsv1annotation.kinetics.provider.TssProvider;
import gedi.virology.hsv1annotation.kinetics.provider.TtsProvider;
import gedi.virology.hsv1annotation.kinetics.quantifier.Average5pSeqCoverageQuantifier;
import gedi.virology.hsv1annotation.kinetics.quantifier.AverageRiboSeqCoverageQuantifier;
import gedi.virology.hsv1annotation.kinetics.quantifier.Deconvolve5pSeqQuantifier;
import gedi.virology.hsv1annotation.kinetics.quantifier.DeconvolveRiboSeqQuantifier;
import gedi.virology.hsv1annotation.kinetics.quantifier.Log2FoldChangeQuantifier;
import gedi.virology.hsv1annotation.kinetics.quantifier.QuantifierSet;
import gedi.virology.hsv1annotation.kinetics.quantifier.ReferenceLog2FoldchangeQuantifier;
import gedi.virology.hsv1annotation.kinetics.quantifier.ReferenceLog2FoldchangeQuantifier.DownstreamCDSReferenceChooser;
import gedi.virology.hsv1annotation.kinetics.quantifier.ReferenceLog2FoldchangeQuantifier.StrongestReferenceChooser;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.NavigableMap;
import java.util.function.Function;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.batik.gvt.GraphicsNode;
import org.rosuda.REngine.Rserve.RserveException;
import org.w3c.dom.svg.SVGDocument;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class Hsv1KineticAnalysis<D> {

	
	private MemoryIntervalTreeStorage<NameAnnotation> mrnas;
	private MemoryIntervalTreeStorage<NameAnnotation> orfs;
	
	private DiskGenomicNumericProvider tss1;
	private DiskGenomicNumericProvider tss2;
	
	private CenteredDiskIntervalTreeStorage<AlignedReadsData> ribo1;
	private CenteredDiskIntervalTreeStorage<AlignedReadsData> ribo2;
	private CodonInference inf1;
	private CodonInference inf2;

	
	private NumericArray tss1Sf;
	private NumericArray tss2Sf;
	
	private NumericArray ribo1Sf;
	private NumericArray ribo2Sf;
	
	private String[] tss1Names;
	private String[] tss2Names;
	
	private String[] ribo1Names;
	private String[] ribo2Names;
	
	
	
	private ArrayList<KineticProvider> providers = new ArrayList<>();
	
	private ArrayList<KineticRegionProvider> regions = new ArrayList<>();
	private ArrayList<QuantifierSet> quantifier = new ArrayList<>();
	
	
	public Hsv1KineticAnalysis(String mrnas, String orfs, String tss1, String tss2, String ribo1, String ribo2, String model1, String model2) throws IOException {
		this.mrnas = new CenteredDiskIntervalTreeStorage<NameAnnotation>(mrnas).toMemory();
		this.orfs = new CenteredDiskIntervalTreeStorage<NameAnnotation>(orfs).toMemory();
		
		this.tss1 = new DiskGenomicNumericProvider(tss1);
		this.tss2 = new DiskGenomicNumericProvider(tss2);
		
		this.ribo1 = new CenteredDiskIntervalTreeStorage<AlignedReadsData>(ribo1);
		this.ribo2 = new CenteredDiskIntervalTreeStorage<AlignedReadsData>(ribo2);
		
		this.inf1 = new CodonInference(RiboModel.fromFile(model1, true));
		this.inf2 = new CodonInference(RiboModel.fromFile(model2, true));
		
		tss1Sf = FileUtils.deserialize(tss1+".sizeFactors");
		tss2Sf = FileUtils.deserialize(tss2+".sizeFactors");
		ribo1Sf = FileUtils.deserialize(ribo1+".sizeFactors");
		ribo2Sf = FileUtils.deserialize(ribo2+".sizeFactors");
		
		tss1Names = StringUtils.split(new LineOrientedFile(tss1+".names").readAllLines()[0],",");
		tss2Names = StringUtils.split(new LineOrientedFile(tss2+".names").readAllLines()[0],",");
		ribo1Names = StringUtils.split(new LineOrientedFile(ribo1+".names").readAllLines()[0],",");
		ribo2Names = StringUtils.split(new LineOrientedFile(ribo2+".names").readAllLines()[0],",");
		
		CodonProvider cp1 = new CodonProvider(this.ribo1, this.inf1);
		CodonProvider cp2 = new CodonProvider(this.ribo2, this.inf2);
		
		
		providers.add(new TssKinetics(this.mrnas, this.tss1, this.tss1Sf,"A"));
		providers.add(new TssKinetics(this.mrnas, this.tss2, this.tss2Sf,"B"));
		providers.add(new ReadoutKinetics(this.mrnas, this.tss1, this.tss1Sf,"A"));
		providers.add(new ReadoutKinetics(this.mrnas, this.tss2, this.tss2Sf,"B"));
		providers.add(new TtsKinetics(this.mrnas, this.tss1, this.tss1Sf,"A"));
		providers.add(new TtsKinetics(this.mrnas, this.tss2, this.tss2Sf,"B"));
		
		KineticRegionProvider mrnasProvider = new FromStorageProvider("mRNAs", this.mrnas);
		KineticRegionProvider mRNAexonBinProvider = new ExonBinProvider("mRNA exon bins",this.mrnas);
		KineticRegionProvider orfsProvider = new FromStorageProvider("ORFs", this.orfs);
		KineticRegionProvider readoutProvider = new ReadoutProvider(this.mrnas);
		KineticRegionProvider tssProvider = new TssProvider(this.mrnas);
		KineticRegionProvider ttsProvider = new TtsProvider(this.mrnas);
		regions.addAll(Arrays.asList(mrnasProvider,mRNAexonBinProvider,orfsProvider,tssProvider,ttsProvider,readoutProvider));
		
		QuantifierSet qReadOut;
		QuantifierSet qTTS;
		QuantifierSet qTSS;
		QuantifierSet qOrfMacoco;
		
		quantifier.add(qTSS = new QuantifierSet(tssProvider,"TSS",tss1Names, Arrays.asList(
				new Average5pSeqCoverageQuantifier("TSS","A", tss1Sf, this.tss1),
				new Average5pSeqCoverageQuantifier("TSS","B", tss2Sf, this.tss2)
				)));
		
		quantifier.add(qReadOut = new QuantifierSet(readoutProvider,"Readout", tss1Names, Arrays.asList(
				new Average5pSeqCoverageQuantifier("Readout","A", tss1Sf, this.tss1),
				new Average5pSeqCoverageQuantifier("Readout","B", tss2Sf, this.tss2)
				)));
		quantifier.add(qTTS = new QuantifierSet(ttsProvider,"TTS", tss1Names, Arrays.asList(
				new Average5pSeqCoverageQuantifier("TTS","A", tss1Sf, this.tss1),
				new Average5pSeqCoverageQuantifier("TTS","B", tss2Sf, this.tss2)
				)));
		
		quantifier.add(new QuantifierSet(readoutProvider,"ReadOut/TTS", tss1Names, Arrays.asList(
				new Log2FoldChangeQuantifier("ReadOut/TTS","A", qReadOut, 0, qTTS, 0, new MAPLog2FoldChange(1,1)),
				new Log2FoldChangeQuantifier("ReadOut/TTS","B", qReadOut, 1, qTTS, 1, new MAPLog2FoldChange(1,1))
				)));
		
		quantifier.add(new QuantifierSet(tssProvider,"TSS to strongest", tss1Names, Arrays.asList(
				new ReferenceLog2FoldchangeQuantifier("TSS to strongest","A", qTSS, 0, new StrongestReferenceChooser() , new MAPLog2FoldChange(1,1)),
				new ReferenceLog2FoldchangeQuantifier("TSS to strongest","B", qTSS, 1, new StrongestReferenceChooser() , new MAPLog2FoldChange(1,1))
				)));
		
		quantifier.add(new QuantifierSet(mRNAexonBinProvider,"Exon bins",tss1Names, Arrays.asList(
				new Average5pSeqCoverageQuantifier("Exon bins","A", tss1Sf, this.tss1).skip5p(5),
				new Average5pSeqCoverageQuantifier("Exon bins","B", tss2Sf, this.tss2).skip5p(5)
				)));
		
		quantifier.add(new QuantifierSet(mrnasProvider,"mRNA EM",tss1Names, Arrays.asList(
				new Deconvolve5pSeqQuantifier("mRNA EM","A", tss1Sf, this.tss1, true).skip5p(5),
				new Deconvolve5pSeqQuantifier("mRNA EM","B", tss2Sf, this.tss2, true).skip5p(5)
				)));
		
		quantifier.add(new QuantifierSet(mrnasProvider,"mRNA MaCoCo",tss1Names, Arrays.asList(
				new Deconvolve5pSeqQuantifier("mRNA MaCoCo","A", tss1Sf, this.tss1, false).skip5p(5),
				new Deconvolve5pSeqQuantifier("mRNA MaCoCo","B", tss2Sf, this.tss2, false).skip5p(5)
				)));
		
		quantifier.add(new QuantifierSet(orfsProvider,"ORFs EM",ribo1Names, Arrays.asList(
				new DeconvolveRiboSeqQuantifier("ORFs EM","A", ribo1Sf,cp1, true),
				new DeconvolveRiboSeqQuantifier("ORFs EM","B", ribo2Sf,cp2, true)
				)));
		
		quantifier.add(qOrfMacoco=new QuantifierSet(orfsProvider,"ORFs MaCoCo",ribo1Names, Arrays.asList(
				new DeconvolveRiboSeqQuantifier("ORFs MaCoCo","A", ribo1Sf,cp1, false),
				new DeconvolveRiboSeqQuantifier("ORFs MaCoCo","B", ribo2Sf,cp2, false)
				)));
		
		quantifier.add(new QuantifierSet(orfsProvider,"ORFs",ribo1Names, Arrays.asList(
				new AverageRiboSeqCoverageQuantifier("ORFs","A", ribo1Sf,cp1),
				new AverageRiboSeqCoverageQuantifier("ORFs","B", ribo2Sf,cp2)
				)));

		quantifier.add(new QuantifierSet(orfsProvider,"uORFs to CDS", ribo1Names, Arrays.asList(
				new ReferenceLog2FoldchangeQuantifier("uORFs to CDS","A", qOrfMacoco, 0, new DownstreamCDSReferenceChooser() , new MAPLog2FoldChange(1,1)),
				new ReferenceLog2FoldchangeQuantifier("uORFs to CDS","B", qOrfMacoco, 1, new DownstreamCDSReferenceChooser() , new MAPLog2FoldChange(1,1))
				)));
		
	}
	
	
	public void analyzeMRnaCluster(String prefix,String...references) throws RserveException, IOException {
		analyzeMRnaCluster(prefix,EI.wrap(references).map(s->(ReferenceSequence)Chromosome.obtain(s)).toArray(ReferenceSequence.class));
	}
	public void analyzeMRnaCluster(String prefix,ReferenceSequence...references) throws RserveException, IOException {
		
		
		MutableInteger index = new MutableInteger(0);
		for (ReferenceSequence ref : EI.wrap(references)
							.demultiplex(r->r.getStrand().equals(Strand.Independent)?
									EI.wrap(r.toPlusStrand(),r.toMinusStrand()):
									EI.singleton(r))
									.loop()) {
			
			int c = (int)mrnas.getTree(ref).groupIterator().count();
			for (MutableTriple<Integer,String,GenomicRegion> cluster : mrnas.getTree(ref)
												.groupIterator()
												.progress(new ConsoleProgress(System.err),c,m->getName(m))
												.map(m->new MutableTriple<Integer,String,GenomicRegion>(
														index.N++,
														getName(m),
														EI.wrap(m.keySet()).collect(new ArrayGenomicRegion(), (a,b)->a.union(b))
														))
												.loop()){
				
//				System.out.println(prefix+StringUtils.padLeft(cluster.Item1+"", 3,'0')+"-"+cluster.Item2+".eps");
				analyze(prefix+StringUtils.padLeft(cluster.Item1+"", 3,'0')+"-"+cluster.Item2+".eps",new ImmutableReferenceGenomicRegion<Void>(ref, cluster.Item3));
			}
			
		}
	}
	
	

	private String getName(NavigableMap<GenomicRegion, NameAnnotation> m) {
		return EI.wrap(m.values())
			.map(a->EI.wrap(a.getName()).map(na->StringUtils.splitField(na, ' ', 0)).first())
			.map(a->EI.wrap(a).map(na->StringUtils.splitField(na, '.', 0)).first())
			.sort().unique(true).concat("-");
	}


	public void analyze(String output, ReferenceGenomicRegion<?> region) throws IOException, RserveException {
		
		LinkedHashMap<KineticRegionProvider,ImmutableReferenceGenomicRegion<NameAnnotation>[]> regions = new LinkedHashMap<KineticRegionProvider, ImmutableReferenceGenomicRegion<NameAnnotation>[]>();
		for (KineticRegionProvider p : this.regions)
			regions.put(p, p.arr(region));
		
		HashMap<KineticRegionProvider, Color[]> colors = assignColors(regions);
		SwingGenoVisViewer genome = createGenoVis(region,regions,colors);
		
		ExtendedIterator<JRenderablePanel<GraphicsNode>> plotIt = EI.wrap(quantifier).map(q->{
			// [rep][region] and within are the conditions
			NumericArray[][] data = EI.wrap(q.getQuantifiers())
					.map(kq->kq.normalize(kq.quantify(region,regions.get(q.getRegionProvider()))))
					.toArray(NumericArray[].class);
			try {
				return createPlot(q.getLabel(),q.getConditions(),data,colors.get(q.getRegionProvider()));
			} catch (Exception e) {
				throw new RuntimeException("Could not plot!", e);
			}
		});
		
		JPanel plots = layoutPlots(plotIt);
		
		JPanel both = new JPanel(new BorderLayout());
		both.add(genome,BorderLayout.NORTH);
		both.add(plots,BorderLayout.CENTER);
			
		new File(output).getAbsoluteFile().getParentFile().mkdirs();
		PaintUtils.screenshot(output, both);
	}

	
	private HashMap<KineticRegionProvider, Color[]> assignColors(
			LinkedHashMap<KineticRegionProvider,ImmutableReferenceGenomicRegion<NameAnnotation>[]> regions) {
		ColorPalettes[] pal = ColorPalettes.qualitativePalettes;

		HashMap<KineticRegionProvider, Color[]> re = new HashMap<KineticRegionProvider, Color[]>();
		int index = 0;
		for (KineticRegionProvider type : regions.keySet()) {
			re.put(type, pal[index++].getPalette(regions.get(type).length));
			index = index%pal.length;
		}
		return re;
	}
	
	private SwingGenoVisViewer createGenoVis(ReferenceGenomicRegion rgr,
			LinkedHashMap<KineticRegionProvider,ImmutableReferenceGenomicRegion<NameAnnotation>[]> regions,
			HashMap<KineticRegionProvider, Color[]> color) throws IOException {
		
		
		PetriNet pipeline = new PetriNet();
		addFixedPN(pipeline);
		for (KineticRegionProvider provider: regions.keySet()) {
			addSpace(pipeline);
			GenomicRegionStorage<NameAnnotation> ext = null;
			if (provider.getType().equals("mRNAs")) ext = mrnas;
			if (provider.getType().equals("ORFs")) ext = orfs;
			addPN(pipeline,regions.get(provider),
					ext,provider, color);
		}
		pipeline.prepare();
		
		
		SwingGenoVisViewer viewer = new SwingGenoVisViewer(pipeline,true);
		EI.wrap(pipeline.getTransitions())
			.map(t->((GenomicRegionDataMappingJob)t.getJob()).getMapper())
			.instanceOf(VisualizationTrack.class)
			.forEachRemaining(viewer::addTrack);
		viewer.setBounds(0, 0, 1024, 1024*1024);
		
		GenomicRegion region = mrnas.ei(rgr).map(r->r.getRegion())
			.chain(orfs.ei(rgr).map(r->r.getRegion()))
			.chain(EI.wrap(regions.values()).demultiplex(m->EI.wrap(m).map(rr->rr.getRegion())))
			.collect((a,b)->a.union(b)).removeIntrons();
		
		
		viewer.setLocation(rgr.getReference(),region.extendAll((int)(region.getTotalLength()*0.1), (int)(region.getTotalLength()*0.1)),false);
		
		return viewer;
	}
	
	
	private JRenderablePanel<GraphicsNode> createPlot(String label,
			String[] conditions, NumericArray[][] data,
			Color[] color) throws IOException, RserveException {
		
		String[] uconditions = conditions;
		int[] use = EI.along(conditions).filter(i->uconditions[i].length()>0).toIntArray();
		
		conditions = ArrayUtils.restrict(conditions, use);
		for (int i=0; i<data.length; i++) {
			for (int j=0; j<data[i].length; j++) {
				data[i][j] = NumericArray.wrap(ArrayUtils.restrict(data[i][j].toDoubleArray(),use));
			}	
		}
		
		double min = EI.wrap(data).demultiplex(l->EI.wrap(l)).mapToDouble(NumericArrayFunction.SaveMin).saveMin(0);
		double max = EI.wrap(data).demultiplex(l->EI.wrap(l)).mapToDouble(NumericArrayFunction.SaveMax).saveMax(min+1);

		int d = use.length;
		
		
		File tmp = File.createTempFile("RConnect", ".svg");
		R r = RConnect.R();
		r.svg(tmp);
		r.evalf("plot(NA,xlim=c(1,%d),ylim=c(%.5f,%.5f),xlab='',ylab='%s',main='',xaxt='n')", d,min,max, label);
		r.evalf("axis(1,at=1:%d,labels=c(%s))", d,EI.wrap(conditions).map(c->"'"+c+"'").concat(","));
		
		for (int repi=0; repi<data.length; repi++) {
			for (int regi=0; regi<data[repi].length; regi++) {
				String col = PaintUtils.encodeColor(color[regi]);
				r.set("dat", data[repi][regi].toDoubleArray());
				r.evalf("lines(1:%d,dat,col='%s',lwd=4, lty=%d)",d,col,repi+1);
			}
		}
		r.closeDevice();
		
		SVGDocument doc = PaintUtils.loadSvgDoc(tmp.getAbsolutePath());
		SvgRenderer ren = new SvgRenderer(doc);
		JRenderablePanel<GraphicsNode> pan = new JRenderablePanel<GraphicsNode>(ren,false);
		
		tmp.delete();
		return pan;
	}
	
	private JPanel layoutPlots(ExtendedIterator<? extends JComponent> it) {
		JPanel re = new JPanel(new GridLayout(0,3));
		for (JComponent c : it.loop()) 
			re.add(c);
		return re;
	}
	
	
	
	
	
	
	
	private void addFixedPN(PetriNet pipeline) {
		ChromosomesTrack chr = new ChromosomesTrack();
		chr.setHeight(25);
		chr.setFont("Arial", 20, true, false);
		pipeline.createTransition(new GenomicRegionDataMappingJob(chr));
		
		PositionTrack pos = new PositionTrack();
		pos.setHeight(20);
		pos.setFont("Arial", 15, false, false);
		pipeline.createTransition(new GenomicRegionDataMappingJob(pos));
	}

	private void addSpace(PetriNet pipeline) {
		pipeline.createTransition(new GenomicRegionDataMappingJob(new SpacerTrack(Color.black, 3)));
	}

	
	private void addPN(
			PetriNet pipeline,
			ImmutableReferenceGenomicRegion<NameAnnotation>[] regions,
			GenomicRegionStorage<NameAnnotation> ext, KineticRegionProvider provider,
			HashMap<KineticRegionProvider, Color[]> color) {
		MemoryIntervalTreeStorage<NameAnnotation> storage = new MemoryIntervalTreeStorage<NameAnnotation>(NameAnnotation.class);
		storage.fill(EI.wrap(regions));
		
		Color[] cols= color.get(provider);
		HashMap<GenomicRegion, Color> colMap = EI.along(regions).index(i->regions[i].getRegion(), i->cols[i]);
		
		StorageSource<NameAnnotation> src = new StorageSource<NameAnnotation>();
		src.add(ext==null?storage:ext);
		Transition srcTrans = pipeline.createTransition(new GenomicRegionDataMappingJob(src));
		
		Place b = pipeline.createPlace(srcTrans.getJob().getOutputClass());
		
		PackRegionTrack<NameAnnotation> track = new PackRegionTrack<NameAnnotation>();
		track.setHspace(0);
		BoxRenderer br = new BoxRenderer();
		br.setHeight(20);
		br.setFont("Arial",20,true,false);
		br.setBackground((ref,reg,d)->colMap.containsKey(reg)?colMap.get(reg):Color.lightGray);
		br.setBorder();
		br.setForceLabel(true);
		track.setBoxRenderer(br);
		track.setId(provider.getType());
		Transition trackTrans = pipeline.createTransition(new GenomicRegionDataMappingJob(track));
		
		pipeline.connect(srcTrans, b);
		pipeline.connect(b, trackTrans, 0);
	}

	
	private static class CodonProvider implements Function<ReferenceGenomicRegion<?>,ExtendedIterator<Codon>> {

		private CenteredDiskIntervalTreeStorage<AlignedReadsData> reads;
		private CodonInference inf;

		public CodonProvider(
				CenteredDiskIntervalTreeStorage<AlignedReadsData> reads,
				CodonInference inf) {
			this.reads = reads;
			this.inf = inf;
		}

		private ReferenceGenomicRegion<?> curRef;
		private ArrayList<Codon> curCodons;
		
		@Override
		public synchronized ExtendedIterator<Codon> apply(ReferenceGenomicRegion<?> t) {
			if (!t.equals(curRef)) {
				ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons = inf.inferCodons(reads, t.getReference(), t.getRegion().getStart(), t.getRegion().getEnd(), 50, null);
				curCodons = new ArrayList<Codon>();
				for (Codon c : codons.getData()) {
					GenomicRegion ind = codons.map(c);
					if (t.getRegion().containsUnspliced(ind))
						curCodons.add(new Codon(ind,c));
				}
				curRef = t;
			}
			return EI.wrap(curCodons);
		}

		
		
		
	}
	
}
