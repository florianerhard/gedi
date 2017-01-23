package gedi.oml;

import java.io.File;
import java.io.IOException;

public class Oml {

	public static <T> T create(String file) throws IOException {
		return new OmlNodeExecutor().execute(new OmlReader().parse(new File(file)));
	}
	
}
