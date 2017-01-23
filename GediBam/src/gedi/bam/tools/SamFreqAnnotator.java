package gedi.bam.tools;

import gedi.util.FunctorUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.io.text.LineOrientedFile;
import htsjdk.samtools.SAMRecord;

import java.io.IOException;
import java.util.Iterator;
import java.util.function.UnaryOperator;

public class SamFreqAnnotator implements UnaryOperator<Iterator<SAMRecord>> {
	
	private int[] count;

	public SamFreqAnnotator(String freqFile) throws IOException {
		Iterator<String> it = new LineOrientedFile(freqFile).lineIterator();
		IntArrayList list = new IntArrayList();
		while (it.hasNext()) {
			String line = it.next();
			String[] s = StringUtils.split(line,'\t');
			list.add(Integer.parseInt(s[1]));
		}
		count = list.toIntArray();
	}
	
	
	public Iterator<SAMRecord> apply(Iterator<SAMRecord> it) {
		
		return FunctorUtils.sideEffectIterator(it,r->{
			r.setAttribute("XR",""+count[Integer.parseInt(r.getReadName())]);
		});
	}
}