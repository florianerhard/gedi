package gedi.proteomics.molecules;

import gedi.util.ArrayUtils;
import gedi.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class Modifications {

	private Modification[] modifications;
	private HashMap<String,Modification> byName;
	
	private Modification[] fixedModifications = new Modification[256];
	private Modification[] variableModifications = new Modification[0];
	private Modification[] labelModifications = new Modification[0];


	public Modifications(Modification[] modifications) {
		this.modifications = modifications;
		byName = new HashMap<String, Modification>();
		for (Modification m : modifications)
			byName.put(m.getTitle(), m);
	}

	public void setFixedModifications(Modification[] fixedModifications) {
		this.fixedModifications = new Modification[256];
		for (Modification m : fixedModifications) {
			if (m.getPosition()!=Modification.ModificationPosition.anywhere)
				throw new RuntimeException("Only anywhere modifications are allowed to be fixed!");
			for (char c : m.getSites())
				this.fixedModifications[c] = m;
		}
	}
	
	public void setLabelModifications(Modification[] labelModifications) {
		this.labelModifications = labelModifications;
	}
	
	public void setVariableModifications(Modification[] variableModifications) {
		this.variableModifications = variableModifications;
	}
	
	public Modification getModification(char aa, String modString) {
		if (fixedModifications[aa]!=null) 
			return fixedModifications[aa];
//		if (aa=='C') return modifications[2]; // carbamidomethyl as fixed mod!
		if (modString.equals("A"))
			return null;
		return modifications[Integer.parseInt(modString)];
	}
	
	public Polymer parse(String seq, String mod) {
		String[] mf = StringUtils.split(mod, ',');
		
		int l = seq.length();
		if (!mf[0].equals("A")) l++;
		if (!mf[mf.length-1].equals("A")) l++;
		
		Monomer[] re = new Monomer[l];
		int index = 0;
		if (!mf[0].equals("A"))
			re[index++] = Monomer.obtain('^', getModification('^',mf[0]));
		for (int i=0; i<seq.length(); i++)
			re[index++] = Monomer.obtain(seq.charAt(i), getModification(seq.charAt(i),mf[i+1]));
		if (!mf[mf.length-1].equals("A")) 
			re[index++] = Monomer.obtain('$', getModification('$',mf[mf.length-1]));
				
		return new Polymer(re);
	}
	
	public Polymer parseRemoveLabel(String seq, String mod) {
		for (Modification l : labelModifications)
			while (mod.contains(","+l.getIndex()+","))
				mod = mod.replace(","+l.getIndex()+",",",A,");
		return parse(seq, mod);
	}


	public Polymer parse(String aaString) {
		if (aaString.startsWith("_")) return parseModifiedSequence(aaString);
		
		ArrayList<Monomer> re = new ArrayList<Monomer>();
		for (int i=0; i<aaString.length(); i++) {
			char singleLetter = aaString.charAt(i);
			String modString = "A";
			if (i+1<aaString.length() && aaString.charAt(i+1)=='[') {
				int s = i+2;
				i = aaString.indexOf(']',i+2);
				modString = aaString.substring(s,i);
			}
			re.add(Monomer.obtain(singleLetter, getModification(singleLetter,modString)));
		}
		return new Polymer(re.toArray(new Monomer[0]));
	}


	private Polymer parseModifiedSequence(String aaString) {
		if (aaString.startsWith("_(ac)")) aaString = "_^[1]"+aaString.substring(5);
		aaString = aaString.replaceAll("\\(ox\\)", "[3]");
		aaString = aaString.substring(1,aaString.length()-1);
		return parse(aaString);
	}

	public Modification getModificationByTitle(String title) {
		return byName.get(title);
	}

	public Modification[] getModificationsByTitle(String[] title) {
		Modification[] re = new Modification[title.length];
		for (int i=0; i<title.length; i++)
			re[i] = getModificationByTitle(title[i]);
		return re;
	}
	
	public Iterator<Polymer> iterate(String sequence, int maxModifications, boolean nterm, boolean cterm) {
		return new VariableModificationIterator(sequence,maxModifications,nterm,cterm);
	}
	
	private class VariableModificationIterator implements Iterator<Polymer> {

		private String sequence;
		private Modification[][] monomers;
		private int[] indices;
		private boolean isValid;
		private boolean finished = false;
		private int[] radix;
		private int maxModifications;
		
		public VariableModificationIterator(String sequence, int maxModifications, boolean nterm,
				boolean cterm) {
			this.maxModifications = maxModifications;
			monomers = new Modification[sequence.length()+2][];
			ArrayList<Modification> mod = new ArrayList<Modification>();
			sequence = "^"+sequence+"$";
			for (int i=0; i<sequence.length(); i++) {
				mod.clear();
				mod.add(fixedModifications[sequence.charAt(i)]); // if not fixed mod: null!
				if (mod.get(0)==null) {
					for (Modification m : variableModifications)
						if (m.isValidSite(sequence.charAt(i),nterm,cterm))
							mod.add(m);
				}
				monomers[i] = mod.toArray(new Modification[0]);
			}
			indices= new int[sequence.length()];
			radix= new int[sequence.length()];
			for (int i=0; i<radix.length; i++)
				radix[i] = monomers[i].length;
			isValid = true;
			this.sequence = sequence;
		}

		@Override
		public boolean hasNext() {
			lookAhead();
			return !finished;
		}

		@Override
		public Polymer next() {
			lookAhead();
			isValid = false;
			
			int l = monomers.length;
			if (monomers[0][indices[0]]==null) l--;
			if (monomers[indices.length-1][indices[indices.length-1]]==null) l--;
			Monomer[] re = new Monomer[l];
			int index = 0;
			for (int i=monomers[0][indices[0]]==null?1:0; index<re.length; i++,index++ )
				re[index] = Monomer.obtain(sequence.charAt(i), monomers[i][indices[i]]);
			
			return new Polymer(re);
		}
		
		private void lookAhead() {
			while (!finished  && !isValid) {
				finished = !ArrayUtils.increment(indices, radix);
				isValid = indices.length-ArrayUtils.count(indices, 0)<=maxModifications;
			}
			
		}

		@Override
		public void remove() {
		}
		
	}

	
}
