package ch.bernmobil.netex.haltelog.repository;

import java.io.IOException;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.bernmobil.netex.haltelog.model.HaltelogEntry;
import ch.bernmobil.netex.haltelog.model.Service;
import ch.bernmobil.netex.haltelog.properties.HaltelogProperties;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;

/**
 * Note: Keep this in sync with HaltelogRepository in SIP-Hub.
 */
public class HaltelogRepository {

	private static final Logger logger = LoggerFactory.getLogger(HaltelogRepository.class);

	private final ElasticsearchClient esClient;
	private final BulkIngester<?> bulkIngester;
	private final String indexName;
	private final String indexPattern;

	public HaltelogRepository(ElasticsearchClient esClient, BulkIngester<?> bulkIngester, HaltelogProperties loggingProperties)
			throws IOException {
		this.esClient = esClient;
		this.bulkIngester = bulkIngester;
		this.indexName = loggingProperties.getElastic().getHalteLogIndexName();
		this.indexPattern = loggingProperties.getElastic().getHalteLogIndexPattern();
	}

	public void save(HaltelogEntry logEntry) {
		IndexOperation<HaltelogEntry> indexOperation = new IndexOperation.Builder<HaltelogEntry>()
				.index(indexName)
				.document(logEntry)
				.build();
		bulkIngester.add(b -> b.index(indexOperation));
	}

	public boolean containsDataForCalendarDay(LocalDate date) {
		try {
			final SearchResponse<HaltelogEntry> response = esClient.search(s -> s
					.index(indexPattern)
					.size(0)
					.query(createServiceAndCalendarDayQuery(date)),
					HaltelogEntry.class
			);
			return response.hits().total().value() > 0;
		} catch (ElasticsearchException | IOException e) {
			logger.error("failed to query elasticsearch", e);
			return false;
		}
	}

	public void deleteDataForCalendarDay(LocalDate date) {
		try {
			esClient.deleteByQuery(d -> d
					.index(indexPattern)
					.conflicts(Conflicts.Proceed)
					.query(createServiceAndCalendarDayQuery(date))
			);
		} catch (ElasticsearchException | IOException e) {
			logger.error("failed to delete documents in elasticsearch", e);
		}
	}

	private static Query createServiceAndCalendarDayQuery(LocalDate date) {
		final Query matchingService = MatchQuery.of(m -> m
				.field("service")
				.query(Service.NETEX.name())
		)._toQuery();
		final Query matchingCalendarDay = MatchQuery.of(m -> m
				.field("kalendertag")
				.query(date.toString())
		)._toQuery();

		return BoolQuery.of(b -> b
				.must(matchingService)
				.must(matchingCalendarDay)
		)._toQuery();
	}
}
