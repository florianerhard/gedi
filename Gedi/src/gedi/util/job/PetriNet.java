package gedi.util.job;

import gedi.util.datastructure.collections.SetStack;
import gedi.util.datastructure.collections.SimpleHistogram;
import gedi.util.job.petrinetmaker.PetriNetMaker;
import gedi.util.mutable.MutableInteger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import cern.colt.bitvector.BitVector;

public class PetriNet {

	private ArrayList<Place> places = new ArrayList<Place>();
	private ArrayList<Transition> transitions = new ArrayList<Transition>();
	
	private boolean prepared = false;
	private boolean dirty = false;
	/// indices
	private ArrayList<Place> sources = new ArrayList<Place>();
	private ArrayList<Place> sinks = new ArrayList<Place>();
	
	
	private HashMap<String,AnnotationMap> annotations = new HashMap<String, AnnotationMap>();
	
	public void prepare() {
		createMissingPlaces();
		for (Transition t : transitions)
			for (int i=0; i<t.getInDegree(); i++)
				if (t.getInput(i).getProducer()!=null) {
					t.getJob().setInput(i, t.getInput(i).getProducer().getJob());
					t.getInput(i).getProducer().getJob().addOutput(t.getJob());
				}
		prepared = true;
	}
	
	public boolean isPrepared() {
		return prepared;
	}

	public Place createPlace(Class<?> tokenClass) {
		if (prepared) throw new RuntimeException("Already prepared!");
		Place re = new Place(tokenClass, places.size());
		places.add(re);
		dirty = true;
		return re;
	}
	
	
	public Transition createTransition(Job job) {
		if (prepared) throw new RuntimeException("Already prepared!");
		Transition re = new Transition(job, transitions.size());
		transitions.add(re);
		dirty = true;
		return re;
	}
	
	public Transition connect(Place p, Transition t, int index) {
		if (prepared) throw new RuntimeException("Already prepared!");
		dirty = true;
		p.addConsumer(t);
		t.setInPlace(p, index);
		return t;
	}
	
	public Place connect(Transition t, Place p) {
		if (prepared) throw new RuntimeException("Already prepared!");
		dirty = true;
		p.setProducer(t);
		t.setOutPlace(p);
		return p;
	}
	
	
	public ArrayList<Place> getPlaces() {
		return places;
	}
	
	public ArrayList<Transition> getTransitions() {
		return transitions;
	}
	
	public ArrayList<Place> getSinks() {
		if (dirty) computeIndices();
		return sinks;
	}
	
	public ArrayList<Place> getSources() {
		if (dirty) computeIndices();
		return sources;
	}
	
	public ExecutionContext createExecutionContext() {
		if (!prepared) throw new RuntimeException("Petri net not prepared!");
		if (dirty) computeIndices();
		
		return new ExecutionContext(this);
	}
	
	public ArrayList<Transition> getTopologicalOrder() {
		ArrayList<Transition> re = new ArrayList<Transition>();
		SimpleHistogram<Transition> visitedInputs = new SimpleHistogram<Transition>();
		
		SetStack<Place> stack = new SetStack<Place>(getSources());
		while (!stack.isEmpty()) {
			Place n = stack.pop();
			for (Transition t : n.getConsumers()) {
				MutableInteger mi = visitedInputs.get(t);
				if (++mi.N==t.getInDegree()) {
					re.add(t);
					if (!stack.contained(t.getOutput()))
						stack.push(t.getOutput());
				}
			}
		}
		
		return re;
		
	}
	
	private void computeIndices() {
		createMissingPlaces();
		// compute sources and sinks
		for (Place p : places) {
			if (p.isSink())
				sinks.add(p);
			if (p.isSource())
				sources.add(p);
		}
		
		sinks.sort((a,b)->a.getId()-b.getId());
		sources.sort((a,b)->a.getId()-b.getId());
		
		
		dirty = false;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Place p : places){
			sb.append(p);
			sb.append("\n");
		}
		for (Transition t : transitions){
			sb.append(t);
			sb.append("\n");
		}
		return sb.toString();
	}


	public void createMissingPlaces() {
		for (Transition t : transitions)
			t.createMissingPlaces(this);
	}
	
	
	public <T> T getAnnotation(String name) {
		AnnotationMap<?, ?> re = annotations.get(name);
		if (re==null) throw new RuntimeException("Anotation "+name+" unknown!");
		return (T) re.get(this);
	}
	
	
	public <T> T getAnnotation(String name, Place p) {
		AnnotationMap<?, ?> re = annotations.get(name);
		if (re==null) throw new RuntimeException("Anotation "+name+" unknown!");
		return (T) re.get(p);
	}
	
	public <T> T getAnnotation(String name, Transition t) {
		AnnotationMap<?, ?> re = annotations.get(name);
		if (re==null) throw new RuntimeException("Anotation "+name+" unknown!");
		return (T) re.get(t);
	}
	
	public <T> void newAnnotation(String name, Class<T> cls) {
		annotations.put(name, new AnnotationMap<Object, T>(name, cls));
	}
	
	public <T> PetriNet setAnnotation(String name, T a) {
		AnnotationMap<Object, T> re = (AnnotationMap<Object, T>) annotations.get(name);
		if (re==null) throw new RuntimeException("Anotation "+name+" unknown!");
		re.put(this,a);
		return this;
	}
	
	public <T> PetriNet setAnnotation(String name, Place p, T a) {
		AnnotationMap<Object, T> re = (AnnotationMap<Object, T>) annotations.get(name);
		if (re==null) throw new RuntimeException("Anotation "+name+" unknown!");
		re.put(p,a);
		return this;
	}
	
	public <T> PetriNet setAnnotation(String name, Transition t, T a) {
		AnnotationMap<Object, T> re = (AnnotationMap<Object, T>) annotations.get(name);
		if (re==null) throw new RuntimeException("Anotation "+name+" unknown!");
		re.put(t,a);
		return this;
	}

	
	public PetriNet clone() {
		PetriNet re = new PetriNet();
		re.dirty = true;
		for (Place p : places)
			re.createPlace(p.getTokenClass());
		for (Transition t : transitions) {
			Transition tr = re.createTransition(t.getJob());
			for (int i=0; i<t.getInDegree(); i++)
				re.connect(re.places.get(t.getInput(i).getId()), tr, i);
			re.connect(tr,re.places.get(t.getOutput().getId()));
		}
		
		for (String a : annotations.keySet())
			re.annotations.put(a, annotations.get(a).clone(k->{
				if (k instanceof Place)
					return re.places.get(((Place)k).getId());
				if (k instanceof Transition)
					return re.transitions.get(((Transition)k).getId());
				if (k==this)
					return re;
				return k;
			},v->v));
		
		return re;
	}
	
	/**
	 * Only places/transitions set as true,unconnected transitions are ok, but no unconnected places.
	 * @param usePlace
	 * @param useTransition
	 * @return
	 */
	public PetriNet clone(BitVector usePlace, BitVector useTransition) {
		PetriNet re = new PetriNet();
		re.dirty = true;
		HashMap<Object, Object> map = new HashMap<Object,Object>();
		for (Place p : places) {
			if (usePlace.getQuick(p.getId())) {
				// check singleton
				boolean nosingle = useTransition.getQuick(p.getProducer().getId());
				for (int o=0; !nosingle && o<p.getConsumers().size(); o++)
					nosingle = useTransition.getQuick(p.getConsumers().get(o).getId());
				if (nosingle) {
					Place np = re.createPlace(p.getTokenClass());
					map.put(p,np);
				}
			}
		}
		for (Transition t : transitions) 
			if (useTransition.getQuick(t.getId())) {
				Transition tr = re.createTransition(t.getJob());
				map.put(t,tr);
				for (int i=0; i<t.getInDegree(); i++) {
					Place p = (Place) map.get(t.getInput(i));
					if (p!=null)
						re.connect(p, tr, i);
				}
				Place p = (Place) map.get(t.getOutput());
				if (p!=null)
					re.connect(tr,p);
			}
		map.put(this,re);
		
		for (String a : annotations.keySet())
			re.annotations.put(a, annotations.get(a).clone(k->map.get(k),v->v));
		
		return re;
	}
	
	/**
	 * Extract petri net's sinks will be a subset of the given sinks (and equal to it if none of the given sinks is necessary to supply any other
	 * given sink.
	 * @param sinks
	 * @return
	 */
	public PetriNet extractSinkSupplying(Collection<Place> sinks) {
		
		BitVector usePlace = new BitVector(places.size());
		BitVector useTransition = new BitVector(transitions.size());
		
		SetStack<Place> mustSupply = new SetStack<Place>(sinks);
		while (!mustSupply.isEmpty()) {
			Place n = mustSupply.pop();
			usePlace.putQuick(n.getId(), true);
			if (!n.isSource()) {
				useTransition.putQuick(n.getProducer().getId(), true);
				for (int i=0; i<n.getProducer().getInDegree(); i++)
					mustSupply.pushIfNeverContained(n.getProducer().getInput(i));
			}
		}
		
		return clone(usePlace, useTransition);
		
	}

	
}
