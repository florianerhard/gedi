package gedi.region.bam;

import gedi.core.data.reads.ContrastMapping;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.io.text.LineOrientedFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BamMerge {

	private String name;
	private String[] originalNames;
	private String[] mappedConditionNames;
	private ContrastMapping mapping;
	
	
	public static BamMerge fromFilesRegex(String regex, String...files) throws IOException {
		LinkedHashMap<String,String> mappings = new LinkedHashMap<String, String>();
		LinkedHashSet<String> mappedConditionNames = new LinkedHashSet<String>();
		Pattern rgx = Pattern.compile(regex);
		
		for (String f : files) {
			Matcher m = rgx.matcher(f);
			if (m.find()) {
				mappings.put(f,m.group());
				mappedConditionNames.add(m.group());
			}
		}
		

		BamMerge re = new BamMerge();
		
		re.mappedConditionNames = mappedConditionNames.toArray(new String[0]);
		re.originalNames = mappings.keySet().toArray(new String[0]);
		
		re.mapping = new ContrastMapping();
		HashMap<String, Integer> index = ArrayUtils.createIndexMap(re.mappedConditionNames);
		int i = 0;
		for (String f : mappings.keySet()) 
			re.mapping.addMapping(i++, index.get(mappings.get(f)),mappings.get(f));
		re.name = regex;
		
		return re;
	}
	
	public static BamMerge fromFile(String path) throws IOException {
		return fromFile(path,new int[0]);
	}
	public static BamMerge fromFile(String path, int... lines) throws IOException {
		
		LinkedHashMap<String,String> mappings = new LinkedHashMap<String, String>();
		LinkedHashSet<String> mappedConditionNames = new LinkedHashSet<String>();
		
		Arrays.sort(lines);
		int ind = 0;
		
		Iterator<String> it = new LineOrientedFile(path).lineIterator();
		int line = 0;
		while (it.hasNext()) {
			String[] p = StringUtils.split(it.next(), '\t');
			if (lines.length==0|| line==lines[ind]) {
				mappings.put(p[0],p[1]);
				mappedConditionNames.add(p[1]);
				if (++ind>=lines.length && lines.length>0) 
					break;
			}
			line++;
		}
		
		BamMerge re = new BamMerge();
		
		re.mappedConditionNames = mappedConditionNames.toArray(new String[0]);
		re.originalNames = mappings.keySet().toArray(new String[0]);
		re.name = FileUtils.getNameWithoutExtension(path);
		
		re.mapping = new ContrastMapping();
		HashMap<String, Integer> index = ArrayUtils.createIndexMap(re.mappedConditionNames);
		int i = 0;
		for (String f : mappings.keySet()) 
			re.mapping.addMapping(i++, index.get(mappings.get(f)),mappings.get(f));
		
		return re;
	}

	public String[] getOriginalNames() {
		return originalNames;
	}
	
	public ContrastMapping getMapping() {
		return mapping;
	}
	
	public String[] getMappedConditionNames() {
		return mappedConditionNames;
	}

	public String getName() {
		return name;
	}

	
}
