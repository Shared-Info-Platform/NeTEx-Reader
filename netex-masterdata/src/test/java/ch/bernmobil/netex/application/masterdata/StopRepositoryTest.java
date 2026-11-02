package ch.bernmobil.netex.application.masterdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.bernmobil.netex.application.masterdata.StopRepository.StopStorage;
import ch.bernmobil.netex.application.masterdata.model.Stop;
import ch.bernmobil.netex.application.masterdata.model.StopPoint;

public class StopRepositoryTest {

	private StopRepository stopRepository;

	@BeforeEach
	public void setup() {
		stopRepository = new StopRepository();
	}

	@Test
	public void testCanGetDidokForSloid() {
		final Stop stop = createStop();
		setStorageWithStops(stop);

		assertThat(stopRepository.getDidokForSloid("ch:1:sloid:8450")).isEqualTo("8508450");
		assertThat(stopRepository.getDidokForSloid("ch:1:sloid:8450::1")).isEqualTo("850845001");
		assertThat(stopRepository.getDidokForSloid("ch:1:sloid:8450::2")).isEqualTo("850845002");
	}

	@Test
	public void testCanGetStopBySloid() {
		final Stop stop = createStop();
		setStorageWithStops(stop);

		assertThat(stopRepository.getSloidForDidok("8508450")).isEqualTo("ch:1:sloid:8450");
		assertThat(stopRepository.getSloidForDidok("850845001")).isEqualTo("ch:1:sloid:8450::1");
		assertThat(stopRepository.getSloidForDidok("850845002")).isEqualTo("ch:1:sloid:8450::2");
	}

	@Test
	public void testIgnoresStopWithoutDidok() {
		final Stop stop = createStop();
		stop.setDidok(null);
		setStorageWithStops(stop);

		assertThat(stopRepository.getDidokForSloid("ch:1:sloid:8450")).isNull();
		assertThat(stopRepository.getDidokForSloid("ch:1:sloid:8450::1")).isEqualTo("850845001");
		assertThat(stopRepository.getDidokForSloid("ch:1:sloid:8450::2")).isEqualTo("850845002");
	}

	@Test
	public void testIgnoresStopPointWithoutDidok() {
		final Stop stop = createStop();
		stop.getStopPoints().get(0).setDidok(null);
		setStorageWithStops(stop);

		assertThat(stopRepository.getDidokForSloid("ch:1:sloid:8450")).isEqualTo("8508450");
		assertThat(stopRepository.getDidokForSloid("ch:1:sloid:8450::1")).isNull();
		assertThat(stopRepository.getDidokForSloid("ch:1:sloid:8450::2")).isEqualTo("850845002");
	}

	@Test
	public void testIgnoresStopWithInvalidDidok() {
		final Stop stop = createStop();
		stop.setDidok("A");
		setStorageWithStops(stop);

		assertThat(stopRepository.getDidokForSloid("ch:1:sloid:8450")).isNull();
		assertThat(stopRepository.getDidokForSloid("ch:1:sloid:8450::1")).isEqualTo("850845001");
		assertThat(stopRepository.getDidokForSloid("ch:1:sloid:8450::2")).isEqualTo("850845002");
	}

	@Test
	public void testIgnoresStopPointWithInvalidDidok() {
		final Stop stop = createStop();
		stop.getStopPoints().get(0).setDidok("85084500000000001");
		setStorageWithStops(stop);

		assertThat(stopRepository.getDidokForSloid("ch:1:sloid:8450")).isEqualTo("8508450");
		assertThat(stopRepository.getDidokForSloid("ch:1:sloid:8450::1")).isNull();
		assertThat(stopRepository.getDidokForSloid("ch:1:sloid:8450::2")).isEqualTo("850845002");
	}

	@Test
	public void testIgnoresStopWithoutSloid() {
		final Stop stop = createStop();
		stop.setSloid(null);
		setStorageWithStops(stop);

		assertThat(stopRepository.getSloidForDidok("8508450")).isNull();
		assertThat(stopRepository.getSloidForDidok("850845001")).isEqualTo("ch:1:sloid:8450::1");
		assertThat(stopRepository.getSloidForDidok("850845002")).isEqualTo("ch:1:sloid:8450::2");
	}

	@Test
	public void testIgnoresStopPointWithoutSloid() {
		final Stop stop = createStop();
		stop.getStopPoints().get(0).setSloid(null);
		setStorageWithStops(stop);

		assertThat(stopRepository.getSloidForDidok("8508450")).isEqualTo("ch:1:sloid:8450");
		assertThat(stopRepository.getSloidForDidok("850845001")).isNull();
		assertThat(stopRepository.getSloidForDidok("850845002")).isEqualTo("ch:1:sloid:8450::2");
	}

	@Test
	public void testReturnsSecondStopOnDuplicateDidok() {
		final Stop stop1 = new Stop();
		stop1.setDidok("8508450");
		stop1.setSloid("ch:1:sloid:1111");
		final Stop stop2 = new Stop();
		stop2.setDidok("8508450");
		stop2.setSloid("ch:1:sloid:2222");
		setStorageWithStops(stop1, stop2);

		assertThat(stopRepository.getSloidForDidok("8508450")).isEqualTo("ch:1:sloid:2222");
	}

	@Test
	public void testReturnsSecondStopOnDuplicateSloid() {
		final Stop stop1 = new Stop();
		stop1.setDidok("8501111");
		stop1.setSloid("ch:1:sloid:8450");
		final Stop stop2 = new Stop();
		stop2.setDidok("8502222");
		stop2.setSloid("ch:1:sloid:8450");
		setStorageWithStops(stop1, stop2);

		assertThat(stopRepository.getDidokForSloid("ch:1:sloid:8450")).isEqualTo("8502222");
	}

	@Test
	public void testReturnsSecondStopPointOnDuplicateDidok() {
		final StopPoint stopPoint1 = new StopPoint();
		stopPoint1.setDidok("850845001");
		stopPoint1.setSloid("ch:1:sloid:8450::1");
		final StopPoint stopPoint2 = new StopPoint();
		stopPoint2.setDidok("850845001");
		stopPoint2.setSloid("ch:1:sloid:8450::2");
		final Stop stop = new Stop();
		stop.setStopPoints(List.of(stopPoint1, stopPoint2));
		setStorageWithStops(stop);

		assertThat(stopRepository.getSloidForDidok("850845001")).isEqualTo("ch:1:sloid:8450::2");
	}

	@Test
	public void testReturnsSecondStopPointOnDuplicateSloid() {
		final StopPoint stopPoint1 = new StopPoint();
		stopPoint1.setDidok("850845001");
		stopPoint1.setSloid("ch:1:sloid:8450::1");
		final StopPoint stopPoint2 = new StopPoint();
		stopPoint2.setDidok("850845002");
		stopPoint2.setSloid("ch:1:sloid:8450::1");
		final Stop stop = new Stop();
		stop.setStopPoints(List.of(stopPoint1, stopPoint2));
		setStorageWithStops(stop);

		assertThat(stopRepository.getDidokForSloid("ch:1:sloid:8450::1")).isEqualTo("850845002");
	}

	private void setStorageWithStops(Stop ... stops) {
		final StopStorage storage = new StopStorage();
		for (final Stop stop : stops) {
			storage.addStop(stop);
		}
		stopRepository.setStopStorage(storage);
	}

	private static Stop createStop() {
		final Stop stop = new Stop();
		stop.setDidok("8508450");
		stop.setSloid("ch:1:sloid:8450");

		final StopPoint stopPoint1 = new StopPoint();
		stopPoint1.setDidok("850845001");
		stopPoint1.setSloid("ch:1:sloid:8450::1");
		stop.getStopPoints().add(stopPoint1);
		final StopPoint stopPoint2 = new StopPoint();
		stopPoint2.setDidok("850845002");
		stopPoint2.setSloid("ch:1:sloid:8450::2");
		stop.getStopPoints().add(stopPoint2);

		return stop;
	}
}
