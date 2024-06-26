package ch.bernmobil.netex.importer.netex.builder;

import ch.bernmobil.netex.importer.ImportState;
import ch.bernmobil.netex.importer.netex.dom.NetexTrainNumber;

/**
 * This class reads the object tree of a NeTEx timetable frame and stores the contained train numbers in the ImportState.
 * It ignores any journeys that may also be contained in this frame.
 */
public class TimetableTrainNumberDomBuilder {

	public static void buildDom(Frame resourceFrame, ImportState state) {
		final ObjectTree serviceFacilitySets = resourceFrame.frameTree.optionalChild("trainNumbers");
		if (serviceFacilitySets != null) {
			serviceFacilitySets.children("TrainNumber").stream().map(TimetableTrainNumberDomBuilder::buildTrainNumber).forEach(state::addTrainNumber);
		}
	}

	private static NetexTrainNumber buildTrainNumber(ObjectTree tree) {
		final NetexTrainNumber result = new NetexTrainNumber();
		result.id = tree.text("id");
		result.forAdvertisement = tree.optionalText("ForAdvertisement");
		return result;
	}
}
