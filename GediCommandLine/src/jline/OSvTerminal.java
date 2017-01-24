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
package jline;

import jline.internal.Log;
import jline.internal.TerminalLineSettings;

/**
 * Terminal that is used for OSv. This is seperate to unix terminal
 * implementation because exec cannot be used as currently used by UnixTerminal.
 *
 * This implimentation is derrived from the implementation at
 * https://github.com/cloudius-systems/mgmt/blob/master/crash/src/main/java/com/cloudius/cli/OSvTerminal.java
 * authored by Or Cohen.
 *
 * @author <a href-"mailto:orc@fewbytes.com">Or Cohen</a>
 * @author <a href="mailto:arun.neelicattu@gmail.com">Arun Neelicattu</a>
 * @since 2.13
 */
public class OSvTerminal
    extends TerminalSupport
{

    public Class<?> sttyClass = null;
    public Object stty = null;

    public OSvTerminal() {
        super(true);

        setAnsiSupported(true);

        try {
            if (stty == null) {
                sttyClass = Class.forName("com.cloudius.util.Stty");
                stty = sttyClass.newInstance();
            }
        } catch (Exception e) {
            Log.warn("Failed to load com.cloudius.util.Stty", e);
        }
    }

    @Override
    public TerminalLineSettings getSettings() {
    	return null;
    }
    
    @Override
    public void init() throws Exception {
        super.init();

        if (stty != null) {
            sttyClass.getMethod("jlineMode").invoke(stty);
        }
    }

    @Override
    public void restore() throws Exception {
        if (stty != null) {
            sttyClass.getMethod("reset").invoke(stty);
        }
        super.restore();

        // Newline in end of restore like in jline.UnixTerminal
        System.out.println();
    }

}
