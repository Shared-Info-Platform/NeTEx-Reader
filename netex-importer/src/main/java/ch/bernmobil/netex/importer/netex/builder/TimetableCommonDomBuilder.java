package ch.bernmobil.netex.importer.netex.builder;

import ch.bernmobil.netex.importer.ImportState;
import ch.bernmobil.netex.importer.netex.dom.NetexServiceFacilitySet;
import ch.bernmobil.netex.importer.xml.MultilingualStringParser.MultilingualString;

public class TimetableCommonDomBuilder {

	public static void buildDom(Frame resourceFrame, ImportState state) {
		final ObjectTree serviceFacilitySets = resourceFrame.frameTree.optionalChild("serviceFacilitySets");
		if (serviceFacilitySets != null) {
			serviceFacilitySets.children("ServiceFacilitySet").stream().map(TimetableCommonDomBuilder::buildServiceFacilitySet).forEach(state::addServiceFacilitySet);
		}
	}

	private static NetexServiceFacilitySet buildServiceFacilitySet(ObjectTree tree) {
		final NetexServiceFacilitySet result = new NetexServiceFacilitySet();
		result.id = tree.text("id");
		result.description = tree.optionalMultilingualString("Description").map(MultilingualString::getText).orElse(null);

		final ObjectTree extensions = tree.optionalChild("Extensions");
		if (extensions != null) {
			final String priority = extensions.optionalText("Priority");
			if (priority != null) {
				result.priority = Integer.parseInt(priority);
			}

			final String condition = extensions.optionalText("Condition");
			if (condition != null) {
				result.condition = Integer.parseInt(condition);
			}
		}

		return result;
	}
}
