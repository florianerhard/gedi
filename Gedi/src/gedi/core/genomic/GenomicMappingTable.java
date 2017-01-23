package gedi.core.genomic;

import gedi.util.datastructure.dataframe.DataColumn;
import gedi.util.datastructure.dataframe.DataFrame;
import gedi.util.io.text.tsv.formats.CsvReader;
import gedi.util.io.text.tsv.formats.CsvReaderFactory;
import gedi.util.mutable.MutablePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Supplier;

public class GenomicMappingTable {

	private String id;
	
	private ArrayList<Supplier<DataFrame>> tablers = new ArrayList<>();
		
	private HashMap<String,HashSet<DataFrame>> tables;
	private HashMap<MutablePair<String,String>,HashMap<String,String>> cache = new HashMap<>();
	
	
	public GenomicMappingTable(String id) {
		this.id = id;
	}
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void merge(GenomicMappingTable other) {
		tables = null;
		cache.clear();
		tablers.addAll(other.tablers);
		
	}
	
	public String getId() {
		return id;
	}

	
	
	public Function<String,String> get(String from, String to) {
		MutablePair<String, String> key = new MutablePair<String, String>(from,to);
		if (!cache.containsKey(key)) {
			if (tables==null) createTable();
			
			HashSet<DataFrame> fs = tables.get(from);
			HashSet<DataFrame> ts = tables.get(to);
			
			HashSet<DataFrame> use = new HashSet<DataFrame>(fs);
			use.retainAll(ts);
			
			if (use.isEmpty()) return null;
			
			HashMap<String,String> map = new HashMap<>();
			for (DataFrame f : use) {
				DataColumn<?> fc = f.getColumn(from);
				DataColumn<?> tc = f.getColumn(to);
				for (int r=0; r<f.rows(); r++)
					map.put(fc.getFactorValue(r).name(), tc.getFactorValue(r).name());
			}
			cache.put(key, map);
		}
		HashMap<String, String> m = cache.get(key);
		return f->m.get(f);
	}

	
	private void createTable() {
		tables = new HashMap<>();
		for (Supplier<DataFrame> t : tablers) {
			DataFrame df = t.get();
			for (int c=0; c<df.columns(); c++)
				tables.computeIfAbsent(df.getColumn(c).name(), x->new HashSet<>()).add(df);
		}
			
	}

	
	public void addCsv(String file, String[] csvFields) {
		
		tablers.add(()->{
			CsvReader reader = new CsvReaderFactory().createReader(file);
			if (reader.getHeader().size()!=csvFields.length) throw new RuntimeException(file+" does not contain the correct number of fields!");
			
			return reader.readDataFrame(csvFields);
		});
			
	}

	
	
}
