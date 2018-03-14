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
 * A holder structure for a suffix array and longest common prefix array of
 * a given sequence. 
 */
public final class SuffixData
{
    private final int [] suffixArray;
    private final int [] lcp;

    SuffixData(int [] sa, int [] lcp)
    {
        this.suffixArray = sa;
        this.lcp = lcp;
    }

    public int [] getSuffixArray()
    {
        return suffixArray;
    }

    public int [] getLCP()
    {
        return lcp;
    }
}
