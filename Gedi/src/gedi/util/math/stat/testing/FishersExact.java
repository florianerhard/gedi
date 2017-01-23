package gedi.util.math.stat.testing;

import gedi.util.ArrayUtils;


public class FishersExact {
    private static final boolean DEBUG = false;
    private double[] f;
    int maxSize;


    /**
     * constructor for FisherExact table
     *
     * @param maxSize is the maximum sum that will be encountered by the table (a+b+c+d)
     */
    public FishersExact(int maxSize) {
        this.maxSize = maxSize;
        double cf = 1.0;
        f = new double[maxSize + 1];
        f[0] = 0.0;
        for (int i = 1; i <= this.maxSize; i++) {
            f[i] = f[i - 1] + Math.log(i);
        }
    }

    public void resize(int maxSize) {
    	if (maxSize>this.maxSize) {
    		f = ArrayUtils.redimPreserve(f, maxSize+1);
    		for (int i = this.maxSize+1; i <= maxSize; i++) 
                f[i] = f[i - 1] + Math.log(i);
            this.maxSize = maxSize;
    	}
    }
    

	public int getMax() {
		return maxSize;
	}

    /**
     * calculates the P-value for this specific state
     *
     * @param a     a, b, c, d are the four cells in a 2x2 matrix
     * @param b
     * @param c
     * @param d
     * @return the P-value
     */
    public final double getP(int a, int b, int c, int d) {
        int n = a + b + c + d;
        if (n > maxSize) {
            return Double.NaN;
        }
        double p;
        p = (f[a + b] + f[c + d] + f[a + c] + f[b + d]) - (f[a] + f[b] + f[c] + f[d] + f[n]);
        return Math.exp(p);
    }

    /**
     * Calculates the one-tail P-value for the Fisher Exact test.  Determines whether to calculate the right- or left-
     * tail, thereby always returning the smallest p-value.
     *
     * @param a     a, b, c, d are the four cells in a 2x2 matrix
     * @param b
     * @param c
     * @param d
     * @return one-tailed P-value (right or left, whichever is smallest)
     */
    public final double getCumulativeP(int a, int b, int c, int d) {
        int min, i;
        int n = a + b + c + d;
        if (n > maxSize) {
            return Double.NaN;
        }
        double p = 0;

        p += getP(a, b, c, d);
        if (DEBUG) {System.out.println("p = " + p);}
        if ((a * d) >= (b * c)) {
            if (DEBUG) {System.out.println("doing R-tail: a=" + a + " b=" + b + " c=" + c + " d=" + d);}
            min = (c < b) ? c : b;
            for (i = 0; i < min; i++) {
                if (DEBUG) {System.out.print("doing round " + i);}
                p += getP(++a, --b, --c, ++d);
                if (DEBUG) {System.out.println("\ta=" + a + " b=" + b + " c=" + c + " d=" + d);}
            }
            System.out.println("");
        }
        if ((a * d) < (b * c)) {
            if (DEBUG) {System.out.println("doing L-tail: a=" + a + " b=" + b + " c=" + c + " d=" + d);}
            min = (a < d) ? a : d;
            for (i = 0; i < min; i++) {
                if (DEBUG) {System.out.print("doing round " + i);}
                double pTemp = getP(--a, ++b, ++c, --d);
                if (DEBUG) {System.out.print("\tpTemp = " + pTemp);}
                p += pTemp;
                if (DEBUG) {System.out.println("\ta=" + a + " b=" + b + " c=" + c + " d=" + d);}
            }
        }
        return p;
    }

    /**
     * Calculates the right-tail P-value for the Fisher Exact test.
     *
     * @param a     a, b, c, d are the four cells in a 2x2 matrix
     * @param b
     * @param c
     * @param d
     * @return one-tailed P-value (right-tail)
     */
    public final double getRightTailedP(int a, int b, int c, int d) {
        int min, i;
        int n = a + b + c + d;
        if (n > maxSize) {
            return Double.NaN;
        }
        double p = 0;

        p += getP(a, b, c, d);
        if (DEBUG) {System.out.println("p = " + p);}
        if (DEBUG) {System.out.println("doing R-tail: a=" + a + " b=" + b + " c=" + c + " d=" + d);}
        min = (c < b) ? c : b;
        for (i = 0; i < min; i++) {
            p += getP(++a, --b, --c, ++d);

        }
        return p;
    }

    /**
     * Calculates the left-tail P-value for the Fisher Exact test.
     *
     * @param a     a, b, c, d are the four cells in a 2x2 matrix
     * @param b
     * @param c
     * @param d
     * @return one-tailed P-value (left-tail)
     */
    public final double getLeftTailedP(int a, int b, int c, int d) {
        int min, i;
        int n = a + b + c + d;
        if (n > maxSize) {
            return Double.NaN;
        }
        double p = 0;

        p += getP(a, b, c, d);
        if (DEBUG) {System.out.println("p = " + p);}
        if (DEBUG) {System.out.println("doing L-tail: a=" + a + " b=" + b + " c=" + c + " d=" + d);}
        min = (a < d) ? a : d;
        for (i = 0; i < min; i++) {
            if (DEBUG) {System.out.print("doing round " + i);}
            double pTemp = getP(--a, ++b, ++c, --d);
            if (DEBUG) {System.out.print("\tpTemp = " + pTemp);}
            p += pTemp;
            if (DEBUG) {System.out.println("\ta=" + a + " b=" + b + " c=" + c + " d=" + d);}
        }


        return p;
    }


    /**
     *   Calculates the two-tailed P-value for the Fisher Exact test.
     *
     *   In order for a table under consideration to have its p-value included
     *   in the final result, it must have a p-value less than the original table's P-value, i.e.
     *   Fisher's exact test computes the probability, given the observed marginal
     *   frequencies, of obtaining exactly the frequencies observed and any configuration more extreme.
     *   By "more extreme," we mean any configuration (given observed marginals) with a smaller probability of
     *   occurrence in the same direction (one-tailed) or in both directions (two-tailed).
     *
     * @param a     a, b, c, d are the four cells in a 2x2 matrix
     * @param b
     * @param c
     * @param d
     * @return two-tailed P-value
     */
    public final double getTwoTailedP(int a, int b, int c, int d) {
        int min, i;
        int n = a + b + c + d;
        if (n > maxSize) {
            return Double.NaN;
        }
        double p = 0;

        double baseP = getP(a, b, c, d);
//         in order for a table under consideration to have its p-value included
//         in the final result, it must have a p-value less than the baseP, i.e.
//         Fisher's exact test computes the probability, given the observed marginal
//         frequencies, of obtaining exactly the frequencies observed and any configuration more extreme.
//         By "more extreme," we mean any configuration (given observed marginals) with a smaller probability of
//         occurrence in the same direction (one-tailed) or in both directions (two-tailed).

        if (DEBUG) {System.out.println("baseP = " + baseP);}
        int initialA = a, initialB = b, initialC = c, initialD = d;
        p += baseP;
        if (DEBUG) {System.out.println("p = " + p);}
        if (DEBUG) {System.out.println("Starting with R-tail: a=" + a + " b=" + b + " c=" + c + " d=" + d);}
        min = (c < b) ? c : b;
        for (i = 0; i < min; i++) {
            if (DEBUG) {System.out.print("doing round " + i);}
            double tempP = getP(++a, --b, --c, ++d);
            if (tempP <= baseP) {
                if (DEBUG) {System.out.print("\ttempP (" + tempP + ") is less than baseP (" + baseP + ")");}
                p += tempP;
            }
            if (DEBUG) {System.out.println(" a=" + a + " b=" + b + " c=" + c + " d=" + d);}
        }

        // reset the values to their original so we can repeat this process for the other side
        a = initialA;
        b = initialB;
        c = initialC;
        d = initialD;

        if (DEBUG) {System.out.println("Now doing L-tail: a=" + a + " b=" + b + " c=" + c + " d=" + d);}
        min = (a < d) ? a : d;
        if (DEBUG) {System.out.println("min = " + min);}
        for (i = 0; i < min; i++) {
            if (DEBUG) {System.out.print("doing round " + i);}
            double pTemp = getP(--a, ++b, ++c, --d);
            if (DEBUG) {System.out.println("  pTemp = " + pTemp);}
            if (pTemp <= baseP) {
                if (DEBUG) {System.out.print("\ttempP (" + pTemp + ") is less than baseP (" + baseP + ")");}
                p += pTemp;
            }
            if (DEBUG) {System.out.println(" a=" + a + " b=" + b + " c=" + c + " d=" + d);}
        }
        return p;
    }
//
//    public static void main(String[] args) {
//
//        if(args.length != 4){
//            System.out.println("Please enter 4 values");
//            System.exit(0);
//        }
//        int[] argInts = new int[args.length];
//
//        for(int i = 0; i < argInts.length; i++){
//            argInts[i] = Integer.parseInt(args[i]);
//        }
//        FisherExact fe = new FisherExact(100);
//
//        System.out.println("\n*****Original algorithm");
//        double cumulativeP = fe.getCumlativeP(argInts[0], argInts[1], argInts[2], argInts[3]);
//        System.out.println("cumulativeP = " + cumulativeP );
//
//        System.out.println("\n*****Modified algorithm");
//        double algorithmSelectedP = fe.getAlgorithmSelected(argInts[0], argInts[1], argInts[2], argInts[3]);
//        System.out.println("algorithmSelectedP = " + algorithmSelectedP);
//
//        System.out.println("\n*****Left Tailed");
//        double leftTailedP = fe.getLeftTailedP(argInts[0], argInts[1], argInts[2], argInts[3]);
//        System.out.println("leftTailedP = " + leftTailedP);
//
//        System.out.println("\n*****Right Tailed");
//        double rightTailedP = fe.getRightTailedP(argInts[0], argInts[1], argInts[2], argInts[3]);
//        System.out.println("rightTailedP = " + rightTailedP);
//
//        System.out.println("\n*****Two Tailed");
//        double twoTailedP = fe.getTwoTailedP(argInts[0], argInts[1], argInts[2], argInts[3]);
//        System.out.println("twoTailedP = " + twoTailedP);
//    }

    public static void main(String[] args) {

        int[][] argInts = new int[15][4];
        argInts[0] = new int[]{2, 3, 6, 4};
        argInts[1] = new int[]{2, 1, 3, 0};
        argInts[2] = new int[]{3, 0, 2, 1};
        argInts[3] = new int[]{1, 2, 0, 3};
        argInts[4] = new int[]{3, 1, 1, 3};
        argInts[5] = new int[]{1, 3, 3, 1};
        argInts[6] = new int[]{0, 1, 1, 0};
        argInts[7] = new int[]{1, 0, 0, 1};
        argInts[8] = new int[]{11, 0, 0, 6};
        argInts[9] = new int[]{10, 1, 1, 5};
        argInts[10] = new int[]{5, 6, 6, 0};
        argInts[11] = new int[]{9, 2, 2, 4};
        argInts[12] = new int[]{6, 5, 5, 1};
        argInts[13] = new int[]{8, 3, 3, 3};
        argInts[14] = new int[]{7, 4, 4, 2};

        FishersExact fe = new FishersExact(100);

        for (int i = 0; i < argInts.length; i++) {
            System.out.println("\na=" + argInts[i][0] + " b=" + argInts[i][1] + " c=" + argInts[i][2] + " d=" + argInts[i][3]);
            System.out.print("*****Original algorithm: ");
            double cumulativeP = fe.getCumulativeP(argInts[i][0], argInts[i][1], argInts[i][2], argInts[i][3]);
            System.out.println("\tcumulativeP = " + cumulativeP);

            System.out.print("*****Left Tailed: ");
            double leftTailedP = fe.getLeftTailedP(argInts[i][0], argInts[i][1], argInts[i][2], argInts[i][3]);
            System.out.println("\tleftTailedP = " + leftTailedP);

            System.out.print("*****Right Tailed: ");
            double rightTailedP = fe.getRightTailedP(argInts[i][0], argInts[i][1], argInts[i][2], argInts[i][3]);
            System.out.println("\trightTailedP = " + rightTailedP);

            System.out.print("*****Two Tailed: ");
            double twoTailedP = fe.getTwoTailedP(argInts[i][0], argInts[i][1], argInts[i][2], argInts[i][3]);
            System.out.println("\ttwoTailedP = " + twoTailedP);
        }
    }

}