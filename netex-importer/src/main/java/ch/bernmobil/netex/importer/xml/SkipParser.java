package ch.bernmobil.netex.importer.xml;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLStreamReader2;

/**
 * This parser lets the StAX parser skip the current XML element including all child-elements and contained text.
 * Note that with a StAX parser, an element cannot be just ignored because the parser parses every part of the XML
 * in sequence. Therefore to skip an element, all contained elements have to be read and only when the end-tag of
 * the original element is found, the element is effectively skipped.
 */
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
