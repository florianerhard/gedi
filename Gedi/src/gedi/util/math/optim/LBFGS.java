package gedi.util.math.optim;

import gedi.util.math.optim.LBFGS.Params;
import gedi.util.math.optim.LBFGS.ProgressCallback;


/**
 * a port of liblbfgs to java
 * http://www.chokkan.org/software/liblbfgs/
 *
 *
	@mainpage libLBFGS: a library of Limited-memory Broyden-Fletcher-Goldfarb-Shanno (L-BFGS)

	@section intro Introduction

	This library is a C port of the implementation of Limited-memory
	Broyden-Fletcher-Goldfarb-Shanno (L-BFGS) method written by Jorge Nocedal.
	The original FORTRAN source code is available at:
	http://www.ece.northwestern.edu/~nocedal/lbfgs.html

	The L-BFGS method solves the unconstrainted minimization problem,

	<pre>
	    minimize F(x), x = (x1, x2, ..., xN),
	</pre>

	only if the objective function F(x) and its gradient G(x) are computable. The
	well-known Newton's method requires computation of the inverse of the hessian
	matrix of the objective function. However, the computational cost for the
	inverse hessian matrix is expensive especially when the objective function
	takes a large number of variables. The L-BFGS method iteratively finds a
	minimizer by approximating the inverse hessian matrix by information from last
	m iterations. This innovation saves the memory storage and computational time
	drastically for large-scaled problems.

	Among the various ports of L-BFGS, this library provides several features:
	- <b>Optimization with L1-norm (Orthant-Wise Limited-memory Quasi-Newton
	  (OWL-QN) method)</b>:
	  In addition to standard minimization problems, the library can minimize
	  a function F(x) combined with L1-norm |x| of the variables,
	  {F(x) + C |x|}, where C is a constant scalar parameter. This feature is
	  useful for estimating parameters of sparse log-linear models (e.g.,
	  logistic regression and maximum entropy) with L1-regularization (or
	  Laplacian prior).
	- <b>Clean C code</b>:
	  Unlike C codes generated automatically by f2c (Fortran 77 into C converter),
	  this port includes changes based on my interpretations, improvements,
	  optimizations, and clean-ups so that the ported code would be well-suited
	  for a C code. In addition to comments inherited from the original code,
	  a number of comments were added through my interpretations.
	- <b>Callback interface</b>:
	  The library receives function and gradient values via a callback interface.
	  The library also notifies the progress of the optimization by invoking a
	  callback function. In the original implementation, a user had to set
	  function and gradient values every time the function returns for obtaining
	  updated values.
	- <b>Thread safe</b>:
	  The library is thread-safe, which is the secondary gain from the callback
	  interface.
	- <b>Cross platform.</b> The source code can be compiled on Microsoft Visual
	  Studio 2010, GNU C Compiler (gcc), etc.
	- <b>Configurable precision</b>: A user can choose single-precision (float)
	  or double-precision (double) accuracy by changing ::LBFGS_FLOAT macro.
	- <b>SSE/SSE2 optimization</b>:
	  This library includes SSE/SSE2 optimization (written in compiler intrinsics)
	  for vector arithmetic operations on Intel/AMD processors. The library uses
	  SSE for float values and SSE2 for double values. The SSE/SSE2 optimization
	  routine is disabled by default.

	This library is used by:
	- <a href="http://www.chokkan.org/software/crfsuite/">CRFsuite: A fast implementation of Conditional Random Fields (CRFs)</a>
	- <a href="http://www.chokkan.org/software/classias/">Classias: A collection of machine-learning algorithms for classification</a>
	- <a href="http://www.public.iastate.edu/~gdancik/mlegp/">mlegp: an R package for maximum likelihood estimates for Gaussian processes</a>
	- <a href="http://infmath.uibk.ac.at/~matthiasf/imaging2/">imaging2: the imaging2 class library</a>
	- <a href="http://search.cpan.org/~laye/Algorithm-LBFGS-0.16/">Algorithm::LBFGS - Perl extension for L-BFGS</a>
	- <a href="http://www.cs.kuleuven.be/~bernd/yap-lbfgs/">YAP-LBFGS (an interface to call libLBFGS from YAP Prolog)</a>

	@section download Download

	- <a href="https://github.com/downloads/chokkan/liblbfgs/liblbfgs-1.10.tar.gz">Source code</a>
	- <a href="https://github.com/chokkan/liblbfgs">GitHub repository</a>

	libLBFGS is distributed under the term of the
	<a href="http://opensource.org/licenses/mit-license.php">MIT license</a>.

	@section changelog History
	- Version 1.10 (2010-12-22):
	    - Fixed compiling errors on Mac OS X; this patch was kindly submitted by
	      Nic Schraudolph.
	    - Reduced compiling warnings on Mac OS X; this patch was kindly submitted
	      by Tamas Nepusz.
	    - Replaced memalign() with posix_memalign().
	    - Updated solution and project files for Microsoft Visual Studio 2010.
	- Version 1.9 (2010-01-29):
	    - Fixed a mistake in checking the validity of the parameters "ftol" and
	      "wolfe"; this was discovered by Kevin S. Van Horn.
	- Version 1.8 (2009-07-13):
	    - Accepted the patch submitted by Takashi Imamichi;
	      the backtracking method now has three criteria for choosing the step
	      length:
	        - ::LBFGS_LINESEARCH_BACKTRACKING_ARMIJO: sufficient decrease (Armijo)
	          condition only
	        - ::LBFGS_LINESEARCH_BACKTRACKING_WOLFE: regular Wolfe condition
	          (sufficient decrease condition + curvature condition)
	        - ::LBFGS_LINESEARCH_BACKTRACKING_STRONG_WOLFE: strong Wolfe condition
	    - Updated the documentation to explain the above three criteria.
	- Version 1.7 (2009-02-28):
	    - Improved OWL-QN routines for stability.
	    - Removed the support of OWL-QN method in MoreThuente algorithm because
	      it accidentally fails in early stages of iterations for some objectives.
	      Because of this change, <b>the OW-LQN method must be used with the
	      backtracking algorithm (::LBFGS_LINESEARCH_BACKTRACKING)</b>, or the
	      library returns ::LBFGSERR_INVALID_LINESEARCH.
	    - Renamed line search algorithms as follows:
	        - ::LBFGS_LINESEARCH_BACKTRACKING: regular Wolfe condition.
	        - ::LBFGS_LINESEARCH_BACKTRACKING_LOOSE: regular Wolfe condition.
	        - ::LBFGS_LINESEARCH_BACKTRACKING_STRONG: strong Wolfe condition.
	    - Source code clean-up.
	- Version 1.6 (2008-11-02):
	    - Improved line-search algorithm with strong Wolfe condition, which was
	      contributed by Takashi Imamichi. This routine is now default for
	      ::LBFGS_LINESEARCH_BACKTRACKING. The previous line search algorithm
	      with regular Wolfe condition is still available as
	      ::LBFGS_LINESEARCH_BACKTRACKING_LOOSE.
	    - Configurable stop index for L1-norm computation. A member variable
	      ::lbfgs_parameter_t::orthantwise_end was added to specify the index
	      number at which the library stops computing the L1 norm of the
	      variables. This is useful to prevent some variables from being
	      regularized by the OW-LQN method.
	    - A sample program written in C++ (sample/sample.cpp).
	- Version 1.5 (2008-07-10):
	    - Configurable starting index for L1-norm computation. A member variable
	      ::lbfgs_parameter_t::orthantwise_start was added to specify the index
	      number from which the library computes the L1 norm of the variables.
	      This is useful to prevent some variables from being regularized by the
	      OWL-QN method.
	    - Fixed a zero-division error when the initial variables have already
	      been a minimizer (reported by Takashi Imamichi). In this case, the
	      library returns ::LBFGS_ALREADY_MINIMIZED status code.
	    - Defined ::LBFGS_SUCCESS status code as zero; removed unused constants,
	      LBFGSFALSE and LBFGSTRUE.
	    - Fixed a compile error in an implicit down-cast.
	- Version 1.4 (2008-04-25):
	    - Configurable line search algorithms. A member variable
	      ::lbfgs_parameter_t::linesearch was added to choose either MoreThuente
	      method (::LBFGS_LINESEARCH_MORETHUENTE) or backtracking algorithm
	      (::LBFGS_LINESEARCH_BACKTRACKING).
	    - Fixed a bug: the previous version did not compute psuedo-gradients
	      properly in the line search routines for OWL-QN. This bug might quit
	      an iteration process too early when the OWL-QN routine was activated
	      (0 < ::lbfgs_parameter_t::orthantwise_c).
	    - Configure script for POSIX environments.
	    - SSE/SSE2 optimizations with GCC.
	    - New functions ::lbfgs_malloc and ::lbfgs_free to use SSE/SSE2 routines
	      transparently. It is uncessary to use these functions for libLBFGS built
	      without SSE/SSE2 routines; you can still use any memory allocators if
	      SSE/SSE2 routines are disabled in libLBFGS.
	- Version 1.3 (2007-12-16):
	    - An API change. An argument was added to lbfgs() function to receive the
	      final value of the objective function. This argument can be set to
	      \c NULL if the final value is unnecessary.
	    - Fixed a null-pointer bug in the sample code (reported by Takashi Imamichi).
	    - Added build scripts for Microsoft Visual Studio 2005 and GCC.
	    - Added README file.
	- Version 1.2 (2007-12-13):
	    - Fixed a serious bug in orthant-wise L-BFGS.
	      An important variable was used without initialization.
	- Version 1.1 (2007-12-01):
	    - Implemented orthant-wise L-BFGS.
	    - Implemented lbfgs_parameter_init() function.
	    - Fixed several bugs.
	    - API documentation.
	- Version 1.0 (2007-09-20):
	    - Initial release.

	@section api Documentation

	- @ref liblbfgs_api "libLBFGS API"

	@section sample Sample code

	@include sample.c

	@section ack Acknowledgements

	The L-BFGS algorithm is described in:
	    - Jorge Nocedal.
	      Updating Quasi-Newton Matrices with Limited Storage.
	      <i>Mathematics of Computation</i>, Vol. 35, No. 151, pp. 773--782, 1980.
	    - Dong C. Liu and Jorge Nocedal.
	      On the limited memory BFGS method for large scale optimization.
	      <i>Mathematical Programming</i> B, Vol. 45, No. 3, pp. 503-528, 1989.

	The line search algorithms used in this implementation are described in:
	    - John E. Dennis and Robert B. Schnabel.
	      <i>Numerical Methods for Unconstrained Optimization and Nonlinear
	      Equations</i>, Englewood Cliffs, 1983.
	    - Jorge J. More and David J. Thuente.
	      Line search algorithm with guaranteed sufficient decrease.
	      <i>ACM Transactions on Mathematical Software (TOMS)</i>, Vol. 20, No. 3,
	      pp. 286-307, 1994.

	This library also implements Orthant-Wise Limited-memory Quasi-Newton (OWL-QN)
	method presented in:
	    - Galen Andrew and Jianfeng Gao.
	      Scalable training of L1-regularized log-linear models.
	      In <i>Proceedings of the 24th International Conference on Machine
	      Learning (ICML 2007)</i>, pp. 33-40, 2007.

	Special thanks go to:
	    - Yoshimasa Tsuruoka and Daisuke Okanohara for technical information about
	      OWL-QN
	    - Takashi Imamichi for the useful enhancements of the backtracking method
	    - Kevin S. Van Horn, Nic Schraudolph, and Tamas Nepusz for bug fixes

	Finally I would like to thank the original author, Jorge Nocedal, who has been
	distributing the effieicnt and explanatory implementation in an open source
	licence.

	@section reference Reference

	- <a href="http://www.ece.northwestern.edu/~nocedal/lbfgs.html">L-BFGS</a> by Jorge Nocedal.
	- <a href="http://research.microsoft.com/en-us/downloads/b1eb1016-1738-4bd5-83a9-370c9d498a03/default.aspx">Orthant-Wise Limited-memory Quasi-Newton Optimizer for L1-regularized Objectives</a> by Galen Andrew.
	- <a href="http://chasen.org/~taku/software/misc/lbfgs/">C port (via f2c)</a> by Taku Kudo.
	- <a href="http://www.alglib.net/optimization/lbfgs.php">C#/C++/Delphi/VisualBasic6 port</a> in ALGLIB.
	- <a href="http://cctbx.sourceforge.net/">Computational Crystallography Toolbox</a> includes
	  <a href="http://cctbx.sourceforge.net/current_cvs/c_plus_plus/namespacescitbx_1_1lbfgs.html">scitbx::lbfgs</a>.
*/
public class LBFGS {
	/*
	 *      C library of Limited memory BFGS (L-BFGS).
	 *
	 * Copyright (c) 1990, Jorge Nocedal
	 * Copyright (c) 2007-2010 Naoaki Okazaki
	 * All rights reserved.
	 *
	 * Permission is hereby granted, free of charge, to any person obtaining a copy
	 * of this software and associated documentation files (the "Software"), to deal
	 * in the Software without restriction, including without limitation the rights
	 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	 * copies of the Software, and to permit persons to whom the Software is
	 * furnished to do so, subject to the following conditions:
	 *
	 * The above copyright notice and this permission notice shall be included in
	 * all copies or substantial portions of the Software.
	 *
	 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	 * THE SOFTWARE.
	 */

	/* $Id$ */

	/*
	 * The default precision of floating point values is 64bit (double).
	 */
//	static final int LBFGS_FLOAT  = 64;

	/*
	 * Activate optimization routines for IEEE754 floating point values.
	 */
//	#ifndef LBFGS_IEEE_FLOAT
//	#define LBFGS_IEEE_FLOAT    1
//	#endif/*LBFGS_IEEE_FLOAT*/

//	#if     LBFGS_FLOAT == 32
//	typedef float lbfgsfloatval_t;
//
//	#elif   LBFGS_FLOAT == 64
//	typedef double lbfgsfloatval_t;
//
//	#else
//	#error "libLBFGS supports single (float; LBFGS_FLOAT = 32) or double (double; LBFGS_FLOAT=64) precision only."
//
//	#endif


	/** 
	 * \addtogroup liblbfgs_api libLBFGS API
	 * @{
	 *
	 *  The libLBFGS API.
	 */

	/**
	 * Return values of lbfgs().
	 * 
	 *  Roughly speaking, a negative value indicates an error.
	 */
	public static enum Status {
	    /** L-BFGS reaches convergence. */
	    LBFGS_SUCCESS,
	    LBFGS_STOP,
	    /** The initial variables already minimize the objective function. */
	    LBFGS_ALREADY_MINIMIZED,

	    /** Unknown error. */
	    LBFGSERR_UNKNOWNERROR,
	    /** Logic error. */
	    LBFGSERR_LOGICERROR,
	    /** Insufficient memory. */
	    LBFGSERR_OUTOFMEMORY,
	    /** The minimization process has been canceled. */
	    LBFGSERR_CANCELED,
	    /** Invalid number of variables specified. */
	    LBFGSERR_INVALID_N,
	    /** Invalid number of variables (for SSE) specified. */
	    LBFGSERR_INVALID_N_SSE,
	    /** The array x must be aligned to 16 (for SSE). */
	    LBFGSERR_INVALID_X_SSE,
	    /** Invalid parameter lbfgs_parameter_t::epsilon specified. */
	    LBFGSERR_INVALID_EPSILON,
	    /** Invalid parameter lbfgs_parameter_t::past specified. */
	    LBFGSERR_INVALID_TESTPERIOD,
	    /** Invalid parameter lbfgs_parameter_t::delta specified. */
	    LBFGSERR_INVALID_DELTA,
	    /** Invalid parameter lbfgs_parameter_t::linesearch specified. */
	    LBFGSERR_INVALID_LINESEARCH,
	    /** Invalid parameter lbfgs_parameter_t::max_step specified. */
	    LBFGSERR_INVALID_MINSTEP,
	    /** Invalid parameter lbfgs_parameter_t::max_step specified. */
	    LBFGSERR_INVALID_MAXSTEP,
	    /** Invalid parameter lbfgs_parameter_t::ftol specified. */
	    LBFGSERR_INVALID_FTOL,
	    /** Invalid parameter lbfgs_parameter_t::wolfe specified. */
	    LBFGSERR_INVALID_WOLFE,
	    /** Invalid parameter lbfgs_parameter_t::gtol specified. */
	    LBFGSERR_INVALID_GTOL,
	    /** Invalid parameter lbfgs_parameter_t::xtol specified. */
	    LBFGSERR_INVALID_XTOL,
	    /** Invalid parameter lbfgs_parameter_t::max_linesearch specified. */
	    LBFGSERR_INVALID_MAXLINESEARCH,
	    /** Invalid parameter lbfgs_parameter_t::orthantwise_c specified. */
	    LBFGSERR_INVALID_ORTHANTWISE,
	    /** Invalid parameter lbfgs_parameter_t::orthantwise_start specified. */
	    LBFGSERR_INVALID_ORTHANTWISE_START,
	    /** Invalid parameter lbfgs_parameter_t::orthantwise_end specified. */
	    LBFGSERR_INVALID_ORTHANTWISE_END,
	    /** The line-search step went out of the interval of uncertainty. */
	    LBFGSERR_OUTOFINTERVAL,
	    /** A logic error occurred; alternatively, the interval of uncertainty
	        became too small. */
	    LBFGSERR_INCORRECT_TMINMAX,
	    /** A rounding error occurred; alternatively, no line-search step
	        satisfies the sufficient decrease and curvature conditions. */
	    LBFGSERR_ROUNDING_ERROR,
	    /** The line-search step became smaller than lbfgs_parameter_t::min_step. */
	    LBFGSERR_MINIMUMSTEP,
	    /** The line-search step became larger than lbfgs_parameter_t::max_step. */
	    LBFGSERR_MAXIMUMSTEP,
	    /** The line-search routine reaches the maximum number of evaluations. */
	    LBFGSERR_MAXIMUMLINESEARCH,
	    /** The algorithm routine reaches the maximum number of iterations. */
	    LBFGSERR_MAXIMUMITERATION,
	    /** Relative width of the interval of uncertainty is at most
	        lbfgs_parameter_t::xtol. */
	    LBFGSERR_WIDTHTOOSMALL,
	    /** A logic error (negative line-search step) occurred. */
	    LBFGSERR_INVALIDPARAMETERS,
	    /** The current search direction increases the objective function value. */
	    LBFGSERR_INCREASEGRADIENT;
	    
		public boolean hasConverged() { return this==LBFGS_SUCCESS || this==LBFGS_STOP; }
		public boolean isError() {
			return this!=LBFGS_SUCCESS && this!=LBFGS_STOP && this!=LBFGS_ALREADY_MINIMIZED;
		}
	};

	/**
	 * Line search algorithms.
	 */
	enum LinesearchAlgorithm {
	    /** The default algorithm (MoreThuente method). */
//	    LBFGS_LINESEARCH_DEFAULT,
		
	    /** MoreThuente method proposd by More and Thuente. */
	    LBFGS_LINESEARCH_MORETHUENTE,
	    
	    /**
	     * Backtracking method with the Armijo condition.
	     *  The backtracking method finds the step length such that it satisfies
	     *  the sufficient decrease (Armijo) condition,
	     *    - f(x + a * d) <= f(x) + lbfgs_parameter_t::ftol * a * g(x)^T d,
	     *
	     *  where x is the current point, d is the current search direction, and
	     *  a is the step length.
	     */
	    LBFGS_LINESEARCH_BACKTRACKING_ARMIJO,
	    
	    /** The backtracking method with the defualt (regular Wolfe) condition. */
//	    LBFGS_LINESEARCH_BACKTRACKING,
	    
	    /**
	     * Backtracking method with regular Wolfe condition.
	     *  The backtracking method finds the step length such that it satisfies
	     *  both the Armijo condition (LBFGS_LINESEARCH_BACKTRACKING_ARMIJO)
	     *  and the curvature condition,
	     *    - g(x + a * d)^T d >= lbfgs_parameter_t::wolfe * g(x)^T d,
	     *
	     *  where x is the current point, d is the current search direction, and
	     *  a is the step length.
	     */
	    LBFGS_LINESEARCH_BACKTRACKING_WOLFE,
	    
	    /**
	     * Backtracking method with strong Wolfe condition.
	     *  The backtracking method finds the step length such that it satisfies
	     *  both the Armijo condition (LBFGS_LINESEARCH_BACKTRACKING_ARMIJO)
	     *  and the following condition,
	     *    - |g(x + a * d)^T d| <= lbfgs_parameter_t::wolfe * |g(x)^T d|,
	     *
	     *  where x is the current point, d is the current search direction, and
	     *  a is the step length.
	     */
	    LBFGS_LINESEARCH_BACKTRACKING_STRONG_WOLFE,
	};
	
	static interface line_search_proc {
		public Status go(
			    int n,
			    double[] x,
			    double[] f,
			    double[] g,
			    double[] s,
			    double[] stp,
			    final double[] xp,
			    final double[] gp,
			    double[] wa,
			    callback_data_t cd,
			    Params param
		);
	}


	/*
	 * static const lbfgs_parameter_t _defparam = {
    m 6, 
    epsilon 1e-5,
    past 0,
    delta 1e-5,
    maxiter 0,
    linesearch LBFGS_LINESEARCH_DEFAULT,
    maxlinesearch 40,
    minstep 1e-20,
    maxstep 1e20,
    ftol 1e-4,
    wolfe 0.9,
    gtol 0.9,
    xtol 1.0e-16,
    orthwantwisec 0.0,
    orthwantwisestart 0,
    orthantwiseend -1,
    
	};
	 */

	/** 
	 * Parameters to control the optimizer.
	 * To control convergence/runtime, see: {@link #max_iterations} and {@link #epsilon}
	 * To get L1 penalization, see: {@link #orthantwise_c}, {@link #orthantwise_start}, {@link #orthantwise_end}
	 */
	public static class Params {
	    /**
	     * The number of corrections to approximate the inverse hessian matrix.
	     *  The L-BFGS routine stores the computation results of previous \ref m
	     *  iterations to approximate the inverse hessian matrix of the current
	     *  iteration. This parameter controls the size of the limited memories
	     *  (corrections). The default value is \c 6. Values less than \c 3 are
	     *  not recommended. Large values will result in excessive computing time.
	     */
		public int             m = 6;

	    /**
	     * Epsilon for convergence test.
	     *  This parameter determines the accuracy with which the solution is to
	     *  be found. A minimization terminates when
	     *      ||g|| < \ref epsilon * max(1, ||x||),
	     *  where ||.|| denotes the Euclidean (L2) norm. The default value is
	     *  \c 1e-5.
	     */
		public double epsilon = 1e-5;

	    /**
	     * Distance for delta-based convergence test.
	     *  This parameter determines the distance, in iterations, to compute
	     *  the rate of decrease of the objective function. If the value of this
	     *  parameter is zero, the library does not perform the delta-based
	     *  convergence test.
	     */
		public int             past = 3; // BTO: changed from '0'

	    /**
	     * Delta for convergence test.
	     *  This parameter determines the minimum rate of decrease of the
	     *  objective function. The library stops iterations when the
	     *  following condition is met:
	     *      (f' - f) / f < \ref delta,
	     *  where f' is the objective value of \ref past iterations ago, and f is
	     *  the objective value of the current iteration.
	     *  The default value is \c 0.
	     *  
	     *  REQUIRES 'past' TO BE SET
	     */
		public double delta = 1e-5;

	    /**
	     * The maximum number of iterations.
	     *  The lbfgs() function terminates an optimization process with
	     *  ::LBFGSERR_MAXIMUMITERATION status code when the iteration count
	     *  exceedes this parameter. Setting this parameter to zero continues an
	     *  optimization process until a convergence or error. The default value
	     *  is \c 0.
	     */
		public int             max_iterations = 0;

	    /**
	     * The line search algorithm.
	     *  This parameter specifies a line search algorithm to be used by the
	     *  L-BFGS routine.
	     */
//	    LinesearchAlgorithm linesearch = LinesearchAlgorithm.LBFGS_LINESEARCH_MORETHUENTE;
	    // BTO: morethuente was default in LibLBFGS, disabling for now
	    LinesearchAlgorithm linesearch = LinesearchAlgorithm.LBFGS_LINESEARCH_BACKTRACKING_WOLFE;

	    /**
	     * The maximum number of trials for the line search.
	     *  This parameter controls the number of function and gradients evaluations
	     *  per iteration for the line search routine. The default value is \c 40.
	     */
	    public int             max_linesearch = 40;

	    /**
	     * The minimum step of the line search routine.
	     *  The default value is \c 1e-20. This value need not be modified unless
	     *  the exponents are too large for the machine being used, or unless the
	     *  problem is extremely badly scaled (in which case the exponents should
	     *  be increased).
	     */
	    public double min_step = 1e-20;

	    /**
	     * The maximum step of the line search.
	     *  The default value is \c 1e+20. This value need not be modified unless
	     *  the exponents are too large for the machine being used, or unless the
	     *  problem is extremely badly scaled (in which case the exponents should
	     *  be increased).
	     */
	    public double max_step = 1e20;

	    /**
	     * A parameter to control the accuracy of the line search routine.
	     *  The default value is \c 1e-4. This parameter should be greater
	     *  than zero and smaller than \c 0.5.
	     */
	    public double ftol = 1e-4;

	    /**
	     * A coefficient for the Wolfe condition.
	     *  This parameter is valid only when the backtracking line-search
	     *  algorithm is used with the Wolfe condition,
	     *  ::LBFGS_LINESEARCH_BACKTRACKING_STRONG_WOLFE or
	     *  ::LBFGS_LINESEARCH_BACKTRACKING_WOLFE .
	     *  The default value is \c 0.9. This parameter should be greater
	     *  the \ref ftol parameter and smaller than \c 1.0.
	     */
	    public double wolfe = 0.9;

	    /**
	     * A parameter to control the accuracy of the line search routine.
	     *  The default value is \c 0.9. If the function and gradient
	     *  evaluations are inexpensive with respect to the cost of the
	     *  iteration (which is sometimes the case when solving very large
	     *  problems) it may be advantageous to set this parameter to a small
	     *  value. A typical small value is \c 0.1. This parameter shuold be
	     *  greater than the \ref ftol parameter (\c 1e-4) and smaller than
	     *  \c 1.0.
	     */
	    public double gtol = 0.9;

	    /**
	     * The machine precision for floating-point values.
	     *  This parameter must be a positive value set by a client program to
	     *  estimate the machine precision. The line search routine will terminate
	     *  with the status code (::LBFGSERR_ROUNDING_ERROR) if the relative width
	     *  of the interval of uncertainty is less than this parameter.
	     */
	    public double xtol = 1e-16;

	    /**
	     * Coeefficient for the L1 norm of variables.
	     *  This parameter should be set to zero for standard minimization
	     *  problems. Setting this parameter to a positive value activates
	     *  Orthant-Wise Limited-memory Quasi-Newton (OWL-QN) method, which
	     *  minimizes the objective function F(x) combined with the L1 norm |x|
	     *  of the variables, {F(x) + C |x|}. This parameter is the coeefficient
	     *  for the |x|, i.e., C. As the L1 norm |x| is not differentiable at
	     *  zero, the library modifies function and gradient evaluations from
	     *  a client program suitably; a client program thus have only to return
	     *  the function value F(x) and gradients G(x) as usual. The default value
	     *  is zero.
	     */
	    public double orthantwise_c = 0;

	    /**
	     * Start index for computing L1 norm of the variables.
	     *  This parameter is valid only for OWL-QN method
	     *  (i.e., \ref orthantwise_c != 0). This parameter b (0 <= b < N)
	     *  specifies the index number from which the library computes the
	     *  L1 norm of the variables x,
	     *      |x| := |x_{b}| + |x_{b+1}| + ... + |x_{N}| .
	     *  In other words, variables x_1, ..., x_{b-1} are not used for
	     *  computing the L1 norm. Setting b (0 < b < N), one can protect
	     *  variables, x_1, ..., x_{b-1} (e.g., a bias term of logistic
	     *  regression) from being regularized. The default value is zero.
	     */
	    public int             orthantwise_start = 0;

	    /**
	     * End index for computing L1 norm of the variables.
	     *  This parameter is valid only for OWL-QN method
	     *  (i.e., \ref orthantwise_c != 0). This parameter e (0 < e <= N)
	     *  specifies the index number at which the library stops computing the
	     *  L1 norm of the variables x,
	     */
	    public int             orthantwise_end = -1;
	};


	/**
	 * Callback interface to provide objective function and gradient evaluations.
	 * [BTO: liblbfgs's wants both at once. this is smart: often this halves
	 * the cost; e.g. evaluate partitions only once for loglinear gradients.]
	 *
	 *  The lbfgs() function call this function to obtain the values of objective
	 *  function and its gradients when needed. A client program must implement
	 *  this function to evaluate the values of the objective function and its
	 *  gradients, given current values of variables.
	 *  
	 *  Protocol: receive x, fill in g, return objective.
	 *  
	 *  @param  x           The current values of variables.
	 *  @param  g           The gradient vector. The callback function must compute
	 *                      the gradient values for the current variables.
	 *  @param  n           The number of variables.
	 *  @param  step        The current step of the line search routine.
	 *  @retval  			The value of the objective function for the current
	 *                          variables.
	 */
	public static interface Function {
		/** receive x. fill in g. return objective. */
		public double evaluate(final double[] x, double[] g, int n, double step);
	}
	
//	typedef lbfgsfloatval_t (*lbfgs_evaluate_t)(
//	    void *instance,
//	    const lbfgsfloatval_t *x,
//	    lbfgsfloatval_t *g,
//	    const int n,
//	    const lbfgsfloatval_t step
//	    );

	/**
	 * Callback interface to receive the progress of the optimization process.
	 *
	 *  The lbfgs() function call this function for each iteration. Implementing
	 *  this function, a client program can store or display the current progress
	 *  of the optimization process.
	 *
	 *  @param  instance    The user data sent for lbfgs() function by the client.  BTO removed
	 *  @param  x           The current values of variables.
	 *  @param  g           The current gradient values of variables.
	 *  @param  fx          The current value of the objective function.
	 *  @param  xnorm       The Euclidean norm of the variables.
	 *  @param  gnorm       The Euclidean norm of the gradients.
	 *  @param  step        The line-search step used for this iteration.
	 *  @param  n           The number of variables.
	 *  @param  k           The iteration count.
	 *  @param  ls          The number of evaluations called for this iteration.
	 *  @retval int         Zero to continue the optimization process. Returning a
	 *                      non-zero value will cancel the optimization process.
	 */
	public static interface ProgressCallback {
		public int apply(
			    double[] x,
			    double[] g,
			    double fx,
			    double xnorm,
			    double gnorm,
			    double step,
			    int n,
			    int iterNum,
			    Status ls);
	}
	
//	typedef int (*lbfgs_progress_t)(
//	    void *instance,
//	    const lbfgsfloatval_t *x,
//	    const lbfgsfloatval_t *g,
//	    const lbfgsfloatval_t fx,
//	    const lbfgsfloatval_t xnorm,
//	    const lbfgsfloatval_t gnorm,
//	    const lbfgsfloatval_t step,
//	    int n,
//	    int k,
//	    int ls
//	    );

	/*
	A user must implement a function compatible with ::lbfgs_evaluate_t (evaluation
	callback) and pass the pointer to the callback function to lbfgs() arguments.
	Similarly, a user can implement a function compatible with ::lbfgs_progress_t
	(progress callback) to obtain the current progress (e.g., variables, function
	value, ||G||, etc) and to cancel the iteration process if necessary.
	Implementation of a progress callback is optional: a user can pass \c NULL if
	progress notification is not necessary.

	In addition, a user must preserve two requirements:
	    - The number of variables must be multiples of 16 (this is not 4).
	    - The memory block of variable array ::x must be aligned to 16.

	This algorithm terminates an optimization
	when:

	    ||G|| < \epsilon \cdot \max(1, ||x||) .

	In this formula, ||.|| denotes the Euclidean norm.
	*/
	
	public static class Result {
		public Status status;
		public int additionalStatus;
		public double objective = Double.MAX_VALUE;
		public Result(Status s) { status=s; }
		public String toString() {
			return String.format("status=%s obj=%g", status, objective);
		}
	}

	static double max3(double a, double b, double c) { return Math.max(Math.max(a,b), c); }

	static class callback_data_t {
	    int n;
	    Function proc_evaluate;
	    ProgressCallback proc_progress;
	}

	static class iteration_data_t {
	    double alpha;
	    // BTO tricky, i think these were aliased pointers or something?
	    double[] s;     /* [n] */
	    double[] y;     /* [n] */
	    double ys;     /* vecdot(y, s) */
	}
//	typedef struct tag_iteration_data iteration_data_t;

	/**
	 * Use default parameters. 
	 * See {@link LBFGS#lfbgs(double[], Function, ProgressCallback, Params)} */
	public static Result lbfgs(
		    double[] init,
		    Function proc_evaluate)
	{
		return lbfgs(init, proc_evaluate, null, new Params());
	}

	/** 
	 * Use default parameters.
	 * See {@link LBFGS#lfbgs(double[], Function, ProgressCallback, Params)} */
	public static Result lbfgs(
		    double[] init,
		    Function proc_evaluate,
		    ProgressCallback proc_progress)
	{
		return lbfgs(init, proc_evaluate, proc_progress, new Params());
	}

	/**
	 * Use debug-friendly parameters & callback.
	 * See {@link LBFGS#lfbgs(double[], Function, ProgressCallback, Params)}
	 */
	public static Result lbfgs(
		    double[] init,
		    int maxIter,
		    Function proc_evaluate)
	{
		Params p = new Params();
		p.max_iterations = maxIter;
		ProgressCallback cb = new ProgressCallback() {
			@Override
			public int apply(double[] x, double[] g, double fx, double xnorm,
					double gnorm, double step, int n, int k, LBFGS.Status ls) {
				System.out.printf("optiter %d obj=%g solelt=%g\n", k, fx, x[0]);
				return 0;
			}
		};
		return lbfgs(init, proc_evaluate, cb, p);
	}

	/** safer & debug-friendly options */
	public static Result lbfgsNice(
		    double[] x,
		    int maxIter,
		    Function proc_evaluate,
		    ProgressCallback cb)
	{
		Params p = new Params();
		p.max_iterations = maxIter;
		return lbfgs(x, proc_evaluate, cb, p);
	}

	/**
	 * Start a L-BFGS optimization.
	 *
	 *  @param  n           The number of variables.
	 *  @param  x           The array of variables. A client program can set
	 *                      default values for the optimization and receive the
	 *                      optimization result through this array.
	 *  @param  proc_evaluate   The callback function to provide function and
	 *                          gradient evaluations given a current values of
	 *                          variables. A client program must implement a
	 *                          callback function compatible with \ref
	 *                          lbfgs_evaluate_t and pass the pointer to the
	 *                          callback function.
	 *  @param  proc_progress   The callback function to receive the progress
	 *                          (the number of iterations, the current value of
	 *                          the objective function) of the minimization
	 *                          process. This argument can be set to \c NULL if
	 *                          a progress report is unnecessary.
	 *  @param  param       The pointer to a structure representing parameters for
	 *                      L-BFGS optimization. A client program can set this
	 *                      parameter to NULL to use the default parameters.
	 *  @retval Result      The status code and final objective.
	 */
	public static Result lbfgs(
	    double[] x,
	    Function proc_evaluate,
	    ProgressCallback proc_progress,
	    Params param)
	{
		int n = x.length;

		Result ret = new Result(null);
	    int i, j, k, /*ls,*/ end, bound;
	    Status ls;
	    double[] step = new double[]{0};

	    /* Constant parameters and their default values. */
	    final int m = param.m;

	    double[] xp;
	    double g[], gp[], pg[];
	    double d[], w[], pf[] = null;
	    iteration_data_t[] lm;
	    iteration_data_t it;
	    double ys, yy;
	    double xnorm, gnorm, beta;
	    double fx[] = new double[]{0}; // singleton passing to linesearch
	    double rate = 0;
//	    line_search_proc linesearch = new line_search_morethuente();
	    line_search_proc linesearch = new line_search_backtracking(); // BTO added for testing
	    
	    /* Construct a callback data. */
	    callback_data_t cd = new callback_data_t();
	    cd.n = n;
	    cd.proc_evaluate = proc_evaluate;
	    cd.proc_progress = proc_progress;


	    /* Check the input parameters for errors. */
	    if (n <= 0) {
	        return new Result(Status.LBFGSERR_INVALID_N);
	    }
	    if (param.epsilon < 0.) {
	        return new Result(Status.LBFGSERR_INVALID_EPSILON);
	    }
	    if (param.past < 0) {
	    	return new Result(Status.LBFGSERR_INVALID_TESTPERIOD);
	    }
	    if (param.delta < 0.) {
	    	return new Result(Status.LBFGSERR_INVALID_DELTA);
	    }
	    if (param.min_step < 0.) {
	    	return new Result(Status.LBFGSERR_INVALID_MINSTEP);
	    }
	    if (param.max_step < param.min_step) {
	    	return new Result(Status.LBFGSERR_INVALID_MAXSTEP);
	    }
	    if (param.ftol < 0.) {
	    	return new Result(Status.LBFGSERR_INVALID_FTOL);
	    }
	    if (param.linesearch == LinesearchAlgorithm.LBFGS_LINESEARCH_BACKTRACKING_WOLFE ||
	        param.linesearch == LinesearchAlgorithm.LBFGS_LINESEARCH_BACKTRACKING_STRONG_WOLFE) {
	        if (param.wolfe <= param.ftol || 1. <= param.wolfe) {
	            return new Result(Status.LBFGSERR_INVALID_WOLFE);
	        }
	    }
	    if (param.gtol < 0.) {
	        return new Result(Status.LBFGSERR_INVALID_GTOL);
	    }
	    if (param.xtol < 0.) {
	        return new Result(Status.LBFGSERR_INVALID_XTOL);
	    }
	    if (param.max_linesearch <= 0) {
	        return new Result(Status.LBFGSERR_INVALID_MAXLINESEARCH);
	    }
	    if (param.orthantwise_c < 0.) {
	        return new Result(Status.LBFGSERR_INVALID_ORTHANTWISE);
	    }
	    if (param.orthantwise_start < 0 || n < param.orthantwise_start) {
	        return new Result(Status.LBFGSERR_INVALID_ORTHANTWISE_START);
	    }
	    if (param.orthantwise_end < 0) {
	        param.orthantwise_end = n;
	    }
	    if (n < param.orthantwise_end) {
	        return new Result(Status.LBFGSERR_INVALID_ORTHANTWISE_END);
	    }
	    if (param.orthantwise_c != 0.) {
            /* Only the backtracking method is available. */
            linesearch = new line_search_backtracking_owlqn();
	    } else {
	        switch (param.linesearch) {
	        case LBFGS_LINESEARCH_MORETHUENTE:
	            linesearch = new line_search_morethuente();
	            break;
	        case LBFGS_LINESEARCH_BACKTRACKING_ARMIJO:
	        case LBFGS_LINESEARCH_BACKTRACKING_WOLFE:
	        case LBFGS_LINESEARCH_BACKTRACKING_STRONG_WOLFE:
	            linesearch = new line_search_backtracking();
	            break;
	        default:
	            return new Result(Status.LBFGSERR_INVALID_LINESEARCH);
	        }
	    }

	    /* Allocate working space. */
	    xp = new double[n];
	    g = new double[n];
	    gp = new double[n];
	    d = new double[n];
	    w = new double[n];

	    if (param.orthantwise_c != 0) {
	        /* Allocate working space for OW-LQN. */
	        pg = new double[n];
	    } else {
	    	pg = new double[0]; // to make java compiler happy
	    }

	    /* Allocate limited memory storage. */
	    lm = new iteration_data_t[m];

	    /* Initialize the limited memory. */
	    for (i = 0;i < m;++i) {
	    	lm[i] = new iteration_data_t();
	        it = lm[i];
	        it.alpha = 0;
	        it.ys = 0;
	        it.s = new double[n]; //(lbfgsfloatval_t*)vecalloc(n * sizeof(lbfgsfloatval_t));
	        it.y = new double[n]; //(lbfgsfloatval_t*)vecalloc(n * sizeof(lbfgsfloatval_t));
//	        if (it->s == NULL || it->y == NULL) {
//	            ret = LBFGSERR_OUTOFMEMORY;
//	            goto lbfgs_exit;
//	        }
	    }

	    /* Allocate an array for storing previous values of the objective function. */
	    if (0 < param.past) {
	        pf = new double[param.past]; //(lbfgsfloatval_t*)vecalloc(param.past * sizeof(lbfgsfloatval_t));
	    }

	    /* Evaluate the function value and its gradient. */
	    fx[0] = cd.proc_evaluate.evaluate(x, g, cd.n, 0);
	    if (0. != param.orthantwise_c) {
	        /* Compute the L1 norm of the variable and add it to the object value. */
	        xnorm = owlqn_x1norm(x, param.orthantwise_start, param.orthantwise_end);
	        fx[0] += xnorm * param.orthantwise_c;
	        owlqn_pseudo_gradient(
	            pg, x, g, n,
	            param.orthantwise_c, param.orthantwise_start, param.orthantwise_end
	            );
	    }

	    /* Store the initial value of the objective function. */
	    if (pf != null) {
	        pf[0] = fx[0];
	    }

	    /*
	        Compute the direction;
	        we assume the initial hessian matrix H_0 as the identity matrix.
	     */
	    if (param.orthantwise_c == 0.) {
	        vecncpy(d, g, n);
	    } else {
	        vecncpy(d, pg, n);
	    }

	    /*
	       Make sure that the initial variables are not a minimizer.
	     */
	    xnorm = vec2norm(x, n);
	    if (param.orthantwise_c == 0.) {
	        gnorm = vec2norm(g, n);
	    } else {
	        gnorm = vec2norm(pg, n);
	    }
	    if (xnorm < 1.0) xnorm = 1.0;
	    if (gnorm / xnorm <= param.epsilon) {
	        return new Result(Status.LBFGS_ALREADY_MINIMIZED);
	    }

	    /* Compute the initial step:
	        step = 1.0 / sqrt(vecdot(d, d, n))
	     */
	    step[0] = vec2norminv(d, n);

	    k = 1;
	    end = 0;
	    for (;;) {
	        /* Store the current position and gradient vectors. */
	        veccpy(xp, x, n);
	        veccpy(gp, g, n);

	        /* Search for an optimal step. */
	        if (param.orthantwise_c == 0.) {
	            ls = linesearch.go(n, x, fx, g, d, step, xp, gp, w, cd, param);
	        } else {
	            ls = linesearch.go(n, x, fx, g, d, step, xp, pg, w, cd, param);
	            owlqn_pseudo_gradient(
	                pg, x, g, n,
	                param.orthantwise_c, param.orthantwise_start, param.orthantwise_end
	                );
	        }
	        if (ls!=null && ls.isError()) {
	            /* Revert to the previous point. */
	            veccpy(x, xp, n);
	            veccpy(g, gp, n);
	            ret.status = ls;
	            ret.objective = fx[0];
	            return ret;
	        }

	        /* Compute x and g norms. */
	        xnorm = vec2norm(x, n);
	        if (param.orthantwise_c == 0.) {
	            gnorm=vec2norm(g, n);
	        } else {
	            gnorm=vec2norm(pg, n);
	        }

	        /* Report the progress. */
	        if (cd.proc_progress != null) {
	        	int stopEarly = cd.proc_progress.apply(x, g, fx[0], xnorm, gnorm, step[0], cd.n, k, ls);
	            if (stopEarly != 0) {
	            	ret.status = Status.LBFGS_STOP;
	            	ret.additionalStatus = stopEarly;
	            	ret.objective = fx[0];
	            	return ret;
	            }
	        }

	        /*
	            Convergence test.
	            The criterion is given by the following formula:
	                |g(x)| / \max(1, |x|) < \epsilon
	         */
	        if (xnorm < 1.0) xnorm = 1.0;
	        if (gnorm / xnorm <= param.epsilon) {
	            /* Convergence. */
	            ret.status = Status.LBFGS_SUCCESS;
	            break;
	        }

	        /*
	            Test for stopping criterion.
	            The criterion is given by the following formula:
	                (f(past_x) - f(x)) / f(x) < \delta
	         */
	        if (pf != null) {
	            /* We don't test the stopping criterion while k < past. */
	            if (param.past <= k) {
	                /* Compute the relative improvement from the past. */
	                rate = (pf[k % param.past] - fx[0]) / fx[0];

	                /* The stopping criterion. */
	                if (rate < param.delta) {
	                    ret.status = Status.LBFGS_STOP;
	                    break;
	                }
	            }

	            /* Store the current value of the objective function. */
	            pf[k % param.past] = fx[0];
	        }

	        if (param.max_iterations != 0 && param.max_iterations < k+1) {
	            /* Maximum number of iterations. */
	            ret.status = Status.LBFGSERR_MAXIMUMITERATION;
	            break;
	        }

	        /*
	            Update vectors s and y:
	                s_{k+1} = x_{k+1} - x_{k} = \step * d_{k}.
	                y_{k+1} = g_{k+1} - g_{k}.
	         */
	        it = lm[end];
	        vecdiff(it.s, x, xp, n);
	        vecdiff(it.y, g, gp, n);

	        /*
	            Compute scalars ys and yy:
	                ys = y^t \cdot s = 1 / \rho.
	                yy = y^t \cdot y.
	            Notice that yy is used for scaling the hessian matrix H_0 (Cholesky factor).
	         */
	        ys=vecdot(it.y, it.s, n);
	        yy=vecdot(it.y, it.y, n);
	        it.ys = ys;

	        /*
	            Recursive formula to compute dir = -(H \cdot g).
	                This is described in page 779 of:
	                Jorge Nocedal.
	                Updating Quasi-Newton Matrices with Limited Storage.
	                Mathematics of Computation, Vol. 35, No. 151,
	                pp. 773--782, 1980.
	         */
	        bound = (m <= k) ? m : k;
	        ++k;
	        end = (end + 1) % m;

	        /* Compute the steepest direction. */
	        if (param.orthantwise_c == 0.) {
	            /* Compute the negative of gradients. */
	            vecncpy(d, g, n);
	        } else {
	            vecncpy(d, pg, n);
	        }

	        j = end;
	        for (i = 0;i < bound;++i) {
	            j = (j + m - 1) % m;    /* if (--j == -1) j = m-1; */
	            it = lm[j];
	            /* \alpha_{j} = \rho_{j} s^{t}_{j} \cdot q_{k+1}. */
	            it.alpha = vecdot(it.s, d, n);
	            it.alpha /= it.ys;
	            /* q_{i} = q_{i+1} - \alpha_{i} y_{i}. */
	            vecadd(d, it.y, -it.alpha, n);
	        }

	        vecscale(d, ys / yy, n);

	        for (i = 0;i < bound;++i) {
	            it = lm[j];
	            /* \beta_{j} = \rho_{j} y^t_{j} \cdot \gamma_{i}. */
	            beta = vecdot(it.y, d, n);
	            beta /= it.ys;
	            /* \gamma_{i+1} = \gamma_{i} + (\alpha_{j} - \beta_{j}) s_{j}. */
	            vecadd(d, it.s, it.alpha - beta, n);
	            j = (j + 1) % m;        /* if (++j == m) j = 0; */
	        }

	        /*
	            Constrain the search direction for orthant-wise updates.
	         */
	        if (param.orthantwise_c != 0.) {
	            for (i = param.orthantwise_start;i < param.orthantwise_end;++i) {
	                if (d[i] * pg[i] >= 0) {
	                    d[i] = 0;
	                }
	            }
	        }

	        /*
	            Now the search direction d is ready. We try step = 1 first.
	         */
	        step[0] = 1.0;
	    }
	    
//	    lbfgs_exit:
	    	
        /* Return the final value of the objective function. */
//        if (ptr_fx != NULL) {
//            *ptr_fx = fx;
//        }

	    ret.objective = fx[0];
        return ret;
	}

	//////////////////////
	
	static double vecdot(double[] a, double[] b, int n) {
		double re = 0;
		for (int i=0; i<n; i++)
			re+=a[i]*b[i];
		return re;
//		return Arr.innerProduct(a, b);
	}
	static void veccpy(double[] y, double[] x, int n) {
		System.arraycopy(x,0, y,0, n);
	}
	static void vecadd(double[] y, final double[] x, final double c, final int n) {
	    int i;

	    for (i = 0;i < n;++i) {
	        y[i] += c * x[i];
	    }
	}
	static void vecncpy(double[] y, double[] x, int n) {
	    for (int i = 0;i < n;++i) {
	        y[i] = -x[i];
	    }
	}
	static void vecset(double[] x, final double c, final int n) { 
		for (int i=0; i < x.length; i++) {
			x[i] = c;
		}
	}
	static void vecdiff(double[] z, final double[] x, final double[] y, final int n) {
	    for (int i = 0; i < n; ++i) {
	        z[i] = x[i] - y[i];
	    }
	}
	static void vecscale(double[] y, final double c, final int n) {
	    for (int i = 0; i < n; ++i) {
	        y[i] *= c;
	    }
	}
	static void vecmul(double[] y, final double[] x, final int n) {
	    for (int i = 0; i < n; ++i) {
	        y[i] *= x[i];
	    }
	}
	static double vec2norm(final double[] x, final int n)
	{
	    double s = vecdot(x, x, n);
	    return Math.sqrt(s);
	}

	static double vec2norminv(final double[] x, final int n)
	{
	    double s = vec2norm(x, n);
	    return 1.0 / s;
	}



	static class line_search_backtracking implements line_search_proc {
	
	public Status go(
	    int n,
	    double[] x,
	    double[] f,
	    double[] g,
	    double[] s,
	    double[] stp, // BTO: um i think this is supposed to be a singleton
	    final double[]  xp,
	    final double[]  gp,
	    double[] wp,
	    callback_data_t cd,
	    Params param
	    )
	{
	    int count = 0;
	    double width, dg;
	    double finit, dginit = 0., dgtest;
	    final double dec = 0.5, inc = 2.1;

	    /* Check the input parameters for errors. */
	    if (stp[0] <= 0.) {
	        return Status.LBFGSERR_INVALIDPARAMETERS;
	    }

	    /* Compute the initial gradient in the search direction. */
	    dginit = vecdot(g, s, n);

	    /* Make sure that s points to a descent direction. */
	    if (0 < dginit) {
	        return Status.LBFGSERR_INCREASEGRADIENT;
	    }

	    /* The initial value of the objective function. */
	    finit = f[0];
	    dgtest = param.ftol * dginit;

	    for (;;) {
	        veccpy(x, xp, n);
	        vecadd(x, s, stp[0], n);

	        /* Evaluate the function and gradient values. */
	        f[0] = cd.proc_evaluate.evaluate(x, g, n, stp[0]);

	        ++count;

	        if (f[0] > finit + stp[0] * dgtest) {
	            width = dec;
	        } else {
	            /* The sufficient decrease condition (Armijo condition). */
	            if (param.linesearch == LinesearchAlgorithm.LBFGS_LINESEARCH_BACKTRACKING_ARMIJO) {
	                /* Exit with the Armijo condition. */
//	                return count; 
	            	return Status.LBFGS_SUCCESS; // BTO changed
	            	
		        }

		        /* Check the Wolfe condition. */
		        dg = vecdot(g, s, n);
		        if (dg < param.wolfe * dginit) {
	    		    width = inc;
		        } else {
			        if(param.linesearch == LinesearchAlgorithm.LBFGS_LINESEARCH_BACKTRACKING_WOLFE) {
			            /* Exit with the regular Wolfe condition. */
//			            return count;
		            	return Status.LBFGS_SUCCESS; // BTO changed
			        }

			        /* Check the strong Wolfe condition. */
			        if(dg > -param.wolfe * dginit) {
			            width = dec;
			        } else {
			            /* Exit with the strong Wolfe condition. */
//			            return count;
		            	return Status.LBFGS_SUCCESS; // BTO changed
			        }
	            }
	        }

	        if (stp[0] < param.min_step) {
	            /* The step is the minimum value. */
	            return Status.LBFGSERR_MINIMUMSTEP;
	        }
	        if (stp[0] > param.max_step) {
	            /* The step is the maximum value. */
	            return Status.LBFGSERR_MAXIMUMSTEP;
	        }
	        if (param.max_linesearch <= count) {
	            /* Maximum number of iteration. */
	            return Status.LBFGSERR_MAXIMUMLINESEARCH;
	        }

	        stp[0] *= width;
	    }
	}
	}



	static class line_search_backtracking_owlqn implements line_search_proc { 
	
	public Status go(
	    int n,
	    double[] x,
	    double[] f,
	    double[] g,
	    double[] s,
	    double[] stp,
	    final double[] xp,
	    final double[] gp,
	    double[] wp,
	    callback_data_t cd,
	    Params param
	    )
	{
	    int i, count = 0;
	    double width = 0.5, norm = 0.;
	    double finit = f[0], dgtest;

	    /* Check the input parameters for errors. */
	    if (stp[0] <= 0.) {
	        return Status.LBFGSERR_INVALIDPARAMETERS;
	    }

	    /* Choose the orthant for the new point. */
	    for (i = 0;i < n;++i) {
	        wp[i] = (xp[i] == 0.) ? -gp[i] : xp[i];
	    }

	    for (;;) {
	        /* Update the current point. */
	        veccpy(x, xp, n);
	        vecadd(x, s, stp[0], n);

	        /* The current point is projected onto the orthant. */
	        owlqn_project(x, wp, param.orthantwise_start, param.orthantwise_end);

	        /* Evaluate the function and gradient values. */
	        f[0] = cd.proc_evaluate.evaluate(x, g, cd.n, stp[0]);

	        /* Compute the L1 norm of the variables and add it to the object value. */
	        norm = owlqn_x1norm(x, param.orthantwise_start, param.orthantwise_end);
	        f[0] += norm * param.orthantwise_c;

	        ++count;

	        dgtest = 0.;
	        for (i = 0;i < n;++i) {
	            dgtest += (x[i] - xp[i]) * gp[i];
	        }

	        if (f[0] <= finit + param.ftol * dgtest) {
	            /* The sufficient decrease condition. */
//	            return count;
	        	return Status.LBFGS_SUCCESS; // BTO changed
	        }

	        if (stp[0] < param.min_step) {
	            /* The step is the minimum value. */
	            return Status.LBFGSERR_MINIMUMSTEP;
	        }
	        if (stp[0] > param.max_step) {
	            /* The step is the maximum value. */
	            return Status.LBFGSERR_MAXIMUMSTEP;
	        }
	        if (param.max_linesearch <= count) {
	            /* Maximum number of iteration. */
	            return Status.LBFGSERR_MAXIMUMLINESEARCH;
	        }

	        stp[0] *= width;
	    }
	}
	}


	static class line_search_morethuente implements line_search_proc {

		@Override
		public Status go(int n, double[] x, double[] f, double[] g, double[] s,
				double[] stp, double[] xp, double[] gp, double[] wa,
				callback_data_t cd, Params param) {
			assert false : "unimplemented";
			return null;
		}
	}

//	static class line_search_morethuente implements line_search_proc {
//	
//	public int go(
//	    int n,
//	    double[] x,
//	    double[] f,
//	    double[] g,
//	    double[] s,
//	    double[] stp,
//	    final double[] xp,
//	    final double[] gp,
//	    double[] wa,
//	    Function evaluator, ProgressCallback progress,
//	    Params param
//	    )
//	{
//	    int count = 0;
//	    int brackt, stage1, uinfo = 0;
//	    lbfgsfloatval_t dg;
//	    lbfgsfloatval_t stx, fx, dgx;
//	    lbfgsfloatval_t sty, fy, dgy;
//	    lbfgsfloatval_t fxm, dgxm, fym, dgym, fm, dgm;
//	    lbfgsfloatval_t finit, ftest1, dginit, dgtest;
//	    lbfgsfloatval_t width, prev_width;
//	    lbfgsfloatval_t stmin, stmax;
//
//	    /* Check the input parameters for errors. */
//	    if (*stp <= 0.) {
//	        return LBFGSERR_INVALIDPARAMETERS;
//	    }
//
//	    /* Compute the initial gradient in the search direction. */
//	    vecdot(&dginit, g, s, n);
//
//	    /* Make sure that s points to a descent direction. */
//	    if (0 < dginit) {
//	        return LBFGSERR_INCREASEGRADIENT;
//	    }
//
//	    /* Initialize local variables. */
//	    brackt = 0;
//	    stage1 = 1;
//	    finit = *f;
//	    dgtest = param->ftol * dginit;
//	    width = param->max_step - param->min_step;
//	    prev_width = 2.0 * width;
//
//	    /*
//	        The variables stx, fx, dgx contain the values of the step,
//	        function, and directional derivative at the best step.
//	        The variables sty, fy, dgy contain the value of the step,
//	        function, and derivative at the other endpoint of
//	        the interval of uncertainty.
//	        The variables stp, f, dg contain the values of the step,
//	        function, and derivative at the current step.
//	    */
//	    stx = sty = 0.;
//	    fx = fy = finit;
//	    dgx = dgy = dginit;
//
//	    for (;;) {
//	        /*
//	            Set the minimum and maximum steps to correspond to the
//	            present interval of uncertainty.
//	         */
//	        if (brackt) {
//	            stmin = min2(stx, sty);
//	            stmax = max2(stx, sty);
//	        } else {
//	            stmin = stx;
//	            stmax = *stp + 4.0 * (*stp - stx);
//	        }
//
//	        /* Clip the step in the range of [stpmin, stpmax]. */
//	        if (*stp < param->min_step) *stp = param->min_step;
//	        if (param->max_step < *stp) *stp = param->max_step;
//
//	        /*
//	            If an unusual termination is to occur then let
//	            stp be the lowest point obtained so far.
//	         */
//	        if ((brackt && ((*stp <= stmin || stmax <= *stp) || param->max_linesearch <= count + 1 || uinfo != 0)) || (brackt && (stmax - stmin <= param->xtol * stmax))) {
//	            *stp = stx;
//	        }
//
//	        /*
//	            Compute the current value of x:
//	                x <- x + (*stp) * s.
//	         */
//	        veccpy(x, xp, n);
//	        vecadd(x, s, *stp, n);
//
//	        /* Evaluate the function and gradient values. */
//	        *f = cd->proc_evaluate(cd->instance, x, g, cd->n, *stp);
//	        vecdot(&dg, g, s, n);
//
//	        ftest1 = finit + *stp * dgtest;
//	        ++count;
//
//	        /* Test for errors and convergence. */
//	        if (brackt && ((*stp <= stmin || stmax <= *stp) || uinfo != 0)) {
//	            /* Rounding errors prevent further progress. */
//	            return LBFGSERR_ROUNDING_ERROR;
//	        }
//	        if (*stp == param->max_step && *f <= ftest1 && dg <= dgtest) {
//	            /* The step is the maximum value. */
//	            return LBFGSERR_MAXIMUMSTEP;
//	        }
//	        if (*stp == param->min_step && (ftest1 < *f || dgtest <= dg)) {
//	            /* The step is the minimum value. */
//	            return LBFGSERR_MINIMUMSTEP;
//	        }
//	        if (brackt && (stmax - stmin) <= param->xtol * stmax) {
//	            /* Relative width of the interval of uncertainty is at most xtol. */
//	            return LBFGSERR_WIDTHTOOSMALL;
//	        }
//	        if (param->max_linesearch <= count) {
//	            /* Maximum number of iteration. */
//	            return LBFGSERR_MAXIMUMLINESEARCH;
//	        }
//	        if (*f <= ftest1 && fabs(dg) <= param->gtol * (-dginit)) {
//	            /* The sufficient decrease condition and the directional derivative condition hold. */
//	            return count;
//	        }
//
//	        /*
//	            In the first stage we seek a step for which the modified
//	            function has a nonpositive value and nonnegative derivative.
//	         */
//	        if (stage1 && *f <= ftest1 && min2(param->ftol, param->gtol) * dginit <= dg) {
//	            stage1 = 0;
//	        }
//
//	        /*
//	            A modified function is used to predict the step only if
//	            we have not obtained a step for which the modified
//	            function has a nonpositive function value and nonnegative
//	            derivative, and if a lower function value has been
//	            obtained but the decrease is not sufficient.
//	         */
//	        if (stage1 && ftest1 < *f && *f <= fx) {
//	            /* Define the modified function and derivative values. */
//	            fm = *f - *stp * dgtest;
//	            fxm = fx - stx * dgtest;
//	            fym = fy - sty * dgtest;
//	            dgm = dg - dgtest;
//	            dgxm = dgx - dgtest;
//	            dgym = dgy - dgtest;
//
//	            /*
//	                Call update_trial_interval() to update the interval of
//	                uncertainty and to compute the new step.
//	             */
//	            uinfo = update_trial_interval(
//	                &stx, &fxm, &dgxm,
//	                &sty, &fym, &dgym,
//	                stp, &fm, &dgm,
//	                stmin, stmax, &brackt
//	                );
//
//	            /* Reset the function and gradient values for f. */
//	            fx = fxm + stx * dgtest;
//	            fy = fym + sty * dgtest;
//	            dgx = dgxm + dgtest;
//	            dgy = dgym + dgtest;
//	        } else {
//	            /*
//	                Call update_trial_interval() to update the interval of
//	                uncertainty and to compute the new step.
//	             */
//	            uinfo = update_trial_interval(
//	                &stx, &fx, &dgx,
//	                &sty, &fy, &dgy,
//	                stp, f, &dg,
//	                stmin, stmax, &brackt
//	                );
//	        }
//
//	        /*
//	            Force a sufficient decrease in the interval of uncertainty.
//	         */
//	        if (brackt) {
//	            if (0.66 * prev_width <= fabs(sty - stx)) {
//	                *stp = stx + 0.5 * (sty - stx);
//	            }
//	            prev_width = width;
//	            width = fabs(sty - stx);
//	        }
//	    }
//
//	    return LBFGSERR_LOGICERROR;
//	}
//	}



	/**
	 * Define the local variables for computing minimizers.
	 */
//	#define USES_MINIMIZER \
//	    lbfgsfloatval_t a, d, gamma, theta, p, q, r, s;

	/**
	 * Find a minimizer of an interpolated cubic function.
	 *  @param  cm      The minimizer of the interpolated cubic.   BTO REMOVED, return instead
	 *  @param  u       The value of one point, u.
	 *  @param  fu      The value of f(u).
	 *  @param  du      The value of f'(u).
	 *  @param  v       The value of another point, v.
	 *  @param  fv      The value of f(v).
	 *  @param  du      The value of f'(v).
	 */
	static double CUBIC_MINIMIZER(double u, double fu, double du, double v, double fv, double dv) {
//	#define CUBIC_MINIMIZER(cm, u, fu, du, v, fv, dv) \
	    double d = (v) - (u);
	    double theta = ((fu) - (fv)) * 3 / d + (du) + (dv);
	    double p = Math.abs(theta);
	    double q = Math.abs(du);
	    double r = Math.abs(dv);
	    double s = max3(p, q, r);
	    /* gamma = s*sqrt((theta/s)**2 - (du/s) * (dv/s)) */
	    double a = theta / s;
	    double gamma = s * Math.sqrt(a * a - ((du) / s) * ((dv) / s));
	    if ((v) < (u)) gamma = -gamma;
	    p = gamma - (du) + theta;
	    q = gamma - (du) + gamma + (dv);
	    r = p / q;
		double cm = (u) + r * d;
	    return cm;
	}

	/**
	 * Find a minimizer of an interpolated cubic function.
	 *  @param  cm      The minimizer of the interpolated cubic.
	 *  @param  u       The value of one point, u.
	 *  @param  fu      The value of f(u).
	 *  @param  du      The value of f'(u).
	 *  @param  v       The value of another point, v.
	 *  @param  fv      The value of f(v).
	 *  @param  du      The value of f'(v).
	 *  @param  xmin    The maximum value.
	 *  @param  xmin    The minimum value.
	 */
//	#define CUBIC_MINIMIZER2(cm, u, fu, du, v, fv, dv, xmin, xmax) \
	static double CUBIC_MINIMIZER2(double u, double fu, double du, double v, 
			double fv, double dv, double xmin, double xmax) {
	    double d = (v) - (u);
	    double theta = ((fu) - (fv)) * 3 / d + (du) + (dv);
	    double p = Math.abs(theta);
	    double q = Math.abs(du);
	    double r = Math.abs(dv);
	    double s = max3(p, q, r);
	    /* gamma = s*sqrt((theta/s)**2 - (du/s) * (dv/s)) */
	    double a = theta / s;
	    double gamma = s * Math.sqrt(Math.max(0, a * a - ((du) / s) * ((dv) / s)));
	    if ((u) < (v)) gamma = -gamma;
	    p = gamma - (dv) + theta;
	    q = gamma - (dv) + gamma + (du);
	    r = p / q;
	    double cm;
	    if (r < 0. && gamma != 0.) {
	        (cm) = (v) - r * d;
	    } else if (a < 0) {
	        (cm) = (xmax);
	    } else {
	        (cm) = (xmin);
	    }
	    return cm;
	}

	/**
	 * Find a minimizer of an interpolated quadratic function.
	 *  @return      The minimizer of the interpolated quadratic.
	 *  @param  u       The value of one point, u.
	 *  @param  fu      The value of f(u).
	 *  @param  du      The value of f'(u).
	 *  @param  v       The value of another point, v.
	 *  @param  fv      The value of f(v).
	 */
	static double QUARD_MINIMIZER(double u, double fu, double du, double v, double fv) {
//	#define QUARD_MINIMIZER(qm, u, fu, du, v, fv) \
	    double a = (v) - (u); 
	    double qm = (u) + (du) / (((fu) - (fv)) / a + (du)) / 2 * a;
	    return qm;
	}

	/**
	 * Find a minimizer of an interpolated quadratic function.
	 *  @param  qm      The minimizer of the interpolated quadratic.
	 *  @param  u       The value of one point, u.
	 *  @param  du      The value of f'(u).
	 *  @param  v       The value of another point, v.
	 *  @param  dv      The value of f'(v).
	 */
//	#define QUARD_MINIMIZER2(qm, u, du, v, dv) \
	static double QUARD_MINIMIZER2(double u, double du, double v, double dv) {
	    double a = (u) - (v); 
	    double qm = (v) + (dv) / ((dv) - (du)) * a;
	    return qm;
	}

	/**
	 * Update a safeguarded trial value and interval for line search.
	 *
	 *  The parameter x represents the step with the least function value.
	 *  The parameter t represents the current step. This function assumes
	 *  that the derivative at the point of x in the direction of the step.
	 *  If the bracket is set to true, the minimizer has been bracketed in
	 *  an interval of uncertainty with endpoints between x and y.
	 *
	 *  @param  x       The pointer to the value of one endpoint.
	 *  @param  fx      The pointer to the value of f(x).
	 *  @param  dx      The pointer to the value of f'(x).
	 *  @param  y       The pointer to the value of another endpoint.
	 *  @param  fy      The pointer to the value of f(y).
	 *  @param  dy      The pointer to the value of f'(y).
	 *  @param  t       The pointer to the value of the trial value, t.
	 *  @param  ft      The pointer to the value of f(t).
	 *  @param  dt      The pointer to the value of f'(t).
	 *  @param  tmin    The minimum value for the trial value, t.
	 *  @param  tmax    The maximum value for the trial value, t.
	 *  @param  brackt  The pointer to the predicate if the trial value is
	 *                  bracketed.
	 *  @retval int     Status value. Zero indicates a normal termination.
	 *  
	 *  @see
	 *      Jorge J. More and David J. Thuente. Line search algorithm with
	 *      guaranteed sufficient decrease. ACM Transactions on Mathematical
	 *      Software (TOMS), Vol 20, No 3, pp. 286-307, 1994.
	 */
//	static int update_trial_interval(
//	    double[] x,
//	    double[] fx,
//	    double[] dx,
//	    double[] y,
//	    double[] fy,
//	    double[] dy,
//	    double[] t,
//	    double[] ft,
//	    double[] dt,
//	    final double tmin,
//	    final double tmax,
//	    int[] brackt
//	    )
//	{
//	    int bound;
//	    int dsign = fsigndiff(dt, dx);
//	    lbfgsfloatval_t mc; /* minimizer of an interpolated cubic. */
//	    lbfgsfloatval_t mq; /* minimizer of an interpolated quadratic. */
//	    lbfgsfloatval_t newt;   /* new trial value. */
//
//	    /* Check the input parameters for errors. */
//	    if (*brackt) {
//	        if (*t <= min2(*x, *y) || max2(*x, *y) <= *t) {
//	            /* The trival value t is out of the interval. */
//	            return LBFGSERR_OUTOFINTERVAL;
//	        }
//	        if (0. <= *dx * (*t - *x)) {
//	            /* The function must decrease from x. */
//	            return LBFGSERR_INCREASEGRADIENT;
//	        }
//	        if (tmax < tmin) {
//	            /* Incorrect tmin and tmax specified. */
//	            return LBFGSERR_INCORRECT_TMINMAX;
//	        }
//	    }
//
//	    /*
//	        Trial value selection.
//	     */
//	    if (*fx < *ft) {
//	        /*
//	            Case 1: a higher function value.
//	            The minimum is brackt. If the cubic minimizer is closer
//	            to x than the quadratic one, the cubic one is taken, else
//	            the average of the minimizers is taken.
//	         */
//	        *brackt = 1;
//	        bound = 1;
//	        CUBIC_MINIMIZER(mc, *x, *fx, *dx, *t, *ft, *dt);
//	        QUARD_MINIMIZER(mq, *x, *fx, *dx, *t, *ft);
//	        if (fabs(mc - *x) < fabs(mq - *x)) {
//	            newt = mc;
//	        } else {
//	            newt = mc + 0.5 * (mq - mc);
//	        }
//	    } else if (dsign) {
//	        /*
//	            Case 2: a lower function value and derivatives of
//	            opposite sign. The minimum is brackt. If the cubic
//	            minimizer is closer to x than the quadratic (secant) one,
//	            the cubic one is taken, else the quadratic one is taken.
//	         */
//	        *brackt = 1;
//	        bound = 0;
//	        CUBIC_MINIMIZER(mc, *x, *fx, *dx, *t, *ft, *dt);
//	        QUARD_MINIMIZER2(mq, *x, *dx, *t, *dt);
//	        if (fabs(mc - *t) > fabs(mq - *t)) {
//	            newt = mc;
//	        } else {
//	            newt = mq;
//	        }
//	    } else if (fabs(*dt) < fabs(*dx)) {
//	        /*
//	            Case 3: a lower function value, derivatives of the
//	            same sign, and the magnitude of the derivative decreases.
//	            The cubic minimizer is only used if the cubic tends to
//	            infinity in the direction of the minimizer or if the minimum
//	            of the cubic is beyond t. Otherwise the cubic minimizer is
//	            defined to be either tmin or tmax. The quadratic (secant)
//	            minimizer is also computed and if the minimum is brackt
//	            then the the minimizer closest to x is taken, else the one
//	            farthest away is taken.
//	         */
//	        bound = 1;
//	        CUBIC_MINIMIZER2(mc, *x, *fx, *dx, *t, *ft, *dt, tmin, tmax);
//	        QUARD_MINIMIZER2(mq, *x, *dx, *t, *dt);
//	        if (*brackt) {
//	            if (fabs(*t - mc) < fabs(*t - mq)) {
//	                newt = mc;
//	            } else {
//	                newt = mq;
//	            }
//	        } else {
//	            if (fabs(*t - mc) > fabs(*t - mq)) {
//	                newt = mc;
//	            } else {
//	                newt = mq;
//	            }
//	        }
//	    } else {
//	        /*
//	            Case 4: a lower function value, derivatives of the
//	            same sign, and the magnitude of the derivative does
//	            not decrease. If the minimum is not brackt, the step
//	            is either tmin or tmax, else the cubic minimizer is taken.
//	         */
//	        bound = 0;
//	        if (*brackt) {
//	            CUBIC_MINIMIZER(newt, *t, *ft, *dt, *y, *fy, *dy);
//	        } else if (*x < *t) {
//	            newt = tmax;
//	        } else {
//	            newt = tmin;
//	        }
//	    }
//
//	    /*
//	        Update the interval of uncertainty. This update does not
//	        depend on the new step or the case analysis above.
//
//	        - Case a: if f(x) < f(t),
//	            x <- x, y <- t.
//	        - Case b: if f(t) <= f(x) && f'(t)*f'(x) > 0,
//	            x <- t, y <- y.
//	        - Case c: if f(t) <= f(x) && f'(t)*f'(x) < 0, 
//	            x <- t, y <- x.
//	     */
//	    if (*fx < *ft) {
//	        /* Case a */
//	        *y = *t;
//	        *fy = *ft;
//	        *dy = *dt;
//	    } else {
//	        /* Case c */
//	        if (dsign) {
//	            *y = *x;
//	            *fy = *fx;
//	            *dy = *dx;
//	        }
//	        /* Cases b and c */
//	        *x = *t;
//	        *fx = *ft;
//	        *dx = *dt;
//	    }
//
//	    /* Clip the new trial value in [tmin, tmax]. */
//	    if (tmax < newt) newt = tmax;
//	    if (newt < tmin) newt = tmin;
//
//	    /*
//	        Redefine the new trial value if it is close to the upper bound
//	        of the interval.
//	     */
//	    if (*brackt && bound) {
//	        mq = *x + 0.66 * (*y - *x);
//	        if (*x < *y) {
//	            if (mq < newt) newt = mq;
//	        } else {
//	            if (newt < mq) newt = mq;
//	        }
//	    }
//
//	    /* Return the new trial value. */
//	    *t = newt;
//	    return 0;
//	}





	static double owlqn_x1norm(
	    final double[] x,
	    final int start,
	    final int n
	    )
	{
	    int i;
	    double norm = 0.;

	    for (i = start;i < n;++i) {
	        norm += Math.abs(x[i]);
	    }

	    return norm;
	}

	static void owlqn_pseudo_gradient(
	    double[] pg,
	    final double[] x,
	    final double[] g,
	    final int n,
	    final double c,
	    final int start,
	    final int end
	    )
	{
	    int i;

	    /* Compute the negative of gradients. */
	    for (i = 0;i < start;++i) {
	        pg[i] = g[i];
	    }

	    /* Compute the psuedo-gradients. */
	    for (i = start;i < end;++i) {
	        if (x[i] < 0.) {
	            /* Differentiable. */
	            pg[i] = g[i] - c;
	        } else if (0. < x[i]) {
	            /* Differentiable. */
	            pg[i] = g[i] + c;
	        } else {
	            if (g[i] < -c) {
	                /* Take the right partial derivative. */
	                pg[i] = g[i] + c;
	            } else if (c < g[i]) {
	                /* Take the left partial derivative. */
	                pg[i] = g[i] - c;
	            } else {
	                pg[i] = 0.;
	            }
	        }
	    }

	    for (i = end;i < n;++i) {
	        pg[i] = g[i];
	    }
	}

	static void owlqn_project(
			double[] d,
	    final double[] sign,
	    final int start,
	    final int end
	    )
	{
	    int i;

	    for (i = start;i < end;++i) {
	        if (d[i] * sign[i] <= 0) {
	            d[i] = 0;
	        }
	    }
	}


}
