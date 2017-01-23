package gedi.proteomics.molecules.properties.hydrophobicity;


import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: tholzman
 * Date: Jul 13, 2006
 * Time: 9:43:40 AM
 */
/**
/*
/* reference, O. V. Krokhin, R. Craig, V. Spicer, W. Ens, K. G. Standing, R. C. Beavis, J. A. Wilkins
/* An improved model for prediction of retention times of tryptic peptides in ion-pair reverse-phase HPLC:
/* its application to protein peptide mapping by off-line HPLC-MALDI MS
/* Molecular and Cellular Proteomics 2004 Sep;3(9):908-19.
/* URL, http://hs2.proteome.ca/SSRCalc/SSRCalc.html
/*
/*
/* These subroutines are based on web version SSRCalculator of the Copyright holder listed as in the following:
/*
/* Version 3.0   2005.02.28
/* Copyright (c) 2005 John Wilkins
/* Sequence Specific Retention Calculator
/* Authors: Oleg Krokhin, Vic Spicer, John Cortens
*/

/* Translated from perl to C, Ted Holzman FHCRC, 6/2006  */
/* Retranslated from C to Java, Ted Holzman FHCRC 7/2006 */
/* NB: This is a version 0.1 direct translation.
/*     An attempt has been made to keep function names, variable names, and algorithms
/*     as close as possible to the original perl.
/*
/*     A real translation into native java idiom would probably be a good idea
*/


public class Hydrophobicity3 {
/* Lookup table data.  These are translations of the .h table in C which is a    */
/* translation of the ReadParmFile perl routine.  This does not read a parameter */
/* file; it makes static initializers for the parameter data.                    */

    public static String VERSION = "Krokhin,3.0";

    private static class AAParams {
        char AA;                     //amino acid residue owning these parameters
                                     //Retention Factors
        double RC;
        double RC1;
        double RC2;
        double RCN;
        double RCN2;
                                     //Short peptide retention factors
        double RCS;
        double RC1S;
        double RC2S;
        double RCNS;
        double RCN2S;

        double UndKRH;               //Factors for aa's near undigested KRH
        double AMASS;                //aa masses in Daltons
                                     //isoelectric factors
        double CT;
        double NT;
        double PK;
                                     //helicity2 bascore & connector multiplier
        double H2BASCORE;
        double H2CMULT;

        AAParams(
           char aa,
           double rc, double rc1, double rc2, double rcn, double rcn2,
           double rcs,double rc1s,double rc2s,double rcns,double rcn2s,
           double undkrh,double amass,
           double ct,double nt, double pk,
           double h2bascore, double h2cmult
        ) {
           AA=aa;
           RC=rc; RC1=rc1; RC2=rc2; RCN=rcn; RCN2=rcn2;
           RCS=rcs; RC1S=rc1s; RC2S=rc2s; RCNS=rcns; RCN2S=rcn2s;
           UndKRH=undkrh; AMASS=amass;
           CT=ct; NT=nt; PK=pk;
           H2BASCORE=h2bascore; H2CMULT=h2cmult;
        }
      }


    private static final AAParams NULLPARAM = new AAParams('\0',0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0);
    private static final AAParams[] AAPARAMS = new AAParams[265];
    static {
    	Arrays.fill(AAPARAMS, NULLPARAM);
        AAPARAMS['A'] = new AAParams('A',01.10,00.35,00.50,00.80,-0.10,00.80,-0.30,00.10,00.80,-0.50,00.00,071.0370,3.55,7.59,00.00,1.0,1.2);
        AAPARAMS['B'] = new AAParams('B',00.15,00.50,00.40,-0.50,-0.50,00.30,00.30,00.70,-0.50,-0.50,00.00,115.0270,4.55,7.50,04.05,0.0,1.1);
        AAPARAMS['C'] = new AAParams('C',00.45,00.90,00.20,-0.80,-0.50,00.50,00.40,00.00,-0.80,-0.50,00.00,103.0090,3.55,7.50,00.00,0.0,1.0);
        AAPARAMS['D'] = new AAParams('D',00.15,00.50,00.40,-0.50,-0.50,00.30,00.30,00.70,-0.50,-0.50,00.00,115.0270,4.55,7.50,04.05,0.0,1.1);
        AAPARAMS['E'] = new AAParams('E',00.95,01.00,00.00,00.00,-0.10,00.50,00.10,00.00,00.00,-0.10,00.00,129.0430,4.75,7.70,04.45,0.0,1.1);
        AAPARAMS['F'] = new AAParams('F',10.90,07.50,09.50,10.50,10.30,11.10,08.10,09.50,10.50,10.30,-0.10,147.0638,3.55,7.50,00.00,0.5,1.0);
        AAPARAMS['G'] = new AAParams('G',-0.35,00.20,00.15,-0.90,-0.70,00.00,00.00,00.10,-0.90,-0.70,00.00,057.0210,3.55,7.50,00.00,0.0,0.3);
        AAPARAMS['H'] = new AAParams('H',-1.45,-0.10,-0.20,-1.30,-1.70,-1.00,00.10,-0.20,-1.30,-1.70,00.00,137.0590,3.55,7.50,05.98,0.0,0.6);
        AAPARAMS['I'] = new AAParams('I',08.00,05.20,06.60,08.40,07.70,07.70,05.00,06.80,08.40,07.70,00.15,113.0840,3.55,7.50,00.00,3.5,1.4);
        AAPARAMS['K'] = new AAParams('K',-2.05,-0.60,-1.50,-1.90,-1.45,-0.20,-1.40,-1.30,-2.20,-1.45,00.00,128.0950,3.55,7.50,10.00,0.0,1.0);
        AAPARAMS['L'] = new AAParams('L',09.30,05.55,07.40,09.60,09.30,09.20,06.00,07.90,09.60,08.70,00.30,113.0840,3.55,7.50,00.00,1.6,1.6);
        AAPARAMS['M'] = new AAParams('M',06.20,04.40,05.70,05.80,06.00,06.20,05.00,05.70,05.80,06.00,00.00,131.0400,3.55,7.00,00.00,1.8,1.0);
        AAPARAMS['N'] = new AAParams('N',-0.85,00.20,-0.20,-1.20,-1.10,-0.85,00.20,-0.20,-1.20,-1.10,00.00,114.0430,3.55,7.50,00.00,0.0,0.4);
        AAPARAMS['P'] = new AAParams('P',02.10,02.10,02.10,00.20,02.10,03.00,01.00,01.50,00.20,02.10,00.00,097.0530,3.55,8.36,00.00,0.0,0.3);
        AAPARAMS['Q'] = new AAParams('Q',-0.40,-0.70,-0.20,-0.90,-1.10,-0.40,-0.80,-0.20,-0.90,-1.10,00.00,128.0590,3.55,7.50,00.00,0.0,1.0);
        AAPARAMS['R'] = new AAParams('R',-1.40,00.50,-1.10,-1.30,-1.10,-0.20,00.50,-1.10,-1.20,-1.10,00.00,156.1010,3.55,7.50,12.00,0.0,1.0);
        AAPARAMS['S'] = new AAParams('S',-0.15,00.80,-0.10,-0.80,-1.20,-0.50,00.40,00.10,-0.80,-1.20,00.00,087.0320,3.55,6.93,00.00,0.0,1.0);
        AAPARAMS['T'] = new AAParams('T',00.65,00.80,00.60,00.40,00.00,00.60,00.80,00.40,00.40,00.00,00.00,101.0480,3.55,6.82,00.00,0.0,1.0);
        AAPARAMS['V'] = new AAParams('V',05.00,02.90,03.40,05.00,04.20,05.10,02.70,03.40,05.00,04.20,-0.30,099.0680,3.55,7.44,00.00,1.4,1.2);
        AAPARAMS['W'] = new AAParams('W',12.25,11.10,11.80,11.00,12.10,12.40,11.60,11.80,11.00,12.10,00.15,186.0790,3.55,7.50,00.00,1.6,1.0);
        AAPARAMS['X'] = new AAParams('X',00.00,00.00,00.00,00.00,00.00,00.00,00.00,00.00,00.00,00.00,00.00,000.0000,0.00,0.00,00.00,0.0,1.0);
        AAPARAMS['Y'] = new AAParams('Y',04.85,03.70,04.50,04.00,04.40,05.10,04.20,04.50,04.00,04.40,-0.20,163.0630,3.55,7.50,10.00,0.2,1.0);
        AAPARAMS['Z'] = new AAParams('Z',00.95,01.00,00.00,00.00,-0.10,00.50,00.10,00.00,00.00,-0.10,00.00,129.0430,4.75,7.70,04.45,0.0,1.1);
    }

    private static final class Isoparams {
        double emin,emax,eK;
        Isoparams(double EMIN, double EMAX, double EK) {
           emin=EMIN; emax=EMAX; eK=EK;
        }
    }

    static {
       Isoparams ISOPARAMS[] = {
          new Isoparams( 3.8 ,  4.0 , 0.880 ),
          new Isoparams( 4.0 ,  4.2 , 0.900 ),
          new Isoparams( 4.2 ,  4.4 , 0.920 ),
          new Isoparams( 4.4 ,  4.6 , 0.940 ),
          new Isoparams( 4.6 ,  4.8 , 0.960 ),
          new Isoparams( 4.8 ,  5.0 , 0.980 ),
          new Isoparams( 5.0 ,  6.0 , 0.990 ),
          new Isoparams( 6.0 ,  7.0 , 0.995 ),
          new Isoparams( 7.0 ,  8.0 , 1.005 ),
          new Isoparams( 8.0 ,  9.0 , 1.010 ),
          new Isoparams( 9.0 ,  9.2 , 1.020 ),
          new Isoparams( 9.2 ,  9.4 , 1.030 ),
          new Isoparams( 9.4 ,  9.6 , 1.040 ),
          new Isoparams( 9.6 ,  9.8 , 1.060 ),
          new Isoparams( 9.8 , 10.0 , 1.080 )
       };
    }
/*
  Translator's note:  For the Java version we are prepending and appending 0s to the "pick" (key) column.  This
  is done dynamically and repeatedly in the perl code.  As far as I can tell, pick is never used
  without the surrounding 0s.
*/

    private static final HashMap<String,Double> CLUSTCOMB = new HashMap<String,Double>();
    static {
        CLUSTCOMB.put("0110"     , 0.3);
        CLUSTCOMB.put("0150"     , 0.4);
        CLUSTCOMB.put("0510"     , 0.4);
        CLUSTCOMB.put("0550"     , 1.3);
        CLUSTCOMB.put("01110"    , 0.5);
        CLUSTCOMB.put("01150"    , 0.7);
        CLUSTCOMB.put("01510"    , 0.7);
        CLUSTCOMB.put("01550"    , 2.1);
        CLUSTCOMB.put("05110"    , 0.7);
        CLUSTCOMB.put("05150"    , 2.1);
        CLUSTCOMB.put("05510"    , 2.1);
        CLUSTCOMB.put("05550"    , 2.8);
        CLUSTCOMB.put("011110"   , 0.7);
        CLUSTCOMB.put("011150"   , 0.9);
        CLUSTCOMB.put("011510"   , 0.9);
        CLUSTCOMB.put("011550"   , 2.2);
        CLUSTCOMB.put("015110"   , 0.9);
        CLUSTCOMB.put("015150"   , 2.2);
        CLUSTCOMB.put("015510"   , 0.9);
        CLUSTCOMB.put("015550"   , 3.0);
        CLUSTCOMB.put("051110"   , 0.9);
        CLUSTCOMB.put("051150"   , 2.2);
        CLUSTCOMB.put("051510"   , 2.2);
        CLUSTCOMB.put("051550"   , 3.0);
        CLUSTCOMB.put("055110"   , 2.2);
        CLUSTCOMB.put("055150"   , 3.0);
        CLUSTCOMB.put("055510"   , 3.0);
        CLUSTCOMB.put("055550"   , 3.5);
        CLUSTCOMB.put("0111110"  , 0.9);
        CLUSTCOMB.put("0111150"  , 1.0);
        CLUSTCOMB.put("0111510"  , 1.0);
        CLUSTCOMB.put("0111550"  , 2.3);
        CLUSTCOMB.put("0115110"  , 1.0);
        CLUSTCOMB.put("0115150"  , 2.3);
        CLUSTCOMB.put("0115510"  , 2.3);
        CLUSTCOMB.put("0115550"  , 3.1);
        CLUSTCOMB.put("0151110"  , 1.0);
        CLUSTCOMB.put("0151150"  , 2.3);
        CLUSTCOMB.put("0151510"  , 2.3);
        CLUSTCOMB.put("0151550"  , 3.1);
        CLUSTCOMB.put("0155110"  , 2.3);
        CLUSTCOMB.put("0155150"  , 3.1);
        CLUSTCOMB.put("0155510"  , 3.1);
        CLUSTCOMB.put("0155550"  , 3.6);
        CLUSTCOMB.put("0511110"  , 1.0);
        CLUSTCOMB.put("0511150"  , 2.3);
        CLUSTCOMB.put("0511510"  , 2.3);
        CLUSTCOMB.put("0511550"  , 3.1);
        CLUSTCOMB.put("0515110"  , 3.6);
        CLUSTCOMB.put("0515150"  , 2.3);
        CLUSTCOMB.put("0515510"  , 3.1);
        CLUSTCOMB.put("0515550"  , 3.6);
        CLUSTCOMB.put("0551110"  , 2.3);
        CLUSTCOMB.put("0551150"  , 3.1);
        CLUSTCOMB.put("0551510"  , 3.1);
        CLUSTCOMB.put("0551550"  , 3.6);
        CLUSTCOMB.put("0555110"  , 3.1);
        CLUSTCOMB.put("0555150"  , 3.6);
        CLUSTCOMB.put("0555510"  , 3.6);
        CLUSTCOMB.put("0555550"  , 4.0);
        CLUSTCOMB.put("01111110" , 1.1);
        CLUSTCOMB.put("01111150" , 1.7);
        CLUSTCOMB.put("01111510" , 1.7);
        CLUSTCOMB.put("01111550" , 2.5);
        CLUSTCOMB.put("01115110" , 1.7);
        CLUSTCOMB.put("01115150" , 2.5);
        CLUSTCOMB.put("01115510" , 2.5);
        CLUSTCOMB.put("01115550" , 3.3);
        CLUSTCOMB.put("01151110" , 1.7);
        CLUSTCOMB.put("01151150" , 2.5);
        CLUSTCOMB.put("01151510" , 2.5);
        CLUSTCOMB.put("01151550" , 3.3);
        CLUSTCOMB.put("01155110" , 2.5);
        CLUSTCOMB.put("01155150" , 3.3);
        CLUSTCOMB.put("01155510" , 3.3);
        CLUSTCOMB.put("01155550" , 3.7);
        CLUSTCOMB.put("01511110" , 1.7);
        CLUSTCOMB.put("01511150" , 2.5);
        CLUSTCOMB.put("01511510" , 2.5);
        CLUSTCOMB.put("01511550" , 3.3);
        CLUSTCOMB.put("01515110" , 2.5);
        CLUSTCOMB.put("01515150" , 3.3);
        CLUSTCOMB.put("01515510" , 3.3);
        CLUSTCOMB.put("01515550" , 3.7);
        CLUSTCOMB.put("01551110" , 2.5);
        CLUSTCOMB.put("01551150" , 3.3);
        CLUSTCOMB.put("01551510" , 3.3);
        CLUSTCOMB.put("01551550" , 3.7);
        CLUSTCOMB.put("01555110" , 3.3);
        CLUSTCOMB.put("01555150" , 3.7);
        CLUSTCOMB.put("01555510" , 3.7);
        CLUSTCOMB.put("01555550" , 4.1);
        CLUSTCOMB.put("05111110" , 1.7);
        CLUSTCOMB.put("05111150" , 2.5);
        CLUSTCOMB.put("05111510" , 2.5);
        CLUSTCOMB.put("05111550" , 3.3);
        CLUSTCOMB.put("05115110" , 2.5);
        CLUSTCOMB.put("05115150" , 3.3);
        CLUSTCOMB.put("05115510" , 3.3);
        CLUSTCOMB.put("05115550" , 3.7);
        CLUSTCOMB.put("05151110" , 2.5);
        CLUSTCOMB.put("05151150" , 3.3);
        CLUSTCOMB.put("05151510" , 3.3);
        CLUSTCOMB.put("05151550" , 3.7);
        CLUSTCOMB.put("05155110" , 3.3);
        CLUSTCOMB.put("05155150" , 3.7);
        CLUSTCOMB.put("05155510" , 3.7);
        CLUSTCOMB.put("05155550" , 4.1);
        CLUSTCOMB.put("05511110" , 2.5);
        CLUSTCOMB.put("05511150" , 3.3);
        CLUSTCOMB.put("05511510" , 3.3);
        CLUSTCOMB.put("05511550" , 3.7);
        CLUSTCOMB.put("05515110" , 3.3);
        CLUSTCOMB.put("05515150" , 3.7);
        CLUSTCOMB.put("05515510" , 3.7);
        CLUSTCOMB.put("05515550" , 4.1);
        CLUSTCOMB.put("05551110" , 3.3);
        CLUSTCOMB.put("05551150" , 3.7);
        CLUSTCOMB.put("05551510" , 3.7);
        CLUSTCOMB.put("05551550" , 4.1);
        CLUSTCOMB.put("05555110" , 3.7);
        CLUSTCOMB.put("05555150" , 4.1);
        CLUSTCOMB.put("05555510" , 4.1);
        CLUSTCOMB.put("05555550" , 4.5);
    }

    private static final HashMap<String,Double> HlxScore4 = new HashMap<String,Double>();
    private static final HashMap<String,Double> HlxScore5 = new HashMap<String,Double>();
    private static final HashMap<String,Double> HlxScore6 = new HashMap<String,Double>();

    static {
        HlxScore4.put("XXUX"  , 0.8);
        HlxScore4.put("XZOX"  , 0.8);
        HlxScore4.put("XUXX"  , 0.8);
        HlxScore4.put("XXOX"  , 0.7);
        HlxScore4.put("XOXX"  , 0.7);
        HlxScore4.put("XZUX"  , 0.7);
        HlxScore4.put("XXOZ"  , 0.7);
        HlxScore4.put("ZXOX"  , 0.7);
        HlxScore4.put("XOZZ"  , 0.7);
        HlxScore4.put("ZOXX"  , 0.7);
        HlxScore4.put("ZOZX"  , 0.7);
        HlxScore4.put("ZUXX"  , 0.7);
        HlxScore4.put("ZXUX"  , 0.5);
        HlxScore4.put("XOZX"  , 0.5);
        HlxScore4.put("XZOZ"  , 0.5);
        HlxScore4.put("XUZX"  , 0.5);
        HlxScore4.put("ZZOX"  , 0.2);
        HlxScore4.put("ZXOZ"  , 0.2);
        HlxScore4.put("ZOXZ"  , 0.2);
        HlxScore4.put("XOXZ"  , 0.2);
        HlxScore4.put("ZZUZ"  , 0.2);
        HlxScore4.put("XUXZ"  , 0.2);
        HlxScore4.put("ZUXZ"  , 0.2);
        HlxScore4.put("XZUZ"  , 0.2);
        HlxScore4.put("XUZZ"  , 0.2);
        HlxScore4.put("ZXUZ"  , 0.2);
        HlxScore4.put("ZOZZ"  , 0.2);
        HlxScore4.put("ZZOZ"  , 0.2);
        HlxScore4.put("ZZUX"  , 0.2);
        HlxScore4.put("ZUZX"  , 0.2);
        HlxScore4.put("XXUZ"  , 0.2);
        HlxScore4.put("ZUZZ"  , 0.2);

        HlxScore5.put("XXOXX" , 3.75);
        HlxScore5.put("XXOXZ" , 3.75);
        HlxScore5.put("XXOZX" , 3.75);
        HlxScore5.put("XZOXX" , 3.75);
        HlxScore5.put("ZXOXX" , 3.75);
        HlxScore5.put("XXOZZ" , 2.7);
        HlxScore5.put("XZOXZ" , 2.7);
        HlxScore5.put("XZOZX" , 2.7);
        HlxScore5.put("ZXOXZ" , 2.7);
        HlxScore5.put("ZXOZX" , 2.7);
        HlxScore5.put("ZZOXX" , 2.7);
        HlxScore5.put("ZXOZZ" , 1.3);
        HlxScore5.put("XZOZZ" , 1.3);
        HlxScore5.put("ZZOXZ" , 1.3);
        HlxScore5.put("ZZOZX" , 1.3);
        HlxScore5.put("ZZOZZ" , 1.3);
        HlxScore5.put("XXUXX" , 3.75);
        HlxScore5.put("XXUXZ" , 3.75);
        HlxScore5.put("XXUZX" , 3.75);
        HlxScore5.put("XZUXX" , 3.75);
        HlxScore5.put("ZXUXX" , 3.75);
        HlxScore5.put("XXUZZ" , 1.1);
        HlxScore5.put("XZUXZ" , 1.1);
        HlxScore5.put("XZUZX" , 1.1);
        HlxScore5.put("ZXUZX" , 1.1);
        HlxScore5.put("ZXUXZ" , 1.1);
        HlxScore5.put("ZZUXX" , 1.1);
        HlxScore5.put("XZUZZ" , 1.3);
        HlxScore5.put("ZXUZZ" , 1.3);
        HlxScore5.put("ZZUXZ" , 1.3);
        HlxScore5.put("ZZUZX" , 1.3);
        HlxScore5.put("ZZUZZ" , 1.3);
        HlxScore5.put("XXOOX" , 1.25);
        HlxScore5.put("ZXOOX" , 1.25);
        HlxScore5.put("XZOOX" , 1.25);
        HlxScore5.put("XOOXX" , 1.25);
        HlxScore5.put("XOOXZ" , 1.25);
        HlxScore5.put("XOOZX" , 1.25);
        HlxScore5.put("XXOOZ" , 1.25);
        HlxScore5.put("ZXOOZ" , 1.25);
        HlxScore5.put("XZOOZ" , 1.25);
        HlxScore5.put("ZZOOX" , 1.25);
        HlxScore5.put("ZZOOZ" , 1.25);
        HlxScore5.put("ZOOXX" , 1.25);
        HlxScore5.put("ZOOXZ" , 1.25);
        HlxScore5.put("ZOOZX" , 1.25);
        HlxScore5.put("XOOZZ" , 1.25);
        HlxScore5.put("ZOOZZ" , 1.25);
        HlxScore5.put("XXOUX" , 1.25);
        HlxScore5.put("ZXOUX" , 1.25);
        HlxScore5.put("XXUOX" , 1.25);
        HlxScore5.put("ZXUOX" , 1.25);
        HlxScore5.put("XOUXX" , 1.25);
        HlxScore5.put("XOUXZ" , 1.25);
        HlxScore5.put("XUOXX" , 1.25);
        HlxScore5.put("XUOXZ" , 1.25);
        HlxScore5.put("XXOUZ" , 0.75);
        HlxScore5.put("ZXOUZ" , 0.75);
        HlxScore5.put("XZOUX" , 0.75);
        HlxScore5.put("XZOUZ" , 0.75);
        HlxScore5.put("ZZOUX" , 0.75);
        HlxScore5.put("ZZOUZ" , 0.75);
        HlxScore5.put("XXUOZ" , 0.75);
        HlxScore5.put("ZXUOZ" , 0.75);
        HlxScore5.put("XZUOX" , 0.75);
        HlxScore5.put("XZUOZ" , 0.75);
        HlxScore5.put("ZZUOX" , 0.75);
        HlxScore5.put("ZZUOZ" , 0.75);
        HlxScore5.put("ZOUXX" , 0.75);
        HlxScore5.put("ZOUXZ" , 0.75);
        HlxScore5.put("XOUZX" , 0.75);
        HlxScore5.put("ZOUZX" , 0.75);
        HlxScore5.put("XOUZZ" , 0.75);
        HlxScore5.put("ZOUZZ" , 0.75);
        HlxScore5.put("ZUOXX" , 0.75);
        HlxScore5.put("ZUOXZ" , 0.75);
        HlxScore5.put("XUOZX" , 0.75);
        HlxScore5.put("ZUOZX" , 0.75);
        HlxScore5.put("XUOZZ" , 0.75);
        HlxScore5.put("ZUOZZ" , 0.75);
        HlxScore5.put("XUUXX" , 1.25);
        HlxScore5.put("XXUUX" , 1.25);
        HlxScore5.put("XXUUZ" , 0.6);
        HlxScore5.put("ZXUUX" , 0.6);
        HlxScore5.put("ZXUUZ" , 0.6);
        HlxScore5.put("XZUUX" , 0.6);
        HlxScore5.put("XZUUZ" , 0.6);
        HlxScore5.put("ZZUUX" , 0.6);
        HlxScore5.put("ZZUUZ" , 0.6);
        HlxScore5.put("ZUUXX" , 0.6);
        HlxScore5.put("XUUXZ" , 0.6);
        HlxScore5.put("ZUUXZ" , 0.6);
        HlxScore5.put("XUUZX" , 0.6);
        HlxScore5.put("ZUUZX" , 0.6);
        HlxScore5.put("XUUZZ" , 0.6);
        HlxScore5.put("ZUUZZ" , 0.6);

        HlxScore6.put("XXOOXX", 3.0);
        HlxScore6.put("XXOOXZ", 3.0);
        HlxScore6.put("ZXOOXX", 3.0);
        HlxScore6.put("ZXOOXZ", 3.0);
        HlxScore6.put("XXOUXX", 3.0);
        HlxScore6.put("XXOUXZ", 3.0);
        HlxScore6.put("XXUOXX", 3.0);
        HlxScore6.put("XXUOXZ", 3.0);
        HlxScore6.put("ZXUOXX", 3.0);
        HlxScore6.put("ZXOUXX", 3.0);
        HlxScore6.put("XXOOZX", 1.6);
        HlxScore6.put("XXOOZZ", 1.6);
        HlxScore6.put("XZOOXX", 1.6);
        HlxScore6.put("XZOOXZ", 1.6);
        HlxScore6.put("XZOOZX", 1.6);
        HlxScore6.put("XZOOZZ", 1.6);
        HlxScore6.put("ZXOOZX", 1.6);
        HlxScore6.put("ZXOOZZ", 1.6);
        HlxScore6.put("ZZOOXX", 1.6);
        HlxScore6.put("ZZOOXZ", 1.6);
        HlxScore6.put("ZXOUXZ", 1.6);
        HlxScore6.put("XZUOXX", 1.6);
        HlxScore6.put("ZXUOXZ", 1.6);
        HlxScore6.put("ZZOOZX", 1.5);
        HlxScore6.put("ZZOOZZ", 1.5);
        HlxScore6.put("XXOUZX", 1.5);
        HlxScore6.put("XXOUZZ", 1.5);
        HlxScore6.put("XZOUXX", 1.5);
        HlxScore6.put("XZOUXZ", 1.5);
        HlxScore6.put("ZXOUZX", 1.5);
        HlxScore6.put("ZXOUZZ", 1.5);
        HlxScore6.put("ZZOUXX", 1.5);
        HlxScore6.put("ZZOUXZ", 1.5);
        HlxScore6.put("XXUOZX", 1.5);
        HlxScore6.put("XXUOZZ", 1.5);
        HlxScore6.put("XZUOXZ", 1.5);
        HlxScore6.put("ZXUOZX", 1.5);
        HlxScore6.put("ZXUOZZ", 1.5);
        HlxScore6.put("ZZUOXX", 1.5);
        HlxScore6.put("ZZUOXZ", 1.5);
        HlxScore6.put("ZZUOZX", 1.25);
        HlxScore6.put("ZZUOZZ", 1.25);
        HlxScore6.put("ZZOUZX", 1.25);
        HlxScore6.put("ZZOUZZ", 1.25);
        HlxScore6.put("XZOUZX", 1.25);
        HlxScore6.put("XZOUZZ", 1.25);
        HlxScore6.put("XZUOZX", 1.25);
        HlxScore6.put("XZUOZZ", 1.25);
        HlxScore6.put("XXUUXX", 1.25);
        HlxScore6.put("XXUUXZ", 1.25);
        HlxScore6.put("ZXUUXX", 1.25);
        HlxScore6.put("XXUUZX", 1.25);
        HlxScore6.put("XXUUZZ", 1.25);
        HlxScore6.put("XZUUXX", 1.25);
        HlxScore6.put("XZUUXZ", 1.25);
        HlxScore6.put("XZUUZX", 0.75);
        HlxScore6.put("XZUUZZ", 0.75);
        HlxScore6.put("ZXUUXZ", 1.25);
        HlxScore6.put("ZXUUZX", 1.25);
        HlxScore6.put("ZXUUZZ", 1.25);
        HlxScore6.put("ZZUUXX", 1.25);
        HlxScore6.put("ZZUUXZ", 1.25);
        HlxScore6.put("ZZUUZX", 0.75);
        HlxScore6.put("ZZUUZZ", 0.75);
     }

// control variables, 0 means leaving them ON, 1 means turning them OFF
// Translator's note:  Some day these may be turned into options.  For the
//    time being they are unchanging, and the tests for them in each function
//    are superfluous and absurd

    private static final int NOELECTRIC=0;
    private static final int NOCLUSTER=0;
    private static final int NODIGEST=0;
    private static final int NOSMALL=0;
    private static final int NOHELIX1=0;
    private static final int NOHELIX2=0;
    private static final int NOEHEL=0;

    //Translator's note.  This constant controls whether "bugs" in the original
    //perl code are maintained.  A conversation with the developers has revealed
    //that the constant data in the static initialization blocks has been "tuned"
    //to the algorithm in its undebugged state.  In other words, using a correct
    //algorithm would invalidate the results.
    private static final boolean DUPLICATE_ORIGINAL_CODE = true;
    //Translator's note:  Some code is supposed to be executed only when
    // $SSRCVERSION==3.  SSRCVERSION was commented out in my version of the perl
    // code.  This may need some reworking.  Speaking with the developers, it
    // was determined that it ought not to have been commented out.  So --
    // ALGORITHM_VERSION may be used to choose the older or newer code
    private static final int ALGORITHM_VERSION = 3;

// Length Scaling length limits and scaling factors
    private static final int LPLim = 20;            // long peptide lower length limit
    private static final int SPLim = 8;             // short peptide upper length limit
    private static final double LPSFac = 0.0270;    // long peptide scaling factor
    private static final double SPSFac = -0.055;    // short peptide scaling factor

// UnDigested (missed cuts) scaling Factors
    private static final double	 UDF21=0.0, UDF22=0.0;    // rightmost
    private static final double  UDF31=1.0, UDF32=0.0;    // inside string

// total correction values, 20..30 / 30..40 / 40..50 /50..500
    private static final double SUMSCALE1=0.27, SUMSCALE2=0.33, SUMSCALE3=0.38, SUMSCALE4=0.447;

// clusterness scaling: i.e. weight to give cluster correction.
    private static final double KSCALE=0.4;

// isoelectric scaling factors
    private static final double	Z01=-0.03,    Z02=0.60,    NDELTAWT = 0.8;   // negative delta values
    private static final double	Z03= 0.00,    Z04=0.00,    PDELTAWT = 1.0;   // positive delta values

// proline chain scores
    private static final double PPSCORE=1.2,	PPPSCORE=3.5,	PPPPSCORE=5.0;

// helix scaling factors
    private static final double	HELIX1SCALE=1.6,	HELIX2SCALE=0.255;

    public static double TSUM3(String sq3) {
       double tsum3 = 0.0;
       int i;
       int sze;

      // Core summation

      sze = sq3.length();
      if(sze < 4) return tsum3;           // peptide is too short ot have any retention
      if(sze < 10) {                      // short peptides use short peptide retention weights
        tsum3 =
           AAPARAMS[sq3.charAt(0)].RC1S +        // Sum weights for 1st
           AAPARAMS[sq3.charAt(1)].RC2S +        // second,
           AAPARAMS[sq3.charAt(sze-1)].RCNS +    // ultimate
           AAPARAMS[sq3.charAt(sze-2)].RCN2S;    // and penultimate aa

        for(i=2; i<sze-2; i++) {                       // add weights for aa's in the middle
           tsum3 += AAPARAMS[sq3.charAt(i)].RCS;
        }
      } else {                            // longer peptides use regular retention weights
         tsum3 =
           AAPARAMS[sq3.charAt(0)].RC1 +         // Sum weights for 1st
           AAPARAMS[sq3.charAt(1)].RC2 +         // second,
           AAPARAMS[sq3.charAt(sze-1)].RCN +     // ultimate
           AAPARAMS[sq3.charAt(sze-2)].RCN2;     // and penultimate aa

         for(i=2; i<sze-2; i++) {                      // add weights for aa's in the middle
            tsum3 += AAPARAMS[sq3.charAt(i)].RC;
         }
      }
      //_log.debug("Core = "+tsum3);

     // 1- smallness - adjust based on tsum score of peptides shorter than 20 aa's.
     tsum3 += smallness(sze,tsum3);
     //_log.debug("smallness = "+tsum3);
     // 2- undigested parts
     tsum3 -= undigested(sq3);
     //_log.debug("undigested = "+tsum3);
     // 3- clusterness # NB:weighting of v1 is now done in subrtn.
     tsum3 -= clusterness(sq3);
     //_log.debug("clusterness = "+tsum3);
     // 4- proline fix
     tsum3 -= proline(sq3);
     //_log.debug("proline = "+tsum3);
     // 5- length scaling correction
     tsum3 *= length_scale(sze);
     //_log.debug("length_scale = "+tsum3);
     // 6- total sum correction
     if (tsum3 >= 20 && tsum3 < 30 ) tsum3-=((tsum3-18) * SUMSCALE1);
     if (tsum3 >= 30 && tsum3 < 40)  tsum3-=((tsum3-18) * SUMSCALE2);
     if (tsum3 >= 40 && tsum3 < 50)  tsum3-=((tsum3-18) * SUMSCALE3);
     if (tsum3 >= 50 )               tsum3-=((tsum3-18) * SUMSCALE4);
     //_log.debug("total sum = "+tsum3);
     // 7- isoelectric change
     tsum3 += newiso(sq3,tsum3);
     //_log.debug("isoelectric = "+tsum3);
     // 8- helicity corrections  #NB: HELIX#SCALE-ing is now done in subrtn.
     tsum3 += helicity1(sq3);
     //_log.debug("helicity1 = "+tsum3);
     tsum3 += helicity2(sq3);
     //_log.debug("helicity2 = "+tsum3);
     tsum3 += helectric(sq3);
     //_log.debug("helectric = "+tsum3);
     return tsum3;
   }

    private static double smallness(int sqlen, double tsum){
        if (NOSMALL == 1) return 0.0;
        if (sqlen < 20) {
          if ((tsum/sqlen) < 0.9) return 3.5*(0.9-(tsum/sqlen));
        }
        if (sqlen < 15) {
          if ((tsum/sqlen) > 2.8) return 2.6*((tsum/sqlen)-2.8);
        }
        return 0.0;
    }

    private static double undigested(String sq){
       if(NODIGEST == 1) return 0.0;

       int xx;
       char re;
       double csum;
       int dd;
       char op1, op2, op3, op4;

       xx = sq.length()-1;
       re = sq.charAt(xx);
       csum = 0.0;

      // rightmost
      if (re == 'R' || re == 'K' || re == 'H') {
         op1 = sq.charAt(xx-1);                          // left by 1
         op2 = sq.charAt(xx-2);                          // left by 2
         csum = UDF21 * AAPARAMS[op1].UndKRH + UDF22 * AAPARAMS[op2].UndKRH;
      }
      // scan through string, starting at second and ending two before left
      //    --translator's note
      //      the perl code does not jibe with the comment above, and will probably need repair
      //      possibly dd should start out as 2, not 0; and should loop to xx-2, not xx.

      //      Note that negative indices on the perl substr function make substrings offset from right
      //      (instead of left) end of string.  The perl loop gets negative indices.  This may be a
      //      a problem.

      for (dd = 0; dd < xx; dd++) {
         re = sq.charAt(dd);
         if (re == 'K' || re == 'R' || re  == 'H') {
            op1 = op2 = op3 = op4 = '\0';
            if(dd-1 >=0 && dd-1 <= xx) op1 = sq.charAt(dd-1);    //left by 1
            if(dd-2 >=0 && dd-2 <= xx) op2 = sq.charAt(dd-2);    //left by 2
            if(DUPLICATE_ORIGINAL_CODE){
               if(dd-1 < 0 && (-(dd-1))<=xx) op1 = sq.charAt(xx+(dd-1)+1);
               if(dd-2 < 0 && (-(dd-2))<=xx) op2 = sq.charAt(xx+(dd-2)+1);
            }
            if(dd+1 >=0 && dd+1 <= xx) op3 = sq.charAt(dd+1);    //right by 1
            if(dd+2 >=0 && dd+2 <= xx) op4 = sq.charAt(dd+2);    //right by 2;

            csum = csum +
               (UDF31 * (AAPARAMS[op1].UndKRH + AAPARAMS[op3].UndKRH)) +
               (UDF32 * (AAPARAMS[op2].UndKRH + AAPARAMS[op4].UndKRH));
         }
      }
      return csum;
    }

// ============================================================
// compute clusterness of a string - v 2,3 algorithm
// code W,L,F,I as 5
// code M,Y,V as 1
// code all others as 0

    private static double clusterness(String sq){
       String cc;
       double score;
       int i;
       String x1;
       int occurs;
       String pt;
       double sk;
       double addit;

       if(NOCLUSTER == 1) return 0.0;
       cc = "0"+sq+"0";
       if(ALGORITHM_VERSION==3) {
           cc = cc.replaceAll("[LIW]","5");
           cc = cc.replaceAll("[AMYV]","1");
           cc = cc.replaceAll("[A-Z]","0");
       } else {
           cc = cc.replaceAll("[LIWF]","5");
           cc = cc.replaceAll("[MYV]","1");
           cc = cc.replaceAll("[A-Z]","0");
       }
       score = 0.0;
//
// Translator's note:  check on true meaning of the algorithm that defines 'occurs'
// Should an encoded aa string such as 015101510 match pick "01510" once or twice?
// The perl code seems to match once.  0151001510 would match twice.

       for(String key: CLUSTCOMB.keySet()){
           occurs = 0;
           Matcher m = Pattern.compile(key).matcher(cc);
           sk = CLUSTCOMB.get(key);
           while(m.find()) occurs++;
           if(occurs>0) {
              addit = sk * occurs;
              score += addit;
           }
       }
       return score*KSCALE;
    }

// ============================================================
//  process based on proline - v 2,3 algorithm
    private static double proline(String sq){
       double score = 0.0;
       if(sq.contains("PP"))   score = PPSCORE;
       if(sq.contains("PPP"))  score = PPPSCORE;
       if(sq.contains("PPPP")) score = PPPPSCORE;
       return score;
    }

// ============================================================
// scaling based on length - v 1,2,3 algorithms {
    private static double length_scale(int sqlen){
       double LS = 1.0;
       if(sqlen < SPLim) {
          LS = 1.0 + SPSFac * (SPLim - sqlen);
       } else {
          if(sqlen > LPLim) {
             LS = 1.0/(1.0 + LPSFac * (sqlen - LPLim));
          }
       }
       return LS;
    }

    private static int eMap(char aa) {
      switch(aa) {
      case 'K': return 0;
      case 'R': return 1;
      case 'H': return 2;
      case 'D': return 3;
      case 'E': return 4;
      case 'C': return 5;
      case 'Y': return 6;
      default: return -1;
      }
    }

// ============================================================
// compute partial charge - v 2,3 algorithms
    private static double _partial_charge(double pK, double pH) {
       double cr = Math.pow(10.0,(pK - pH));
       return cr / ( cr + 1.0 );
    }

// ============================================================
//    - v 2,3 algorithms
    private static double electric(String sq){
       int ss;
       char s1;
       char s2;
       int i;
       double z;
       double best;
       double min;
       double check;
       char e;
       double pk0;
       double pk1;
       double step1;

       int aaCNT[] = {0,0,0,0,0,0,0};

  // Translator's Note: this is commented out in the perl source
  // if (NOELECTRIC == 1) { return 1.0; }

  // get c and n terminus acids
       ss = sq.length();
       s1 = sq.charAt(0);
       s2 = sq.charAt(ss-1);
       pk0 = AAPARAMS[s1].CT;
       pk1 = AAPARAMS[s2].NT;

  // count them up
       for(i=0; i<ss; i++) {
          e = sq.charAt(i);
          int index = eMap(e);
          if(index >= 0) aaCNT[index]++;
        }

  // cycle through pH values looking for closest to zero
  // coarse pass
        best=0.0; min=100000; step1=0.3;

        for(z = 0.01; z <= 14.0; z = z+step1) {
           check = CalcR(z,pk0,pk1,aaCNT);
           if (check<0) check=0-check;
           if (check < min) {
              min = check;
              best = z;
           }
        }

        double best1 = best;

  // fine pass
        min=100000;
        for (z = best1-step1; z <=  best1 + step1; z=z+0.01) {
           check = CalcR(z,pk0,pk1,aaCNT);
           if(check<0) check=0-check;
           if(check < min) {
              min=check;
              best=z;
           }
        }
        return best;
    }

// ============================================================
// compute R - v 2,3 algorithms
    private static double CalcR(double pH, double PK0, double PK1, int CNTref[]){
       double cr0 =
                                _partial_charge( PK0,     pH    )                    // n terminus
          + CNTref[eMap('K')] * _partial_charge( AAPARAMS['K'].PK,   pH    )  // lys
          + CNTref[eMap('R')] * _partial_charge( AAPARAMS['R'].PK,   pH    )  // arg
          + CNTref[eMap('H')] * _partial_charge( AAPARAMS['H'].PK,   pH    )  // his
          - CNTref[eMap('D')] * _partial_charge( pH,   AAPARAMS['D'].PK    )  // asp
          - CNTref[eMap('E')] * _partial_charge( pH,   AAPARAMS['E'].PK    )  // glu
          - CNTref[eMap('Y')] * _partial_charge( pH,   AAPARAMS['Y'].PK    )  // try
          -                     _partial_charge( pH,      PK1                     ); // c terminus
  /*
  // The following was taken out of the formula for R
  //  - $CNTref->{C} * _partial_charge( $pH,      $PK{C} )    // cys
  */
       return cr0;
    }

    private static double newiso(String sq, double tsum){
       int i;
       double mass;
       char cf1;
       double delta1;
       double corr01 = 0.0;
       double pi1;
       double lmass;

       if (NOELECTRIC == 1) return 0.0;
  // compute mass
       mass = 0.0;
       for(i=0; i<sq.length(); i++) {
          cf1 = sq.charAt(i);
          mass += AAPARAMS[cf1].AMASS;
       }
  // compute isoelectric value
       pi1 = electric(sq);
       lmass = 1.8014 * Math.log(mass);

  // make mass correction
       delta1 = pi1 - 19.107 + lmass;
  //apply corrected value as scaling factor

       if (delta1 < 0.0) {
          corr01 = (tsum * Z01 + Z02) * NDELTAWT * delta1;
       }
       if (delta1 > 0.0) {
          corr01 = (tsum * Z03 + Z04) * PDELTAWT * delta1;
       }
       return corr01;
    }

// ============================================================
// called by helicity1  - v 3 algorithm
    private static double heli1TermAdj(String ss1, int ix2, int sqlen){
       char m;
       int where=0;
       int i;

       for(i = 0; i < ss1.length(); i++) {
          m = ss1.charAt(i);
          if(m == 'O' || m == 'U') {
             where = i;
             if(!DUPLICATE_ORIGINAL_CODE)break;
          }
       }

       where += ix2;

       if (where<2) { return 0.20; }
       if (where<3) { return 0.25; }
       if (where<4) { return 0.45; }

       if (where>sqlen-3) { return 0.2;  }
       if (where>sqlen-4) { return 0.75; }
       if (where>sqlen-5) { return 0.65; }

       return 1.0;
    }

// ============================================================
// helicity1 adjust for short helices or sections - v 3 algorithm
//
    private static double helicity1(String sq){
       String hc; //helicity coded sq
       int i,j;
       double sum;
       String hc4,hc5,hc6;
       double sc4, sc5, sc6;
       double trmAdj4, trmAdj5, trmAdj6;
       int sqlen;

       if (NOHELIX1 == 1) { return 0.0; }

       hc = sq;

  /* translator's note:  notice lowercase 'z'.  This never appears in any patterns to which this
     string is compared, and will never match any helicity patterns.
  */
       hc = hc.replaceAll("[PHRK]","z");
       hc = hc.replaceAll("[WFIL]","X");
       hc = hc.replaceAll("[YMVA]","Z");
       hc = hc.replaceAll("[DE]","O");
       hc = hc.replaceAll("[GSPCNKQHRT]","U");

       sum = 0.0;
       sqlen = hc.length();

  //Translator's note:
  //this loop should be reviewed  carefully

       for (i=0; i<sqlen-3; i++){
          hc6=hc5=hc4="";
          sc6=sc5=sc4=0.0;
          if(hc.substring(i).length() >= 6) {
             hc6 = hc.substring(i,i+6);
             sc6 = 0.0;
             if(HlxScore6.get(hc6) != null) {
                sc6 = HlxScore6.get(hc6);
             }
          }
          if(sc6 > 0) {
             trmAdj6 = heli1TermAdj(hc6,i,sqlen);
             sum += (sc6 * trmAdj6);
             i=i+1; //??
             continue;
          }

          if(hc.substring(i).length() >= 5) {
             hc5 =hc.substring(i,i+5);
             sc5 = 0.0;
             if(HlxScore5.get(hc5) != null) {
                sc5 = HlxScore5.get(hc5);
             }
          }
          if(sc5 > 0) {
             trmAdj5 = heli1TermAdj(hc5,i,sqlen);
             sum += (sc5 * trmAdj5);
             i=i+1; //??
             continue;
          }

          if(hc.substring(i).length() >= 4) {
             hc4 = hc.substring(i,i+4);
             sc4 = 0.0;
             if(HlxScore4.get(hc4) != null) {
                sc4 = HlxScore4.get(hc4);
             }
          }
          if(sc4 > 0) {
             trmAdj4 = heli1TermAdj(hc4,i,sqlen);
             sum += (sc4 * trmAdj4);
             i=i+1; //??
             continue;
          }
       }
       return  HELIX1SCALE * sum;
    }

// ============================================================
// called by heli2calc  - v 3 algorithm
    private static double evalH2pattern(String pattern, String testsq, int posn, char etype){
       char f01 = pattern.charAt(0);
       double prod1 = AAPARAMS[f01].H2BASCORE;
       int i;
       double mult = 1.0;
       String fpart;
       char gpart;
       double s3;
       char testAAl, testAAr;
       int iss=0;
       int OFF1 = 2;
       String testsqCopy;
       int acount = 1;
       char far1 = '\0';
       char far2 = '\0';

       testAAl = testsq.charAt(OFF1+posn);
       testAAr = testsq.charAt(OFF1+posn+2);
       testsqCopy = testsq.substring(OFF1+posn+1);
       mult = connector(f01,testAAl,testAAr,"--",far1,far2);
       prod1 = prod1*mult;
       if (etype == '*') prod1 = prod1 * 25.0;
       if(mult == 0.0) {
          return 0.0;
       }
       for(i=1; i<pattern.length()-2; i = i + 3) {
          fpart = pattern.substring(i,i+2);
          if((i+2) < pattern.length()){
              gpart = pattern.charAt(i+2);
          }  else {
              gpart = '\0';
          }
          s3 = AAPARAMS[gpart].H2BASCORE;
          if(fpart.equals("--")) {
             iss=0; far1='\0'; far2='\0';
          }
          if(fpart.equals("<-")) {
             iss=1; far1=testsqCopy.charAt(i+1); far2='\0';
          }
          if(fpart.equals("->")) {
             iss=-1; far1='\0'; far2=testsqCopy.charAt(i+3);
          }

          testAAl = testsqCopy.charAt(i+1+iss);
          testAAr = testsqCopy.charAt(i+3+iss);

          mult = connector(gpart,testAAl,testAAr,fpart,far1,far2);

          if(etype == '*') {
             if( mult != 0.0 || acount < 3) {
	            prod1 = prod1 * 25.0 * s3 * mult;
             }
          }

          if(etype == '+') {
             prod1 = prod1 + s3 * mult;
          }

          if(mult == 0.0) {
             return prod1;
          } else {}

          acount++;
       }
       return prod1;
    }

// ============================================================
// called by evalH2pattern  - v 3 algorithm
    private static double connector(char acid, char lp, char rp, String ct, char far1, char far2){
       double mult = 1.0;

       if (ct.contains("<-")) { mult *= 0.2; }
       if (ct.contains("->")) { mult *= 0.1; }

       mult *= AAPARAMS[lp].H2CMULT;
       if(lp != rp) mult *= AAPARAMS[rp].H2CMULT;

       if(acid == 'A' || acid == 'Y' || acid == 'V' || acid == 'M') {
          if(lp == 'P' || lp == 'G' || rp == 'P' || rp == 'G') mult = 0.0;
          if(ct.contains("->") || ct.contains("<-")) mult = 0.0;
       }

       if(acid == 'L' || acid == 'W' || acid == 'F' || acid == 'I') {
          if(((lp == 'P' || lp == 'G') || (rp == 'P' || rp == 'G')) && (!ct.contains("--"))) mult = 0.0;
          if(((far1 == 'P' || far1 == 'G') || (far2 == 'P' || far2 == 'G')) && (ct.contains("<-") || ct.contains("->"))) mult = 0.0;
       }
       return mult;
    }

    private static final int HISC = 0;
    private static final int GSC  = 1;

// ============================================================
// called by helicity2  - v 3 algorithm
    private static double[] heli2Calc(String sq){
    // Translator's note: in the original perl and translated C, this function
    // was void and returned values through double pointer arguments. Like this:
    //
    // void  heli2Calc(char *sq, double *hisc, double *gsc)
    //

       double ret[] = new double[2];
       String pass1;
       String sqCopy;
       String prechop;
       String traps; //not my()'ed in perl source
       int i;
       char m;
       String lc;
       String pat;
       String best = "";
       int zap;
       char f1,f2,f3;
       int subt;
       String sq2;
       int llim=50;
       double hiscore=0.0;
       double skore;
       double gscore;   //not my()'ed in perl source
       int best_pos = 0;
       String tmp = "x";

       if (sq.length() < 11) {
          ret[HISC] = 0.0;
          ret[GSC]  = 0.0;
          return ret;
       }

       prechop = sq;
       sqCopy = sq.substring(2,sq.length()-2);

       pass1 = sqCopy.replaceAll("[WFILYMVA]","1");
       pass1 = pass1.replaceAll("[GSPCNKQHRTDE]","0");

       gscore = 0.0;

       for (i=0; i<pass1.length(); i++) {
          m = pass1.charAt(i);
          if(m == '1') {
             lc = pass1.substring(i);
             sq2 = sqCopy.substring(i);
             pat="";
             zap = 0; subt = 0;

             while(zap<=llim && subt<2) {
	            if(zap < 0 || zap >= lc.length()) {f1 = '0';} else {f1 = lc.charAt(zap); }
                if(zap-1 < 0 || zap-1 >= lc.length()) {f2 = '0';} else {f2 = lc.charAt(zap-1);}
                if(zap+1 < 0 || zap+1 >= lc.length()) {f3 = '0';} else {f3 = lc.charAt(zap+1);}

                if(f1 == '1') {
	               if(zap > 0) pat += "--";
                   tmp = sq2.substring(zap,zap+1);
                   pat += tmp;
                } else
	               if(f2 == '1' && f1 == '0') {
	                 subt++;
                     if(subt < 2) {
	                   pat += "->";
                       tmp=sq2.substring(zap-1,zap);
                       pat += tmp;
                     }
                   } else
	                  if(f3 == '1' && f1 == '0') {
	                     subt++;
                         if(subt < 2) {
		                   pat += "<-";
                           tmp = sq2.substring(zap+1,zap+2);
                           pat += tmp;
                         }
	                  }

	     if(f1 == '0' && f2 == '0' && f3 == '0') zap = 1000;
         zap += 3;
      }
      if(pat.length() > 4) {
	     traps = prechop;
         skore = evalH2pattern(pat,traps,i-1,'*');
         if(skore>=hiscore) {
	        hiscore=skore;
            best = pat;
            best_pos=i;
         }
       }
    }
  }

  if(hiscore > 0.0) {
    gscore=hiscore;
    traps = prechop;
    hiscore=evalH2pattern(best,traps,best_pos-1,'+');

    ret[HISC] = hiscore;
    ret[GSC] = gscore;
    return ret;
  } else {
     ret[HISC] = 0.0;
     ret[GSC] = 0.0;
     return ret;
  }
  }

// ============================================================
// helicity2 adjust for long helices - v 3 algorithm
    private static double helicity2(String sq){
       if(NOHELIX2==1)  return 0.0;
       double h2FwBk,FwHiscor,FwGscor,BkHiscor,BkGscor;
       double h2mult,lenMult,NoPMult;
       String Bksq = "";
       int i;
       Bksq = new String(new StringBuffer(sq).reverse());
       double fhg[] = heli2Calc(sq);
       FwHiscor = fhg[HISC]; FwGscor = fhg[GSC];
       double rhg[] = heli2Calc(Bksq);
       BkHiscor = rhg[HISC]; BkGscor = rhg[GSC];
       if (BkGscor>FwGscor)
          { h2FwBk = BkHiscor; }
       else
          { h2FwBk = FwHiscor; }
       lenMult = 0.0;
       if (sq.length()>30) { lenMult=1; }
       NoPMult = 0.75;
       if(sq.contains("P")) NoPMult = 0.0;
       h2mult = 1.0 + lenMult + NoPMult;
       return HELIX2SCALE * h2mult * h2FwBk;
    }

    private static double helectric(String sq){
       if (NOEHEL==1 ) { return 0.0; }
       if(sq.length() > 14) return 0.0;
       if(sq.length() < 4) return 0.0;
       String mpart = sq.substring(sq.length()-4);

       if(mpart.charAt(0) == 'D' || mpart.charAt(0) == 'E') {
          mpart = mpart.substring(1,3);
          if(mpart.matches(".*[PGKRH].*")) return 0.0;
          mpart = mpart.replaceAll("[LI]","X");
          mpart = mpart.replaceAll("[AVYFWM]","Z");
          mpart = mpart.replaceAll("[GSPCNKQHRTDE]","U");

          if(mpart.equals("XX")) return 1.0;
          if(mpart.equals("ZX")) return 0.5;
          if(mpart.equals("XZ")) return 0.5;
          if(mpart.equals("ZZ")) return 0.4;
          if(mpart.equals("XU")) return 0.4;
          if(mpart.equals("UX")) return 0.4;
          if(mpart.equals("ZU")) return 0.2;
          if(mpart.equals("UZ")) return 0.2;
       }
       return 0;
    }

    public static void main(String argv[]) {
//        BasicConfigurator.configure();
        String pep;
        try {
           BufferedReader in = new BufferedReader(new FileReader(argv[0]));
           while((pep = in.readLine()) != null) {
              System.out.println(pep+"\t"+TSUM3(pep));
           }
        } catch (Exception e)  {
           System.err.println("problem in hydrophobicity test: "+e);
           e.printStackTrace();
        }
    }
}
