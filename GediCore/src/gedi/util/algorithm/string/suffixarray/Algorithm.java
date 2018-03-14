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
package gedi.util.algorithm.string.suffixarray;

/**
 * An enum with constants indicating algorithms and their variants. This is of little
 * practical use, but a handy factory method {@link #getInstance()} is also provided.
 */
public enum Algorithm
{
    /** Karkkainen-Sanders. */
    SKEW("Kärkkäinen-Sanders"),

    /** Karkkainen-Sanders, with decorators allowing arbitrary input. */
    SKEW_D("Kärkkäinen-Sanders (decorated for arbitrary input symbols)"),

    /** Yuta Mori's divsufsort algorithm. */
    DIVSUFSORT("Mori's algorithm"),

    /** Yuta Mori's implementation of SA-IS. */
    SAIS("SA-IS algorithm"),

    /** Klaus-Bernd Schürmann's bucket pointer refinement algorithm */
    BPR("Klaus-Bernd Schürmann's bpr algorithm"),

    /** Deep-Shallow algorithm by Manzini and Ferragina. */
    DEEP_SHALLOW("Manzini-Ferragina"),

    /** "Larrson-Sadakane qsufsort algorithm */
    QSUFSORT("Larrson-Sadakane qsufsort algorithm");

    /** Full name of the algorithm. */
    private final String name;

    /*
     * 
     */
    private Algorithm(String name)
    {
        this.name = name;
    }

    /**
     * @return Same as {@link #getInstance()}, but returns the algorithm instance
     *         decorated to work with any range or distribution of input symbols
     *         (respecting each algorithm's constraints).
     */
    public ISuffixArrayBuilder getDecoratedInstance()
    {
        switch (this)
        {
            case SKEW:
                return new DensePositiveDecorator(new ExtraTrailingCellsDecorator(
                    getInstance(), SuffixArrays.MAX_EXTRA_TRAILING_SPACE));

            default:
                return getInstance();
        }
    }

    /**
     * @return Create and return an algorithm instance.
     */
    public ISuffixArrayBuilder getInstance()
    {
        switch (this)
        {
            case SKEW:
                return new Skew();

            case DIVSUFSORT:
                return new DivSufSort();

            case SAIS:
                return new SAIS();

            case QSUFSORT:
                return new QSufSort();

            case BPR:
                return new BPR();

            case DEEP_SHALLOW:
                return new DeepShallow();

        }

        throw new RuntimeException("No algorithm for constant: " + this);
    }

    /**
     * If it is possbile, create memory conserving instance of an algorithm, <b>this
     * instance will overwrite input.</b>
     * <p>
     * If not, create default instance
     * 
     * @return Create and return low memory consuming instance.
     */
    public ISuffixArrayBuilder getMemoryConservingInstance()
    {
        switch (this)
        {
            case QSUFSORT:
                return new QSufSort(false);
            case BPR:
                return new BPR(false);
            case DEEP_SHALLOW:
                return new DeepShallow(false);
            default:
                return getInstance();
        }
    }

    /**
     * Return the full name of the algorithm.
     */
    public String getName()
    {
        return name;
    }
}
