package ch.bernmobil.netex.importer.netex.builder;

import java.util.List;

import ch.bernmobil.netex.importer.ImportState;
import ch.bernmobil.netex.importer.netex.dom.NetexOperator;
import ch.bernmobil.netex.importer.netex.dom.NetexResponsibilitySet;
import ch.bernmobil.netex.importer.netex.dom.NetexTypeOfNotice;
import ch.bernmobil.netex.importer.netex.dom.NetexTypeOfProductCategory;
import ch.bernmobil.netex.importer.netex.dom.NetexVehicleType;
import ch.bernmobil.netex.importer.xml.MultilingualStringParser.MultilingualString;

public class ResourceDomBuilder {

	public static void buildDom(Frame resourceFrame, ImportState state) {
		final ObjectTree organisations = resourceFrame.frameTree.optionalChild("organisations");
		if (organisations != null) {
			organisations.children("Operator").stream().map(ResourceDomBuilder::buildOperator).forEach(state::addOperator);
		}

		final ObjectTree responsibilitySets = resourceFrame.frameTree.optionalChild("responsibilitySets");
		if (responsibilitySets != null) {
			responsibilitySets.children("ResponsibilitySet").stream().map(child -> buildResponsibilitySet(child, state)).forEach(state::addResponsibilitySet);
		}

		final ObjectTree typesOfValue = resourceFrame.frameTree.optionalChild("typesOfValue");
		if (typesOfValue != null) {
			typesOfValue.children("ValueSet").stream()
					.map(valueSet -> valueSet.child("values"))
					.map(values -> values.children("TypeOfNotice"))
					.flatMap(List::stream)
					.map(ResourceDomBuilder::buildTypeOfNotice).forEach(state::addTypeOfNotice);

			typesOfValue.children("ValueSet").stream()
					.map(valueSet -> valueSet.child("values"))
					.map(values -> values.children("TypeOfProductCategory"))
					.flatMap(List::stream)
					.map(ResourceDomBuilder::buildTypeOfProductCategory).forEach(state::addTypeOfProductCategory);
		}

		final ObjectTree vehicleTypes = resourceFrame.frameTree.optionalChild("vehicleTypes");
		if (vehicleTypes != null) {
			vehicleTypes.children("VehicleType").stream().map(ResourceDomBuilder::buildVehicleType).forEach(state::addVehicleType);
		}
	}

	private static NetexOperator buildOperator(ObjectTree tree) {
		final NetexOperator result = new NetexOperator();
		result.id = tree.text("id");
		result.privateCode = tree.optionalText("PrivateCode");
		result.name = tree.optionalText("Name");
		result.shortName = tree.optionalText("ShortName");
		return result;
	}

	private static NetexResponsibilitySet buildResponsibilitySet(ObjectTree tree, ImportState state) {
		final NetexResponsibilitySet result = new NetexResponsibilitySet();
		result.id = tree.text("id");
		result.name = tree.optionalMultilingualString("Name").map(MultilingualString::getText).orElse(null);
		result.privateCode = tree.optionalText("PrivateCode");

		for (final ObjectTree role : tree.child("roles").children("ResponsibilityRoleAssignment")) {
			final String operatorId = role.child("ResponsibleOrganisationRef").text("ref");
			final NetexOperator operator = state.getOperators().get(operatorId);
			if (operator == null) {
				throw new IllegalArgumentException("unknown Operator with id " + operatorId);
			}

			final String stakeholderRoleType = role.text("StakeholderRoleType");
			switch (stakeholderRoleType) {
				case "EntityLegalOwnership":
					result.legalOwner = operator;
					break;
				case "Operation":
					result.operator = operator;
					break;
				default:
					throw new IllegalArgumentException("unknown role type " + stakeholderRoleType);
			}
		}

		return result;
	}

	private static NetexTypeOfNotice buildTypeOfNotice(ObjectTree tree) {
		final NetexTypeOfNotice result = new NetexTypeOfNotice();
		result.id = tree.text("id");
		result.name = tree.text("Name");
		result.privateCode = tree.text("PrivateCode");
		return result;
	}

	private static NetexTypeOfProductCategory buildTypeOfProductCategory(ObjectTree tree) {
		final NetexTypeOfProductCategory result = new NetexTypeOfProductCategory();
		result.id = tree.text("id");
		result.name = tree.multilingualString("Name").text;
		result.shortName = tree.multilingualString("ShortName").text;
		return result;
	}

	private static NetexVehicleType buildVehicleType(ObjectTree tree) {
		final NetexVehicleType result = new NetexVehicleType();
		result.id = tree.text("id");
		result.shortName = tree.multilingualString("ShortName").text;
		result.lowFloor = Boolean.parseBoolean(tree.text("LowFloor"));
		result.hasLiftOrRamp = Boolean.parseBoolean(tree.text("HasLiftOrRamp"));
		result.hasHoist = Boolean.parseBoolean(tree.text("HasHoist"));
		return result;
	}
}
