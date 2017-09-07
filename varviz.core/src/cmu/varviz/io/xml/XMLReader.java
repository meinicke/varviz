package cmu.varviz.io.xml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.function.Function;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import cmu.varviz.trace.Method;
import cmu.varviz.trace.MethodElement;
import cmu.varviz.trace.NodeColor;
import cmu.varviz.trace.Shape;
import cmu.varviz.trace.Statement;
import cmu.varviz.trace.Trace;
import cmu.varviz.utils.ContextParser;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.bdd.BDDFeatureExprFactory;

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
					NamedNodeMap attributes = child.getAttributes();
					String name = getName(attributes);
					Method<String> mainMethod = new Method<>(name, BDDFeatureExprFactory.True());
					setFile(attributes, mainMethod);
					setLine(attributes,mainMethod);
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
			NamedNodeMap attributes = child.getAttributes();
			if (child.getNodeName().equals(METHOD)) {
				String name = getName(attributes);
				FeatureExpr ctx = getcontext(attributes);
				Method<String> method = new Method<>(name, parentMethod, ctx);

				setFile(attributes, method);
				setLine(attributes, method);

				parseChildren(child, method);
			} else if (child.getNodeName().equals(STATEMENT)) {
				String name = getName(attributes);
				FeatureExpr ctx = getcontext(attributes);
				Statement<String> statement = new Statement<String>(name, parentMethod, ctx);

				setColor(attributes, statement);
				setShape(attributes, statement);
				setLine(attributes, statement);
				setValues(attributes, statement);
			}
		}
	}

	private void setValues(NamedNodeMap attributes, Statement<String> statement) {
		String oldValue = getAttribute(attributes, "old", v -> v);
		String value = getAttribute(attributes, "new", v -> v);
		
		if (oldValue != null) {
			statement.setOldValue(ContextParser.StringToConditional(oldValue));
		}
		if (value != null) {
			statement.setValue(ContextParser.StringToConditional(value));
		}
	}

	private String getName(NamedNodeMap attributes) {
		return getAttribute(attributes, NAME, v -> v);
	}

	private FeatureExpr getcontext(NamedNodeMap attributes) {
		return getAttribute(attributes, CTX, v -> v != null ? ContextParser.getContext(v) : BDDFeatureExprFactory.True());
	}

	private void setFile(NamedNodeMap attributes, Method<?> method) {
		method.setFile(getAttribute(attributes, FILE, v -> v));
	}

	private void setLine(NamedNodeMap attributes, MethodElement<?> s) {
		s.setLineNumber(getAttribute(attributes, LINE, v -> v != null ? Integer.parseInt(v) : DEFAULT_LINE_NUMBER));
	}

	private void setShape(NamedNodeMap attributes, Statement<String> s) {
		s.setShape(getAttribute(attributes, SHAPE, v -> v != null ? Shape.valueOf(v) : null));
	}

	private void setColor(NamedNodeMap attributes, Statement<String> s) {
		s.setColor(getAttribute(attributes, COLOR, v -> v != null ? NodeColor.valueOf(v) : null));
	}

	private static <T> T getAttribute(NamedNodeMap attributes, String attributeName, Function<String, T> converter) {
		Node node = attributes.getNamedItem(attributeName);
		String value = node == null ? null : node.getNodeValue();
		return converter.apply(value);
	}

	private Document parse(String input) throws IOException, SAXException, ParserConfigurationException {
		return parse(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
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
