package ch.bernmobil.netex.application.masterdata;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.bernmobil.netex.application.masterdata.model.Operator;

public class OperatorRepository {

	private static final Logger logger = LoggerFactory.getLogger(OperatorRepository.class);

	private OperatorStorage operatorStorage = new OperatorStorage();

	public void setOperatorStorage(OperatorStorage operatorStorage) {
		this.operatorStorage = operatorStorage;
	}

	public Operator getOperatorForOrganisationNumber(String organisationNumber) {
		return operatorStorage.operatorsByOrganisationNumber.get(organisationNumber);
	}

	public static class OperatorStorage {
		private final Map<String, Operator> operatorsByOrganisationNumber = new HashMap<>();

		public void addOperator(Operator operator) {
			final LocalDate today = LocalDate.now();
			final boolean isValidFromNotAfterToday = operator.getValidFrom() == null || !operator.getValidFrom().isAfter(today);
			final boolean isValidToNotBeforeToday = operator.getValidTo() == null || !operator.getValidTo().isBefore(today);
			if (isValidFromNotAfterToday && isValidToNotBeforeToday) {
				if (operatorsByOrganisationNumber.put(operator.getOrganisationNumber(), operator) != null) {
					logger.warn("duplicate operator for organisation number {}", operator.getOrganisationNumber());
				}
			}
		}
	}
}
