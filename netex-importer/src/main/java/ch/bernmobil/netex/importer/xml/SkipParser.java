package ch.bernmobil.netex.importer.xml;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLStreamReader2;

public class SkipParser implements Parser {

	public static final SkipParser INSTANCE = new SkipParser();

	@Override
	public Object parse(final XMLStreamReader2 reader) throws XMLStreamException {
		final int elementDepth = reader.getDepth();

		while (reader.hasNext()) {
			final int eventType = reader.next();
			switch (eventType) {
				case XMLStreamConstants.END_ELEMENT:
					final int currentDepth = reader.getDepth();
					if (currentDepth > elementDepth) {
						break;
					} else if (currentDepth == elementDepth) {
						return null;
					} else {
						throw new IllegalStateException("expected end tag at level " + elementDepth + " but missed it");
					}
				default:
					break;
			}
		}

		throw new IllegalStateException("reached end of stream");
	}
}
