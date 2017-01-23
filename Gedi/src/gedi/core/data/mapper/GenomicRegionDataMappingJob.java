package gedi.core.data.mapper;

import java.util.function.Function;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.util.ReflectionUtils;
import gedi.util.job.ExecutionContext;
import gedi.util.job.Job;
import gedi.util.mutable.MutableHeptuple;
import gedi.util.mutable.MutableOctuple;
import gedi.util.mutable.MutablePair;
import gedi.util.mutable.MutableQuadruple;
import gedi.util.mutable.MutableQuintuple;
import gedi.util.mutable.MutableSextuple;
import gedi.util.mutable.MutableTriple;
import gedi.util.mutable.MutableTuple;

public class GenomicRegionDataMappingJob<FROM,TO> implements Job<TO> {
	
	public static final String REFERENCE = "GenomicRegionDataMappingJob.Reference";
	public static final String REGION = "GenomicRegionDataMappingJob.Region";
	public static final String PIXELMAPPING = "GenomicRegionDataMappingJob.PixelMapping";
	
	
	private GenomicRegionDataMapping annot;
	private GenomicRegionDataMapper<FROM,TO> mapper;
	
	private Function<MutableTuple,FROM> input;
	private Class[] inputclasses;
	private String id = null;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public GenomicRegionDataMappingJob(GenomicRegionDataMapper<FROM,TO> mapper) {
		this.mapper = mapper;
		mapper.setJob(this);
		GenomicRegionDataMapping[] a = mapper.getClass().getAnnotationsByType(GenomicRegionDataMapping.class);
		if (a.length!=1) throw new IllegalArgumentException("Mapper class does not have mandatory annotation GenomicRegionDataMapping");
		annot = a[0];
		inputclasses = annot.fromType();
		
		try {
			// second is to throw an exception on missing method
			if (annot.fromType().length==0 && (ReflectionUtils.findMethodAllowSuper(mapper.getClass(), "map", ReferenceSequence.class, GenomicRegion.class, PixelLocationMapping.class, Void.class))!=null) 
				input = t->null;
			else if (annot.fromType().length==1 && (ReflectionUtils.findMethodAllowSuper(mapper.getClass(), "map", ReferenceSequence.class, GenomicRegion.class, PixelLocationMapping.class, annot.fromType()[0]))!=null) 
				input = t->t.get(0);
			else if (annot.fromType().length==2 && (ReflectionUtils.findMethodAllowSuper(mapper.getClass(), "map", ReferenceSequence.class, GenomicRegion.class, PixelLocationMapping.class, MutablePair.class))!=null) 
				input = t->(FROM)new MutablePair(t.get(0), t.get(1));
			else if (annot.fromType().length==3 && (ReflectionUtils.findMethodAllowSuper(mapper.getClass(), "map", ReferenceSequence.class, GenomicRegion.class, PixelLocationMapping.class, MutableTriple.class))!=null) 
				input = t->(FROM)new MutableTriple(t.get(0), t.get(1), t.get(2));
			else if (annot.fromType().length==4 && (ReflectionUtils.findMethodAllowSuper(mapper.getClass(), "map", ReferenceSequence.class, GenomicRegion.class, PixelLocationMapping.class, MutableQuadruple.class))!=null) 
				input = t->(FROM)new MutableQuadruple(t.get(0), t.get(1), t.get(2), t.get(3));
			else if (annot.fromType().length==5 && (ReflectionUtils.findMethodAllowSuper(mapper.getClass(), "map", ReferenceSequence.class, GenomicRegion.class, PixelLocationMapping.class, MutableQuintuple.class))!=null) 
				input = t->(FROM)new MutableQuintuple(t.get(0), t.get(1), t.get(2), t.get(3), t.get(4));
			else if (annot.fromType().length==6 && (ReflectionUtils.findMethodAllowSuper(mapper.getClass(), "map", ReferenceSequence.class, GenomicRegion.class, PixelLocationMapping.class, MutableSextuple.class))!=null) 
				input = t->(FROM)new MutableSextuple(t.get(0), t.get(1), t.get(2), t.get(3), t.get(4), t.get(5));
			else if (annot.fromType().length==7 && (ReflectionUtils.findMethodAllowSuper(mapper.getClass(), "map", ReferenceSequence.class, GenomicRegion.class, PixelLocationMapping.class, MutableHeptuple.class))!=null) 
				input = t->(FROM)new MutableHeptuple(t.get(0), t.get(1), t.get(2), t.get(3), t.get(4), t.get(5), t.get(6));
			else if (annot.fromType().length==8 && (ReflectionUtils.findMethodAllowSuper(mapper.getClass(), "map", ReferenceSequence.class, GenomicRegion.class, PixelLocationMapping.class, MutableOctuple.class))!=null) 
				input = t->(FROM)new MutableOctuple(t.get(0), t.get(1), t.get(2), t.get(3), t.get(4), t.get(5), t.get(6), t.get(7));
			else if (annot.fromType().length>1 && (ReflectionUtils.findMethodAllowSuper(mapper.getClass(), "map", ReferenceSequence.class, GenomicRegion.class, PixelLocationMapping.class, MutableTuple.class))!=null)
				input = t->(FROM)t;
			else 
				throw new RuntimeException("No matching method found!");				
			
			
		} catch (Exception e) {
			throw new RuntimeException("Annotation does not match!",e);
		}
	}
	
	private int outputs = 0;
	public void addOutput(Job output) {
		outputs++;
	}
	public void setInput(int index, Job input) {
		if (input instanceof GenomicRegionDataMappingJob)
			mapper.setInput(index, ((GenomicRegionDataMappingJob)input).mapper);
	}

	@Override
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	@Override
	public boolean isDisabled() {
		if (mapper instanceof DisablingGenomicRegionDataMapper)
			return ((DisablingGenomicRegionDataMapper<?,?>)mapper).isDisabled();
		if (!mapper.hasSideEffect() && outputs==0)
			return true;
		return false;
	}
	
	public GenomicRegionDataMapper<FROM, TO> getMapper() {
		return mapper;
	}

	@Override
	public TO execute(ExecutionContext context, MutableTuple input) {
		if (input.size()!=inputclasses.length) throw new RuntimeException("Input tuple does not match annotation class definition!");
		
		FROM data  = this.input.apply(input);
		TO re = mapper.map(context.getContext(REFERENCE), context.getContext(REGION),context.getContext(PIXELMAPPING), data);
		return re;
	}

	@Override
	public Class[] getInputClasses() {
		return inputclasses;
	}

	@Override
	public Class getOutputClass() {
		return annot.toType();
	}
	
	public void setTupleSize(int size) {
		if (!isTupleInput())
			throw new RuntimeException("Not allowed!");
		
		inputclasses = new Class[size];
		for (int i = 0; i < inputclasses.length; i++) 
			inputclasses[i] = Object.class;
		
		input = t->(FROM)t;
				
	}


	public boolean isTupleInput() {
		return annot.fromType().length==1 && annot.fromType()[0]==MutableTuple.class;
	}
	

}
