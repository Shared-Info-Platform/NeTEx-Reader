package ch.bernmobil.netex.importer.netex.builder;

import java.time.LocalDateTime;

/**
 * Helper class to store the contents of a NeTEx frame.
 */
public class Frame {

	public String frameName;
	public ObjectTree frameTree;
	public CompositeFrameHeader compositeFrameHeader;

	public static class CompositeFrameHeader {
		public LocalDateTime validFrom;
		public LocalDateTime validTo;
		public Integer timeZoneOffset;
		public Integer summerTimeZoneOffset;
		public String defaultLanguage;
	}
}
