package ch.bernmobil.netex.importer.netex.builder;

import java.util.Map;

import ch.bernmobil.netex.importer.ImportState;
import ch.bernmobil.netex.importer.netex.dom.NetexQuay;
import ch.bernmobil.netex.importer.netex.dom.NetexStopPlace;

/**
 * This class reads the object tree of a NeTEx site frame and stores the contained entities in the ImportState.
 */
public class SiteDomBuilder {

	public static void buildDom(Frame siteFrame, ImportState state) {
		final ObjectTree stopPlaces = siteFrame.frameTree.optionalChild("stopPlaces");
		if (stopPlaces != null) {
			stopPlaces.children("StopPlace").stream().map(SiteDomBuilder::buildStopPlace).forEach(state::addStopPlace);
		}
	}

	private static NetexStopPlace buildStopPlace(ObjectTree tree) {
		final NetexStopPlace result = new NetexStopPlace();
		result.id = tree.text("id");
		result.name = tree.optionalText("Name");
		result.shortName = tree.optionalText("ShortName");
		result.privateCode = tree.optionalText("PrivateCode");

		final Map<String, String> keyValueMap = BuilderHelper.buildMapFromKeyList(tree.optionalChild("keyList"));
		result.didok = keyValueMap.get("DIDOK");
		result.sloid = keyValueMap.get("SLOID");

		final ObjectTree quays = tree.optionalChild("quays");
		if (quays != null) {
			quays.children("Quay").stream().map(SiteDomBuilder::buildQuay).forEach(quay -> {
				result.quays.add(quay);
				quay.stopPlace = result;
			});
		}

		return result;
	}

	private static NetexQuay buildQuay(ObjectTree tree) {
		final NetexQuay result = new NetexQuay();
		result.id = tree.text("id");
		result.publicCode = tree.optionalText("PublicCode");

		final Map<String, String> keyValueMap = BuilderHelper.buildMapFromKeyList(tree.optionalChild("keyList"));
		result.sloid = keyValueMap.get("SLOID");

		return result;
	}
}
