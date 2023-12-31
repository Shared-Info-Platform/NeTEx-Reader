package ch.bernmobil.netex.importer;

import java.io.File;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;

import ch.bernmobil.netex.importer.parser.Parser;

public class Importer {

	public static void main(String[] args) throws XMLStreamException {
		final File directory = new File("C:\\projects\\bernmobil\\files\\netex\\prod_netex_tt_1.10_che_ski_2024_oev-schweiz__1_1_202312200612");
		final File file = new File(directory, "PROD_NETEX_TT_1.10_CHE_SKI_2024_OEV-SCHWEIZ_RESOURCE_1_1_202312200612.xml");
//		final File file = new File(directory, "PROD_NETEX_TT_1.10_CHE_SKI_2024_OEV-SCHWEIZ_SITE_1_1_202312200612.xml");
//		final File file = new File(directory, "PROD_NETEX_TT_1.10_CHE_SKI_2024_OEV-SCHWEIZ_SERVICE_1_1_202312200612.xml");
//		final File file = new File(directory, "PROD_NETEX_TT_1.10_CHE_SKI_2024_OEV-SCHWEIZ_SERVICECALENDAR_1_1_202312200612.xml");
//		final File file = new File(directory, "PROD_NETEX_TT_1.10_CHE_SKI_2024_OEV-SCHWEIZ_COMMON_1_1_202312200612.xml");
//		final File file = new File(directory, "PROD_NETEX_TT_1.10_CHE_SKI_2024_OEV-SCHWEIZ_TIMETABLE_1_193_202312200612.xml");

		new Importer().importPublication(file);
	}

	private void importPublication(File file) throws XMLStreamException {
		final XMLInputFactory2 factory = (XMLInputFactory2)XMLInputFactory2.newInstance();
		final XMLStreamReader2 reader = factory.createXMLStreamReader(file);

		final Parser parser = ParserDefinitions.createPublicationDeliveryParser(ParserDefinitions.createCommonFramesParser());
		final Object result = parser.parse(reader);
	}

	private void importTest(File file) throws XMLStreamException {
		System.out.println("import " + file);

		final XMLInputFactory2 factory = (XMLInputFactory2)XMLInputFactory2.newInstance();
		final XMLStreamReader2 reader = factory.createXMLStreamReader(file);

		while (reader.hasNext()) {
			final int eventType = reader.next();
			switch (eventType) {
				case XMLStreamConstants.START_DOCUMENT:
					System.out.println("start document");
					break;
				case XMLStreamConstants.END_DOCUMENT:
					System.out.println("end document");
					break;
				case XMLStreamConstants.START_ELEMENT:
					System.out.println("start element " + reader.getDepth() + " " + reader.getName() + " / " + reader.getLocalName());
					for (int i = 0; i < reader.getAttributeCount(); ++i) {
						System.out.println("  attribute: " + reader.getAttributeName(i) + " / " + reader.getAttributeLocalName(i) + " / " + reader.getAttributeValue(i));
					}
					break;
				case XMLStreamConstants.END_ELEMENT:
					System.out.println("end element");
					break;
				case XMLStreamConstants.ATTRIBUTE:
					System.out.println("attribute");
					break;
				case XMLStreamConstants.CHARACTERS:
					final String text = reader.getText();
					System.out.println("characters " + (text.isBlank() ? "" : text));
					break;
				case XMLStreamConstants.COMMENT:
					System.out.println("comment");
					break;
				default:
					System.out.println("something else: " + eventType);
					break;
			}
		}
	}
}
