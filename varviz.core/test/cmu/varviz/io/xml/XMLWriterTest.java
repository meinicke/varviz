package cmu.varviz.io.xml;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.Test;

import cmu.varviz.trace.Trace;
import cmu.vaviz.testutils.TraceFactory;

public class XMLWriterTest {
	
	@Test
	public void test() throws ParserConfigurationException, TransformerException {
		Trace trace = TraceFactory.createTrace();
		
		XMLWriter writer = new XMLWriter(trace);
		writer.write();
	}

}
