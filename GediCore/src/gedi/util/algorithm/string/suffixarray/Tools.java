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
 * Utility methods used throughout entire project.
 */
final class Tools
{
    private Tools()
    {
        // No instances.
    }

    /**
     * Check if all symbols in the given range are greater than 0, return
     * <code>true</code> if so, <code>false</code> otherwise.
     */
    static final boolean allPositive(int [] input, int start, int length)
    {
        for (int i = length - 1, index = start; i >= 0; i--, index++)
        {
            if (input[index] <= 0)
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Determine the maximum value in a slice of an array.
     */
    static final int max(int [] input, int start, int length)
    {
        assert length >= 1;

        int max = input[start];
        for (int i = length - 2, index = start + 1; i >= 0; i--, index++)
        {
            final int v = input[index];
            if (v > max)
            {
                max = v;
            }
        }

        return max;
    }

    /**
     * Determine the minimum value in a slice of an array.
     */
    static final int min(int [] input, int start, int length)
    {
        assert length >= 1;

        int min = input[start];
        for (int i = length - 2, index = start + 1; i >= 0; i--, index++)
        {
            final int v = input[index];
            if (v < min)
            {
                min = v;
            }
        }

        return min;
    }

    /**
     * Calculate minimum and maximum value for a slice of an array.
     */
    static MinMax minmax(int [] input, final int start, final int length)
    {
        int max = input[start];
        int min = max;
        for (int i = length - 2, index = start + 1; i >= 0; i--, index++)
        {
            final int v = input[index];
            if (v > max)
            {
                max = v;
            }
            if (v < min)
            {
                min = v;
            }
        }

        return new MinMax(min, max);
    }

    /**
     * Throw {@link AssertionError} if a condition is <code>false</code>. This should
     * be called when the assertion must be always verified (as in the case of verifying
     * the algorithm's preconditions). For other, internal assertions, one should use 
     * <code>assert</code> keyword so that such assertions can be disabled at run-time (for
     * performance reasons).
     */
    static final void assertAlways(boolean condition, String msg)
    {
        if (!condition)
        {
            throw new AssertionError(msg);
        }
    }
}
