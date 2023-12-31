package ch.bernmobil.netex.importer.xml;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLStreamReader2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
					final String text = reader.getText();
					if (result == null) {
						result = text;
					} else {
						result += text;
					}
					break;
				case XMLStreamConstants.END_ELEMENT:
					final int currentDepth = reader.getDepth();
					if (currentDepth == elementDepth) {
						return result;
					} else {
						throw new IllegalStateException("found end tag at unexpected depth " + currentDepth);
					}
				case XMLStreamConstants.START_ELEMENT:
					throw new IllegalStateException("expected text but found child element " + reader.getLocalName());
				default:
					LOGGER.info("unexpected event type {}", eventType);
					break;
			}
		}

		throw new IllegalStateException("expected end element but reached end of stream");
	}
}
