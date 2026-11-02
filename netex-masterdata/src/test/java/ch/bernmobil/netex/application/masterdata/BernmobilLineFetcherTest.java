package ch.bernmobil.netex.application.masterdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.jupiter.api.Test;

import ch.bernmobil.netex.application.masterdata.model.Line;

public class BernmobilLineFetcherTest {

	private static class BernmobilLineFetcherWithMockedJson extends BernmobilLineFetcher {
		private final String json;
		private boolean firstCall = true;

		public BernmobilLineFetcherWithMockedJson(String json) {
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

		final BernmobilLineFetcher fetcher = new BernmobilLineFetcherWithMockedJson(json);
		final List<Line> lines = fetcher.fetchLines();
		assertThat(lines).isEmpty();
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

		final BernmobilLineFetcher fetcher = new BernmobilLineFetcherWithMockedJson(json);
		final List<Line> lines = fetcher.fetchLines();
		assertThat(lines).isEmpty();
	}

	@Test
	public void testFetchSingleLine() throws IOException, URISyntaxException {
		final String json = """
				{
				  "page": 0,
				  "content": [
				    {
				      "linienId": "85:820:1",
				      "slnid": "ch:1:slnid:1234",
				      "number": "1",
				      "operator": {
				        "organisationNumber": 820
				      }
				    }
				  ],
				  "size": 1,
				  "totalElements": 11,
				  "totalPages": 11
				}""";

		final BernmobilLineFetcher fetcher = new BernmobilLineFetcherWithMockedJson(json);
		final List<Line> lines = fetcher.fetchLines();
		assertThat(lines).hasSize(1);
		final Line line = lines.get(0);
		assertThat(line.getLineId()).isEqualTo("85:820:1");
		assertThat(line.getSlnid()).isEqualTo("ch:1:slnid:1234");
		assertThat(line.getText()).isEqualTo("1");
		assertThat(line.getOrganisationNumber()).isEqualTo("820");
		assertThat(line.getRegionNumber()).isNull();
	}

	@Test
	public void testFetchSinglePostautoLine() throws IOException, URISyntaxException {
		final String json = """
				{
				  "page": 0,
				  "content": [
				    {
				      "linienId": "85:801:1337",
				      "slnid": "ch:1:slnid:4567",
				      "number": "88",
				      "operator": {
				        "organisationNumber": 801
				      }
				    }
				  ],
				  "size": 1,
				  "totalElements": 11,
				  "totalPages": 11
				}""";

		final BernmobilLineFetcher fetcher = new BernmobilLineFetcherWithMockedJson(json);
		final List<Line> lines = fetcher.fetchLines();
		assertThat(lines).hasSize(1);
		final Line line = lines.get(0);
		assertThat(line.getLineId()).isEqualTo("85:801:1337");
		assertThat(line.getSlnid()).isEqualTo("ch:1:slnid:4567");
		assertThat(line.getText()).isEqualTo("88");
		assertThat(line.getOrganisationNumber()).isEqualTo("801");
		assertThat(line.getRegionNumber()).isEqualTo("1337"); // parse region code from linienId for Postauto (801)
	}

	@Test
	public void testFetchMultipleLines() throws IOException, URISyntaxException {
		final String json = """
				{
				  "page": 0,
				  "content": [
				    {
				      "linienId": "85:820:1",
				      "slnid": "ch:1:slnid:1234",
				      "number": "1",
				      "operator": {
				        "organisationNumber": 820
				      }
				    },
				    {
				      "linienId": "85:801:1337",
				      "slnid": "ch:1:slnid:4567",
				      "number": "88",
				      "operator": {
				        "organisationNumber": 801
				      }
				    }
				  ],
				  "size": 2,
				  "totalElements": 11,
				  "totalPages": 6
				}""";

		final BernmobilLineFetcher fetcher = new BernmobilLineFetcherWithMockedJson(json);
		final List<Line> lines = fetcher.fetchLines();
		assertThat(lines).hasSize(2);
		assertThat(lines.get(0).getLineId()).isEqualTo("85:820:1");
		assertThat(lines.get(1).getLineId()).isEqualTo("85:801:1337");
	}

	@Test
	public void testFetchLineWithNoOperator() throws IOException, URISyntaxException {
		final String json = """
				{
				  "page": 0,
				  "content": [
				    {
				      "linienId": "85:820:1",
				      "slnid": "ch:1:slnid:1234",
				      "number": "1"
				    }
				  ],
				  "size": 1,
				  "totalElements": 11,
				  "totalPages": 11
				}""";

		final BernmobilLineFetcher fetcher = new BernmobilLineFetcherWithMockedJson(json);
		final List<Line> lines = fetcher.fetchLines();
		assertThat(lines).hasSize(1);
		final Line line = lines.get(0);
		assertThat(line.getLineId()).isEqualTo("85:820:1");
		assertThat(line.getSlnid()).isEqualTo("ch:1:slnid:1234");
		assertThat(line.getText()).isEqualTo("1");
		assertThat(line.getOrganisationNumber()).isNull();
		assertThat(line.getRegionNumber()).isNull();
	}

	@Test
	public void testFetchLineWithNoOrganisationNumber() throws IOException, URISyntaxException {
		final String json = """
				{
				  "page": 0,
				  "content": [
				    {
				      "linienId": "85:820:1",
				      "slnid": "ch:1:slnid:1234",
				      "number": "1",
				      "operator": {
				      }
				    }
				  ],
				  "size": 1,
				  "totalElements": 11,
				  "totalPages": 11
				}""";

		final BernmobilLineFetcher fetcher = new BernmobilLineFetcherWithMockedJson(json);
		final List<Line> lines = fetcher.fetchLines();
		assertThat(lines).hasSize(1);
		final Line line = lines.get(0);
		assertThat(line.getLineId()).isEqualTo("85:820:1");
		assertThat(line.getSlnid()).isEqualTo("ch:1:slnid:1234");
		assertThat(line.getText()).isEqualTo("1");
		assertThat(line.getOrganisationNumber()).isNull();
		assertThat(line.getRegionNumber()).isNull();
	}

	@Test
	public void testFetchLineWithNoInformation() throws IOException, URISyntaxException {
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

		final BernmobilLineFetcher fetcher = new BernmobilLineFetcherWithMockedJson(json);
		final List<Line> lines = fetcher.fetchLines();
		assertThat(lines).hasSize(1);
	}

	@Test
	public void testFetchLineWithNoId() throws IOException, URISyntaxException {
		final String json = """
				{
				  "page": 0,
				  "content": [
				    {
				      "number": "1"
				    }
				  ],
				  "size": 1,
				  "totalElements": 11,
				  "totalPages": 11
				}""";


		final BernmobilLineFetcher fetcher = new BernmobilLineFetcherWithMockedJson(json);
		final List<Line> lines = fetcher.fetchLines();
		assertThat(lines).hasSize(1);
	}

	@Test
	// just a smoke test to see if the code can handle real data including ignored attributes (without testing the values in depth)
	public void testFetchWithRealData() throws IOException, URISyntaxException {
		final String json = """
				{
				  "page": 100,
				  "content": [
				    {
				      "id": 25390,
				      "operator": {
				        "creationDate": "2022-08-04T14:16:27.949+00:00",
				        "creator": "fxatlsy",
				        "editionDate": "2022-08-04T14:16:27.949+00:00",
				        "editor": "fxatlsy",
				        "status": "VALIDATED",
				        "id": 1345,
				        "sboid": "ch:1:sboid:100602",
				        "said": "100602",
				        "descriptionDe": "PostAuto AG",
				        "descriptionFr": "CarPostal SA",
				        "descriptionIt": "AutoPostale SA",
				        "descriptionEn": "PostBus Ltd",
				        "abbreviationDe": "PAG",
				        "abbreviationFr": "PAG",
				        "abbreviationIt": "PAG",
				        "abbreviationEn": "PAG",
				        "organisationNumber": 801,
				        "contactEnterpriseEmail": "planbox@postauto.ch",
				        "businessTypes": [
				          "STREET"
				        ],
				        "validFrom": "2019-09-10",
				        "validTo": "9999-12-31",
				        "etagVersion": 0,
				        "operatorId": "85:801",
				        "imsDescription": "PostAuto AG (PAG)"
				      },
				      "creationDate": "2022-07-19T15:52:43.911+00:00",
				      "creator": "fxatlsy",
				      "description": "Brig - Gamsen - Eyholz/Brigerbad - Visp",
				      "editionDate": "2026-10-19T16:46:01.684+00:00",
				      "editor": "e527763",
				      "etagVersion": 4,
				      "linienId": "85:801:650",
				      "longName": null,
				      "slnid": "ch:1:slnid:1025859",
				      "status": "VALIDATED",
				      "validFrom": "2017-12-10T00:00:00.000+00:00",
				      "validTo": "2026-12-12T00:00:00.000+00:00",
				      "rgbBack": "#FFCC00",
				      "rgbFont": null,
				      "partnersys": {
				        "id": "1020",
				        "name": "pag-itcs-prod-proxy",
				        "displayName": "Partnerinstanz für PAG ISA",
				        "stage": "prod",
				        "rgbText": null,
				        "rgbBack": null,
				        "entitytype": "ITCS",
				        "isInternal": "true",
				        "dbid": "2",
				        "ownerid": "1",
				        "services": [
				          {
				            "enabled": true,
				            "service": "aus",
				            "role": "server",
				            "version": "xsd_2017d",
				            "directDelivery": false
				          },
				          {
				            "enabled": true,
				            "service": "aus",
				            "role": "client",
				            "version": "xsd_2017d",
				            "directDelivery": false
				          },
				          {
				            "enabled": true,
				            "service": "ausref",
				            "role": "server",
				            "version": "xsd_2017d",
				            "directDelivery": false
				          },
				          {
				            "enabled": true,
				            "service": "ausref",
				            "role": "client",
				            "version": "xsd_2017d",
				            "directDelivery": false
				          },
				          {
				            "enabled": false,
				            "service": "dfi",
				            "role": "server",
				            "version": "xsd_2017d",
				            "directDelivery": false
				          },
				          {
				            "enabled": false,
				            "service": "dfi",
				            "role": "client",
				            "version": "xsd_2017d",
				            "directDelivery": false
				          },
				          {
				            "enabled": true,
				            "service": "ans",
				            "role": "server",
				            "version": "xsd_2017d",
				            "directDelivery": false
				          },
				          {
				            "enabled": false,
				            "service": "ans",
				            "role": "client",
				            "version": "xsd_2017d",
				            "directDelivery": false
				          },
				          {
				            "enabled": false,
				            "service": "ums",
				            "role": "server",
				            "version": "siri2.0q",
				            "directDelivery": false
				          },
				          {
				            "enabled": false,
				            "service": "ums",
				            "role": "client",
				            "version": "siri2.0q",
				            "directDelivery": false
				          },
				          {
				            "enabled": false,
				            "service": "vis",
				            "role": "server",
				            "version": "xsd_2017d",
				            "directDelivery": false
				          },
				          {
				            "enabled": false,
				            "service": "vis",
				            "role": "client",
				            "version": "xsd_2017d",
				            "directDelivery": false
				          }
				        ]
				      },
				      "rtrmon": true,
				      "partnerLineid": null,
				      "isSharedLine": null,
				      "isSubline": false,
				      "comment": "",
				      "lineConcessionType": "FEDERALLY_LICENSED_OR_APPROVED_LINE",
				      "lineType": "ORDERLY",
				      "number": "621/622",
				      "offerCategory": "B",
				      "shortNumber": null,
				      "swissLineNumber": "r.12.620:K",
				      "sublines": null
				    }
				  ],
				  "size": 1,
				  "totalElements": 3831,
				  "totalPages": 3831
				}""";

		final BernmobilLineFetcher fetcher = new BernmobilLineFetcherWithMockedJson(json);
		final List<Line> lines = fetcher.fetchLines();
		assertThat(lines).hasSize(1);
		assertThat(lines.get(0).getLineId()).isEqualTo("85:801:650");
		assertThat(lines.get(0).getSlnid()).isEqualTo("ch:1:slnid:1025859");
		assertThat(lines.get(0).getText()).isEqualTo("621/622");
		assertThat(lines.get(0).getOrganisationNumber()).isEqualTo("801");
		assertThat(lines.get(0).getRegionNumber()).isEqualTo("650");
	}
}
