package cmu.varviz.io.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cmu.conditional.Conditional;
import cmu.varviz.trace.Method;
import cmu.varviz.trace.MethodElement;
import cmu.varviz.trace.NodeColor;
import cmu.varviz.trace.Shape;
import cmu.varviz.trace.Statement;
import cmu.varviz.trace.Trace;
import cmu.varviz.utils.ContextParser;
import de.fosd.typechef.featureexpr.FeatureExpr;

public class XMLWriter implements XMLvarviz {

	private final Trace trace;

	public XMLWriter(Trace trace) {
		this.trace = trace;
	}

	public String write() throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setIgnoringComments(true);
		dbf.setIgnoringElementContentWhitespace(false);
		dbf.setCoalescing(true);
		dbf.setExpandEntityReferences(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.newDocument();
		// Create the Xml Representation
		return createXMLDocument(doc);
	}

	private String createXMLDocument(Document doc) throws TransformerException {
		Element root = doc.createElement(ROOT);

		writeTraceElements(trace.getMain(), doc, root);

		doc.appendChild(root);
		// Transform the Xml Representation into a String
		Transformer transfo = TransformerFactory.newInstance().newTransformer();
		transfo.setOutputProperty(OutputKeys.METHOD, "xml");
		transfo.setOutputProperty(OutputKeys.INDENT, "yes");
		StreamResult result = new StreamResult(new StringWriter());
		DOMSource source = new DOMSource(doc);
		transfo.transform(source, result);
		return prettyPrint(result.getWriter().toString());
	}

	private void writeTraceElements(MethodElement<?> methodElement, Document doc, Element parent) {

		String elementType = methodElement instanceof Statement ? STATEMENT : METHOD;
		Element element = doc.createElement(elementType);
		element.setAttribute(NAME, methodElement.toString());

		int lineNumber = methodElement.getLineNumber();
		if (lineNumber != DEFAULT_LINE_NUMBER) {
			element.setAttribute(LINE, Integer.toString(lineNumber));
		}

		FeatureExpr ctx = methodElement.getCTX();
		if (!Conditional.isTautology(ctx)) {
			element.setAttribute(CTX, Conditional.getCTXString(ctx));
		}

		setColor(methodElement, element);
		if (methodElement instanceof Statement) {
			setShape((Statement<?>) methodElement, element);
			setValues((Statement<?>) methodElement, element);
		}

		if (methodElement instanceof Method) {
			Method<?> method = (Method<?>) methodElement;
			element.setAttribute(FILE, method.getFile());
			for (MethodElement<?> child : method.getChildren()) {
				writeTraceElements(child, doc, element);
			}
		}

		parent.appendChild(element);
	}

	private void setValues(Statement<?> statement, Element element) {
		Conditional<?> oldValue = statement.getOldValue();
		Conditional<?> value = statement.getValue();
		if (oldValue != null) {
			element.setAttribute("old", ContextParser.ConditionalToString(oldValue));
		}
		if (value != null) {
			element.setAttribute("new", ContextParser.ConditionalToString(value));
		}
	}
	
	@SuppressWarnings("unused")
	private String createString(Conditional<?> value) {
		if (value.isOne()) {
			return value.getValue().toString();
		} else {
			final StringBuilder text = new StringBuilder();
			for (Entry<?, FeatureExpr> entry : value.toMap().entrySet()) {
				text.append(entry.getKey());
				text.append(" : ");
				text.append(Conditional.getCTXString(entry.getValue()));
				text.append('\n');
			}
			return text.subSequence(0, text.length() - 1).toString();
		}
	}

	private void setColor(MethodElement<?> methodElement, Element element) {
		NodeColor color = methodElement.getColor();
		if (color != null) {
			element.setAttribute(COLOR, color.name());
		}
	}

	private void setShape(Statement<?> methodElement, Element element) {
		Shape shape = methodElement.getShape();
		if (shape != null) {
			element.setAttribute(SHAPE, shape.name());
		}
	}

	public void writeToFile(File file) throws ParserConfigurationException, TransformerException {
		try (FileOutputStream output = new FileOutputStream(file)) {
			if (!file.exists()) {
				file.createNewFile();
			}
			output.write(write().getBytes(Charset.availableCharsets().get("UTF-8")));
			output.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Inserts indentations into the text
	 * 
	 * @param text
	 * @return
	 */
	private String prettyPrint(String text) {
		StringBuilder result = new StringBuilder();
		String line;
		int indentLevel = 0;
		BufferedReader reader = new BufferedReader(new StringReader(text));
		try {
			line = reader.readLine();
			while (line != null) {
				if (line.startsWith("</")) {
					indentLevel--;
					for (int i = 0; i < indentLevel; i++) {
						result.append("\t");
					}
				}

				else if (line.startsWith("<")) {
					for (int i = 0; i < indentLevel; i++) {
						result.append("\t");
					}
					if (!line.contains("</")) {
						indentLevel++;
					}
				} else {
					for (int i = 0; i < indentLevel; i++) {
						result.append("\t");
					}
				}
				result.append(line + "\n");
				if (line.contains("/>")) {
					indentLevel--;
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result.toString();
	}

}
