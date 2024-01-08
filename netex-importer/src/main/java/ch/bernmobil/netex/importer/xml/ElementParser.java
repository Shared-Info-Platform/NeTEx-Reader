package ch.bernmobil.netex.importer.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLStreamReader2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads a element from XML using the StAX parser. Iterates over all attributes and child elements and either
 * parses them with a corresponding sub-parser or ignores them (if no corresponding child-parser is defined).
 *
 * Example:
 * <element>
 *   <child/>
 *   <otherChild/>
 *   <multiChild/>
 *   <multiChild/>
 *   <multiChild/>
 * </element>
 */
public class ElementParser implements Parser {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElementParser.class);

	private final boolean isRoot;
	private final Set<String> attributes = new HashSet<>();
	private final Map<String, Parser> childParsers = new HashMap<>();

	public ElementParser() {
		this(false);
	}

	/**
	 * If this parser is defined for the root element of the XML tree, then no attributes are parsed and no
	 * end-tag is expected (instead the end of the document is expected).
	 */
	public ElementParser(boolean isRoot) {
		this.isRoot = isRoot;
	}

	/**
	 * Tells the parser to parse the value of an attribute with the given name.
	 */
	public ElementParser withAttribute(String attributeName) {
		attributes.add(attributeName);
		return this;
	}

	/**
	 * Tells the parser to use a child-parser to parse the child element with the given name.
	 */
	public ElementParser withChild(String childName, Parser childParser) {
		childParsers.put(childName, childParser);
		return this;
	}

	@Override
	public Object parse(final XMLStreamReader2 reader) throws XMLStreamException {
		final int elementDepth = reader.getDepth();
		final Map<String, List<Object>> children = new HashMap<>();

		// read all expected attributes (unless it's the root node)
		if (!isRoot) {
			for (int i = 0; i < reader.getAttributeCount(); ++i) {
				final String attributeName = reader.getAttributeLocalName(i);
				// only read expected attributes; ignore all others
				if (attributes.contains(attributeName)) {
					final String attributeValue = reader.getAttributeValue(i);
					children.put(attributeName, Collections.singletonList(attributeValue));
				}
			}
		}

		while (reader.hasNext()) {
			final int eventType = reader.next();
			switch (eventType) {
				case XMLStreamConstants.START_ELEMENT:
					// found the start of a new child element
					final String childName = reader.getLocalName();
					if (!childParsers.containsKey(childName)) {
						// child is unknown, log a warning
						LOGGER.warn("unexpected child element " + childName);
					}
					// look for a parser for this child
					final Parser parser = childParsers.get(childName);
					try {
						if (parser != null) {
							// parser defined: parse the child and put the result in a list
							// (because there could be multiple children with the same name)
							List<Object> list = children.get(childName);
							if (list == null) {
								list = new ArrayList<>();
								children.put(childName, list);
							}
							list.add(parser.parse(reader));
						} else {
							// parser not defined (or explicitly defined null): skip the child
							SkipParser.INSTANCE.parse(reader);
						}
					} catch (RuntimeException e) {
						throw new IllegalStateException("exception while parsing child " + childName, e);
					}
					break;
				case XMLStreamConstants.CHARACTERS:
					// found characters: this is not expected here unless it's blanks (used for formatting the XML)
					final String text = reader.getText();
					if (!text.isBlank()) {
						throw new IllegalStateException("found non-blank characters in element: " + text);
					}
					break;
				case XMLStreamConstants.END_ELEMENT:
					// found end of element: element is complete (check if it is actually the right end-tag)
					final int currentDepth = reader.getDepth();
					if (currentDepth == elementDepth) {
						return children;
					} else {
						throw new IllegalStateException("found end tag at unexpected depth " + currentDepth);
					}
				case XMLStreamConstants.END_DOCUMENT:
					// found end of document: element is complete (if it's the root node; otherwise it's an exception)
					if (isRoot && elementDepth == 0) {
						return children;
					} else {
						throw new IllegalStateException("found end of stream at unexpected position");
					}
				case XMLStreamConstants.COMMENT:
					// found a comment: ignore it
					break;
				default:
					// found unexpected content
					LOGGER.info("unexpected event type {}", eventType);
					break;
			}
		}

		throw new IllegalStateException("reached end of stream");
	}
}
