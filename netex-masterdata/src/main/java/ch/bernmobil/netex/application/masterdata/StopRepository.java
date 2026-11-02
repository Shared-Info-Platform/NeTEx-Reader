package ch.bernmobil.netex.application.masterdata;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.bernmobil.netex.application.masterdata.model.Stop;
import ch.bernmobil.netex.application.masterdata.model.StopPoint;

public class StopRepository {

	private static final Logger logger = LoggerFactory.getLogger(StopRepository.class);

	private StopStorage stopStorage = new StopStorage();

	public void setStopStorage(StopStorage stopStorage) {
		this.stopStorage = stopStorage;
	}

	public String getSloidForDidok(String didok) {
		return stopStorage.sloidByDidok.get(didok);
	}

	public String getDidokForSloid(String sloid) {
		return stopStorage.didokBySloid.get(sloid);
	}

	public static class StopStorage {
		private static final Pattern DIDOK_STOP_PATTERN = Pattern.compile("\\d{7}");
		private static final Pattern DIDOK_STOP_POINT_PATTERN = Pattern.compile("(\\d{7})\\d{2}");

		private final Map<String, String> sloidByDidok = new HashMap<>();
		private final Map<String, String> didokBySloid = new HashMap<>();

		public void addStop(Stop stop) {
			if (stop.getDidok() != null && DIDOK_STOP_PATTERN.matcher(stop.getDidok()).matches() && stop.getSloid() != null) {
				if (sloidByDidok.put(stop.getDidok(), stop.getSloid()) != null) {
					logger.warn("duplicate stop for didok {}", stop.getDidok());
				}
				if (didokBySloid.put(stop.getSloid(), stop.getDidok()) != null) {
					logger.warn("duplicate stop for sloid {}", stop.getSloid());
				}
			}

			for (final StopPoint stopPoint : stop.getStopPoints()) {
				if (stopPoint.getDidok() != null && DIDOK_STOP_POINT_PATTERN.matcher(stopPoint.getDidok()).matches()
						&& stopPoint.getSloid() != null) {
					if (sloidByDidok.put(stopPoint.getDidok(), stopPoint.getSloid()) != null) {
						logger.warn("duplicate stop point for didok {}", stopPoint.getDidok());
					}
					if (didokBySloid.put(stopPoint.getSloid(), stopPoint.getDidok()) != null) {
						logger.warn("duplicate stop point for sloid {}", stopPoint.getSloid());
					}
				}
			}
		}
	}
}
