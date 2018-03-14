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
 * A decorator around {@link ISuffixArrayBuilder} that accepts any input symbols and maps
 * it to non-negative, compact (dense) alphabet. Relative symbols order is preserved (changes are
 * limited to a constant shift and compaction of symbols). The input is remapped in-place,
 * but additional space is required for the mapping.
 */
public final class DensePositiveDecorator implements ISuffixArrayBuilder
{
    private final ISuffixArrayBuilder delegate;

    /*
     * 
     */
    public DensePositiveDecorator(ISuffixArrayBuilder delegate)
    {
        this.delegate = delegate;
    }

    /*
     * 
     */
    @Override
    public int [] buildSuffixArray(int [] input, final int start, final int length)
    {
        final MinMax minmax = Tools.minmax(input, start, length);

        final ISymbolMapper mapper;
        if (minmax.range() > 0x10000)
        {
            throw new RuntimeException("Large symbol space not implemented yet.");
        }
        else
        {
            mapper = new DensePositiveMapper(input, start, length);
        }

        mapper.map(input, start, length);
        try
        {
            return delegate.buildSuffixArray(input, start, length);
        }
        finally
        {
            mapper.undo(input, start, length);
        }
    }
}
