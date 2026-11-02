package ch.bernmobil.netex.application.masterdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import ch.bernmobil.netex.application.masterdata.model.Operator;

public class BernmobilOperatorFetcherTest {

	private static class BernmobilOperatorFetcherWithMockedJson extends BernmobilOperatorFetcher {
		private final String json;
		private boolean firstCall = true;

		public BernmobilOperatorFetcherWithMockedJson(String json) {
			super(null, 1);
			this.json = json;
		}

		@Override
		String fetchJson(int pageNumber, int limit) {
			if (firstCall) {
				firstCall = false;
				return json;
			} else {
				return null;
			}
		}
	}

	@Test
	public void testFetchEmptyObject() throws IOException, URISyntaxException {
		final String json = "{}";

		final BernmobilOperatorFetcher fetcher = new BernmobilOperatorFetcherWithMockedJson(json);
		final List<Operator> operators = fetcher.fetchOperators();
		assertThat(operators).isEmpty();
	}

	@Test
	public void testFetchEmptyPage() throws IOException, URISyntaxException {
		final String json = """
				{
				  "page": 11,
				  "content": [],
				  "size": 1,
				  "totalElements": 11,
				  "totalPages": 11
				}""";

		final BernmobilOperatorFetcher fetcher = new BernmobilOperatorFetcherWithMockedJson(json);
		final List<Operator> operators = fetcher.fetchOperators();
		assertThat(operators).isEmpty();
	}

	@Test
	public void testFetchSingleOperator() throws IOException, URISyntaxException {
		final String json = """
				{
				  "page": 0,
				  "content": [
				    {
				      "organisationNumber": 820,
				      "validFrom": "1900-01-01",
				      "validTo": "9999-12-31",
				      "operatorId": "85:820"
				    }
				  ],
				  "size": 1,
				  "totalElements": 11,
				  "totalPages": 11
				}""";

		final BernmobilOperatorFetcher fetcher = new BernmobilOperatorFetcherWithMockedJson(json);
		final List<Operator> operators = fetcher.fetchOperators();
		assertThat(operators).hasSize(1);
		final Operator operator = operators.get(0);
		assertThat(operator.getOperatorId()).isEqualTo("85:820");
		assertThat(operator.getOrganisationNumber()).isEqualTo("820");
		assertThat(operator.getValidFrom()).isEqualTo(LocalDate.of(1900, 1, 1));
		assertThat(operator.getValidTo()).isEqualTo(LocalDate.of(9999, 12, 31));
	}

	@Test
	public void testFetchMultipleOperators() throws IOException, URISyntaxException {
		final String json = """
				{
				  "page": 0,
				  "content": [
				    {
				      "organisationNumber": 820,
				      "operatorId": "85:820"
				    },
				    {
				      "organisationNumber": 801,
				      "operatorId": "85:801"
				    }
				  ],
				  "size": 2,
				  "totalElements": 11,
				  "totalPages": 6
				}""";

		final BernmobilOperatorFetcher fetcher = new BernmobilOperatorFetcherWithMockedJson(json);
		final List<Operator> operators = fetcher.fetchOperators();
		assertThat(operators).hasSize(2);
		assertThat(operators.get(0).getOperatorId()).isEqualTo("85:820");
		assertThat(operators.get(1).getOperatorId()).isEqualTo("85:801");
	}

	@Test
	public void testFetchOperatorWithNoInformation() throws IOException, URISyntaxException {
		final String json = """
				{
				  "page": 0,
				  "content": [
				    {
				    }
				  ],
				  "size": 1,
				  "totalElements": 11,
				  "totalPages": 11
				}""";

		final BernmobilOperatorFetcher fetcher = new BernmobilOperatorFetcherWithMockedJson(json);
		final List<Operator> operators = fetcher.fetchOperators();
		assertThat(operators).hasSize(1);
	}

	@Test
	public void testFetchOperatorWithNoId() throws IOException, URISyntaxException {
		final String json = """
				{
				  "page": 0,
				  "content": [
				    {
				      "organisationNumber": 820
				    }
				  ],
				  "size": 1,
				  "totalElements": 11,
				  "totalPages": 11
				}""";


		final BernmobilOperatorFetcher fetcher = new BernmobilOperatorFetcherWithMockedJson(json);
		final List<Operator> operators = fetcher.fetchOperators();
		assertThat(operators).hasSize(1);
	}

	@Test
	// just a smoke test to see if the code can handle real data including ignored attributes (without testing the values in depth)
	public void testFetchWithRealData() throws IOException, URISyntaxException {
		final String json = """
				{
				  "page": 0,
				  "content": [
				    {
				      "creationDate": "2022-08-04T14:16:27.543+00:00",
				      "creator": "fxatlsy",
				      "editionDate": "2022-08-04T14:16:27.543+00:00",
				      "editor": "fxatlsy",
				      "status": "VALIDATED",
				      "id": 1000,
				      "sboid": "ch:1:sboid:100186",
				      "said": "100186",
				      "descriptionDe": "Kandersteg-Stock",
				      "descriptionFr": "Kandersteg-Stock",
				      "descriptionIt": "Kandersteg-Stock",
				      "descriptionEn": "Kandersteg-Stock",
				      "abbreviationDe": "LKS",
				      "abbreviationFr": "LKS",
				      "abbreviationIt": "LKS",
				      "abbreviationEn": "LKS",
				      "organisationNumber": 228,
				      "contactEnterpriseEmail": "dominic.hurni@sunnbuel.ch",
				      "businessTypes": [
				        "RAILROAD"
				      ],
				      "validFrom": "1900-01-01",
				      "validTo": "2022-07-24",
				      "etagVersion": 0,
				      "operatorId": "85:228",
				      "imsDescription": null
				    }
				  ],
				  "size": 1,
				  "totalElements": 2171,
				  "totalPages": 2171
				}""";

		final BernmobilOperatorFetcher fetcher = new BernmobilOperatorFetcherWithMockedJson(json);
		final List<Operator> operators = fetcher.fetchOperators();
		assertThat(operators).hasSize(1);
		assertThat(operators.get(0).getOperatorId()).isEqualTo("85:228");
		assertThat(operators.get(0).getOrganisationNumber()).isEqualTo("228");
		assertThat(operators.get(0).getValidFrom()).isEqualTo(LocalDate.of(1900, 1, 1));
		assertThat(operators.get(0).getValidTo()).isEqualTo(LocalDate.of(2022, 7, 24));
	}
}
