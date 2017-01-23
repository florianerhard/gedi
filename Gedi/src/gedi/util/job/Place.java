package gedi.util.job;

import java.util.ArrayList;

public class Place {

	private Class<?> cls;
	private int id;
	private ArrayList<Transition> consumers = new ArrayList<Transition>();
	private Transition producer;
	
	
	Place(Class<?> cls, int id) {
		this.cls = cls;
		this.id = id;
	}
	
	Place addConsumer(Transition t) {
		consumers.add(t);
		return this;
	}
	
	Place setProducer(Transition t) {
		if (producer!=null) throw new RuntimeException("Only one producer allowed!");
		this.producer = t;
		return this;
	}
	
	public Class<?> getTokenClass() {
		return cls;
	}
	
	@Override
	public String toString() {
		return "{"+id+":"+cls.getSimpleName()+"}";
	}
	
	public boolean isSource() {
		return producer==null;
	}
	
	public boolean isSink() {
		return consumers.isEmpty();
	}
	
	public int getId() {
		return id;
	}
	
	public ArrayList<Transition> getConsumers() {
		return consumers;
	}
	
	public Transition getProducer() {
		return producer;
	}
	
}
