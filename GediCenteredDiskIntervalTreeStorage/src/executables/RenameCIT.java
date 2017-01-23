package executables;

import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class RenameCIT {

	public static void main(String[] args) throws IOException {
		if (args.length<1) {
			usage();
			System.exit(1);
		}
		
		if (!new File(args[0]).exists()) {
			System.out.println("File "+args[0]+" does not exist!");
			usage();
			System.exit(1);
		}
		
		if (args.length<2) {
			System.out.println("No mapping given!");
			usage();
			System.exit(1);
		}
		
		HashMap<String,String> mapping = new HashMap<String, String>();
		for (int i=1; i<args.length; i++) {
			String[] p = StringUtils.split(args[i], "->");
			if (p.length!=2) {
				System.out.println("Wrong mapping format: "+args[i]);
				usage();
				System.exit(1);
			}
			mapping.put(p[0], p[1]);
		}
		
		
		new CenteredDiskIntervalTreeStorage(args[0]).renameChromosomes(mapping);
		
		
	}

	private static void usage() {
		System.out.println("RenameCIT <file> [from->to ...]");
	}
	
}
