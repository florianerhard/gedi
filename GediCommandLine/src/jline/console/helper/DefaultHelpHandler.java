package jline.console.helper;

import java.io.IOException;

import jline.console.ConsoleReader;

public class DefaultHelpHandler implements HelpHandler {

	
    public boolean help(ConsoleReader reader, String help, int tabCount) throws IOException {

        if (tabCount>1) {
        	reader.println();
        	reader.println(help);
	        reader.drawLine();
        }
        return true;
    }

}
