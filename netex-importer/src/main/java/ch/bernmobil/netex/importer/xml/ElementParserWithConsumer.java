package ch.bernmobil.netex.importer.xml;

import java.util.function.Consumer;

import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLStreamReader2;

public class ElementParserWithConsumer extends ElementParser {

	private final Consumer<Object> consumer;

	public ElementParserWithConsumer(Consumer<Object> consumer) {
		super();
		this.consumer = consumer;
	}

	@Override
	public Object parse(XMLStreamReader2 reader) throws XMLStreamException {
		consumer.accept(super.parse(reader));
		return null; // don't keep the object in memory as it was already consumed
	}
}
