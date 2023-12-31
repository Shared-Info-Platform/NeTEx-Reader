package ch.bernmobil.netex.importer.parser;

import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLStreamReader2;

public class MultilingualStringParser implements Parser {

	private static final TextParser VALUE_PARSER = new TextParser();
	private static final String LANG_ATTRIBUTE = "lang";

	@Override
	public Object parse(XMLStreamReader2 reader) throws XMLStreamException {
		String language = null;
		for (int i = 0; i < reader.getAttributeCount(); ++i) {
			final String attributeName = reader.getAttributeLocalName(i);
			if (attributeName.equals(LANG_ATTRIBUTE)) {
				language = reader.getAttributeValue(i);
				break;
			}
		}

		final String text = (String) VALUE_PARSER.parse(reader);

		return new MultilingualString(language, text);
	}

	public static class MultilingualString {
		public final String language;
		public final String text;

		public MultilingualString(String language, String text) {
			this.language = language;
			this.text = text;
		}
	}
}
