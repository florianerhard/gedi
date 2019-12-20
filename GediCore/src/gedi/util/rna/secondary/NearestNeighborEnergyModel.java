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
package gedi.util.rna.secondary;

import static gedi.util.rna.secondary.EnergyUtils.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import gedi.util.ReflectionUtils;
import gedi.util.StringUtils;

public class NearestNeighborEnergyModel  {

	


	private static final int[] stack_energies_radix = {NBPAIRS};
	private static final int[] mismatch_interior_radix = {5*5,5};
	private static final int[] mismatch_hairpin_radix = {5*5,5};
	private static final int[] int11_energies_radix = {NBPAIRS*5*5,5*5,5};
	private static final int[] int21_energies_radix = {NBPAIRS*5*5*5,5*5*5,5*5,5};
	private static final int[] int22_energies_radix = {NBPAIRS*4*4*4*4,4*4*4*4,4*4*4,4*4,4};
	private static final int[] dangle5_radix = {5}; 
	private static final int[] dangle3_radix = {5};

	public final int[] stack_energies = new int[NBPAIRS*NBPAIRS];
	public final int[] hairpin = new int[MAXLOOP+1];
	public final int[] bulge = new int[MAXLOOP+1];
	public final int[] internal_loop = new int[MAXLOOP+1];
	public final int[] mismatch_interior = new int[NBPAIRS*5*5];
	public final int[] mismatch_hairpin = new int[NBPAIRS*5*5];
	public final int[] int11_energies = new int[NBPAIRS*NBPAIRS*5*5];
	public final int[] int21_energies = new int[NBPAIRS*NBPAIRS*5*5*5];
	public final int[] int22_energies = new int[NBPAIRS*NBPAIRS*4*4*4*4];
	public final int[] dangle5 = new int[8*5]; 
	public final int[] dangle3 = new int[8*5];
	public final int[] ML_params = new int[4];
	public final int[] NINIO = new int[2];
	public final Map<String,Integer> tetraLoop = new TreeMap<String, Integer>();
	public final Map<String,Integer> triLoop = new TreeMap<String, Integer>();

	public int[] MLintern = new int[NBPAIRS+1];
	public int TerminalAU;
	public int DuplexInit = 410;
	public int MAX_NINIO;

	public double Sigma = 0.92;
	public double Beta1 = 5.1;
	public double Beta2 = 0.1;
	public double Beta3 = 0.1;
	public double Beta1Prime = 4.1;
	public double SigmaPrime = 0.95;
	public double Gamma1 = 5; // energy penalty for large inter-hybrid loops: Gamma1+Gamma2*(1/(n1+n2))+Gamma3*|log(n1/n2)|
	public double Gamma2 = 0.1;
	public double Gamma3 = 0.1;


	private int dangles = 1;
	private boolean logML = false;
	private boolean tetra_loop = true;
	private char backtrack_type = 'N';
	private boolean debug = false;
	
	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public void setDangles(int dangles) {
		this.dangles = dangles;
	}

	public int getDangles() {
		return dangles;
	}

	public double getRT() {
		return _RT;
	}

	public double computeEnergy(CharSequence sequence, int[] bpt) {
		return computeIntEnergy(sequence,bpt)*0.01;
	}

	public int computeIntEnergy(CharSequence sequence, int[] bpt) {
		return computeIntEnergy(sequence, bpt, false);
	}
	
	public double computeEnergy(CharSequence sequence, int[] bpt, boolean local) {
		return computeIntEnergy(sequence,bpt, local)*0.01;
	}
	
	public int computeIntEnergy(CharSequence sequence, int[] bpt, boolean local) {
		if (sequence.length()+1!=bpt.length)
			throw new IllegalArgumentException("Sequence has a different length than basepair table!");
		int[] S = new int[sequence.length()+2];
		int[] S1 = new int[sequence.length()+2];
		encodeSequence(sequence, S, S1);

		int   i, length, energy;


		length = S[0];
		if (local)
			energy = 0;
		else
			energy =  backtrack_type=='M' ? ML_Energy(0, 0,bpt,S,S1) : ML_Energy(0, 1,bpt,S,S1);

		//		  if (eos_debug>0)
		//		    printf("External loop                           : %5d\n", energy);
		for (i=1; i<=length; i++) {
			if (bpt[i]==0) continue;
			energy += stack_energy(i, sequence.toString(),bpt,S,S1);
			i=bpt[i];
		}
		//		  for (i=1; !SAME_STRAND(i,length); i++) {
		//		    if (!SAME_STRAND(i,pair_table[i])) {
		//		      energy+=P->DuplexInit;
		//		      break;
		//		    }
		//		  }
		return energy;
	}



	public double computeEnergy(CharSequence forwardSequence,
			CharSequence backwardSequence) {

		int l = forwardSequence.length();
		if (backwardSequence.length()!=l)
			throw new IllegalArgumentException("Sequences must have same length!");

		int[] Sf = new int[l+2];
		encodeSequence(forwardSequence, Sf, null);
		int[] Sb = new int[l+2];
		encodeSequence(backwardSequence, Sb, null);

		int energy = 0;
		for (int i=1; i<=l-1; i++) {
			int a = pair[Sf[i]][Sb[l-i+1]];
			int b = pair[Sb[l-i]][Sf[i+1]];
			energy+=stack(a, b);
		}


		//		if (forwardSequence!=backwardSequence)
		//			energy+=DuplexInit;
		return energy*0.01;
	}

	public double stackingEnergy(char f5, char b5, char f3, char b3) {
		return stack(
				pair[encode_char(f5)][encode_char(b5)],
				pair[encode_char(b3)][encode_char(f3)]
		)*0.01;
	}

//	public int[] encodeSequence(CharSequence sequence, boolean leadingSize, Character correct) {
//		final int offset = leadingSize?1:0;
//		int[] re = new int[sequence.length()+offset];
//		if (leadingSize)
//			re[0]=sequence.length();
//		for (int i=offset; i<re.length; i++) {
//			re[i] = encode_char(Character.toUpperCase(sequence.charAt(i-offset)));
//			if (correct!=null && re[i]==0)
//				re[i] = encode_char(Character.toUpperCase(correct));
//		}
//		return re;
//	}
//
//	public void encodeSequence(CharSequence sequence,int[] S, int[] S1) {
//		int i,l;
//
//		l = sequence.length();
//		/* S1 exists only for the special X K and I bases and energy_set!=0 */
//		S[0] = l;
//
//		for (i=1; i<=l; i++) { /* make numerical encoding of sequence */
//			S[i]= encode_char(Character.toUpperCase(sequence.charAt(i-1)));
//			if (S1!=null)
//				S1[i] = alias[S[i]];   /* for mismatches of nostandard bases */
//		}
//		/* for circular folding add first base at position n+1 and last base at
//			position 0 in S1	*/
//		S[l+1] = S[1];
//		if (S1!=null) {
//			S1[l+1]=S1[1]; 
//			S1[0] = S1[l];
//		}
//	}
	
	public int energy_of_struct_pt(String string, int[] ptable,
			int[] s, int[] s1) {
		/* auxiliary function for kinfold,
     for most purposes call energy_of_struct instead */

		int   i, length, energy;

		int[] pair_table = ptable;
		int[] S = s;
		int[] S1 = s1;

		length = S[0];
		energy =  backtrack_type=='M' ? ML_Energy(0, 0,pair_table,S,S1) : ML_Energy(0, 1,pair_table,S,S1);
//		if (eos_debug>0)
//			printf("External loop                           : %5d\n", energy);
		for (i=1; i<=length; i++) {
			if (pair_table[i]==0) continue;
			energy += stack_energy(i, string,pair_table,S,S1);
			i=pair_table[i];
		}
//		for (i=1; !SAME_STRAND(i,length); i++) {
//			if (!SAME_STRAND(i,pair_table[i])) {
//				energy+=P->DuplexInit;
//				break;
//			}
//		}
		return energy;
	}

	public int stack_energy(int i, final String string, int[] pair_table, int[] S, int[] S1)
	{
		/* calculate energy of substructure enclosed by (i,j) */
		int ee, energy = 0;
		int j, p, q, type;

		j=pair_table[i];
		type = pair[S[i]][S[j]];
		if (type==0) {
			type=7;
			//	    if (eos_debug>=0)
			//	      fprintf(stderr,"WARNING: bases %d and %d (%c%c) can't pair!\n", i, j,
			//		      string[i-1],string[j-1]);
		}

		p=i; q=j;
		while (p<q) { /* process all stacks and interior loops */
			int type_2;
			while (pair_table[++p]==0);
			while (pair_table[--q]==0);
			if ((pair_table[q]!=(short)p)||(p>q)) break;
			type_2 = pair[S[q]][S[p]];
			if (type_2==0) {
				type_2=7;
				//	      if (eos_debug>=0)
				//		fprintf(stderr,"WARNING: bases %d and %d (%c%c) can't pair!\n", p, q,
				//			string[p-1],string[q-1]);
			}
			/* energy += LoopEnergy(i, j, p, q, type, type_2); */
			//	    if ( SAME_STRAND(i,p) && SAME_STRAND(q,j) )
			ee = LoopEnergy(p-i-1, j-q-1, type, type_2,
					S1[i+1], S1[j-1], S1[p-1], S1[q+1]);
			if (debug)
				System.err.println(i+"\t"+j+"\t"+ee);
			//	    else
			//	      ee = ML_Energy(cut_in_loop(i), 1);
			//	    if (eos_debug>0)
			//	      printf("Interior loop (%3d,%3d) %c%c; (%3d,%3d) %c%c: %5d\n",
			//		     i,j,string[i-1],string[j-1],p,q,string[p-1],string[q-1], ee);
			energy += ee;
			i=p; j=q; type = rtype[type_2];
		} /* end while */

		/* p,q don't pair must have found hairpin or multiloop */

		if (p>q) {                       /* hair pin */
			//	    if (SAME_STRAND(i,j))
			ee = HairpinE(j-i-1, type, S1[i+1], S1[j-1], string.substring(i-1));
			//	    else
			//	      ee = ML_Energy(cut_in_loop(i), 1);
			if (debug)
				System.err.println(i+"\t"+j+"\t"+ee);
			energy += ee;
			//	    if (eos_debug>0)
			//	      printf("Hairpin  loop (%3d,%3d) %c%c              : %5d\n",
			//		     i, j, string[i-1],string[j-1], ee);

			return energy;
		}

		/* (i,j) is exterior pair of multiloop */
		while (p<j) {
			/* add up the contributions of the substructures of the ML */
			energy += stack_energy(p, string,pair_table,S,S1);
			p = pair_table[p];
			/* search for next base pair in multiloop */
			while (pair_table[++p]==0);
		}
		{
			int ii;
			ii = cut_in_loop(i,pair_table);
			ee = (ii==0) ? ML_Energy(i,0,pair_table,S,S1) : ML_Energy(ii, 1,pair_table,S,S1);
			if (debug)
				System.err.println(i+"\t"+j+"\t"+ee);
		}
		energy += ee;
		//	  if (eos_debug>0)
		//	    printf("Multi    loop (%3d,%3d) %c%c              : %5d\n",
		//		   i,j,string[i-1],string[j-1],ee);

		return energy;
	}

	public int HairpinE(int size, int type, int si1, int sj1, String string) {
		int energy;
		energy = (size <= 30) ? hairpin(size) :
			hairpin[30]+(int)(lxc37*Math.log((size)/30.));
		if (tetra_loop)
			if (size == 4) { /* check for tetraloop bonus */
				//		      char tl[7]={0}, *ts;
				//		      strncpy(tl, string, 6);
				//		      if ((ts=strstr(P->Tetraloops, tl)))
				//			energy += P->TETRA_ENERGY[(ts - P->Tetraloops)/7];
				energy+=tetraloopE(string.substring(0, 6));
			}
		if (size == 3) {
			//		    char tl[6]={0,0,0,0,0,0}, *ts;
			//		    strncpy(tl, string, 5);
			//		    if ((ts=strstr(P->Triloops, tl)))
			//		      energy += P->Triloop_E[(ts - P->Triloops)/6];
			energy+=triloopE(string.substring(0, 5));
			if (type>2)  /* neither CG nor GC */
				energy += TerminalAU; /* penalty for closing AU GU pair */
		}
		else  /* no mismatches for tri-loops */
			energy += mismatchH(type,si1,sj1);

		return energy;
	}

	/**
	 * Gets the maximal size of the loop, that has energy cost <= en.
	 * @param en
	 * @return
	 */
	public int maxInternalLoop(int en) {
		if (en<=internal_loop[4]) return 5;
		if (en<=internal_loop[30]) {
			int p = Arrays.binarySearch(internal_loop, en);
			if (p>0) return p+1;
			else return -p-1;
		}
		else {
			return (int)Math.ceil(30*Math.exp((en-internal_loop[30])/lxc37));
		}
	}

	/**
	 * Gets the maximal size of the bulge, that has energy cost <= en.
	 * @param en
	 * @return
	 */
	public int maxBulge(int en) {
		if (en<=bulge[2]) return 3;
		if (en<=bulge[30]) {
			int p = Arrays.binarySearch(bulge, en);
			if (p>0) return p+1;
			else return -p-1;
		}
		else {
			return (int)Math.ceil(30*Math.exp((en-bulge[30])/lxc37));
		}
	}

	public final double BasePairEnergy(int i, int j, int type) {
		return 0;
	}
	
	public final int intBasePairEnergy(int i, int j, int type) {
		return 0;
	}
	
	
	public double LoopEnergy(int i, int p, int q, int j, int type, int type_2,
			int si1, int sj1, int sp1, int sq1) {
		return LoopEnergy(p-i-1, j-q-1, type, type_2, si1, sj1, sp1, sq1);
	}
	
	public int intLoopEnergy(int i, int p, int q, int j, int type, int type_2,
			int si1, int sj1, int sp1, int sq1) {
		return LoopEnergy(p-i-1, j-q-1, type, type_2, si1, sj1, sp1, sq1);
	}
	
	/**
	 * i--p--q--j <br>
	 * i to p contains n1 bases <br>
	 * q to j contains n2 bases <br>
	 * i-j is type <br>
	 * q-p is type_2 <br>
	 * next base after i is si1 <br>
	 * next base previous to j is sj1 <br>
	 * next base previous to p is sp1 <br>
	 * next base after q is sq1 <br>
	 * 
	 * @param n1
	 * @param n2
	 * @param type
	 * @param type_2
	 * @param si1
	 * @param sj1
	 * @param sp1
	 * @param sq1
	 * @return
	 */
	public final int LoopEnergy(int n1, int n2, int type, int type_2,
			int si1, int sj1, int sp1, int sq1) {
		/* compute energy of degree 2 loop (stack bulge or interior) */
		int nl, ns, energy;

		if (n1>n2) { nl=n1; ns=n2;}
		else {nl=n2; ns=n1;}

		if (nl == 0)
			return stack(type,type_2);    /* stack */

		if (ns==0) {                       /* bulge */
			energy = (nl<=MAXLOOP)?bulge(nl):
				(bulge(30)+(int)(lxc37*Math.log(nl/30.)));
			if (nl==1) energy += stack(type,type_2);
			else {
				if (type>2) energy += TerminalAU;
				if (type_2>2) energy += TerminalAU;
			}
			return energy;
		}
		else {                             /* interior loop */
			if (ns==1) {
				if (nl==1)                     /* 1x1 loop */
					return int11(type,type_2,si1,sj1);
				if (nl==2) {                   /* 2x1 loop */
					if (n1==1)
						energy = int21(type,type_2,si1,sq1,sj1);
					else
						energy = int21(type_2,type,sq1,si1,sp1);
					return energy;
				}
			}
			else if (n1==2 && n2==2)         /* 2x2 loop */
				return int22(type,type_2,si1,sp1,sq1,sj1);
			{ /* generic interior loop (no else here!)*/
				energy = (n1+n2<=MAXLOOP)?(internal_loop(n1+n2)):
					(internal_loop(30)+(int)(lxc37*Math.log((n1+n2)/30.)));

				energy += Math.min(MAX_NINIO, (nl-ns)*NINIO[0]);

				energy += mismatchI(type,si1,sj1)+
				mismatchI(type_2,sq1,sp1);
			}
		}
		return energy;
	}
	
	public int genericLoopEnergy(int n1, int n2) {
		int nl, ns, energy;

		if (n1>n2) { nl=n1; ns=n2;}
		else {nl=n2; ns=n1;}
		
		energy = (n1+n2<=MAXLOOP)?(internal_loop(n1+n2)):
			(internal_loop(30)+(int)(lxc37*Math.log((n1+n2)/30.)));

		energy += Math.min(MAX_NINIO, (nl-ns)*NINIO[0]);

		return energy;
	}



	public int loop_energy(int[] ptable, int[] s, int[] s1, int i) {
		/* compute energy of a single loop closed by base pair (i,j) */
		int j, type, p,q, energy;
		//		  short *Sold, *S1old, *ptold;

		int[] pair_table = ptable;
		int[] S = s;
		int[] S1 = s1;
		//		  ptold=pair_table;   Sold = S;   S1old = S1;
		//		  pair_table = ptable;   S = s;   S1 = s1;

		if (i==0) { /* evaluate exterior loop */
			energy = ML_Energy(0,1,pair_table,S,S1);
			//		    pair_table=ptold; S=Sold; S1=S1old;
			return energy;
		}
		j = pair_table[i];
		if (j<i) throw new RuntimeException("i is unpaired in loop_energy()");
		type = pair[S[i]][S[j]];
		if (type==0) {
			type=7;
			//		    if (eos_debug>=0)
			//		      fprintf(stderr,"WARNING: bases %d and %d (%c%c) can't pair!\n", i, j,
			//			      Law_and_Order[S[i]],Law_and_Order[S[j]]);
		}
		p=i; q=j;


		while (pair_table[++p]==0);
		while (pair_table[--q]==0);
		if (p>q) { /* Hairpin */
			StringBuilder loopseq = new StringBuilder(8);
			//			if (SAME_STRAND(i,j)) {
			if (j-i-1<7) {
				int u;
				for (u=0; i+u<=j; u++) loopseq.append(Law_and_Order_ARRAY[S[i+u]]);
			}
			energy = HairpinE(j-i-1, type, S1[i+1], S1[j-1], loopseq.toString());
			//			} else {
			//				energy = ML_Energy(cut_in_loop(i,pair_table), 1);
			//			}
		}
		else if (pair_table[q]!=(short)p) { /* multi-loop */
			int ii;
			ii = cut_in_loop(i,pair_table);
			energy = (ii==0) ? ML_Energy(i,0,pair_table,S,S1) : ML_Energy(ii, 1,pair_table,S,S1);
		}
		else { /* found interior loop */
			int type_2;
			type_2 = pair[S[q]][S[p]];
			if (type_2==0) {
				type_2=7;
				//				if (eos_debug>=0)
				//					fprintf(stderr,"WARNING: bases %d and %d (%c%c) can't pair!\n", p, q,
				//							Law_and_Order[S[p]],Law_and_Order[S[q]]);
			}
			/* energy += LoopEnergy(i, j, p, q, type, type_2); */
			//			if ( SAME_STRAND(i,p) && SAME_STRAND(q,j) )
			energy = LoopEnergy(p-i-1, j-q-1, type, type_2,
					S1[i+1], S1[j-1], S1[p-1], S1[q+1]);
			//			else
			//				energy = ML_Energy(cut_in_loop(i), 1);
		}

		//		pair_table=ptold; S=Sold; S1=S1old;
		return energy;
	}


	public int ML_Energy(int i, int is_extloop, int[] pair_table, int[] S, int[] S1) {
		/* i is the 5'-base of the closing pair (or 0 for exterior loop)
		     loop is scored as ML if extloop==0 else as exterior loop

		     since each helix can coaxially stack with at most one of its
		     neighbors we need an auxiliarry variable  cx_energy
		     which contains the best energy given that the last two pairs stack.
		     energy  holds the best energy given the previous two pairs do not
		     stack (i.e. the two current helices may stack)
		     We don't allow the last helix to stack with the first, thus we have to
		     walk around the Loop twice with two starting points and take the minimum
		 */

		int energy, cx_energy, best_energy=INF;
		int i1, j, p, q, u=0, x, type, count, mlclosing, mlbase;
		int[] mlintern = new int[NBPAIRS+1];

		if (is_extloop!=0) {
			for (x = 0; x <= NBPAIRS; x++)
				mlintern[x] = MLintern[x]-MLintern[1]; /* 0 or TerminalAU */
			mlclosing = mlbase = 0;
		} else {
			for (x = 0; x <= NBPAIRS; x++) mlintern[x] = MLintern[x];
			mlclosing = ML_params[1]; mlbase = ML_params[0];
		}

		for (count=0; count<2; count++) { /* do it twice */
			int ld5 = 0; /* 5' dangle energy on prev pair (type) */
			if ( i==0 ) {
				j = pair_table[0]+1;
				type = 0;  /* no pair */
			}
			else {
				j = pair_table[i];
				type = pair[S[j]][S[i]]; if (type==0) type=7;

				if (dangles==3) { /* prime the ld5 variable */
					//			if (SAME_STRAND(j-1,j)) 
					{
						ld5 = dangle5(type,S1[j-1]);
						if ((p=pair_table[j-2])!=0 
								//					  && SAME_STRAND(j-2, j-1)
						)
							if (dangle3(pair[S[p]][S[j-2]],S1[j-1])<ld5) ld5 = 0;
					}
				}
			}
			i1=i; p = i+1; u=0;
			energy = 0; cx_energy=INF;
			do { /* walk around the multi-loop */
				int tt, new_cx = INF;

				/* hope over unpaired positions */
				while (p <= pair_table[0] && pair_table[p]==0) p++;

				/* memorize number of unpaired positions */
				u += p-i1-1;
				/* get position of pairing partner */
				if ( p == pair_table[0]+1 )
					q = tt = 0; /* virtual root pair */
				else {
					q  = pair_table[p];
					/* get type of base pair P->q */
					tt = pair[S[p]][S[q]]; if (tt==0) tt=7;
				}

				energy += mlintern[tt];
				cx_energy += mlintern[tt];

				if (dangles!=0) {
					int dang5=0, dang3=0, dang;
					if (
							//					(SAME_STRAND(p-1,p))&&
							(p>1))
						dang5=dangle5(tt,S1[p-1]);      /* 5'dangle of pq pair */
					if (
							//					(SAME_STRAND(i1,i1+1))&&
							(i1<S[0]))
						dang3 = dangle3(type,S1[i1+1]); /* 3'dangle of previous pair */

					switch (p-i1-1) {
					case 0: /* adjacent helices */
						if (dangles==2)
							energy += dang3+dang5;
						else if (dangles==3 && i1!=0) {
							//			    if (SAME_STRAND(i1,p)) 
							{
								new_cx = energy + stack(rtype[type],rtype[tt]);
								/* subtract 5'dangle and TerminalAU penalty */
								new_cx += -ld5 - mlintern[tt]-mlintern[type]+2*mlintern[1];
							}
							ld5=0;
							energy = Math.min(energy, cx_energy);
						}
						break;
					case 1: /* 1 unpaired base between helices */
						dang = (dangles==2)?(dang3+dang5):Math.min(dang3, dang5);
						if (dangles==3) {
							energy = energy +dang; ld5 = dang - dang3;
							/* may be problem here: Suppose
			       cx_energy>energy, cx_energy+dang5<energy
			       and the following helices are also stacked (i.e.
			       we'll subtract the dang5 again */
							if (cx_energy+dang5 < energy) {
								energy = cx_energy+dang5;
								ld5 = dang5;
							}
							new_cx = INF;  /* no coax stacking with mismatch for now */
						} else
							energy += dang;
						break;
					default: /* many unpaired base between helices */
						energy += dang5 +dang3;
						if (dangles==3) {
							energy = Math.min(energy, cx_energy + dang5);
							new_cx = INF;  /* no coax stacking possible */
							ld5 = dang5;
						}
					}
					type = tt;
				}
				if (dangles==3) cx_energy = new_cx;
				i1 = q; p=q+1;
			} while (q!=i);
			best_energy = Math.min(energy, best_energy); /* don't use cx_energy here */
			/* fprintf(stderr, "%6.2d\t", energy); */
			if (dangles!=3 || is_extloop!=0) break;  /* may break cofold with co-ax */
			/* skip a helix and start again */
			while (pair_table[p]==0) p++;
			if (i == pair_table[p]) break;
			i = pair_table[p];
		}
		energy = best_energy;
		energy += mlclosing;
		/* logarithmic ML loop energy if logML */
		if ( (0==is_extloop) && logML && (u>6) )
			energy += 6*mlbase+(int)(lxc37*Math.log((double)u/6.));
		else
			energy += mlbase*u;
		/* fprintf(stderr, "\n"); */
		return energy;
	}

	public final int multiloop(int u) {
		return logML && (u>6) ?
				6*ML_params[0]+(int)(lxc37*Math.log((double)u/6.))
				:
					ML_params[0]*u;
	}

	public final int cut_in_loop(int i, int[] pair_table) {
		return 0;
		//		  /* walk around the loop;  return j pos of pair after cut if
		//		     cut_point in loop else 0 */
		//		  int  p, j;
		//		  p = j = pair_table[i];
		//		  do {
		//		    i  = pair_table[p];  p = i+1;
		//		    while ( pair_table[p]==0 ) p++;
		//		  } while (p!=j && SAME_STRAND(i,p));
		//		  return SAME_STRAND(i,p) ? 0 : pair_table[p];
	}


	public final int stack(int bp1, int bp2) {
		return stack_energies[(bp1-1)*stack_energies_radix[0]+(bp2-1)];
	}

	public int hairpin(int size) {
		return hairpin[size];
	} 
	public final int bulge(int nl) {
		return bulge[nl];
	}

	public final int internal_loop(int i) {
		return internal_loop[i];
	}


	public int mismatchI(int type, int si1, int sj1) {
		return mismatch_interior[(type-1)*mismatch_interior_radix[0]+
		                         si1*mismatch_interior_radix[1]+
		                         sj1];
	}

	public int mismatchH(int type, int si1, int sj1) {
		return mismatch_hairpin[(type-1)*mismatch_hairpin_radix[0]+
		                        si1*mismatch_hairpin_radix[1]+
		                        sj1];
	}

	public final int int11(int type, int type_2, int si1, int sj1) {
		return int11_energies[(type-1)*int11_energies_radix[0]+
		                      (type_2-1)*int11_energies_radix[1]+
		                      si1*int11_energies_radix[2]+
		                      sj1];
	}

	public final int int21(int type, int type_2, int si1, int sq1, int sj1) {
		return int21_energies[(type-1)*int21_energies_radix[0]+
		                      (type_2-1)*int21_energies_radix[1]+
		                      si1*int21_energies_radix[2]+
		                      sq1*int21_energies_radix[3]+
		                      sj1];
	}

	public final int int22(int type, int type_2, int si1, int sp1, int sq1, int sj1) {
		return int22_energies[(type-1)*int22_energies_radix[0]+
		                      (type_2-1)*int22_energies_radix[1]+
		                      (si1-1)*int22_energies_radix[2]+
		                      (sp1-1)*int22_energies_radix[3]+
		                      (sq1-1)*int22_energies_radix[4]+
		                      (sj1-1)];
	}

	public final int dangle5(int pair, int base) {
		return dangle5[pair*dangle5_radix[0]+
		               base];
	}

	public final int dangle3(int pair, int base) {
		return dangle3[pair*dangle3_radix[0]+
		               base];
	}


	public int triloopE(String loop) {
		Integer tri = triLoop.get(loop);
		return tri==null?0:tri.intValue();
	}

	public int tetraloopE(String loop) {
		Integer tetra = tetraLoop.get(loop);
		return tetra==null?0:tetra.intValue();
	}

	public static String VIENNA_HEADER = "## RNAfold parameter file";


	public static NearestNeighborEnergyModel readFromVienna() throws IOException {
		return readFromVienna(new InputStreamReader(NearestNeighborEnergyModel.class.getResourceAsStream("/resources/rna_turner2004.par")));
	}

	public static NearestNeighborEnergyModel readFromVienna(File file) throws IOException {
		return readFromVienna(new FileReader(file));
	}


	public static NearestNeighborEnergyModel readFromVienna(Reader reader) throws IOException {
		NearestNeighborEnergyModel re = new NearestNeighborEnergyModel();

		BufferedReader br = new BufferedReader(reader);
		if (!br.readLine().startsWith(VIENNA_HEADER))
			throw new IOException("Not a valid ViennaRNA parameter file!");

		String[] block;

		while ((block=readNextBlock(br))!=null) {
			String name = block[0].substring(2).trim();
			if (name.equals("Tetraloops"))
				parseLoops(block,re.tetraLoop);
			else if (name.equals("Triloops"))
				parseLoops(block,re.triLoop);
			else
				try {
					Field field = ReflectionUtils.findAnyField(re.getClass(),name,false);
					if (field!=null)
						parseNumbers(block,(int[]) field.get(re));
				} catch (Exception e) {
					throw new IOException("Could not read block "+block[0],e);
				}
		}

		for (int i=0; i<=NBPAIRS; i++) { /* includes AU penalty */
			re.MLintern[i] = re.ML_params[2];
			re.MLintern[i] +=  (i>2)?re.ML_params[3]:0;
		}
		re.TerminalAU = re.ML_params[3];
		re.MAX_NINIO = re.NINIO[1];

		correctMaximalZero(re.dangle3);
		correctMaximalZero(re.dangle5);

		return re;
	}

	private static void correctMaximalZero(int[] a) {
		for (int i=0; i<a.length; i++)
			a[i]=Math.min(0, a[i]);
	}

	private static void parseLoops(String[] block, Map<String, Integer> map) {
		for (int i=1; i<block.length; i++) {
			StringTokenizer st = new StringTokenizer(block[i]);
			map.put(st.nextToken(), Integer.parseInt(st.nextToken()));
		}
	}

	private static void parseNumbers(String[] block, int[] array) throws IOException {
		if (array==null)
			return;

		int index = 0;
		for (int i=1; i<block.length; i++) {
			StringTokenizer st = new StringTokenizer(block[i]);
			while (st.hasMoreTokens()) {
				String n = st.nextToken();
				if (StringUtils.isInt(n))
					array[index++] = Integer.parseInt(n);
				else  if(n.equals("NST"))
					array[index++] = NST;
				else  if(n.equals("INF"))
					array[index++] = INF;
				else throw new IOException("Cannot parse "+n+" as integer!");
			}
		}
	}


	private static String[] readNextBlock(BufferedReader br) throws IOException {
		ArrayList<String> re = new ArrayList<String>();
		String line;
		while ((line=br.readLine())!=null) {
			if (line.trim().length()==0 || line.startsWith("/*"))
				continue;
			if (line.startsWith("#") && re.size()>0) {
				br.reset();
				break;
			}
			re.add(line);

			br.mark(1<<12);
		}
		if (re.size()==0)
			return null;
		else
			return re.toArray(new String[0]);
	}





}