/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */
package gedi.util.datastructure.tree.suffixTree.construction;


import gedi.util.datastructure.tree.suffixTree.tree.Localization;
import gedi.util.datastructure.tree.suffixTree.tree.SuffixTree;
import gedi.util.datastructure.tree.suffixTree.tree.SuffixTreeStorage;
import gedi.util.sequence.Alphabet;

public class UkkonenSuffixTreeBuilder extends AbstractSuffixTreeBuilder {
	
	public static final String SUFFIX_LINK_NAME = "SuffixLinks";

	public UkkonenSuffixTreeBuilder(Alphabet alphabet, boolean check) {
		super(alphabet,check);
	}
	
	public <T extends SuffixTree> T build(T suffixTree, CharSequence...input) {
		for (CharSequence s : input)
			check(s);
		CharSequence gen = buildGeneralizedInput(input);
		
		int n = gen.length();
		int x,y;
		
		int[] slink = new int[2*gen.length()-1];
		
		suffixTree.initialize(getGeneralizedAlphabet(),gen);
		slink[0]=-1;
		
		Localization l = new Localization(suffixTree,0,1,1);
		
		SuffixTreeStorage storage = suffixTree.getStorage();
		storage.createSuperRootAndRoot();
		storage.addLeaf(0, 0);
		
//		System.out.println(gen);
		
		int leaf = 1;
		for (int i=1; i<n; i++) {
			
			x = y = -1;
			char ti = gen.charAt(i);
//			System.out.println("Processing "+ti);
			
			while (storage.lookup(l,ti)<0) {
				y = insert(storage,l,i,ti,slink,leaf++);
//				System.out.println("Insert "+ti+" at "+l);
				
				if (x>=0) 
					slink[x]=y;
				x = y;
				l.setNode(slink[l.getNode()]);
				storage.canonize(l);
			}
			if (x>=0) 
				slink[x]=l.getNode();
			
			l.setInfixEnd(i+1); 
			storage.canonize(l);
			
//			System.out.println("Done, alpha="+l);
//			System.out.println();
		}
		
		int[] sl = new int[storage.getMaxNode()+1];
		System.arraycopy(slink, 0, sl, 0, sl.length);
		suffixTree.setNodeAttribute(SUFFIX_LINK_NAME, sl);
		
		suffixTree.finished();
		
		return suffixTree;
	}

	private int insert(SuffixTreeStorage storage, Localization l, int i, char ti, int[] slink, int leaf) {
		int re = l.getNode();
		if (!l.isNode())
			re = storage.split(l);
			
		int nodeIndex = storage.addLeaf(re, i);
		slink[nodeIndex] = leaf;
		return re;
	}
	
	
}
