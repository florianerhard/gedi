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
/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package jline.console;

/**
 * The kill ring class keeps killed text in a fixed size ring. In this
 * class we also keep record of whether or not the last command was a
 * kill or a yank. Depending on this, the class may behave
 * different. For instance, two consecutive kill-word commands fill
 * the same slot such that the next yank will return the two
 * previously killed words instead that only the last one. Likewise
 * yank pop requires that the previous command was either a yank or a
 * yank-pop.
 */
public final class KillRing {

    /**
     * Default size is 60, like in emacs.
     */
    private static final int DEFAULT_SIZE = 60;

    private final String[] slots;
    private int head = 0;
    private boolean lastKill = false;
    private boolean lastYank = false;

    /**
     * Creates a new kill ring of the given size.
     */
    public KillRing(int size) {
        slots = new String[size];
    }

    /**
     * Creates a new kill ring of the default size. See {@link #DEFAULT_SIZE}.
     */
    public KillRing() {
        this(DEFAULT_SIZE);
    }

    /**
     * Resets the last-yank state.
     */
    public void resetLastYank() {
        lastYank = false;
    }

    /**
     * Resets the last-kill state.
     */
    public void resetLastKill() {
        lastKill = false;
    }

    /**
     * Returns {@code true} if the last command was a yank.
     */
    public boolean lastYank() {
        return lastYank;
    }

    /**
     * Adds the string to the kill-ring. Also sets lastYank to false
     * and lastKill to true.
     */
    public void add(String str) {
        lastYank = false;

        if (lastKill) {
            if (slots[head] != null) {
                slots[head] += str;
                return;
            }
        }

        lastKill = true;
        next();
        slots[head] = str;
    }

    /**
     * Adds the string to the kill-ring product of killing
     * backwards. If the previous command was a kill text one then
     * adds the text at the beginning of the previous kill to avoid
     * that two consecutive backwards kills followed by a yank leaves
     * things reversed.
     */
    public void addBackwards(String str) {
        lastYank = false;

        if (lastKill) {
            if (slots[head] != null) {
                slots[head] = str + slots[head];
                return;
            }
        }

        lastKill = true;
        next();
        slots[head] = str;
    }

    /**
     * Yanks a previously killed text. Returns {@code null} if the
     * ring is empty.
     */
    public String yank() {
        lastKill = false;
        lastYank = true;
        return slots[head];
    }

    /**
     * Moves the pointer to the current slot back and returns the text
     * in that position. If the previous command was not yank returns
     * null.
     */
    public String yankPop() {
        lastKill = false;
        if (lastYank) {
            prev();
            return slots[head];
        }
        return null;
    }

    /**
     * Moves the pointer to the current slot forward. If the end of
     * the slots is reached then points back to the beginning.
     */
    private void next() {
        if (head == 0 && slots[0] == null) {
            return;
        }
        head++;
        if (head == slots.length) {
            head = 0;
        }
    }

    /**
     * Moves the pointer to the current slot backwards. If the
     * beginning of the slots is reached then traverses the slot
     * backwards until one with not null content is found.
     */
    private void prev() {
        head--;
        if (head == -1) {
            int x = (slots.length - 1);
            for (; x >= 0; x--) {
                if (slots[x] != null) {
                    break;
                }
            }
            head = x;
        }
    }
}
