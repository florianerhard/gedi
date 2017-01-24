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
package jline.console.history;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;

import jline.internal.Log;

import static jline.internal.Preconditions.checkNotNull;

/**
 * {@link History} using a file for persistent backing.
 * <p/>
 * Implementers should install shutdown hook to call {@link FileHistory#flush}
 * to save history to disk.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.0
 */
public class FileHistory
    extends MemoryHistory
    implements PersistentHistory, Flushable
{
    private final File file;

    public FileHistory(final File file) throws IOException {
        this.file = checkNotNull(file).getAbsoluteFile();
        load(file);
    }

    public File getFile() {
        return file;
    }

    public void load(final File file) throws IOException {
        checkNotNull(file);
        if (file.exists()) {
            Log.trace("Loading history from: ", file);
            load(new FileReader(file));
        }
    }

    public void load(final InputStream input) throws IOException {
        checkNotNull(input);
        load(new InputStreamReader(input));
    }

    public void load(final Reader reader) throws IOException {
        checkNotNull(reader);
        BufferedReader input = new BufferedReader(reader);

        String item;
        while ((item = input.readLine()) != null) {
            internalAdd(item);
        }
    }

    public void flush() throws IOException {
        Log.trace("Flushing history");

        if (!file.exists()) {
            File dir = file.getParentFile();
            if (!dir.exists() && !dir.mkdirs()) {
                Log.warn("Failed to create directory: ", dir);
            }
            if (!file.createNewFile()) {
                Log.warn("Failed to create file: ", file);
            }
        }

        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(file)));
        try {
            for (Entry entry : this) {
                out.println(entry.value());
            }
        }
        finally {
            out.close();
        }
    }

    public void purge() throws IOException {
        Log.trace("Purging history");

        clear();

        if (!file.delete()) {
            Log.warn("Failed to delete history file: ", file);
        }
    }
}