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
package gedi.util.oml;

import gedi.util.StringUtils;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.jhp.Jhp;
import gedi.util.nashorn.JS;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;



public class OmlReader {
	static final Logger log = Logger.getLogger( OmlReader.class.getName() );
	
	private OmlNode root;
	
	private JS js;
	
	
	public OmlReader setJs(JS js) {
		this.js = js;
		return this;
	}
	
	public OmlNode parse(String xmlContent) {
		InputSource is = new InputSource(new StringReader(xmlContent));
		is.setPublicId("from source");
		return parse(is);
	}
	
	
	public OmlNode parse(File file) throws IOException {
		if (file.getPath().endsWith("jhp")) {
			String src = new LineOrientedFile(file.getPath()).readAllText();
			src = new Jhp(js).apply(src);
			new LineOrientedFile(file.getPath()+".processed").writeAllText(src);
			
			InputSource is = new InputSource(new StringReader(src));
			is.setPublicId(file.getPath());
			return parse(is);
		}
		
		InputSource is = new InputSource(new FileReader(file));
		is.setPublicId(file.getPath());
		return parse(is);
	}
	
	public OmlNode parse(InputSource input) {
		try {
			SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			OmlHandler handler = new OmlHandler();
			log.log(Level.INFO, "Reading oml "+input.getPublicId());
			saxParser.parse(input, handler);
			log.log(Level.INFO, "Done reading oml "+input.getPublicId());
			return root;
		} catch (Exception e) {
			throw new RuntimeException("Cannot parse input "+input.getSystemId(),e);
		}
		
	}
	
	
	private class OmlHandler extends DefaultHandler {
		
		private Stack<OmlNode> chain = new Stack<OmlNode>();
		
		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			
			OmlNode node = new OmlNode(qName);
			for (int i=0; i<attributes.getLength(); i++) {
				String name = attributes.getQName(i);
				String val = attributes.getValue(i);
				if (name.equalsIgnoreCase(OmlNode.IDATTRIBUTE))
					node.setId(val);
				else if (name.equalsIgnoreCase(OmlNode.CLASSATTRIBUTE))
					node.setClasses(StringUtils.split(val,' '));
				else
					node.addAttribute(name, val);
			}
			if (!chain.isEmpty()) {
				chain.peek().addChild(node);
				node.setParent(chain.peek());
			}
			chain.push(node);
		}
		
		@Override
		public void characters(char ch[], int start, int length)
	            throws SAXException {

	        String data = new String(ch, start, length);
	        if (data.length()>0)
	        	chain.peek().addText(data);
		}
		
		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			root = chain.pop();
		}
		
	}
	
}
