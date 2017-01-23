package gedi.util.io.text.genbank;

import gedi.util.functions.ExtendedIterator;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public class GenbankFeatureIterator implements ExtendedIterator<GenbankFeature> {

	private int currentLine;
	private GenbankFile file;
	private Set<String> featureNames;
	
	private GenbankFeature nextFeature;
	private String[] features;
	private String featureIndent;
	
	public GenbankFeatureIterator(GenbankFile file, String[] featureNames) throws IOException {
		this.file = file;
		this.featureNames = new HashSet<String>(Arrays.asList(featureNames));
		
		features = file.loadFeatures();
		featureIndent = features[0];
		featureIndent = featureIndent.substring(0,featureIndent.indexOf(featureIndent.trim()));
	}

	@Override
	public boolean hasNext() {
		lookAhead();
		return nextFeature!=null;
	}

	@Override
	public GenbankFeature next() {
		lookAhead();
		GenbankFeature re = nextFeature;
		nextFeature = null;
		return re;
	}
	
	
	private void lookAhead() {
		
		if (currentLine>=features.length)
			return;
		
		while (nextFeature==null && currentLine<features.length) {
			String line = features[currentLine].trim();
			int spaceIndex = line.indexOf(' ');
			
			String name = spaceIndex>=0 ? line.substring(0,spaceIndex) : line;
			StringBuilder posBuilder = new StringBuilder();
			posBuilder.append(spaceIndex>=0 ? line.substring(spaceIndex).trim() : "");
			
			while (!features[currentLine+1].trim().startsWith("/") && Character.isWhitespace(features[currentLine+1].charAt(featureIndent.length()+1)))
				posBuilder.append(features[++currentLine].trim());
			
			
			// read forward to next feature
			int nextCurrentLine = ++currentLine;
			for (; nextCurrentLine<features.length && features[nextCurrentLine].startsWith(featureIndent) && Character.isWhitespace(features[nextCurrentLine].charAt(featureIndent.length()+1)); nextCurrentLine++);
			String[] mapSlice = Arrays.copyOfRange(features, currentLine, nextCurrentLine);
			
			currentLine = nextCurrentLine;
			
			if (featureNames.isEmpty() || featureNames.contains(name))
				nextFeature = new GenbankFeature(file,name,posBuilder.toString(),mapSlice);
		}
	}


}
