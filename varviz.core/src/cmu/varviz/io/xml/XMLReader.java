package cmu.varviz.io.xml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import cmu.varviz.trace.Method;
import cmu.varviz.trace.Statement;
import cmu.varviz.trace.Trace;
import cmu.varviz.utils.ContextParser;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.FeatureExprFactory;

public class XMLReader implements XMLvarviz {

	public XMLReader() {

	}

	public Trace readFromFile(File file) throws ParserConfigurationException, TransformerException, IOException, SAXException {
		String fileName = file.getPath();
		InputStream inputStream = null;
		inputStream = new FileInputStream(fileName);
		return readXML(inputStream);
	}

	public Trace readXML(InputStream xmlContent) throws ParserConfigurationException, TransformerException, IOException, SAXException {
		Document doc = parse(xmlContent);
		return getTrace(doc);
	}

	public Trace readXML(String xmlContent) throws ParserConfigurationException, TransformerException, IOException, SAXException {
		Document doc = parse(xmlContent);
		return getTrace(doc);
	}

	private Trace getTrace(Document doc) throws NumberFormatException {
		Trace coverage = new Trace();
		for (Element rootNode : getElements(doc.getElementsByTagName(ROOT))) {
			NodeList children = rootNode.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeName().equals(METHOD)) {
					Node name = child.getAttributes().getNamedItem(NAME);
					Method<String> mainMethod = new Method<>(name.getNodeValue(), FeatureExprFactory.True());
					String file = child.getAttributes().getNamedItem(FILE).getNodeValue();
					mainMethod.setFile(file);
					String line = child.getAttributes().getNamedItem(LINE).getNodeValue();
					mainMethod.setLineNumber(Integer.parseInt(line));
					coverage.setMain(mainMethod);
					parseChildren(child, mainMethod);
				}
			}
		}
		return coverage;
	}

	private void parseChildren(Node node, Method<String> parentMethod) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeName().equals(METHOD)) {
				String name = child.getAttributes().getNamedItem(NAME).getNodeValue();
				String line = child.getAttributes().getNamedItem(LINE).getNodeValue();
				String ctxString = child.getAttributes().getNamedItem(CTX).getNodeValue();
				FeatureExpr ctx = ContextParser.getContext(ctxString);
				Method<String> method = new Method<>(name, parentMethod, ctx);
				String file = child.getAttributes().getNamedItem(FILE).getNodeValue();
				method.setFile(file);
				method.setLineNumber(Integer.parseInt(line));
				parseChildren(child, method);
			} else if (child.getNodeName().equals(STATEMENT)) {
				String name = child.getAttributes().getNamedItem(NAME).getNodeValue();
				String line = child.getAttributes().getNamedItem(LINE).getNodeValue();
				String ctxString = child.getAttributes().getNamedItem(CTX).getNodeValue();
				FeatureExpr ctx = ContextParser.getContext(ctxString);
				Statement<String> s = new Statement<String>(name, parentMethod, ctx);
				s.setLineNumber(Integer.parseInt(line));
			}
		}
	}

	private Document parse(String input) throws IOException, SAXException, ParserConfigurationException {
		InputStream stream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
		return parse(stream);
	}

	private Document parse(InputStream stream) throws IOException, SAXException, ParserConfigurationException {
		Document doc = PositionalXMLReader.readXML(stream);
		doc.getDocumentElement().normalize();
		return doc;
	}

	/**
	 * @param nodeList
	 * @return The child nodes from type Element of the given NodeList.
	 */
	private ArrayList<Element> getElements(NodeList nodeList) {
		ArrayList<Element> elements = new ArrayList<Element>(nodeList.getLength());
		for (int temp = 0; temp < nodeList.getLength(); temp++) {
			org.w3c.dom.Node nNode = nodeList.item(temp);
			if (nNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				elements.add(eElement);
			}
		}
		return elements;
	}

}
