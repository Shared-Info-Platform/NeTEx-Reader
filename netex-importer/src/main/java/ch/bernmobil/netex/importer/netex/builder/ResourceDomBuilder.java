package ch.bernmobil.netex.importer.netex.builder;

import ch.bernmobil.netex.importer.ImportState;
import ch.bernmobil.netex.importer.netex.dom.NetexOperator;
import ch.bernmobil.netex.importer.netex.dom.NetexResponsibilitySet;
import ch.bernmobil.netex.importer.parser.MultilingualStringParser.MultilingualString;

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
}
