package ch.bernmobil.netex.importer.xml;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLStreamReader2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads a text from an XML element using the StAX parser. A text is just a bunch of characters in an XML element,
 * for example <element>TEXT</element>. Therefore this parsers appends all characters found until the current
 * element ends.
 */
public class TextParser implements Parser {

	private static final Logger LOGGER = LoggerFactory.getLogger(TextParser.class);

	@Override
	public Object parse(final XMLStreamReader2 reader) throws XMLStreamException {
		final int elementDepth = reader.getDepth();
		String result = null;

		while (reader.hasNext()) {
			final int eventType = reader.next();
			switch (eventType) {
				case XMLStreamConstants.CHARACTERS:
					// found characters: add to text
					final String text = reader.getText();
					if (result == null) {
						result = text;
					} else {
						result += text;
					}
					break;
				case XMLStreamConstants.END_ELEMENT:
					// found end of element: text is complete (check if it is actually the right end-tag)
					final int currentDepth = reader.getDepth();
					if (currentDepth == elementDepth) {
						return result;
					} else {
						throw new IllegalStateException("found end tag at unexpected depth " + currentDepth);
					}
				case XMLStreamConstants.START_ELEMENT:
					// found a child element: this is not expected here
					throw new IllegalStateException("expected text but found child element " + reader.getLocalName());
				default:
					// found unexpected content
					LOGGER.info("unexpected event type {}", eventType);
					break;
			}
		}

		throw new IllegalStateException("expected end element but reached end of stream");
	}
}
