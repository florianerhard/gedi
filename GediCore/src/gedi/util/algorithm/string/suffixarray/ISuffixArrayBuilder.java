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
 * An algorithm that can produce a <i>suffix array</i> for a sequence of integer symbols.
 * 
 * @see #buildSuffixArray(int[], int, int)
 */
public interface ISuffixArrayBuilder
{
    /**
     * Computes suffix array for sequence of symbols (integers). The processed sequence is
     * a subsequence of <code>input</code> determined by <code>start</code> and
     * <code>length</code> parameters.
     * <p>
     * Concrete implementations may have additional requirements and constraints
     * concerning the input. For example, it is quite common that extra cells are required
     * after <code>start + length</code> to store special marker symbols. Also, some
     * algorithms may require non-negative symbols in the input. For such constrained
     * algorithms, use various decorators and adapters available in this package.
     * 
     * @param input A sequence of input symbols, int-coded.
     * @param start The starting index (inclusive) in <code>input</code>.
     * @param length Number of symbols to process.
     * @return An array of indices such that the suffix of <code>input</code> at index
     *         <code>result[i]</code> is lexicographically larger or equal to any other
     *         suffix that precede it. Note that the output array may be larger than
     *         <code>input.length</code>, in which case only the first
     *         <code>input.length</code> elements are of relevance.
     *         <p>
     *         The returned array contains suffix indexes starting from 0 (so
     *         <code>start</code> needs to be added manually to access a given suffix in
     *         <code>input</code>).
     */
    int [] buildSuffixArray(int [] input, int start, int length);
}