package gedi.util.plotting;

import java.util.HashMap;

import gedi.core.data.table.Table;
import gedi.util.datastructure.dataframe.DataFrame;
import gedi.util.functions.EI;

public class Aes {

	private String name;
	private int index;
	private String value;
	
	public Aes(String name, int index) {
		this.name = name;
		this.index = index;
	}
	
	public Aes(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	public String getName() {
		return name;
	}
	
	public String getValue(DataFrame df) {
		if (value!=null)
			return value;
		return df.getColumn(index).name();
	}

	public static Aes x(int index) {
		return new Aes("x",index);
	}
	public static Aes x(String name) {
		return new Aes("x",name);
	}
	
	public static Aes y(int index) {
		return new Aes("y",index);
	}
	public static Aes y(String name) {
		return new Aes("y",name);
	}

	public static Aes alpha(int index) {
		return new Aes("alpha",index);
	}
	public static Aes alpha(String name) {
		return new Aes("alpha",name);
	}


	public static Aes color(int index) {
		return new Aes("color",index);
	}
	public static Aes color(String name) {
		return new Aes("color",name);
	}

	public static Aes fill(int index) {
		return new Aes("fill",index);
	}
	public static Aes fill(String name) {
		return new Aes("fill",name);
	}


	public static Aes linetype(int index) {
		return new Aes("linetype",index);
	}
	public static Aes linetype(String name) {
		return new Aes("linetype",name);
	}

	public static Aes size(int index) {
		return new Aes("size",index);
	}
	public static Aes size(String name) {
		return new Aes("size",name);
	}

	public static Aes weight(int index) {
		return new Aes("weight",index);
	}
	public static Aes weight(String name) {
		return new Aes("weight",name);
	}

	public static Aes xmin(int index) {
		return new Aes("xmin",index);
	}
	public static Aes xmin(String name) {
		return new Aes("xmin",name);
	}

	public static Aes xmax(int index) {
		return new Aes("xmax",index);
	}
	public static Aes xmax(String name) {
		return new Aes("xmax",name);
	}

	public static Aes ymin(int index) {
		return new Aes("ymin",index);
	}
	public static Aes ymin(String name) {
		return new Aes("ymin",name);
	}
	
	public static Aes ymax(int index) {
		return new Aes("ymax",index);
	}
	public static Aes ymax(String name) {
		return new Aes("ymax",name);
	}
	
	public static Aes shape(int index) {
		return new Aes("shape",index);
	}
	public static Aes shape(String name) {
		return new Aes("shape",name);
	}

	public static Aes lower(int index) {
		return new Aes("lower",index);
	}
	public static Aes lower(String name) {
		return new Aes("lower",name);
	}

	public static Aes middle(int index) {
		return new Aes("middle",index);
	}
	public static Aes middle(String name) {
		return new Aes("middle",name);
	}

	public static Aes upper(int index) {
		return new Aes("upper",index);
	}
	public static Aes upper(String name) {
		return new Aes("upper",name);
	}

	public static Aes angle(int index) {
		return new Aes("angle",index);
	}
	public static Aes angle(String name) {
		return new Aes("angle",name);
	}

	public static Aes family(int index) {
		return new Aes("family",index);
	}
	public static Aes family(String name) {
		return new Aes("family",name);
	}
	

	public static Aes fontface(int index) {
		return new Aes("fontface",index);
	}
	public static Aes fontface(String name) {
		return new Aes("fontface",name);
	}

	public static Aes hjust(int index) {
		return new Aes("hjust",index);
	}
	public static Aes hjust(String name) {
		return new Aes("hjust",name);
	}

	public static Aes vjust(int index) {
		return new Aes("vjust",index);
	}
	public static Aes vjust(String name) {
		return new Aes("vjust",name);
	}

	public static Aes lineheight(int index) {
		return new Aes("lineheight",index);
	}
	public static Aes lineheight(String name) {
		return new Aes("lineheight",name);
	}

	public static void append(StringBuilder cmd, DataFrame df, Aes[] aes, boolean leadingComma) {
		if (aes.length==0) return;
		
		if (leadingComma) cmd.append(", ");
		
		cmd.append("aes(");
		for (int i=0; i<aes.length; i++) {
			if (i>0) cmd.append(",");
			cmd.append(aes[i].getName()).append("=").append(aes[i].getValue(df));
		}
		cmd.append(")");
		
	}
	
	
}
