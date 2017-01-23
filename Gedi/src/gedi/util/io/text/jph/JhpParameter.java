package gedi.util.io.text.jph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class JhpParameter {

	private ArrayList<ArrayList<JhpVariable>[]> params = new ArrayList<>();
	
	public JhpParameter() {
		nextFile();
	}
	
	public void nextFile() {
		params.add(new ArrayList[] {new ArrayList<JhpVariable>(),new ArrayList<JhpVariable>()});
	}
	
	public void varin(String name, String description, boolean mandatory) {
		params.get(params.size()-1)[0].add(new JhpVariable(name, description, mandatory));
	}

	public void varout(String name, String description) {
		params.get(params.size()-1)[1].add(new JhpVariable(name, description, true));
	}

	public String getUsage(String...presentVars) {
		HashSet<String> present = new HashSet<>(Arrays.asList(presentVars));
		StringBuilder sb = new StringBuilder();
		
		sb.append("Input:\n");
		for (ArrayList<JhpVariable>[] file : params) {
			
			for (JhpVariable in : file[0]) {
				if (!present.contains(in.name)) {
					sb.append(" ").append(in.name).append("\t\t").append(in.description);
					if (!in.mandatory) sb.append(" [optional]");
					sb.append("\n");
				}
				present.add(in.name);
			}
			
			for (JhpVariable in : file[1])
				present.add(in.name);
			
		}
		
		sb.append("\n");
		present.clear();
		sb.append("Output:\n");
		for (ArrayList<JhpVariable>[] file : params) {
			
			for (JhpVariable in : file[0])
				present.add(in.name);
			
			for (JhpVariable in : file[1]) {
				if (!present.contains(in.name)) {
					sb.append(" ").append(in.name).append("\t\t").append(in.description);
					if (!in.mandatory) sb.append(" [optional]");
					sb.append("\n");
				}
				present.add(in.name);
			}
			
		}
		
		return sb.toString();
	}


	private static class JhpVariable {
		String name;
		String description;
		boolean mandatory;
		public JhpVariable(String name, String description, boolean mandatory) {
			this.name = name;
			this.description = description;
			this.mandatory = mandatory;
		}
		
	}
	
}
