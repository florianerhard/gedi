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
package gedi.bam.tools;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.data.reads.AlignedReadsVariation;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.feature.AlignedReadsDataToFeatureProgram;
import gedi.core.region.feature.GenomicRegionFeatureProgram;
import gedi.region.bam.FactoryGenomicRegion;
import gedi.util.ArrayUtils;
import gedi.util.FunctorUtils;
import gedi.util.FunctorUtils.ResortIterator;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.graph.SimpleDirectedGraph;
import gedi.util.datastructure.graph.SimpleDirectedGraph.AdjacencyNode;
import gedi.util.io.randomaccess.diskarray.VariableSizeDiskArray;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.orm.Orm;
import gedi.util.sequence.CountDnaSequence;
import gedi.util.sequence.DnaSequence;
import gedi.util.sequence.MismatchGraphBuilder;
import gedi.util.sequence.ObjectEditDistanceGraphBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import cern.colt.bitvector.BitVector;

public class SamMismatchCorrectionBarcodeAnnotator implements UnaryOperator<Iterator<SAMRecord>> {
	
	VariableSizeDiskArray<CountDnaSequence> bc;
	
	
	private int[] cumNumCond;
	
	private AlignedReadsDataToFeatureProgram[] programs = null;
	private MutableReferenceGenomicRegion<AlignedReadsData> rgr = new MutableReferenceGenomicRegion<AlignedReadsData>();
	private boolean removeUnmapped = true;
	
	private double m = 0.02;

	private ToIntFunction<CharSequence> conditioner;
	private int minLength;


	private BiConsumer<DefaultAlignedReadsData, Object> countSetter = Orm.getFieldSetter(DefaultAlignedReadsData.class, "count");
	
//	public SamMismatchCorrectionBarcodeAnnotator(String bcFile, String...patterns) throws IOException {
//		this(bcFile,18,patterns);
//	}
//	
//	public SamMismatchCorrectionBarcodeAnnotator(String bcFile, int minLength, String...patterns) throws IOException {
//		bc = new VariableSizeDiskArray<CountDnaSequence>(bcFile,()->new CountDnaSequence("", 0));
//		
//		this.minLength = minLength;
//		
//		Pattern[] pats = new Pattern[patterns.length];
//		for (int i=0; i<patterns.length; i++)
//			pats[i] = Pattern.compile(patterns[i]);
//		cumNumCond = new int[] {pats.length};
//		
//		conditioner = arr->{
//			for (int p=0; p<patterns.length; p++) {
//				if (pats[p].matcher(arr).find()) {
//					return p;
//				}
//			}
//			return -1;
//		};
//	}
	
	
	public SamMismatchCorrectionBarcodeAnnotator(String bcFile, int offset, String conditionFile) throws IOException {
		this(bcFile,15,offset,conditionFile);
	}
	public SamMismatchCorrectionBarcodeAnnotator(String bcFile, int minLength, int offset, String conditionFile) throws IOException {
		bc = new VariableSizeDiskArray<CountDnaSequence>(bcFile,()->new CountDnaSequence("", 0));
		
		this.minLength = minLength;
		String[] barcodes = new LineOrientedFile(conditionFile).lineIterator().skip(1).map(s->StringUtils.splitField(s, '\t', 0)).toArray(new String[0]);
		
		HashMap<String, Integer> index = ArrayUtils.createIndexMap(barcodes);
		int end = offset+barcodes[0].length();
		cumNumCond = new int[] {barcodes.length};
		
		conditioner = arr->{
			Integer r = index.get(arr.subSequence(offset, end).toString());
			if (r==null) return -1;
			return r;
		};
	}
	
	public SamMismatchCorrectionBarcodeAnnotator(String bcFile, int offset, String[] barcodes) throws IOException {
		this(bcFile,15,offset,barcodes);
	}
	public SamMismatchCorrectionBarcodeAnnotator(String bcFile, int minLength, int offset, String[] barcodes) throws IOException {
		bc = new VariableSizeDiskArray<CountDnaSequence>(bcFile,()->new CountDnaSequence("", 0));
		
		this.minLength = minLength;
		
		HashMap<String, Integer> index = ArrayUtils.createIndexMap(barcodes);
		int end = offset+barcodes[0].length();
		cumNumCond = new int[] {barcodes.length};
		
		conditioner = arr->{
			Integer r = index.get(arr.subSequence(offset, end).toString());
			if (r==null) return -1;
			return r;
		};
	}
	
	
	public void updateHeader(SAMFileHeader header) {
		header.addComment("XR-count:"+cumNumCond[0]);
	}
	
	
	public void startPrograms(GenomicRegionFeatureProgram<AlignedReadsData> collapsed, GenomicRegionFeatureProgram<AlignedReadsData> corrected, GenomicRegionFeatureProgram<AlignedReadsData> total) {
		programs = new AlignedReadsDataToFeatureProgram[]{
				new AlignedReadsDataToFeatureProgram(collapsed),
				new AlignedReadsDataToFeatureProgram(corrected),
				new AlignedReadsDataToFeatureProgram(total)
				};
		
		for (int i=0; i<programs.length; i++)
			if (programs[i]!=null)
				programs[i].getProgram().begin();
		
	}
	
	public void finishPrograms() {
		if (programs!=null)
			for (int i=0; i<programs.length; i++)
				if (programs[i]!=null)
					programs[i].getProgram().end();
	}
	
	public CountDnaSequence get(long index) throws IOException {
		return bc.get(index);
	}
	
	public long size() {
		return bc.length();
	}
	
	public Iterator<SAMRecord> apply(Iterator<SAMRecord> it) {
		
		Comparator<SAMRecord> strict = new SamRecordPosComparator(true).thenComparing((a,b)-> {
			return BamUtils.getRecordsGenomicRegion(a).compareTo(BamUtils.getRecordsGenomicRegion(b));
		});
		
		ResortIterator<SAMRecord> resort = FunctorUtils.resortIterator(SAMRecord.class, it, new SamRecordPosComparator(false), strict, false);
		
		Iterator<SAMRecord[]> ait = FunctorUtils.multiplexIterator(resort, strict, SAMRecord.class);
		Iterator<SAMRecord[]> sit = FunctorUtils.mappedIterator(ait,sa->{
			try {
				int len = -1;
				HashMap<BarcodedMismatchSequence,SAMRecord> list = new HashMap<BarcodedMismatchSequence,SAMRecord>();
				for (SAMRecord r : sa) {
					String seq = r.getReadString();
					if (r.getReadNegativeStrandFlag())
						seq = SequenceUtils.getDnaReverseComplement(seq);
//					String seq = BamUtils.restoreSequence(r, true);
					for (CountDnaSequence cds : bc.getCollection(Integer.parseInt(r.getReadName()),new ArrayList<CountDnaSequence>())){
						BarcodedMismatchSequence bcl = new BarcodedMismatchSequence(cds, seq);
						list.put(bcl,r);
						if (len!=-1 && bcl.length()!=len) 
							throw new RuntimeException("Not applicable to insertions and deletions!");
						len = bcl.length();
					}
				}
				BarcodedMismatchSequence[] arr = list.keySet().toArray(new BarcodedMismatchSequence[0]);
				

				BitVector keep = new BitVector(arr.length);
				if (arr.length==1) {
					keep.putQuick(0, true);
				}
				else if (arr.length>0) {
					MismatchGraphBuilder<BarcodedMismatchSequence> gb = new MismatchGraphBuilder<BarcodedMismatchSequence>(arr);
					SimpleDirectedGraph<BarcodedMismatchSequence> g;
					try {
						g = gb.build();
					} catch (StringIndexOutOfBoundsException e) {
						for (SAMRecord r : sa)
							System.out.println(BamUtils.getRecordsGenomicRegion(r)+"\t"+r.getSAMString());
						throw new RuntimeException("Cannot build mismatch graph!",e);
					}
					
//					keep.not();
//					Arrays.sort(arr,(a,b)->Integer.compare(a.getCount(), b.getCount()));
//					HashMap<BarcodedMismatchSequence,Integer> index = ArrayUtils.createIndexMap(arr);
//					for (int i=0; i<arr.length; i++) {
//						if (keep.getQuick(i)) {
//							int c = arr[i].getCount();
//							for (AdjacencyNode<BarcodedMismatchSequence> n = g.getTargets(arr[i]); n!=null; n=n.next) {
//								if (n.node.getCount()<c)
//									keep.putQuick(index.get(n.node), false);
//							}
//						}
//					}
					
					for (int i=0; i<arr.length; i++) {
						double e = 0;
						// should rather be getSources, but this doesnt matter here!
						for (AdjacencyNode<BarcodedMismatchSequence> n = g.getTargets(arr[i]); n!=null; n=n.next) 
							 e += n.node.getCount()*m;
						
						// e is the expected number of reads due to sequencing errors from similar reads
						if (e<arr[i].getCount()) // keep if there are more reads here!
							keep.putQuick(i, true);
					}
					
				}
				
				// count per condition
				HashMap<SAMRecord,int[][]> count = new HashMap<SAMRecord,int[][]>();
				for (int i=0; i<keep.size(); i++) {
					boolean keepit=keep.getQuick(i);
					
					int p = conditioner.applyAsInt(arr[i]);
					if (p==-1) p=cumNumCond[0];
					// put into last condition slot
					
					int[][] c = count.get(list.get(arr[i]));
					if (c==null) count.put(list.get(arr[i]), c = new int[3][cumNumCond[0]+1]);
					
					c[0][p]++;
					if (keepit)
						c[1][p]++;
					c[2][p]+=arr[i].getCount();
					
				}
				
//				keep.forEachIndexFromToInState(0, keep.size()-1, true, i->{
//					for (int p=0; p<patterns.length; p++) {
//						if (patterns[p].matcher(arr[i]).find()) {
//							int[] c = count.get(list.get(arr[i]));
//							if (c==null) count.put(list.get(arr[i]), c = new int[patterns.length]);
////							c[p]+=arr[i].getCount();
//							c[p]++;
//						}
//					}
//					return true;
//					});
				
				
				if (programs!=null) {
					for (int mode=0; mode<programs.length; mode++)
						for (SAMRecord s : count.keySet()) {
							int[] c = count.get(s)[1];
							if (ArrayUtils.sum(c)>0) {
								s.setAttribute("XR",""+StringUtils.concat(",",count.get(s)[mode]));
								
								FactoryGenomicRegion reg = BamUtils.getFactoryGenomicRegion(s, cumNumCond, false, false);
								reg.add(s,0);
								
								programs[mode].accept(rgr .set(BamUtils.getReference(s),reg,reg.create()));
							}
						}
				}
				
				sa = new SAMRecord[count.size()];
				int index = 0;
				for (SAMRecord s : count.keySet()) {
					int[] c = count.get(s)[1];
					if (ArrayUtils.sum(c,0,c.length-1)>0)
						sa[index++] = s;
				}
				if (index<sa.length)
					sa = ArrayUtils.redimPreserve(sa, index);
				
				for (int i=0; i<sa.length; i++)
					sa[i].setAttribute("XR",""+StringUtils.concat(",",count.get(sa[i])[1]));
				
				
				if (sa.length==0) return sa;
				
				if (removeUnmapped && sa[0].getReferenceName().equals("Unmapped"))
					return new SAMRecord[0];
				
				if (sa[0].getReadLength()<minLength)
					return new SAMRecord[0];
				
				return sa;
			} catch (Exception e) {
				throw new RuntimeException("Could not infer fragment counts!",e);
			}
		});
	
		return FunctorUtils.demultiplexIterator(sit, a->FunctorUtils.arrayIterator(a));
		
	}


	// count: original reads, original reads with simple collapsed barcodes, corrected reads, corrected reads matching one of the barcodes
	double[] total = new double[4];
	private LineWriter totalOut;
	public void setTotalOut(LineWriter totalOut) throws IOException {
		this.totalOut = totalOut;
		totalOut.writeLine("Original\tCollapsed barcode\tCorrected\tCorrected condition");
	}
	public LineWriter getTotalOut() {
		return totalOut;
	}
	public ReferenceGenomicRegion<AlignedReadsData> transform(ReferenceGenomicRegion<AlignedReadsData> r) {
		try {
			AlignedReadsData data = r.getData();

			HashMap<DnaSequence,IntArrayList> bcToIndices = data.getDistinctSequences()>1?new HashMap<DnaSequence, IntArrayList>():null;
			ArrayList<CountDnaSequence> barcodeList = new ArrayList<CountDnaSequence>();
			IntArrayList distinctList = new IntArrayList();
			List<AlignedReadsVariation>[] varlists = new List[data.getDistinctSequences()];
			DoubleArrayList expected = new DoubleArrayList();
			
			for (int d=0; d<data.getDistinctSequences(); d++) {
				CountDnaSequence[] bcs = bc.getCollection(data.getId(d),new ArrayList<CountDnaSequence>()).toArray(new CountDnaSequence[0]);
				
				if (bcs.length==1 && data.getDistinctSequences()==1) {
					// just a stupid singleton!
					expected.add(0);
					if (bcToIndices!=null)
						bcToIndices.computeIfAbsent(new DnaSequence(bcs[0]), s->new IntArrayList(2)).add(barcodeList.size());
					barcodeList.add(bcs[0]);
					distinctList.add(d);
				} else {
					// compute edit distance 1 barcodes within this variation sequence
					SimpleDirectedGraph<CountDnaSequence> gb = new MismatchGraphBuilder<CountDnaSequence>(bcs).build();
					for (CountDnaSequence cds : bcs) {
						
						double e = 0;
						for (AdjacencyNode<CountDnaSequence> n = gb.getTargets(cds); n!=null; n=n.next) 
							 e += n.node.getCount()*m;
						expected.add(e);
						
						if (bcToIndices!=null)
							bcToIndices.computeIfAbsent(new DnaSequence(cds), s->new IntArrayList(2)).add(barcodeList.size());
						barcodeList.add(cds);
						distinctList.add(d);
					}
				}
			}
			// now add all values from identical barcodes from distinct variation sequences
			if (bcToIndices!=null)
				for (IntArrayList l : bcToIndices.values()) {
					if (l.size()>1) {
						for (int i=0; i<l.size(); i++) {
							for (int j=0; j<l.size(); j++) {
								if (i!=j) {
									int i1 = l.getInt(i);
									int i2 = l.getInt(j);
									if (ObjectEditDistanceGraphBuilder.isEditDistance1(
											Arrays.asList(data.getVariations(distinctList.getInt(i1))),
											Arrays.asList(data.getVariations(distinctList.getInt(i2))),
											(a,b)->a.getPosition()==b.getPosition() && a.isMismatch()==b.isMismatch()
											))
										expected.increment(i1, barcodeList.get(i2).getCount()*m);
								}
							}	
						}
					}
				}
			// keep iff expected<observed count!
			
			
			// count per condition: collapse, corrected, reads
			int[][][] count = new int[3][data.getDistinctSequences()][cumNumCond[0]];
			boolean found = false;
			for (int i=0; i<barcodeList.size(); i++) {
				CountDnaSequence barcode = barcodeList.get(i);
				
				boolean keepit=expected.getDouble(i)<barcode.getCount();
				int d = distinctList.getInt(i);
				
				int p = conditioner.applyAsInt(barcode);
				if (p!=-1) {
					found = true;
					
					count[0][d][p]++;
					if (keepit)
						count[1][d][p]++;
					count[2][d][p]+=barcode.getCount();
				}
				
				double m = Math.max(1, r.getData().getMultiplicity(d));
				total[0]+=barcode.getCount()/m;
				total[1]+=1/m;
				if (keepit) total[2]+=1/m;
				if (keepit && p!=-1) total[3]+=1/m;
			}

			if (!found) {
				return null;
			}
			
			
			if (programs!=null) {
				MutableReferenceGenomicRegion<AlignedReadsData> pr = new MutableReferenceGenomicRegion<AlignedReadsData>()
						.set(r.getReference(), r.getRegion());
				
				for (int mode=0; mode<programs.length; mode++) {
					DefaultAlignedReadsData ard = new DefaultAlignedReadsData(r.getData());
					countSetter.accept(ard,count[mode]);
					pr.setData(ard);
					programs[mode].accept(pr);
				}
			}
			
			countSetter.accept((DefaultAlignedReadsData) r.getData(),count[1]);
			
			
			// remove empty distinct sequences
			boolean thereIsEmpty = false;
			for (int i=0; i<count[1].length; i++) {
				if (ArrayUtils.sum(count[1][i])==0) {
					thereIsEmpty = true;
					break;
				}
			}
			
			if (thereIsEmpty) {
				AlignedReadsDataFactory fac = new AlignedReadsDataFactory(cumNumCond[0]);
				fac.start();
				for (int i=0; i<count[1].length; i++) {
					if (ArrayUtils.sum(count[1][i])>0) 
						fac.add(data, i);
				}
				if (fac.getDistinctSequences()==0)
					return null;
				return new ImmutableReferenceGenomicRegion<AlignedReadsData>(r.getReference(), r.getRegion(), fac.create());
			}
			return r;
		} catch (IOException e) {
			throw new RuntimeException("Could not infer fragment counts!",e);
		}
	}
	
	public void finish() throws IOException {
		if (totalOut!=null) {
			totalOut.writef("%.0f\t%.0f\t%.0f\t%.0f\n",total[0],total[1],total[2],total[3]);
			totalOut.close();
		}
	}
	
	private static class BarcodedMismatchSequence implements CharSequence {

		private CountDnaSequence barcode;
		private String sequence;
		
		public BarcodedMismatchSequence(CountDnaSequence barcode, String sequence) {
			this.barcode = barcode;
			this.sequence = sequence;
		}
		
		@Override
		public int length() {
			return barcode.length()+sequence.length();
		}

		@Override
		public char charAt(int index) {
			if (index<barcode.length()) return barcode.charAt(index);
			index-=barcode.length();
			return sequence.charAt(index);
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			return toString().substring(start, end);
		}
		
		@Override
		public String toString() {
			return StringUtils.toString(this)+"\t"+barcode.getCount();
		}
		
		private int hashCode = 0;
		@Override
		public int hashCode() {
			if (hashCode==0) hashCode = StringUtils.hashCode(this);
			return hashCode;
		}
		
		@Override
		public boolean equals(Object obj) {
			return StringUtils.equals(this,obj);
		}

		public int getCount() {
			return barcode.getCount();
		}
		
	}
}