package gedi.core.data.table;

import gedi.core.workspace.loader.WorkspaceItemLoader;
import gedi.util.io.text.tsv.formats.CsvReaderFactory;

import java.nio.file.Path;

public class CsvTableLoader implements WorkspaceItemLoader<Table> {
	

	public static final String[] extensions = {"csv","tsv"};
	
	@Override
	public String[] getExtensions() {
		return extensions;
	}
	
	@Override
	public boolean hasOptions() {
		return true;
	}

	@Override
	public void updateOptions(Path path) {
		
	}

	@Override
	public Table load(Path path) {
		return new CsvReaderFactory().createReader(path).readTable();
	}
	
//	
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	@Override
//	public Table load(Path path) {
//		try (LineIterator it = new LineIterator(Files.newInputStream(path),"#")) {
//			
//			// first, infer the separator!
//			String[] inferLines = new String[this.inferLines.get()];
//			int index = 0;
//			while (it.hasNext() && index<inferLines.length) 
//				inferLines[index++] = it.next();
//			
//			if (index==0) return null;
//					
//			char sep = inferSeparator(inferLines,index);
//			
//			String[][] fields = new String[index][];
//			for (int i=0; i<fields.length;i++) 
//				fields[i] = StringUtils.splitQuoted(inferLines[i], sep);
//			
//			int cols = fields[0].length;
//			
//			// infer types
//			int[] types = new int[cols];
//			for (int i=0; i<cols; i++) 
//				types[i] = inferType(fields,1,index,i); // start with 1 due to potential header!
//			
//			int minHeaderType = parsers.length-1;
//			boolean oneGreater = false;
//			for (int i=0; i<cols; i++) {
//				int ht = inferType(fields,0,1,i);
//				minHeaderType = Math.min(ht,minHeaderType);
//				oneGreater |= ht>types[i];
//			}
//			
//			boolean hasHeader = minHeaderType==parsers.length-1 || oneGreater;
//			
//			String[] headerNames = new String[cols];
//			int start = 0;
//			if (hasHeader) {
//				System.arraycopy(fields[0], 0, headerNames, 0, headerNames.length);
//				start = 1;
//			} else {
//				for (int i=0; i<headerNames.length; i++) 
//					headerNames[i] = "C"+StringUtils.padLeft(i+1+"", 1+(int)Math.log10(headerNames.length), '0');
//			}
//			
//			
//			// obtain and fill table	
//			String name = FileUtils.getFullNameWithoutExtension(path.getFileName());
//			String creator = getClass().getSimpleName();
//			BeanGenerator gen = new BeanGenerator();
//			gen.setNamingPolicy(new GediNamingPolicy(StringUtils.toJavaIdentifier(name)));
//			for (int i=0; i<cols; i++) {
//				String lab = StringUtils.toJavaIdentifier(headerNames[i]);
//				gen.addProperty(lab, classOrder[types[i]]);
//			}
//			Class cls = (Class) gen.createClass();
//			int version = Tables.getInstance().getMostRecentVersion(TableType.Temporary,name)+1;
//			Table tab = Tables.getInstance().create(TableType.Temporary,Tables.getInstance().buildMeta(name, creator, version, description.get(), cls));
//			
//			tab.beginAddBatch();
//			// first fill from inference array
//			for (int i=start; i<index; i++) {
//				String[] currentFields = fields[i];
//				tab.add(Orm.fromFunctor(cls, (fi)->parsers[types[fi]].apply(currentFields[fi])));
//			}
//			// then the remaining from the iterator
//			while (it.hasNext()) {
//				String[] currentFields = StringUtils.split(it.next(),sep);
//				if (currentFields.length!=cols) throw new RuntimeException("Invalid number of columns: "+StringUtils.concat(sep, currentFields));
//				tab.add(Orm.fromFunctor(cls, (fi)->parsers[types[fi]].apply(currentFields[fi])));
//			}
//			
//			
//			tab.endAddBatch();
//			
//			return tab;
//		
//		
//		
//		} catch (Exception e) {
//			Tables.log.log(Level.SEVERE, "Could not read CSV file "+path.toString(), e);
//			throw new RuntimeException("Could not read CSV file "+path.toString(), e);
//		}
//	}

//	
//	private static final Class[] classOrder = {boolean.class,int.class,long.class,double.class,MutableReferenceGenomicRegion.class,String.class};
//	private static final Parser[] parsers = {new BooleanParser(),new IntegerParser(),new LongParser(),new DoubleParser(), new ReferenceGenomicRegionParser<>(), new StringParser()};
//	private int inferType(String[][] fields, int start, int end, int c) {
//		int p = 0;
//		for (int i=start; i<end; i++) {
//			while (!canParse(parsers[p], fields[i][c]))
//				p++;
//			if (p==parsers.length-1)
//				return p;
//		}
//		return p;
//	}
//	
//	private boolean canParse(Parser p, String v) {
//		try {
//			p.apply(v);
//			return true;
//		} catch (Throwable t) {
//			return false;
//		}
//	}
//
//	private char inferSeparator(String[] lines, int n) {
//		if (n==0) return separators[0];
//		int[] c = new int[separators.length];
//		for (int i=0; i<separators.length; i++) {
//			char sep = separators[i];
//			c[i] = StringUtils.countQuoted(lines[0], sep);
//			for (int j=1; j<n; j++) {
//				int tc = StringUtils.countQuoted(lines[j], sep);
//				if (tc!=c[i]) {
//					c[i]=-1;
//					break;
//				}
//			}
//		}
//		
//		for (int i=0; i<c.length; i++) 
//			if (c[i]>0) return separators[i];
//		
//		for (int i=0; i<c.length; i++) 
//			if (c[i]==0) return separators[i];
//		
//		throw new RuntimeException("Cannot determine separator!");
//	}
//
	@Override
	public Class<Table> getItemClass() {
		return Table.class;
	}
	
	
//	private ObjectProperty<CsvHeaderPresent> header = new SimpleObjectProperty<CsvTableLoader.CsvHeaderPresent>(this, "header", CsvHeaderPresent.Infer);
//	private IntegerProperty inferLines = new SimpleIntegerProperty(this,"inferLines",100);
//	private StringProperty separator = new SimpleStringProperty(this,"separator","\t");
//	private StringProperty description = new SimpleStringProperty(this,"description","");
//	
	
	
}
