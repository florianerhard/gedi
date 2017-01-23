package gedi.util.io.text.genbank;
 
import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegion;

import java.io.IOException;
import java.util.Arrays;


public class ComplementFeaturePosition extends AbstractFeaturePosition {
	
	final static String prefix = "complement(";
	
	
	private GenbankFeaturePosition subPosition;
	
	public ComplementFeaturePosition(GenbankFeature feature, String descriptor) {
		super(feature, descriptor);
		
		
		if (!descriptor.startsWith(prefix) || !descriptor.endsWith(")"))
			throw new RuntimeException("does not match "+prefix+".*)!");
		
		String subDescriptor = descriptor.substring(prefix.length(),descriptor.length()-1);
		
		subPosition = feature.parsePosition(subDescriptor);
		leftMost = subPosition.getStartInSource();
		rightMost = subPosition.getEndInSource();
	}
	
	@Override
	public String extractFeatureFromSource() throws IOException {
		return getDnaReverseComplement(subPosition.extractFeatureFromSource());
	}
	
	@Override
	public String extractDownstreamFromSource(int numBases) throws IOException {
		return getDnaReverseComplement(subPosition.extractUpstreamFromSource(numBases));
	}

	@Override
	public String extractUpstreamFromSource(int numBases) throws IOException {
		return getDnaReverseComplement(subPosition.extractDownstreamFromSource(numBases));
	}
	
	
	private static final char[] compl_nucleotides = {'T','G','C','A'};
	private static final int[] inv_nucleotides = new int[256];
	static {
		Arrays.fill(inv_nucleotides,-1);
		inv_nucleotides['A'] = inv_nucleotides['a'] = 0;
		inv_nucleotides['C'] = inv_nucleotides['c'] = 1;
		inv_nucleotides['G'] = inv_nucleotides['g'] = 2;
		inv_nucleotides['T'] = inv_nucleotides['t'] = 3;
		
		inv_nucleotides['U'] = inv_nucleotides['u'] = 3;
	}
	private static String getDnaReverseComplement(String dnaSequence) {
		dnaSequence = dnaSequence.toLowerCase();
		StringBuilder sb = new StringBuilder(dnaSequence.length());
		for (int i=dnaSequence.length()-1; i>=0; i--)
			sb.append(getDnaComplement(dnaSequence.charAt(i)));
		return sb.toString();
	}
	
	
	private static char getDnaComplement(char nucleotide) {
		return compl_nucleotides[inv_nucleotides[nucleotide]];
	}
	
	@Override
	public boolean isExact() {
		return subPosition.isExact();
	}
	
	@Override
	public GenbankFeaturePosition[] getSubPositions() {
		return new GenbankFeaturePosition[] {subPosition};
	}
	
	@Override
	public GenomicRegion toGenomicRegion() {
		return subPosition.toGenomicRegion();
	}
	
	@Override
	public Strand getStrand() {
		return subPosition.getStrand().toOpposite();
	}
	

}
