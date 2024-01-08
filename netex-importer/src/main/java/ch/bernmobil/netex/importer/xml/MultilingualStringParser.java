package ch.bernmobil.netex.importer.xml;

import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLStreamReader2;

/**
 * Same like TextParser but it also reads an optional attribute "lang" from the containing element. Returns the
 * text and the (optional) language as a MultilingualString.
 *
 * Example: <element lang="de">TEXT</element>
 */
public class MultilingualStringParser implements Parser {

	private static final TextParser TEXT_PARSER = new TextParser();
	private static final String LANG_ATTRIBUTE = "lang";

	@Override
	public Object parse(XMLStreamReader2 reader) throws XMLStreamException {
		// check if there is a "lang" attribute and read its content
		String language = null;
		for (int i = 0; i < reader.getAttributeCount(); ++i) {
			final String attributeName = reader.getAttributeLocalName(i);
			if (attributeName.equals(LANG_ATTRIBUTE)) {
				language = reader.getAttributeValue(i);
				break;
			}
		}

		// parse text with the regular text parser.
		final String text = (String) TEXT_PARSER.parse(reader);

		// return both in a helper object
		return new MultilingualString(language, text);
	}

	public static class MultilingualString {
		public final String language;
		public final String text;

		public MultilingualString(String language, String text) {
			this.language = language;
			this.text = text;
		}

		public String getLanguage() {
			return language;
		}

		public String getText() {
			return text;
		}
	}
}
