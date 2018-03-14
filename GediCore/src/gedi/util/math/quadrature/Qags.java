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
package gedi.util.math.quadrature;

import java.util.function.DoubleUnaryOperator;

/** qagse.java, qags.java, qelg.java, qk21.java, and qpsrt.java are
 * derivative works (translations) of Fortran code by Robert Piessens
 * and Elise de Doncker. These five files are released under GPL
 * by permission of Robert Piessens. In response to my question,
 * <pre>
 *  >I would like to have your permission to distribute my
 *  >Java translation of your QUADPACK routines under the
 *  >terms of the GPL. Do I have your permission to do so?
 * </pre>
 * Robert Piessens writes:
 * <pre>
 *  Date:  Mon, 28 Jan 2002 14:41:58 +0100
 *  To: "Robert Dodier" <robert_dodier@yahoo.com>
 *  From: "Robert Piessens" <Robert.Piessens@cs.kuleuven.ac.be>
 *  Subject: Re: Permission to redistribute QUADPACK translation?
 *  
 *  OK, You have my permission.
 *  
 *  Robert Piessens
 * </pre>
 */
public class Qags implements java.io.Serializable
{
	public int[] iwork = null;
	public double[] work = null;
	public int[] neval = new int[1], last = new int[1];

	public boolean verbose_errors = false;

	public void do_qags ( DoubleUnaryOperator f,double a,double b,double epsabs,double epsrel,double [ ] result,double [ ] abserr, int [ ] ier, int limit ) throws Exception
	{
		int lenw = 4*limit;
		if ( iwork == null || iwork.length != limit ) iwork = new int[ limit ];
		if ( work == null || work.length != lenw ) work = new double[ lenw ];

		int lvl = 0,l1,l2,l3;

		ier [ 0 ] = 6;
		neval[0] = 0;
		last[0] = 0;
		result [ 0 ] = 0;
		abserr [ 0 ] = 0;
		if ( ! ( limit < 1 || lenw < limit * 4 ) )
		{
			l1 = limit+1;
			l2 = limit+l1;
			l3 = limit+l2;

			double[] alist = new double[ limit ];
			double[] blist = new double[ limit ];
			double[] rlist = new double[ limit ];
			double[] elist = new double[ limit ];

			Qagse.do_qagse ( f , a , b , epsabs , epsrel , limit , result , abserr , neval , ier , alist , blist , rlist , elist , iwork , last );

			System.arraycopy( alist, 0, work, 0, limit );
			System.arraycopy( blist, 0, work, limit, limit );
			System.arraycopy( rlist, 0, work, 2*limit, limit );
			System.arraycopy( elist, 0, work, 3*limit, limit );

			lvl = 0;
		}
		if ( ier [ 0 ] == 6 ) lvl = 1;
		if ( ier [ 0 ] != 0 && verbose_errors ) System.err.println ( xerror ( "abnormal return from do_qags" ,ier [ 0 ] ,lvl ) );
		return;
	}

	public static String xerror ( String message, int errno, int level )
	{
		String level_msg;
		switch ( level )
		{
			case -1:
				level_msg = "(one-time warning)";
				break;
			case 0:
				level_msg = "(warning)";
				break;
			case 1:
				level_msg = "(recoverable error)";
				break;
			case 2:
				level_msg = "(fatal error)";
				break;
			default:
				level_msg = "(unknown level: " +level+ ")";
		}
		return message+ ", error number: " +errno+ " " +level_msg;
	}

	public static void main( String[] args )
	{
		double a, b;
		double epsabs = 1e-6, epsrel = 1e-6;
		double[] result = new double[1];
		double[] abserr = new double[1];
		double[] resabs = new double[1];
		double[] resasc = new double[1];

		a = Double.parseDouble( args[0] );
		b = Double.parseDouble( args[1] );
		int limit = Integer.parseInt( args[2] );
		System.err.println( "a: "+a+"  b: "+b+"  limit: "+limit );
		DoubleUnaryOperator integrand = new Gaussian_bump();

		int[] ier = new int[1];
		int lenw = 4*limit;

		Qags q = new Qags();

		try
		{
			q.do_qags( integrand, a, b, epsabs, epsrel, result, abserr, ier, limit );
		}
		catch (Exception e) { e.printStackTrace(); return; }

		System.err.println( "result: "+result[0] );
		System.err.println( "abserr: "+abserr[0] );
		System.err.println( "neval:  "+q.neval[0] );
		System.err.println( "ier:    "+ier[0] );

		System.err.println( "limit: "+limit+" last: "+q.last[0] );
		int i;
		System.err.print( "iwork: " );
		for ( i = 0; i < q.iwork.length; i++ ) System.err.print( q.iwork[i]+" " );
		System.err.println("");
		System.err.println( "a\tb\tI\terr  (from work):" );
		for ( i = 0; i < q.last[0]; i++ )
		{
			System.err.println( q.work[i]+"\t"+q.work[limit+i]+"\t"+q.work[2*limit+i]+"\t"+q.work[3*limit+i] );
		}
	}
}

class Gaussian_bump implements DoubleUnaryOperator
{
	public double applyAsDouble( double x )
	{
		return Math.exp( -(1/2.0)*x*x )/Math.sqrt( 2*Math.PI );
	}
}