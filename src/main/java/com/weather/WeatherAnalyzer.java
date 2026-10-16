package com.weather;

import java.time.Month;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/// All weather calculations, written with **lambdas and streams**.
///
/// Every method is a pure `static` function: it takes a list of
/// [WeatherRecord]s and returns a result without changing the input.
///
/// Example:
///
/// ```java
/// var data = WeatherCsvParser.parse(Path.of("weatherdata.csv"));
///
/// WeatherAnalyzer.averageTemperature(data, Month.AUGUST); // OptionalDouble[25.27...]
/// WeatherAnalyzer.daysAboveThreshold(data, 30.0).size();  // 5
/// WeatherAnalyzer.countRainyDays(data);                   // 132
/// ```
public interface WeatherAnalyzer {

    /// Average temperature for one month (all years in the file combined).
    ///
    /// ```java
    /// OptionalDouble avg = WeatherAnalyzer.averageTemperature(data, Month.AUGUST);
    /// avg.ifPresent(a -> System.out.printf("%.1f °C%n", a));
    /// ```
    ///
    /// @param data  the records to analyse
    /// @param month the month to average
    /// @return the average in °C, or empty if the month has no data
    static OptionalDouble averageTemperature(List<WeatherRecord> data, Month month) {
        return data.stream()
                .filter(r -> r.month() == month)
                .mapToDouble(WeatherRecord::temperature)
                .average();
    }

    /// All days whose temperature is **strictly above** the threshold.
    ///
    /// ```java
    /// List<WeatherRecord> hot = WeatherAnalyzer.daysAboveThreshold(data, 30.0);
    /// hot.forEach(r -> System.out.println(r.date() + " " + r.temperature()));
    /// ```
    ///
    /// @param data      the records to analyse
    /// @param threshold temperature in °C
    /// @return matching days, in their original order
    static List<WeatherRecord> daysAboveThreshold(List<WeatherRecord> data, double threshold) {
        return data.stream()
                .filter(aboveThreshold(threshold))
                .toList();
    }

    /// A reusable predicate (a lambda stored in a variable) for "warmer than `threshold`".
    ///
    /// ```java
    /// Predicate<WeatherRecord> over30 = WeatherAnalyzer.aboveThreshold(30);
    /// long n = data.stream().filter(over30).count();
    /// ```
    ///
    /// @param threshold temperature in °C
    /// @return a predicate testing `temperature > threshold`
    static Predicate<WeatherRecord> aboveThreshold(double threshold) {
        return r -> r.temperature() > threshold;
    }

    /// Number of days with any precipitation.
    ///
    /// ```java
    /// long rainy = WeatherAnalyzer.countRainyDays(data);
    /// ```
    ///
    /// @param data the records to analyse
    /// @return count of days where [WeatherRecord#isRainy()] is `true`
    static long countRainyDays(List<WeatherRecord> data) {
        return data.stream().filter(WeatherRecord::isRainy).count();
    }

    /// Number of rainy days in a given month.
    ///
    /// @param data  the records to analyse
    /// @param month the month to count
    /// @return count of rainy days in that month
    static long countRainyDays(List<WeatherRecord> data, Month month) {
        return data.stream()
                .filter(r -> r.month() == month)
                .filter(WeatherRecord::isRainy)
                .count();
    }

    /// Average temperature for every month present, in calendar order.
    ///
    /// ```java
    /// WeatherAnalyzer.monthlyAverages(data)
    ///         .forEach((m, avg) -> System.out.println(m + " -> " + avg));
    /// ```
    ///
    /// @param data the records to analyse
    /// @return a sorted map of month to average °C
    static Map<Month, Double> monthlyAverages(List<WeatherRecord> data) {
        return data.stream()
                .collect(Collectors.groupingBy(
                        WeatherRecord::month,
                        TreeMap::new,
                        Collectors.averagingDouble(WeatherRecord::temperature)));
    }

    /// How many days fall into each [WeatherCategory].
    ///
    /// ```java
    /// WeatherAnalyzer.categoryCounts(data); // {Cold=154, Hot=5, Mild=91, Warm=115}
    /// ```
    ///
    /// @param data the records to analyse
    /// @return category label mapped to number of days, sorted by label
    static Map<String, Long> categoryCounts(List<WeatherRecord> data) {
        return data.stream()
                .collect(Collectors.groupingBy(
                        r -> r.category().label(),
                        TreeMap::new,
                        Collectors.counting()));
    }

    /// Min / max / average / count of temperatures in one pass.
    ///
    /// @param data the records to analyse
    /// @return summary statistics for the temperature column
    static DoubleSummaryStatistics temperatureStats(List<WeatherRecord> data) {
        return data.stream().mapToDouble(WeatherRecord::temperature).summaryStatistics();
    }

    /// The single hottest day.
    ///
    /// @param data the records to analyse
    /// @return the record with the highest temperature, or empty for no data
    static Optional<WeatherRecord> hottestDay(List<WeatherRecord> data) {
        return data.stream().max(Comparator.comparingDouble(WeatherRecord::temperature));
    }

    /// The single coldest day.
    ///
    /// @param data the records to analyse
    /// @return the record with the lowest temperature, or empty for no data
    static Optional<WeatherRecord> coldestDay(List<WeatherRecord> data) {
        return data.stream().min(Comparator.comparingDouble(WeatherRecord::temperature));
    }

    /// Total rainfall in millimetres.
    ///
    /// @param data the records to analyse
    /// @return sum of all precipitation values
    static double totalPrecipitation(List<WeatherRecord> data) {
        return data.stream().mapToDouble(WeatherRecord::precipitation).sum();
    }

    /// Describes a day's rain using **record deconstruction patterns** (Java 21):
    /// the `switch` pulls the components straight out of the record.
    ///
    /// ```java
    /// WeatherAnalyzer.describeRain(new WeatherRecord(LocalDate.of(2023, 8, 2), 35, 60, 0.2));
    /// // "Light rain"
    /// ```
    ///
    /// @param day the day to describe
    /// @return `"Dry"`, `"Light rain"`, `"Rain"`, `"Heavy rain"` or `"Humid, no rain"`
    static String describeRain(WeatherRecord day) {
        return switch (day) {
            case WeatherRecord(var date, var temp, var humidity, var rain) when rain >= 10.0 -> "Heavy rain";
            case WeatherRecord(var date, var temp, var humidity, var rain) when rain >= 2.5 -> "Rain";
            case WeatherRecord(var date, var temp, var humidity, var rain) when rain > 0.0 -> "Light rain";
            case WeatherRecord(var date, var temp, var humidity, var rain) when humidity >= 80 -> "Humid, no rain";
            case WeatherRecord r -> "Dry";
        };
    }

    /// The month that has the most records, used to pick a sensible default
    /// in the GUI.
    ///
    /// @param data the records to analyse
    /// @return the busiest month, or empty for no data
    static Optional<Month> mostRecordedMonth(List<WeatherRecord> data) {
        return data.stream()
                .collect(Collectors.groupingBy(WeatherRecord::month, TreeMap::new, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }
}
