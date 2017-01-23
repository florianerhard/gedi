package gems.test;

import java.io.IOException;

import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;

public class CheckSort {

	public static void main(String[] args) throws IOException {
		LineIterator it = new LineOrientedFile(LineOrientedFile.STDOUT).lineIterator();
		
		
		
		MutableReferenceGenomicRegion tmp;
		MutableReferenceGenomicRegion curr = new MutableReferenceGenomicRegion();
		MutableReferenceGenomicRegion last = new MutableReferenceGenomicRegion();
		while (it.hasNext()) {
			curr.parse(it.next());
			if (last.getReference()!=null) {
				if (last.compareTo(curr)>=0)
					throw new RuntimeException(last+" >= "+curr);
			}
			
			tmp = curr;
			curr = last;
			last = tmp;
			
		}
		
	}
	
}
