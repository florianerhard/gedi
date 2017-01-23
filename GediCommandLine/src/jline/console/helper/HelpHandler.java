package jline.console.helper;

import java.io.IOException;

import jline.console.ConsoleReader;

public interface HelpHandler {

	boolean help(ConsoleReader reader, String help, int tabCount) throws IOException;
}
