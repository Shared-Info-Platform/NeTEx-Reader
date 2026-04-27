package ch.bernmobil.netex.persistence.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import ch.bernmobil.netex.persistence.Constants;
import ch.bernmobil.netex.persistence.PersistenceConfig;
import ch.bernmobil.netex.persistence.model.Call;
import ch.bernmobil.netex.persistence.model.CallAggregation;
import ch.bernmobil.netex.persistence.model.CallWithJourney;
import ch.bernmobil.netex.persistence.model.JourneyAggregation;
import ch.bernmobil.netex.persistence.model.JourneyWithCalls;
import ch.bernmobil.netex.persistence.model.RouteAggregation;
import ch.bernmobil.netex.persistence.model.Call.Arrival;
import ch.bernmobil.netex.persistence.model.Call.Departure;
import ch.bernmobil.netex.persistence.model.RouteAggregation.StopPlace;

@SpringBootTest(classes = PersistenceConfig.class)
@ActiveProfiles("test")
public class NetexRepositoryIntegrationTest {

	private static final String DATABASE_NAME = "netex-test";

	@Autowired
	private MongoClient mongoClient;

	private MongoDatabase database;
	private NetexRepository repository;

	@BeforeEach
	private void setup() {
		database = mongoClient.getDatabase(DATABASE_NAME);
		repository = new NetexRepository(mongoClient, DATABASE_NAME);
		repository.deleteAll();
	}

	@Test
	public void testCanInsertJourneys() {
		final List<JourneyWithCalls> journeys = new ArrayList<>();
		for (int i = 0; i < Constants.MAX_NUMBER_OF_CALLS_PER_MONGODB_WRITE / 10; ++i) {
			journeys.add(createJourney(i));
		}

		final MongoCollection<JourneyWithCalls> collection = database.getCollection("Journeys", JourneyWithCalls.class);
		assertThat(collection.countDocuments()).isEqualTo(0);

		repository.writeJourneys(journeys);

		// check number of documents
		assertThat(collection.countDocuments()).isEqualTo(Constants.MAX_NUMBER_OF_CALLS_PER_MONGODB_WRITE / 10);

		// check content of sample document
		final JourneyWithCalls document = collection.find(Filters.eq("id9")).first();
		assertThat(document).isNotNull();
		assertThat(document.id).isEqualTo("id9");
		assertThat(document.calendarDay).isEqualTo("2025-12-28");
		assertThat(document.operatorCode).isEqualTo("operator");
		assertThat(document.lineCode).isEqualTo("line");
		assertThat(document.directionType).isEqualTo("direction");
		assertThat(document.directionId).isEqualTo("dir1");
		assertThat(document.calls).hasSize(25);
		assertThat(document.calls.getLast().order).isEqualTo(24);
		assertThat(document.calls.getLast().stopPlaceCode).isEqualTo("stop24");
		assertThat(document.calls.getLast().arrival.time).isEqualTo(Instant.ofEpochSecond(24));
		assertThat(document.calls.getLast().departure.time).isEqualTo(Instant.ofEpochSecond(24));
		assertThat(document.departureTime).isEqualTo(Instant.ofEpochSecond(0));
		assertThat(document.arrivalTime).isEqualTo(Instant.ofEpochSecond(24));
	}

	private static JourneyWithCalls createJourney(int id) {
		final JourneyWithCalls journey = new JourneyWithCalls();
		journey.id = "id" + id;
		journey.calendarDay = LocalDate.of(2025, 12, 28).toString();
		journey.operatorCode = "operator";
		journey.lineCode = "line";
		journey.directionType = "direction";
		journey.directionId = "dir1";
		for (int j = 0; j < 25; ++j) {
			final Call call = new Call();
			call.order = j;
			call.stopPlaceCode = "stop" + j;
			call.arrival = new Arrival();
			call.arrival.time = Instant.ofEpochSecond(j);
			call.departure = new Departure();
			call.departure.time = Instant.ofEpochSecond(j);
			journey.calls.add(call);
		}
		journey.departureTime = journey.calls.getFirst().departure.time;
		journey.arrivalTime = journey.calls.getLast().arrival.time;
		return journey;
	}

	@Test
	public void testCanInsertCalls() {
		final List<CallWithJourney> calls = new ArrayList<>();
		for (int i = 0; i < Constants.MAX_NUMBER_OF_CALLS_PER_MONGODB_WRITE * 2.5; ++i) {
			calls.add(createCall(i));
		}

		final MongoCollection<CallWithJourney> collection = database.getCollection("Calls", CallWithJourney.class);
		assertThat(collection.countDocuments()).isEqualTo(0);

		repository.writeCalls(calls);

		// check number of documents
		assertThat(collection.countDocuments()).isEqualTo((long) (Constants.MAX_NUMBER_OF_CALLS_PER_MONGODB_WRITE * 2.5));

		// check content of sample document
		final CallWithJourney document = collection.find(Filters.eq("id9")).first();
		assertThat(document).isNotNull();
		assertThat(document.id).isEqualTo("id9");
		assertThat(document.calendarDay).isEqualTo("2025-12-28");
		assertThat(document.operatorCode).isEqualTo("operator");
		assertThat(document.lineCode).isEqualTo("line");
		assertThat(document.directionType).isEqualTo("direction");
		assertThat(document.directionId).isEqualTo("dir1");
		assertThat(document.order).isEqualTo(9);
		assertThat(document.stopPlaceCode).isEqualTo("stop9");
		assertThat(document.arrival.time).isEqualTo(Instant.ofEpochSecond(9));
		assertThat(document.departure.time).isEqualTo(Instant.ofEpochSecond(9));
	}

	private static CallWithJourney createCall(int id) {
		final CallWithJourney call = new CallWithJourney();
		call.id = "id" + id;
		call.calendarDay = LocalDate.of(2025, 12, 28).toString();
		call.operatorCode = "operator";
		call.lineCode = "line";
		call.directionType = "direction";
		call.directionId = "dir1";
		call.order = id;
		call.stopPlaceCode = "stop" + id;
		call.arrival = new Arrival();
		call.arrival.time = Instant.ofEpochSecond(id);
		call.departure = new Departure();
		call.departure.time = Instant.ofEpochSecond(id);
		return call;
	}

	@Test
	public void testCanInsertJourneyAggregations() {
		final List<JourneyAggregation> aggregations = new ArrayList<>();
		for (int i = 0; i < Constants.MAX_NUMBER_OF_AGGREGATIONS_PER_MONGODB_WRITE * 2.5; ++i) {
			aggregations.add(createJourneyAggregation(i, i));
		}

		final MongoCollection<JourneyAggregation> collection = database.getCollection("JourneyAggregations", JourneyAggregation.class);
		assertThat(collection.countDocuments()).isEqualTo(0);

		repository.writeJourneyAggregations(aggregations);

		// check number of documents
		assertThat(collection.countDocuments()).isEqualTo((long) (Constants.MAX_NUMBER_OF_AGGREGATIONS_PER_MONGODB_WRITE * 2.5));

		// check content of sample document
		final JourneyAggregation document = collection.find(Filters.eq("2025-12-28_operator_line9_region")).first();
		assertThat(document).isNotNull();
		assertThat(document.calendarDay).isEqualTo("2025-12-28");
		assertThat(document.operatorCode).isEqualTo("operator");
		assertThat(document.lineCode).isEqualTo("line9");
		assertThat(document.regionCode).isEqualTo("region");
		assertThat(document.journeys).isEqualTo(9);
	}

	@Test
	public void testCanInsertJourneyAggregationsWithoutRegionCode() {
		final JourneyAggregation aggregation = createJourneyAggregation(1, 1);
		aggregation.regionCode = null;

		final MongoCollection<JourneyAggregation> collection = database.getCollection("JourneyAggregations", JourneyAggregation.class);
		assertThat(collection.countDocuments()).isEqualTo(0);

		repository.writeJourneyAggregations(List.of(aggregation));

		// check content of sample document
		final JourneyAggregation document = collection.find(Filters.eq("2025-12-28_operator_line1_null")).first();
		assertThat(document).isNotNull();
		assertThat(document.calendarDay).isEqualTo("2025-12-28");
		assertThat(document.operatorCode).isEqualTo("operator");
		assertThat(document.lineCode).isEqualTo("line1");
		assertThat(document.regionCode).isNull();
		assertThat(document.journeys).isEqualTo(1);
	}

	@Test
	public void testCanUpdateJourneyAggregations() {
		final MongoCollection<JourneyAggregation> collection = database.getCollection("JourneyAggregations", JourneyAggregation.class);
		assertThat(collection.countDocuments()).isEqualTo(0);

		for (int i = 0; i < 3; ++i) {
			repository.writeJourneyAggregations(List.of(createJourneyAggregation(1, i + 1)));
		}

		// check number of documents
		assertThat(collection.countDocuments()).isEqualTo(1);

		// check that document contains sum of individual counts (1 + 2 + 3 = 6)
		final JourneyAggregation document = collection.find(Filters.eq("2025-12-28_operator_line1_region")).first();
		assertThat(document).isNotNull();
		assertThat(document.journeys).isEqualTo(6);
	}

	private static JourneyAggregation createJourneyAggregation(int id, int number) {
		final JourneyAggregation aggregation = new JourneyAggregation();
		aggregation.calendarDay = LocalDate.of(2025, 12, 28).toString();
		aggregation.operatorCode = "operator";
		aggregation.lineCode = "line" + id;
		aggregation.regionCode = "region";
		aggregation.journeys = number;
		return aggregation;
	}

	@Test
	public void testCanInsertCallAggregations() {
		final List<CallAggregation> aggregations = new ArrayList<>();
		for (int i = 0; i < Constants.MAX_NUMBER_OF_AGGREGATIONS_PER_MONGODB_WRITE * 2.5; ++i) {
			aggregations.add(createCallAggregation(i, i));
		}

		final MongoCollection<CallAggregation> collection = database.getCollection("CallAggregations", CallAggregation.class);
		assertThat(collection.countDocuments()).isEqualTo(0);

		repository.writeCallAggregations(aggregations);

		// check number of documents
		assertThat(collection.countDocuments()).isEqualTo((long) (Constants.MAX_NUMBER_OF_AGGREGATIONS_PER_MONGODB_WRITE * 2.5));

		// check content of sample document
		final CallAggregation document = collection.find(Filters.eq("2025-12-28_stop9_operator_line_region")).first();
		assertThat(document).isNotNull();
		assertThat(document.calendarDay).isEqualTo("2025-12-28");
		assertThat(document.stopPlaceCode).isEqualTo("stop9");
		assertThat(document.operatorCode).isEqualTo("operator");
		assertThat(document.lineCode).isEqualTo("line");
		assertThat(document.regionCode).isEqualTo("region");
		assertThat(document.calls).isEqualTo(9);
	}

	@Test
	public void testCanInsertCallAggregationsWithoutRegionCode() {
		final CallAggregation aggregation = createCallAggregation(1, 1);
		aggregation.regionCode = null;

		final MongoCollection<CallAggregation> collection = database.getCollection("CallAggregations", CallAggregation.class);
		assertThat(collection.countDocuments()).isEqualTo(0);

		repository.writeCallAggregations(List.of(aggregation));

		// check content of sample document
		final CallAggregation document = collection.find(Filters.eq("2025-12-28_stop1_operator_line_null")).first();
		assertThat(document).isNotNull();
		assertThat(document.calendarDay).isEqualTo("2025-12-28");
		assertThat(document.stopPlaceCode).isEqualTo("stop1");
		assertThat(document.operatorCode).isEqualTo("operator");
		assertThat(document.lineCode).isEqualTo("line");
		assertThat(document.regionCode).isNull();
		assertThat(document.calls).isEqualTo(1);
	}

	@Test
	public void testCanUpdateCallAggregations() {
		final MongoCollection<CallAggregation> collection = database.getCollection("CallAggregations", CallAggregation.class);
		assertThat(collection.countDocuments()).isEqualTo(0);

		for (int i = 0; i < 3; ++i) {
			repository.writeCallAggregations(List.of(createCallAggregation(1, i + 1)));
		}

		// check number of documents
		assertThat(collection.countDocuments()).isEqualTo(1);

		// check that document contains sum of individual counts (1 + 2 + 3 = 6)
		final CallAggregation document = collection.find(Filters.eq("2025-12-28_stop1_operator_line_region")).first();
		assertThat(document).isNotNull();
		assertThat(document.calls).isEqualTo(6);
	}

	private static CallAggregation createCallAggregation(int id, int number) {
		final CallAggregation aggregation = new CallAggregation();
		aggregation.calendarDay = LocalDate.of(2025, 12, 28).toString();
		aggregation.stopPlaceCode = "stop" + id;
		aggregation.operatorCode = "operator";
		aggregation.lineCode = "line";
		aggregation.regionCode = "region";
		aggregation.calls = number;
		return aggregation;
	}

	@Test
	public void testCanInsertRouteAggregation() {
		final List<RouteAggregation> aggregations = new ArrayList<>();
		for (int i = 0; i < Constants.MAX_NUMBER_OF_AGGREGATIONS_PER_MONGODB_WRITE * 2.5; ++i) {
			aggregations.add(createRouteAggregation(i, i));
		}

		final MongoCollection<RouteAggregation> collection = database.getCollection("RouteAggregations", RouteAggregation.class);
		assertThat(collection.countDocuments()).isEqualTo(0);

		repository.writeRouteAggregations(aggregations);

		// check number of documents
		assertThat(collection.countDocuments()).isEqualTo((long) (Constants.MAX_NUMBER_OF_AGGREGATIONS_PER_MONGODB_WRITE * 2.5));

		// check content of sample document
		final RouteAggregation document = collection.find(Filters
				.eq("2025-12-28_operator_line_region_direction_dir1_" + List.of(new StopPlace("0", "stopX"), new StopPlace("9", "stop9")).hashCode()))
				.first();
		assertThat(document).isNotNull();
		assertThat(document.calendarDay).isEqualTo("2025-12-28");
		assertThat(document.operatorCode).isEqualTo("operator");
		assertThat(document.lineCode).isEqualTo("line");
		assertThat(document.regionCode).isEqualTo("region");
		assertThat(document.directionType).isEqualTo("direction");
		assertThat(document.directionId).isEqualTo("dir1");
		assertThat(document.stopPlaces).hasSize(2);
		assertThat(document.stopPlaces.get(0)).isEqualTo(new StopPlace("0", "stopX"));
		assertThat(document.stopPlaces.get(1)).isEqualTo(new StopPlace("9", "stop9"));
		assertThat(document.journeys).isEqualTo(9);
	}

	@Test
	public void testCanInsertRouteAggregationWithoutRegionCode() {
		final RouteAggregation aggregation = createRouteAggregation(1, 1);
		aggregation.regionCode = null;

		final MongoCollection<RouteAggregation> collection = database.getCollection("RouteAggregations", RouteAggregation.class);
		assertThat(collection.countDocuments()).isEqualTo(0);

		repository.writeRouteAggregations(List.of(aggregation));

		// check content of sample document
		final RouteAggregation document = collection.find(Filters
				.eq("2025-12-28_operator_line_null_direction_dir1_" + List.of(new StopPlace("0", "stopX"), new StopPlace("1", "stop1")).hashCode()))
				.first();
		assertThat(document).isNotNull();
		assertThat(document.calendarDay).isEqualTo("2025-12-28");
		assertThat(document.operatorCode).isEqualTo("operator");
		assertThat(document.lineCode).isEqualTo("line");
		assertThat(document.regionCode).isNull();
		assertThat(document.directionType).isEqualTo("direction");
		assertThat(document.directionId).isEqualTo("dir1");
		assertThat(document.stopPlaces).hasSize(2);
		assertThat(document.stopPlaces.get(0)).isEqualTo(new StopPlace("0", "stopX"));
		assertThat(document.stopPlaces.get(1)).isEqualTo(new StopPlace("1", "stop1"));
		assertThat(document.journeys).isEqualTo(1);
	}

	@Test
	public void testCanUpdateRouteAggregation() {
		final MongoCollection<RouteAggregation> collection = database.getCollection("RouteAggregations", RouteAggregation.class);
		assertThat(collection.countDocuments()).isEqualTo(0);

		for (int i = 0; i < 3; ++i) {
			repository.writeRouteAggregations(List.of(createRouteAggregation(1, i + 1)));
		}

		// check number of documents
		assertThat(collection.countDocuments()).isEqualTo(1);

		// check that document contains sum of individual counts (1 + 2 + 3 = 6)
		final RouteAggregation document = collection.find(Filters
				.eq("2025-12-28_operator_line_region_direction_dir1_" + List.of(new StopPlace("0", "stopX"), new StopPlace("1", "stop1")).hashCode()))
				.first();
		assertThat(document).isNotNull();
		assertThat(document.journeys).isEqualTo(6);
	}

	private static RouteAggregation createRouteAggregation(int id, int number) {
		final RouteAggregation aggregation = new RouteAggregation();
		aggregation.calendarDay = LocalDate.of(2025, 12, 28).toString();
		aggregation.operatorCode = "operator";
		aggregation.lineCode = "line";
		aggregation.regionCode = "region";
		aggregation.directionType = "direction";
		aggregation.directionId = "dir1";
		aggregation.stopPlaces = List.of(new StopPlace("0", "stopX"), new StopPlace(Integer.toString(id), "stop" + id));
		aggregation.journeys = number;
		return aggregation;
	}

	@Test
	public void testDatabaseIsNotEmptyWhenJourneyInserted() {
		assertThat(repository.isDatabaseEmpty()).isTrue();
		repository.writeJourneys(List.of(createJourney(1)));
		assertThat(repository.isDatabaseEmpty()).isFalse();
	}

	@Test
	public void testDatabaseIsNotEmptyWhenCallInserted() {
		assertThat(repository.isDatabaseEmpty()).isTrue();
		repository.writeCalls(List.of(createCall(1)));
		assertThat(repository.isDatabaseEmpty()).isFalse();
	}

	@Test
	public void testDatabaseIsNotEmptyWhenJourneyAggregationInserted() {
		assertThat(repository.isDatabaseEmpty()).isTrue();
		repository.writeJourneyAggregations(List.of(createJourneyAggregation(1, 1)));
		assertThat(repository.isDatabaseEmpty()).isFalse();
	}

	@Test
	public void testDatabaseIsNotEmptyWhenCallAggregationInserted() {
		assertThat(repository.isDatabaseEmpty()).isTrue();
		repository.writeCallAggregations(List.of(createCallAggregation(1, 1)));
		assertThat(repository.isDatabaseEmpty()).isFalse();
	}

	@Test
	public void testDatabaseIsNotEmptyWhenRouteAggregationInserted() {
		assertThat(repository.isDatabaseEmpty()).isTrue();
		repository.writeRouteAggregations(List.of(createRouteAggregation(1, 1)));
		assertThat(repository.isDatabaseEmpty()).isFalse();
	}

	@Test
	public void testDatabaseContainsDataForCalendarDayWhenJourneyInserted() {
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 28))).isFalse();
		repository.writeJourneys(List.of(createJourney(1)));
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 28))).isTrue();
	}

	@Test
	public void testDatabaseContainsDataForCalendarDayWhenCallsInserted() {
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 28))).isFalse();
		repository.writeCalls(List.of(createCall(1)));
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 28))).isTrue();
	}

	@Test
	public void testDatabaseContainsDataForCalendarDayWhenJourneyAggregationInserted() {
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 28))).isFalse();
		repository.writeJourneyAggregations(List.of(createJourneyAggregation(1, 1)));
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 28))).isTrue();
	}

	@Test
	public void testDatabaseContainsDataForCalendarDayWhenCallAggregationInserted() {
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 28))).isFalse();
		repository.writeCallAggregations(List.of(createCallAggregation(1, 1)));
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 28))).isTrue();
	}

	@Test
	public void testDatabaseContainsDataForCalendarDayWhenRouteAggregationInserted() {
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 28))).isFalse();
		repository.writeRouteAggregations(List.of(createRouteAggregation(1, 1)));
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 28))).isTrue();
	}

	@Test
	public void testCanGetNumberOfJourneysForCalendarDay() {
		final JourneyWithCalls journey1 = createJourney(1);
		final JourneyWithCalls journey2 = createJourney(2);
		final JourneyWithCalls journey3 = createJourney(3);
		journey1.calendarDay = LocalDate.of(2025, 12, 1).toString();
		journey2.calendarDay = LocalDate.of(2025, 12, 1).toString();
		journey3.calendarDay = LocalDate.of(2025, 12, 2).toString(); // other date

		repository.writeJourneys(List.of(journey1, journey2, journey3));

		assertThat(repository.getNumberOfJourneysForCalendarDay(LocalDate.of(2025, 12, 1))).isEqualTo(2);
		assertThat(repository.getNumberOfJourneysForCalendarDay(LocalDate.of(2025, 12, 2))).isEqualTo(1);
		assertThat(repository.getNumberOfJourneysForCalendarDay(LocalDate.of(2025, 12, 3))).isEqualTo(0);
	}

	@Test
	public void testCanGetJourneysForCalendarDay() {
		final JourneyWithCalls journey1 = createJourney(1);
		final JourneyWithCalls journey2 = createJourney(2);
		final JourneyWithCalls journey3 = createJourney(3);
		journey1.calendarDay = LocalDate.of(2025, 12, 1).toString();
		journey2.calendarDay = LocalDate.of(2025, 12, 1).toString();
		journey3.calendarDay = LocalDate.of(2025, 12, 2).toString(); // other date

		repository.writeJourneys(List.of(journey1, journey2, journey3));

		assertThat(repository.getJourneysForCalendarDay(LocalDate.of(2025, 12, 1))).extracting(j -> j.id).containsExactlyInAnyOrder("id1",
				"id2");
		assertThat(repository.getJourneysForCalendarDay(LocalDate.of(2025, 12, 2))).extracting(j -> j.id).containsExactlyInAnyOrder("id3");
		assertThat(repository.getJourneysForCalendarDay(LocalDate.of(2025, 12, 3))).isEmpty();
	}

	@Test
	public void testCanGetCallsForCalendarDay() {
		final CallWithJourney call1 = createCall(1);
		final CallWithJourney call2 = createCall(2);
		final CallWithJourney call3 = createCall(3);
		call1.calendarDay = LocalDate.of(2025, 12, 1).toString();
		call2.calendarDay = LocalDate.of(2025, 12, 1).toString();
		call3.calendarDay = LocalDate.of(2025, 12, 2).toString(); // other date

		repository.writeCalls(List.of(call1, call2, call3));

		assertThat(repository.getCallsForCalendarDay(LocalDate.of(2025, 12, 1))).extracting(c -> c.id).containsExactlyInAnyOrder("id1",
				"id2");
		assertThat(repository.getCallsForCalendarDay(LocalDate.of(2025, 12, 2))).extracting(c -> c.id).containsExactlyInAnyOrder("id3");
		assertThat(repository.getCallsForCalendarDay(LocalDate.of(2025, 12, 3))).isEmpty();
	}

	@Test
	public void testCanGetJourneyAggregationsForCalendarDay() {
		final JourneyAggregation aggregation1 = createJourneyAggregation(1, 1);
		final JourneyAggregation aggregation2 = createJourneyAggregation(2, 1);
		final JourneyAggregation aggregation3 = createJourneyAggregation(3, 1);
		aggregation1.calendarDay = LocalDate.of(2025, 12, 1).toString();
		aggregation2.calendarDay = LocalDate.of(2025, 12, 1).toString();
		aggregation3.calendarDay = LocalDate.of(2025, 12, 2).toString(); // other date

		repository.writeJourneyAggregations(List.of(aggregation1, aggregation2, aggregation3));

		assertThat(repository.getJourneyAggregationsForCalendarDay(LocalDate.of(2025, 12, 1))).extracting(a -> a.lineCode)
				.containsExactlyInAnyOrder("line1", "line2");
		assertThat(repository.getJourneyAggregationsForCalendarDay(LocalDate.of(2025, 12, 2))).extracting(a -> a.lineCode)
				.containsExactlyInAnyOrder("line3");
		assertThat(repository.getJourneyAggregationsForCalendarDay(LocalDate.of(2025, 12, 3))).isEmpty();
	}

	@Test
	public void testCanGetCallAggregationsForCalendarDay() {
		final CallAggregation aggregation1 = createCallAggregation(1, 1);
		final CallAggregation aggregation2 = createCallAggregation(2, 1);
		final CallAggregation aggregation3 = createCallAggregation(3, 1);
		aggregation1.calendarDay = LocalDate.of(2025, 12, 1).toString();
		aggregation2.calendarDay = LocalDate.of(2025, 12, 1).toString();
		aggregation3.calendarDay = LocalDate.of(2025, 12, 2).toString(); // other date

		repository.writeCallAggregations(List.of(aggregation1, aggregation2, aggregation3));

		assertThat(repository.getCallAggregationsForCalendarDay(LocalDate.of(2025, 12, 1))).extracting(a -> a.stopPlaceCode)
				.containsExactlyInAnyOrder("stop1", "stop2");
		assertThat(repository.getCallAggregationsForCalendarDay(LocalDate.of(2025, 12, 2))).extracting(a -> a.stopPlaceCode)
				.containsExactlyInAnyOrder("stop3");
		assertThat(repository.getCallAggregationsForCalendarDay(LocalDate.of(2025, 12, 3))).isEmpty();
	}

	@Test
	public void testCanGetRouteAggregationsForCalendarDay() {
		final RouteAggregation aggregation1 = createRouteAggregation(1, 1);
		final RouteAggregation aggregation2 = createRouteAggregation(2, 1);
		final RouteAggregation aggregation3 = createRouteAggregation(3, 1);
		aggregation1.calendarDay = LocalDate.of(2025, 12, 1).toString();
		aggregation2.calendarDay = LocalDate.of(2025, 12, 1).toString();
		aggregation3.calendarDay = LocalDate.of(2025, 12, 2).toString(); // other date

		repository.writeRouteAggregations(List.of(aggregation1, aggregation2, aggregation3));

		assertThat(repository.getRouteAggregationsForCalendarDay(LocalDate.of(2025, 12, 1))).extracting(a -> a.stopPlaces.getLast().code())
				.containsExactlyInAnyOrder("1", "2");
		assertThat(repository.getRouteAggregationsForCalendarDay(LocalDate.of(2025, 12, 2))).extracting(a -> a.stopPlaces.getLast().code())
				.containsExactlyInAnyOrder("3");
		assertThat(repository.getRouteAggregationsForCalendarDay(LocalDate.of(2025, 12, 3))).isEmpty();
	}

	@Test
	public void testCanDeleteDataForCalendarDay() {
		insertDataForDeleteTests();

		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 11, 30))).isFalse();
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 1))).isTrue();
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 2))).isTrue();
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 3))).isTrue();
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 4))).isFalse();

		repository.deleteDataForCalendarDay(LocalDate.of(2025, 12, 2));

		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 11, 30))).isFalse();
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 1))).isTrue();
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 2))).isFalse();
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 3))).isTrue();
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 4))).isFalse();
	}

	@Test
	public void testCanDeleteDataUpToCalendarDay() {
		insertDataForDeleteTests();

		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 11, 30))).isFalse();
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 1))).isTrue();
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 2))).isTrue();
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 3))).isTrue();
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 4))).isFalse();

		repository.deleteDataUpToCalendarDay(LocalDate.of(2025, 12, 2));

		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 11, 30))).isFalse();
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 1))).isFalse();
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 2))).isFalse();
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 3))).isTrue();
		assertThat(repository.containsDataForCalendarDay(LocalDate.of(2025, 12, 4))).isFalse();
	}

	private void insertDataForDeleteTests() {
		{
			final JourneyWithCalls journey1 = createJourney(1);
			final JourneyWithCalls journey2 = createJourney(2);
			final JourneyWithCalls journey3 = createJourney(3);
			journey1.calendarDay = LocalDate.of(2025, 12, 1).toString();
			journey2.calendarDay = LocalDate.of(2025, 12, 2).toString();
			journey3.calendarDay = LocalDate.of(2025, 12, 3).toString();
			repository.writeJourneys(List.of(journey1, journey2, journey3));
		}
		{
			final CallWithJourney call1 = createCall(1);
			final CallWithJourney call2 = createCall(2);
			final CallWithJourney call3 = createCall(3);
			call1.calendarDay = LocalDate.of(2025, 12, 1).toString();
			call2.calendarDay = LocalDate.of(2025, 12, 2).toString();
			call3.calendarDay = LocalDate.of(2025, 12, 3).toString();
			repository.writeCalls(List.of(call1, call2, call3));
		}
		{
			final JourneyAggregation aggregation1 = createJourneyAggregation(1, 1);
			final JourneyAggregation aggregation2 = createJourneyAggregation(1, 1);
			final JourneyAggregation aggregation3 = createJourneyAggregation(1, 1);
			aggregation1.calendarDay = LocalDate.of(2025, 12, 1).toString();
			aggregation2.calendarDay = LocalDate.of(2025, 12, 2).toString();
			aggregation3.calendarDay = LocalDate.of(2025, 12, 3).toString();
			repository.writeJourneyAggregations(List.of(aggregation1, aggregation2, aggregation3));
		}
		{
			final CallAggregation aggregation1 = createCallAggregation(1, 1);
			final CallAggregation aggregation2 = createCallAggregation(1, 1);
			final CallAggregation aggregation3 = createCallAggregation(1, 1);
			aggregation1.calendarDay = LocalDate.of(2025, 12, 1).toString();
			aggregation2.calendarDay = LocalDate.of(2025, 12, 2).toString();
			aggregation3.calendarDay = LocalDate.of(2025, 12, 3).toString();
			repository.writeCallAggregations(List.of(aggregation1, aggregation2, aggregation3));
		}
		{
			final RouteAggregation aggregation1 = createRouteAggregation(1, 1);
			final RouteAggregation aggregation2 = createRouteAggregation(1, 1);
			final RouteAggregation aggregation3 = createRouteAggregation(1, 1);
			aggregation1.calendarDay = LocalDate.of(2025, 12, 1).toString();
			aggregation2.calendarDay = LocalDate.of(2025, 12, 2).toString();
			aggregation3.calendarDay = LocalDate.of(2025, 12, 3).toString();
			repository.writeRouteAggregations(List.of(aggregation1, aggregation2, aggregation3));
		}
	}
}
