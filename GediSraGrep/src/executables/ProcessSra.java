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

package executables;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarInputStream;

import gedi.app.classpath.ClassPathCache;
import gedi.sra.processing.SraProcessor;
import gedi.sra.processing.SraTopLevel;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.program.CommandLineHandler;
import gedi.util.program.GediParameter;
import gedi.util.program.GediParameterSet;
import gedi.util.program.GediProgram;
import gedi.util.program.GediProgramContext;
import gedi.util.program.parametertypes.FileParameterType;
import gedi.util.program.parametertypes.StringParameterType;

public class ProcessSra {


	public static void main(String[] args) throws IOException {
		
		ProcessSraParameterSet params = new ProcessSraParameterSet();
		GediProgram pipeline = GediProgram.create("ProcessSra",
				new ProcessSraProgram(params)
				);
		GediProgram.run(pipeline, null, new CommandLineHandler("ProcessSra","ProcessSra processes the SRA metadata using plugins.",args));

	}

	public static class ProcessSraProgram extends GediProgram {



		public ProcessSraProgram(ProcessSraParameterSet params) {
			addInput(params.sra);
			addInput(params.json);
			addInput(params.report);
			addOutput(params.oreport);
		}

		@Override
		public String execute(GediProgramContext context) throws Exception {
			String sra = getParameter(0);
			ArrayList<String> json = getParameters(1);

			SraProcessor[] processors = EI.wrap(json).map(f->{
				try {
					Class<SraProcessor> cls = ClassPathCache.getInstance().getClass(FileUtils.getNameWithoutExtension(f));
					SraProcessor re = cls.newInstance();
					return DynamicObject.parseJson(new File(f)).applyTo(re);
				} catch (Exception e) { throw new RuntimeException("Cannot create processor: "+f,e);}

			}).toArray(SraProcessor.class);

			TarInputStream tis = new TarInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(sra))));

			EI.wrap(()->{
				try {
					return tis.getNextEntry();
				} catch (IOException e) {
					throw new RuntimeException("Could not read tar file!",e);
				}
			})
			.filter(te->!te.getName().substring(te.getName().lastIndexOf('/')+1).startsWith("SRA_"))
			.map(te->new BufferedTarEntry(tis, te))
			.block(te->te.isDir)
			.map(SraFolder::new)
			.filter(sf->!sf.isEmpty())
			.progress(sf->sf.getName()+" @"+StringUtils.getShortHumanReadableMemory(tis.getCurrentOffset()))
			//			.parallelized(ei->ei
			.map(sf->{
				StringBuilder sb = new StringBuilder();
				sb.append(sf.getName());
				for (SraProcessor p : processors)
					sb.append("\t").append(p.process(sf.getName(), sf::parse, sf::get));
				return sb.toString();
			})
			//			)
			.print("SRA\t"+EI.wrap(json).map(f->FileUtils.getNameWithoutExtension(f)).concat("\t"), getOutputFile(0).getPath());

			tis.close();


			return null;
		}
	}

	private static class BufferedTarEntry {
		String name;
		boolean isDir;
		byte[] buffer;
		public BufferedTarEntry(TarInputStream tis, TarEntry e) {
			this.name = e.getName();
			if (name.startsWith("NCBI_SRA"))
				name = name.substring(name.indexOf('/')+1);

			this.isDir = e.isDirectory();
			if (!isDir) {
				buffer = new byte[(int) e.getSize()];
				try {
					int in = tis.read(buffer);
					if (in!=buffer.length)
						throw new RuntimeException("Size does not match for "+e.getName()+" in: "+in+" expected: "+e.getSize()+" buff: "+new String(buffer));
				} catch (IOException e1) {
					throw new RuntimeException("Could not read tar!",e1);
				}
			}
		}
	}

	private static class SraFolder {
		HashMap<String,byte[]> bufferMap = new HashMap<>();
		String name;
		public SraFolder(ArrayList<BufferedTarEntry> entries) {

			for (int i=1; i<entries.size(); i++) {
				BufferedTarEntry e = entries.get(i);
				name = extractAndCheckName(e);
				String type = e.name.substring(name.length()*2+2,e.name.length()-4);
				bufferMap.put(type, e.buffer);
			}
		}

		public boolean isEmpty() {
			return bufferMap.isEmpty();
		}

		private String extractAndCheckName(BufferedTarEntry e) {
			String n = e.name;

			int p = n.indexOf('/');
			if (p==-1)
				throw new RuntimeException("Unexpected name: "+n);
			String re = n.substring(0,p);
			if (name!=null && !re.equals(name))
				throw new RuntimeException("Mixed names: "+n+", "+name);
			if (!n.startsWith(re+"/"+re+".") || !n.endsWith(".xml")) 
				throw new RuntimeException("Unexpected name: "+n);
			return re;
		}

		public String get(SraTopLevel type) {
			String name = type.name().toLowerCase();
			byte[] ee = bufferMap.get(name);
			if (ee==null) return null;
			return new String(ee);
		}
		public <T> T parse(SraTopLevel type) {
			String name = type.name().toLowerCase();
			byte[] re = bufferMap.get(name);
			if (re==null) return null;
			try {
				return  ((JAXBElement<T>) JAXBContext.newInstance("gedi.sra.schema."+name).createUnmarshaller().unmarshal(new ByteArrayInputStream(re))).getValue();
			} catch (JAXBException e) {
				throw new RuntimeException("Could not parse "+name+" Source:\n"+new String(re),e);
			}
		}

		public String getName() {
			return name;
		}


	}

	public static class ProcessSraParameterSet extends GediParameterSet {
		public GediParameter<String> json = new GediParameter<String>(this,"i", "Input json file", true, new StringParameterType());
		public GediParameter<String> sra = new GediParameter<String>(this,"sra", "SRA metadata file (from ftp://ftp-trace.ncbi.nlm.nih.gov/sra/reports/Metadata/)", false, new StringParameterType());
		public GediParameter<String> report = new GediParameter<String>(this,"report", "Output file", false, new StringParameterType());

		public GediParameter<File> oreport = new GediParameter<File>(this,"${report}", "Report file", false, new FileParameterType());

	}
}
