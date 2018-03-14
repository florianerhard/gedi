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
package gedi.util.io.text.tsv.formats;

import gedi.core.data.annotation.NameProvider;
import gedi.core.data.annotation.ScoreProvider;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.ArrayUtils;
import gedi.util.PaintUtils;
import gedi.util.StringUtils;

import java.awt.Color;
import java.util.Comparator;



/**
 *    1.  chrom - The name of the chromosome (e.g. chr3, chrY, chr2_random) or scaffold (e.g. scaffold10671).
   2. chromStart - The starting position of the feature in the chromosome or scaffold. The first base in a chromosome is numbered 0.
   3. chromEnd - The ending position of the feature in the chromosome or scaffold. The chromEnd base is not included in the display of the feature. For example, the first 100 bases of a chromosome are defined as chromStart=0, chromEnd=100, and span the bases numbered 0-99. 

The 9 additional optional BED fields are:

   4. name - Defines the name of the BED line. This label is displayed to the left of the BED line in the Genome Browser window when the track is open to full display mode or directly to the left of the item in pack mode.
   5. score - A score between 0 and 1000. If the track line useScore attribute is set to 1 for this annotation data set, the score value will determine the level of gray in which this feature is displayed (higher numbers = darker gray). This table shows the Genome Browser's translation of BED score values into shades of gray:
      shade 	  	  	  	  	  	  	  	  	 
      score in range   	≤ 166 	167-277 	278-388 	389-499 	500-611 	612-722 	723-833 	834-944 	≥ 945
   6. strand - Defines the strand - either '+' or '-'.
   7. thickStart - The starting position at which the feature is drawn thickly (for example, the start codon in gene displays).
   8. thickEnd - The ending position at which the feature is drawn thickly (for example, the stop codon in gene displays).
   9. itemRgb - An RGB value of the form R,G,B (e.g. 255,0,0). If the track line itemRgb attribute is set to "On", this RBG value will determine the display color of the data contained in this BED line. NOTE: It is recommended that a simple color scheme (eight colors or less) be used with this attribute to avoid overwhelming the color resources of the Genome Browser and your Internet browser.
  10. blockCount - The number of blocks (exons) in the BED line.
  11. blockSizes - A comma-separated list of the block sizes. The number of items in this list should correspond to blockCount.
  12. blockStarts - A comma-separated list of block starts. All of the blockStart positions should be calculated relative to chromStart. The number of items in this list should correspond to blockCount. 
 * @author erhard
 *
 */
public class BedEntry  {



	private static final char SCORE_SEPARATOR = '|';
	public String chromosome;
	public int start;
	public int end;
	public String name;
	public String score;
	public Strand strand;
	public int thickStart;
	public int thickEnd;
	public Color rgb;
	public int blockCount;
	public int[] blockSizes;
	public int[] blockStarts;
	public int cols = 3;

	private BedEntry() {}

	public BedEntry(ReferenceGenomicRegion<? extends NameProvider> rgr) {
		chromosome = rgr.getReference().getName();
		start = rgr.getRegion().getStart();
		end = rgr.getRegion().getEnd();
		name = rgr.getData().getName();
		score = (rgr.getData() instanceof ScoreProvider)?""+(int)((ScoreProvider)rgr.getData()).getScore():"0";
		strand = rgr.getReference().getStrand();
		cols = 6;
		if (rgr.getRegion().getNumParts()>1) {
			blockCount = rgr.getRegion().getNumParts();
			blockSizes = rgr.getRegion().getLengths();
			blockStarts = rgr.getRegion().getStarts();
			correctBlockStarts();
			cols = 12;
		}
	}
	
	public BedEntry(Object[] values) {
		parseValues(values);
	}

	
	public static BedEntry parseValues(String line) {
		return parseValues(StringUtils.split(line, '\t'));
		
	}
	
	public static BedEntry parseValues(String[] values) {
		BedEntry re = new BedEntry();
		re.chromosome = (String) values[0];
		re.start = Integer.parseInt(values[1]);
		re.end =Integer.parseInt(values[2]);
		re.name = values.length>3?(String)values[3]:"";
		re.score = values.length>4?(String)values[4]:"0";
		re.strand = values.length>5?Strand.parse(values[5]):Strand.Independent;
		re.thickStart = values.length>6&&values[6].length()>0?Integer.parseInt(values[6]):-1;
		re.thickEnd = values.length>7&&values[7].length()>0?Integer.parseInt(values[7]):-1;
		re.rgb = values.length>8&&values[8].length()>0?PaintUtils.parseColor(values[8]):null;
		re.blockCount = values.length>9&&values[9].length()>0?Integer.parseInt(values[9]):-1;
		re.blockSizes = values.length>10&&values[10].length()>0?ArrayUtils.parseIntArray(values[10].endsWith(",")?values[10].substring(0,values[10].length()-1):values[10],','):null;
		re.blockStarts = values.length>11&&values[11].length()>0?ArrayUtils.parseIntArray(values[11].endsWith(",")?values[11].substring(0,values[11].length()-1):values[11],','):null;
		re.cols = values.length;
		re.correctBlockStarts();
		return re;
	}
	
	private void correctBlockStarts() {
		if (blockStarts!=null) {
			if (blockStarts.length==0) throw new RuntimeException("No blocks defined");
			if (blockStarts[0]==start) {
				for (int i=0; i<blockStarts.length; i++)
					blockStarts[i]-=start;
			}
			if (blockStarts[0]!=0) throw new RuntimeException("First blockstart is neither 0 nor chromStart!");
		}
	}

	public BedEntry parseValues(Object[] values) {
		this.chromosome = (String) values[0];
		this.start = (Integer)values[1];
		this.end = (Integer)values[2];
		this.name = values.length>3?(String)values[3]:"";
		this.score = values.length>4?(String)values[4]:"0";
		this.strand = values.length>5?(Strand)values[5]:Strand.Independent;
		cols = 6;
		this.thickStart = values.length>6&&values[6]!=null?(Integer)values[6]:-1;
		if (thickStart!=-1) cols = 7;
		this.thickEnd = values.length>7&&values[7]!=null?(Integer)values[7]:-1;
		if (thickEnd!=-1) cols = 8;
		this.rgb = values.length>8&&values[8]!=null?PaintUtils.parseColor(values[8]):null;
		if (rgb!=null) cols = 9;
		this.blockCount = values.length>9&&values[9]!=null?(Integer)values[9]:-1;
		if (blockCount!=-1) cols = 10;
		this.blockSizes = values.length>10&&values[10]!=null?(int[])values[10]:null;
		if (blockSizes!=null) cols = 11;
		this.blockStarts = values.length>11&&values[11]!=null?(int[])values[11]:null;
		if (blockStarts!=null) cols = 12;
		correctBlockStarts();
		return this;
	}
	
	public int getCols() {
		return cols;
	}
	
	

	public String getChromosome() {
		return chromosome;
	}
	public String getStrand() {
		return strand.toString();
	}
	
	public void setStrand(String strand) {
		this.strand = Strand.parse(strand);
	}
	public void setChromosome(String chromosome) {
		this.chromosome = chromosome;
	}
	public int getStart() {
		return start;
	}
	public void setStart(int start) {
		this.start = start;
	}
	public int getEnd() {
		return end;
	}
	public void setEnd(int end) {
		this.end = end;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
		cols = Math.max(cols, 4);
	}
	public String getScore() {
		return score;
	}
	public String getScore(int component) {
		int p = 0;
		int i=0;
		for (int sepIndex=score.indexOf(SCORE_SEPARATOR); sepIndex>=0; sepIndex = score.substring(p).indexOf(SCORE_SEPARATOR)) {
			if (i++==component)
				return score.substring(p,p+sepIndex);
			p += sepIndex+1;
		}
		if (i==component)
			return score.substring(p);
		return null;
	}
	public String getName(int component) {
		int p = 0;
		int i=0;
		for (int sepIndex=name.indexOf(SCORE_SEPARATOR); sepIndex>=0; sepIndex = name.substring(p).indexOf(SCORE_SEPARATOR)) {
			if (i++==component)
				return name.substring(p,p+sepIndex);
			p += sepIndex+1;
		}
		if (i==component)
			return name.substring(p);
		return null;
	}
	
	public int getIntScore() {
		return Integer.parseInt(score);
	}
	public int getIntScore(int component) {
		return Integer.parseInt(getScore(component));
	}
	
	public double getDoubleScore() {
		return Double.parseDouble(score);
	}
	public double getDoubleScore(int component) {
		return Double.parseDouble(getScore(component));
	}
	
	
	public void setScore(String score) {
		this.score = score;
		cols = Math.max(cols, 5);
	}
	
	
	public int getThickStart() {
		return thickStart;
	}
	public void setThickStart(int thickStart) {
		this.thickStart = thickStart;
		cols = Math.max(cols, 7);
	}
	public int getThickEnd() {
		return thickEnd;
	}
	public void setThickEnd(int thickEnd) {
		this.thickEnd = thickEnd;
		cols = Math.max(cols, 8);
	}
	
	public boolean hasThick() {
		return cols>=8;
	}
	
	public Color getRgb() {
		return rgb;
	}
	public void setRgb(Color rgb) {
		this.rgb = rgb;
		cols = 9;
	}
	public int getBlockCount() {
		return blockCount;
	}
	public void setBlockCount(int blockCount) {
		this.blockCount = blockCount;
		cols = Math.max(cols, 10);
	}
	public int[] getBlockSizes() {
		return blockSizes;
	}
	public void setBlockSizes(int[] blockSizes) {
		this.blockSizes = blockSizes;
		cols = Math.max(cols, 11);
	}
	public int[] getBlockStarts() {
		return blockStarts;
	}
	public void setBlockStarts(int[] blockStarts) {
		this.blockStarts = blockStarts;
		correctBlockStarts();
		cols = Math.max(cols, 12);
	}
	
	public boolean hasBlocks() {
		return cols>=12;
	}

	public int getSize() {
		return end-start;
	}

	protected BedEntry createNewInstance() {
		return new BedEntry();
	}
	
	public BedEntry clone() {
		BedEntry re = createNewInstance();
		re.chromosome = chromosome;
		re.start = start;
		re.end = end;
		re.name = name;
		re.score = score;
		re.strand = strand;
		re.thickEnd = thickEnd;
		re.thickStart = thickStart;
		re.rgb = rgb;
		re.blockCount = blockCount;
		re.blockSizes = blockSizes==null?null:blockSizes.clone();
		re.blockStarts = blockStarts==null?null:blockStarts.clone();
		re.cols = cols;
		return re;
	}

	@Override
	public String toString() {
		return toString(cols);
	}

	public String toString(int cols) {
		StringBuilder sb = new StringBuilder();
		sb.append(chromosome);
		sb.append('\t');
		sb.append(this.start);
		sb.append('\t');
		sb.append(this.end);
		if (cols>3) {
			sb.append('\t');
			sb.append(this.name);
		}
		if (cols>4) {
			sb.append('\t');
			sb.append(this.score);
		}
		if (cols>5) {
			sb.append('\t');
			sb.append(strand.toString());
		}
		if (cols>6) {
			sb.append('\t');
			sb.append(this.thickStart);
		}
		if (cols>7) {
			sb.append('\t');
			sb.append(this.thickEnd);
		}
		if (cols>8) {
			sb.append('\t');
			sb.append(this.rgb==null?"0":PaintUtils.toIntRgb(this.rgb));
		}
		if (cols>9) {
			sb.append('\t');
			sb.append(this.blockCount);
		}
		if (cols>10) {
			sb.append('\t');
			sb.append(StringUtils.concat(",",this.blockSizes));
			sb.append(',');
		}
		if (cols>11) {
			sb.append('\t');
			sb.append(StringUtils.concat(",",this.blockStarts));
			sb.append(',');
		}
		return sb.toString();
	}

	
	public static class ByEndComparator implements Comparator<BedEntry> {

		@Override
		public int compare(BedEntry o1, BedEntry o2) {
			return o1.getEnd()-o2.getEnd();
		}

	}


	public static class ByStartComparator implements Comparator<BedEntry> {

		@Override
		public int compare(BedEntry o1, BedEntry o2) {
			return o1.getStart()-o2.getStart();
		}

	}
	

	public static class StrandAwareByChromosomeComparator implements Comparator<BedEntry> {

		@Override
		public int compare(BedEntry o1, BedEntry o2) {
			
			
			int re = ReferenceSequence.compareChromosomeNames(o1.getChromosome(), o2.getChromosome());
			if (re==0)
				re = Strand.compare(o1.strand,o2.strand);
			if (re==0)
				re = o1.getStart()-o2.getStart();
			if (re==0)
				re = o1.getEnd()-o2.getEnd();
			return re;
		}

	}
	
	public static class StrandLaterByChromosomeComparator implements Comparator<BedEntry> {

		@Override
		public int compare(BedEntry o1, BedEntry o2) {
			int re = ReferenceSequence.compareChromosomeNames(o1.getChromosome(), o2.getChromosome());
			if (re==0)
				re = o1.getStart()-o2.getStart();
			if (re==0)
				re = o1.getEnd()-o2.getEnd();
			if (re==0)
				re = Strand.compare(o1.strand,o2.strand);
			return re;
		}

	}
	


	public int getIntersectionSize(BedEntry bedEntry) {
		if (!getChromosome().equals(bedEntry.getChromosome()) || strand!=bedEntry.strand)
			return 0;
		else return Math.min(end,bedEntry.end)-Math.max(start, bedEntry.start);
	}

	public boolean isPositiveStrand() {
		return strand==Strand.Plus;
	}


	public GenomicRegion getGenomicRegion() {
		if (blockCount==-1)	return new ArrayGenomicRegion(start, end);
		
		int[] coords = new int[blockStarts.length*2];
		for (int i=0; i<blockStarts.length; i++) {
			coords[i*2] = start+blockStarts[i];
			coords[i*2+1] = start+blockStarts[i]+blockSizes[i];
		}
		
		return new ArrayGenomicRegion(coords);
	}
	
	public ReferenceSequence getReferenceSequence() {
		return Chromosome.obtain(getChromosome(), getStrand());
	}
	
	public MutableReferenceGenomicRegion<BedEntry> getReferenceGenomicRegion(MutableReferenceGenomicRegion<BedEntry> re) {
		return re.set(getReferenceSequence(), getGenomicRegion(), this);
	}
}
