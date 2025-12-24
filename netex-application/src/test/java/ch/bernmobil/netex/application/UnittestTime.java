package ch.bernmobil.netex.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.time.ZoneId;

public class UnittestTime implements InstantSource {

  private Instant currentInstant;

  public UnittestTime(Instant currentInstant) {
    this.currentInstant = currentInstant;
  }

  @Override
  public Instant instant() {
    return currentInstant;
  }

  public void set(Instant currentInstant) {
    this.currentInstant = currentInstant;
  }

  public void advance(Duration duration) {
    this.currentInstant = this.currentInstant.plus(duration);
  }

  public Clock createClock() {
    return withZone(ZoneId.systemDefault());
  }
}
