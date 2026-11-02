package ch.bernmobil.netex.application.masterdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.bernmobil.netex.application.masterdata.OperatorRepository.OperatorStorage;
import ch.bernmobil.netex.application.masterdata.model.Operator;

public class OperatorRepositoryTest {

	private OperatorRepository operatorRepository;

	@BeforeEach
	public void setup() {
		operatorRepository = new OperatorRepository();
	}

	@Test
	public void testCanGetOperatorByOrganisationNumber() {
		final Operator operator1 = new Operator();
		operator1.setOperatorId("85:801");
		operator1.setOrganisationNumber("801");
		final Operator operator2 = new Operator();
		operator2.setOperatorId("85:820");
		operator2.setOrganisationNumber("820");

		setStorageWithOperators(operator1, operator2);

		assertThat(operatorRepository.getOperatorForOrganisationNumber("801")).isSameAs(operator1);
		assertThat(operatorRepository.getOperatorForOrganisationNumber("820")).isSameAs(operator2);
		assertThat(operatorRepository.getOperatorForOrganisationNumber("840")).isNull();
	}

	@Test
	public void testReturnsSecondOperatorOnDuplicateOrganisationNumber() {
		final Operator operator1 = new Operator();
		operator1.setOperatorId("85:801");
		operator1.setOrganisationNumber("111");
		final Operator operator2 = new Operator();
		operator2.setOperatorId("85:820");
		operator2.setOrganisationNumber("111");

		setStorageWithOperators(operator1, operator2);

		assertThat(operatorRepository.getOperatorForOrganisationNumber("111")).isSameAs(operator2);
	}

	@Test
	public void testDoesNotReturnOperatorThatIsNotValidAnymore() {
		final Operator operator = new Operator();
		operator.setOperatorId("85:801");
		operator.setOrganisationNumber("801");
		operator.setValidFrom(LocalDate.of(1900, 1, 1));
		operator.setValidTo(LocalDate.of(2020, 12, 31));

		setStorageWithOperators(operator);

		assertThat(operatorRepository.getOperatorForOrganisationNumber("801")).isNull();
	}

	@Test
	public void testDoesNotReturnOperatorThatIsNotYetValid() {
		final Operator operator = new Operator();
		operator.setOperatorId("85:801");
		operator.setOrganisationNumber("801");
		operator.setValidFrom(LocalDate.of(2100, 1, 1)); // fix this test in the year 2100
		operator.setValidTo(LocalDate.of(9999, 12, 31));

		setStorageWithOperators(operator);

		assertThat(operatorRepository.getOperatorForOrganisationNumber("801")).isNull();
	}

	@Test
	public void testReturnsOperatorThatIsValidToday() {
		final Operator operator = new Operator();
		operator.setOperatorId("85:801");
		operator.setOrganisationNumber("801");
		operator.setValidFrom(LocalDate.of(2020, 12, 31));
		operator.setValidTo(LocalDate.of(2100, 1, 1)); // fix this test in the year 2100

		setStorageWithOperators(operator);

		assertThat(operatorRepository.getOperatorForOrganisationNumber("801")).isSameAs(operator);
	}

	@Test
	public void testReturnsOperatorThatIsValidTodayIfThereAreMultipleVersions() {
		final Operator version1 = new Operator();
		version1.setOperatorId("85:801");
		version1.setOrganisationNumber("801");
		version1.setValidFrom(LocalDate.of(1900, 1, 1));
		version1.setValidTo(LocalDate.of(2020, 12, 31));
		final Operator version2 = new Operator();
		version2.setOperatorId("85:801");
		version2.setOrganisationNumber("801");
		version2.setValidFrom(LocalDate.of(2020, 12, 31));
		version2.setValidTo(LocalDate.of(2100, 1, 1)); // fix this test in the year 2100
		final Operator version3 = new Operator();
		version3.setOperatorId("85:801");
		version3.setOrganisationNumber("801");
		version3.setValidFrom(LocalDate.of(2100, 1, 1)); // fix this test in the year 2100
		version3.setValidTo(LocalDate.of(9999, 12, 31));

		setStorageWithOperators(version1, version2, version3);

		assertThat(operatorRepository.getOperatorForOrganisationNumber("801")).isSameAs(version2);
	}

	private void setStorageWithOperators(Operator ... operators) {
		final OperatorStorage storage = new OperatorStorage();
		for (final Operator operator : operators) {
			storage.addOperator(operator);
		}
		operatorRepository.setOperatorStorage(storage);
	}
}
