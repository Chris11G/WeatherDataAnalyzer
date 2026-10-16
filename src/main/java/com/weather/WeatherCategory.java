package com.weather;

/// A temperature category: **Hot**, **Warm**, **Mild** or **Cold**.
///
/// Modelled as a `sealed` interface whose only permitted implementations are
/// records. Because the compiler knows every subtype, a pattern-matching
/// `switch` over a `WeatherCategory` is checked for exhaustiveness and needs
/// no `default` branch (Java 21 pattern matching for `switch`).
///
/// | Category | Temperature range |
/// |----------|-------------------|
/// | Hot      | `>= 30 °C`        |
/// | Warm     | `20 – 29.9 °C`    |
/// | Mild     | `10 – 19.9 °C`    |
/// | Cold     | `< 10 °C`         |
///
/// Example:
///
/// ```java
/// WeatherCategory c = WeatherCategory.of(32.5);
/// String text = switch (c) {
///     case WeatherCategory.Hot h  -> "Stay hydrated, it's " + h.temperature() + "°C";
///     case WeatherCategory.Warm w -> "Nice day";
///     case WeatherCategory.Mild m -> "Bring a light jacket";
///     case WeatherCategory.Cold k -> "Bundle up";
/// };
/// ```
public sealed interface WeatherCategory
        permits WeatherCategory.Hot, WeatherCategory.Warm, WeatherCategory.Mild, WeatherCategory.Cold {

    /// Lowest temperature (°C) that counts as Hot.
    double HOT_MIN = 30.0;
    /// Lowest temperature (°C) that counts as Warm.
    double WARM_MIN = 20.0;
    /// Lowest temperature (°C) that counts as Mild.
    double MILD_MIN = 10.0;

    /// The temperature that was classified, in °C.
    ///
    /// @return the original temperature reading
    double temperature();

    /// A day at or above [#HOT_MIN].
    ///
    /// @param temperature the reading in °C
    record Hot(double temperature) implements WeatherCategory { }

    /// A day from [#WARM_MIN] up to (not including) [#HOT_MIN].
    ///
    /// @param temperature the reading in °C
    record Warm(double temperature) implements WeatherCategory { }

    /// A day from [#MILD_MIN] up to (not including) [#WARM_MIN].
    ///
    /// @param temperature the reading in °C
    record Mild(double temperature) implements WeatherCategory { }

    /// A day below [#MILD_MIN].
    ///
    /// @param temperature the reading in °C
    record Cold(double temperature) implements WeatherCategory { }

    /// Classifies a temperature using an **enhanced `switch`** with guarded
    /// type patterns (`case Double d when ...`).
    ///
    /// ```java
    /// WeatherCategory.of(35.0); // Hot[temperature=35.0]
    /// WeatherCategory.of(22.1); // Warm[temperature=22.1]
    /// WeatherCategory.of(-3.0); // Cold[temperature=-3.0]
    /// ```
    ///
    /// @param temperature a temperature in °C
    /// @return the matching category record
    /// @throws IllegalArgumentException if `temperature` is `NaN`
    static WeatherCategory of(double temperature) {
        return switch (Double.valueOf(temperature)) {
            case Double t when t.isNaN() -> throw new IllegalArgumentException("temperature is NaN");
            case Double t when t >= HOT_MIN -> new Hot(t);
            case Double t when t >= WARM_MIN -> new Warm(t);
            case Double t when t >= MILD_MIN -> new Mild(t);
            case Double t -> new Cold(t);
        };
    }

    /// Human-readable name, produced by an exhaustive pattern-matching `switch`.
    ///
    /// ```java
    /// WeatherCategory.of(31).label(); // "Hot"
    /// ```
    ///
    /// @return `"Hot"`, `"Warm"`, `"Mild"` or `"Cold"`
    default String label() {
        return switch (this) {
            case Hot h -> "Hot";
            case Warm w -> "Warm";
            case Mild m -> "Mild";
            case Cold c -> "Cold";
        };
    }

    /// An emoji icon for the category, shown in the GUI table.
    ///
    /// @return a single emoji string
    default String icon() {
        return switch (this) {
            case Hot h -> "🔥";
            case Warm w -> "☀️";
            case Mild m -> "⛅";
            case Cold c -> "❄️";
        };
    }

    /// The CSS style class used to colour this category in the GUI
    /// (see `styles.css`).
    ///
    /// @return for example `"cat-hot"`
    default String styleClass() {
        return "cat-" + label().toLowerCase();
    }

    /// A short piece of advice for the day, using the record component
    /// directly from the matched pattern.
    ///
    /// @return a one-line suggestion
    default String advice() {
        return switch (this) {
            case Hot h when h.temperature() >= 35 -> "Extreme heat, stay indoors";
            case Hot h -> "Stay hydrated";
            case Warm w -> "Great day to be outside";
            case Mild m -> "Bring a light jacket";
            case Cold c when c.temperature() <= 0 -> "Freezing, watch for ice";
            case Cold c -> "Wear a warm coat";
        };
    }
}
