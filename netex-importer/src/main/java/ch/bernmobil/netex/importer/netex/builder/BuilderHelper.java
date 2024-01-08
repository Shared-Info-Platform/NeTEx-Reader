package ch.bernmobil.netex.importer.netex.builder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ch.bernmobil.netex.importer.netex.builder.Frame.CompositeFrameHeader;

/**
 * Helper class that helps reading NeTEx frames.
 */
public class BuilderHelper {

	public static final String RESOURCE_FRAME_NAME = "ResourceFrame";
	public static final String SITE_FRAME_NAME = "SiteFrame";
	public static final String SERVICE_FRAME_NAME = "ServiceFrame";
	public static final String SERVICE_CALENDAR_FRAME_NAME = "ServiceCalendarFrame";
	public static final String TIMETABLE_FRAME_NAME = "TimetableFrame";

	public static Frame getFrame(ObjectTree root, String frameName) {
		final ObjectTree frames = root.child("PublicationDelivery").child("dataObjects").child("CompositeFrame").child("frames");
		final ObjectTree frameTree = frames.optionalChild(frameName);

		if (frameTree != null) {
			final Frame result = new Frame();
			result.frameName = frameName;
			result.frameTree = frameTree;
			result.compositeFrameHeader = getCompositeFrameHeader(root);
			return result;
		} else {
			return null;
		}
	}

	private static CompositeFrameHeader getCompositeFrameHeader(ObjectTree root) {
		final CompositeFrameHeader result = new CompositeFrameHeader();
		final ObjectTree compositeFrame = root.child("PublicationDelivery").child("dataObjects").child("CompositeFrame");
		result.validFrom = LocalDateTime.parse(compositeFrame.child("ValidBetween").text("FromDate"));
		result.validTo = LocalDateTime.parse(compositeFrame.child("ValidBetween").text("ToDate"));

		final ObjectTree frameDefaults = compositeFrame.optionalChild("FrameDefaults");
		if (frameDefaults != null) {
			final ObjectTree defaultLocale = frameDefaults.optionalChild("DefaultLocale");
			if (defaultLocale != null) {
				final String timeZoneOffset = defaultLocale.optionalText("TimeZoneOffset");
				if (timeZoneOffset != null) {
					result.timeZoneOffset = Integer.parseInt(timeZoneOffset);
				}

				final String summerTimeZoneOffset = defaultLocale.optionalText("SummerTimeZoneOffset");
				if (summerTimeZoneOffset != null) {
					result.summerTimeZoneOffset = Integer.parseInt(summerTimeZoneOffset);
				}

				result.defaultLanguage = defaultLocale.optionalText("DefaultLanguage");
			}
		}

		return result;
	}

	public static Map<String, String> buildMapFromKeyList(ObjectTree tree) {
		if (tree == null) {
			return Collections.emptyMap();
		}

		final Map<String, String> result = new HashMap<>();
		for (final ObjectTree entry : tree.children("KeyValue")) {
			result.put(entry.text("Key"), entry.text("Value"));
		}
		return result;
	}
}
