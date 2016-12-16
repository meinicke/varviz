package cmu.varviz.io.xml;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.Test;
import org.xml.sax.SAXException;

import cmu.varviz.trace.Trace;
import cmu.vaviz.testutils.TraceFactory;

public class XMLReaderTest {

	@Test
	public void test() throws ParserConfigurationException, TransformerException, IOException, SAXException {
		Trace trace = TraceFactory.createTrace();

		XMLWriter writer = new XMLWriter(trace);
		String content = writer.write();

		XMLReader reader = new XMLReader();
		Trace traceRead = reader.readXML(content);

		writer = new XMLWriter(traceRead);
		String newContent = writer.write();

		System.out.println(content);

		System.out.println(newContent);

		assertEquals(content, newContent);
	}

}
