package gedi.util.job;

import gedi.util.mutable.MutableTuple;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;


/**
 * For simulation: {@link PetriNet#createExecutionContext()}, then add context items (e.g. ReferenceSequence, GenomicRegion), create a scheduler and
 * push it into {@link ExecutorLoops}.
 * 
 * @author erhard
 *
 */
public class ExecutionContext {

	
	public static final String UID = "UID";

	private PetriNet pn;

	private HashMap<String,AnnotationMap<?,?>> context = new HashMap<String, AnnotationMap<?,?>>();
	private HashMap<Place,Object> tokens = new HashMap<Place, Object>();
	private Set<Transition> disabled = Collections.synchronizedSet(new HashSet<Transition>());
	
	private boolean executing = false;
	
	private int execution = 0;
	
	ExecutionContext(PetriNet pn) {
		this.pn = pn;
	}
	
	/**
	 * Context stays
	 * @return
	 */
	public ExecutionContext reset() {
		if (!executing) return this;
		executing = false;
		execution++;
		tokens.clear();
		disabled.clear();
		return this;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String n : context.keySet()) {
			if (context.get(n).containsKey(this)) {
				if (sb.length()>0) sb.append(",");
				sb.append(n+": "+context.get(n).get(this));
			}
		}
		return sb.toString();
	}
	
	public boolean isDisabled(Transition t) {
		return disabled.contains(t);
	}
	
	public void setDisabled(Transition t, boolean disabled) {
		if (disabled) this.disabled.add(t);
		else this.disabled.remove(t);
	}
	
	/**
	 * Disables all transitions that need not to fire in order to reach all reachable sinks (a sink is not reachable, if a manually disabled 
	 * transition is necesssary).
	 */
	public void disableUnneccessary() {
		
		Stack<Transition> dfs = new Stack<Transition>();
		dfs.addAll(disabled);
		while (!dfs.isEmpty()) {
			Transition t = dfs.pop();
			
			// disable all successors
			for (Transition s : t.getOutput().getConsumers()) {
				if (disabled.add(s))
					dfs.push(s);
			}
			
			// disable predecessors, if they do not have any enabled successor
			for (int i=0; i<t.getInDegree(); i++) {
				Place p = t.getInput(i);
				if (p.getProducer()!=null && disabled.containsAll(p.getConsumers())) {
					if (disabled.add(p.getProducer()))
						dfs.push(p.getProducer());
				}
			}
		}
		
	}
	
	public Set<Transition> getDisabledTransitions() {
		return disabled;
	}
	
	public int startExecution() {
		executing = true;
		return getExecutionId();
	}
	
	public int getExecutionId() {
		return execution;
	}
	
	
	public boolean needsInput(Place p) {
		return !p.isSink() && p.getTokenClass()!=Void.class && !containsToken(p);
	}

	public boolean isReady(Transition t) {
		if (disabled.contains(t)) return false;
		for (int i=0; i<t.getInDegree(); i++)
			if (t.getInput(i).getTokenClass()!=Void.class && !containsToken(t.getInput(i)))
				return false;
		
		if (!containsToken(t.getOutput()))
			return true;
		
		return false;
	}
	
	public MutableTuple createInput(Transition transition) {
		if (!executing) throw new RuntimeException("Call startExecution first!");
		MutableTuple re = new MutableTuple(transition.getJob().getInputClasses());
		for (int i=0; i<re.size(); i++) 
			re.set(i, getToken(transition.getInput(i)));
		return re;
	}
	
	
	public void putToken(Place o, Object token) {
		if (!executing) throw new RuntimeException("Call startExecution first!");
		synchronized (tokens) {
			if (!tokens.containsKey(o)) 
				tokens.put(o, token);
		}
	}
	
	
	public boolean isFinished() {
		synchronized (tokens) {
			for (Place p : pn.getSinks())
				if (!tokens.containsKey(p))
					return false;
			return true;
		}
	}
	
	public boolean containsToken(Place p) {
		synchronized (tokens) {
			return tokens.containsKey(p);
		}
	}
	
	public <T> T getToken(Place p) {
		synchronized (tokens) {
			return (T) tokens.get(p);
		}
	}
	
	public PetriNet getPetrNet() {
		return pn;
	}
	
	
	
	public <T> T getContext(String name) {
		AnnotationMap<?, ?> re = context.get(name);
		if (re==null) throw new RuntimeException("Annotation "+name+" unknown!");
		return (T) re.get(this);
	}
	
	
	public <T> T getContext(String name, Place p) {
		AnnotationMap<?, ?> re = context.get(name);
		if (re==null) throw new RuntimeException("Annotation "+name+" unknown!");
		return (T) re.get(p);
	}
	
	public <T> T getContext(String name, Transition t) {
		AnnotationMap<?, ?> re = context.get(name);
		if (re==null) throw new RuntimeException("Annotation "+name+" unknown!");
		return (T) re.get(t);
	}
	
	public <T> ExecutionContext newContext(String name, Class<T> cls) {
		context.put(name, new AnnotationMap<Object, T>(name, cls));
		return this;
	}
	
	public <T> ExecutionContext setContext(String name, T a) {
		if (executing) throw new RuntimeException("Call reset first!");
		
		AnnotationMap<Object, T> re = (AnnotationMap<Object, T>) context.get(name);
		if (re==null) throw new RuntimeException("Annotation "+name+" unknown!");
		re.put(this,a);
		return this;
	}
	
	public <T> ExecutionContext setContext(String name, Place p, T a) {
		if (executing) throw new RuntimeException("Call reset first!");
		AnnotationMap<Object, T> re = (AnnotationMap<Object, T>) context.get(name);
		if (re==null) throw new RuntimeException("Annotation "+name+" unknown!");
		re.put(p,a);
		return this;
	}
	
	public <T> ExecutionContext setContext(String name, Transition t, T a) {
		if (executing) throw new RuntimeException("Call reset first!");
		AnnotationMap<Object, T> re = (AnnotationMap<Object, T>) context.get(name);
		if (re==null) throw new RuntimeException("Annotation "+name+" unknown!");
		re.put(t,a);
		return this;
	}

	

	
	
}
