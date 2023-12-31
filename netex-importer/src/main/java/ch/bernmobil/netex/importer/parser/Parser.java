package ch.bernmobil.netex.importer.parser;

import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLStreamReader2;

public interface Parser {
	Object parse(final XMLStreamReader2 reader) throws XMLStreamException;
}
