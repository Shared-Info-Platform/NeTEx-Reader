package ch.bernmobil.netex.importer.parser;

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

public class ElementParser implements Parser {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElementParser.class);

	private final boolean isRoot;
	private final Set<String> attributes = new HashSet<>();
	private final Map<String, Parser> childParsers = new HashMap<>();

	public ElementParser() {
		this(false);
	}

	public ElementParser(boolean isRoot) {
		this.isRoot = isRoot;
	}

	public ElementParser withAttribute(String attributeName) {
		attributes.add(attributeName);
		return this;
	}

	public ElementParser withChild(String childName, Parser childParser) {
		childParsers.put(childName, childParser);
		return this;
	}

	@Override
	public Object parse(final XMLStreamReader2 reader) throws XMLStreamException {
		final int elementDepth = reader.getDepth();
		final Map<String, List<Object>> children = new HashMap<>();

		if (!isRoot) {
			for (int i = 0; i < reader.getAttributeCount(); ++i) {
				final String attributeName = reader.getAttributeLocalName(i);
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
					final String childName = reader.getLocalName();
					if (!childParsers.containsKey(childName)) {
						LOGGER.warn("unexpected child element " + childName);
					}
					final Parser parser = childParsers.get(childName);
					try {
						if (parser != null) {
							List<Object> list = children.get(childName);
							if (list == null) {
								list = new ArrayList<>();
								children.put(childName, list);
							}
//							LOGGER.info("parse {}", childName);
							list.add(parser.parse(reader));
						} else {
//							LOGGER.info("ignore {}", childName);
							SkipParser.INSTANCE.parse(reader);
						}
					} catch (RuntimeException e) {
						throw new IllegalStateException("exception while parsing child " + childName, e);
					}
					break;
				case XMLStreamConstants.CHARACTERS:
					final String text = reader.getText();
					if (!text.isBlank()) {
						throw new IllegalStateException("found non-blank characters in element: " + text);
					}
					break;
				case XMLStreamConstants.END_ELEMENT:
					final int currentDepth = reader.getDepth();
					if (currentDepth == elementDepth) {
						return children;
					} else {
						throw new IllegalStateException("found end tag at unexpected depth " + currentDepth);
					}
				case XMLStreamConstants.END_DOCUMENT:
					if (isRoot && elementDepth == 0) {
						return children;
					} else {
						throw new IllegalStateException("found end of stream at unexpected position");
					}
				case XMLStreamConstants.COMMENT:
					break;
				default:
					LOGGER.info("unexpected event type {}", eventType);
					break;
			}
		}

		throw new IllegalStateException("reached end of stream");
	}
}
