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
 * A decorator around {@link ISuffixArrayBuilder} that:
 * <ul>
 * <li>provides extra space after the input for end-of-string markers</li>
 * <li>shifts the input to zero-based positions.</li>
 * </ul>
 */
public final class ExtraTrailingCellsDecorator implements ISuffixArrayBuilder
{
    private final ISuffixArrayBuilder delegate;
    private final int extraCells;

    /**
     * @see SuffixArrays#MAX_EXTRA_TRAILING_SPACE
     */
    public ExtraTrailingCellsDecorator(ISuffixArrayBuilder delegate, int extraCells)
    {
        this.delegate = delegate;
        this.extraCells = extraCells;
    }

    /*
     * 
     */
    @Override
    public int [] buildSuffixArray(int [] input, final int start, final int length)
    {
        if (start == 0 && start + length + extraCells < input.length)
        {
            return delegate.buildSuffixArray(input, start, length);
        }

        final int [] shifted = new int [input.length + extraCells];
        System.arraycopy(input, start, shifted, 0, length);

        final int [] SA = delegate.buildSuffixArray(shifted, 0, length);

        return SA;
    }
}
