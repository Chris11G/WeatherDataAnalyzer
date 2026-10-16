package com.weather;

import java.time.LocalDate;
import java.time.Month;
import java.util.Objects;

/// One day of weather observations, read from a single row of `weatherdata.csv`.
///
/// This is a Java **record** (final in Java 16): the compiler generates the
/// constructor, accessors, `equals`, `hashCode` and `toString` for us, so the
/// type stays immutable and needs no explicit class.
///
/// | Component       | Unit | Example      |
/// |-----------------|------|--------------|
/// | `date`          | ISO  | `2023-08-01` |
/// | `temperature`   | °C   | `32.5`       |
/// | `humidity`      | %    | `65`         |
/// | `precipitation` | mm   | `0.2`        |
///
/// Example:
///
/// ```java
/// var day = new WeatherRecord(LocalDate.of(2023, 8, 1), 32.5, 65, 0.0);
/// day.month();     // AUGUST
/// day.isRainy();   // false
/// day.category();  // Hot[temperature=32.5]
/// ```
///
/// @param date          the day the readings were taken
/// @param temperature   air temperature in degrees Celsius
/// @param humidity      relative humidity in percent, `0`–`100`
/// @param precipitation rainfall in millimetres, never negative
public record WeatherRecord(LocalDate date, double temperature, double humidity, double precipitation) {

    /// Compact canonical constructor that validates every component.
    ///
    /// ```java
    /// new WeatherRecord(LocalDate.now(), 20, 150, 0); // throws IllegalArgumentException
    /// ```
    ///
    /// @throws NullPointerException     if `date` is `null`
    /// @throws IllegalArgumentException if a reading is out of range or not a number
    public WeatherRecord {
        Objects.requireNonNull(date, "date must not be null");
        if (Double.isNaN(temperature)) {
            throw new IllegalArgumentException("temperature must be a number");
        }
        if (humidity < 0 || humidity > 100) {
            throw new IllegalArgumentException("humidity must be between 0 and 100: " + humidity);
        }
        if (precipitation < 0) {
            throw new IllegalArgumentException("precipitation cannot be negative: " + precipitation);
        }
    }

    /// Returns `true` when any rain fell on this day (precipitation above `0.0` mm).
    ///
    /// ```java
    /// new WeatherRecord(LocalDate.of(2023, 8, 2), 35.0, 60, 0.2).isRainy(); // true
    /// ```
    ///
    /// @return whether the day counts as a rainy day
    public boolean isRainy() {
        return precipitation > 0.0;
    }

    /// Returns the calendar month of this record, used for monthly averages.
    ///
    /// @return the month of [#date()]
    public Month month() {
        return date.getMonth();
    }

    /// Classifies this day's temperature as Hot, Warm, Mild or Cold.
    ///
    /// Delegates to [WeatherCategory#of(double)], which uses an enhanced `switch`.
    ///
    /// @return the temperature category for this day
    public WeatherCategory category() {
        return WeatherCategory.of(temperature);
    }
}
