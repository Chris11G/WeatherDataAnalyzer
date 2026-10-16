package com.weather;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/// Builds the plain-text weather report using **text blocks** (Java 15).
///
/// A text block (`"""`) keeps multi-line output readable in the source, and
/// `String.formatted(...)` fills in the values.
///
/// ```java
/// String report = ReportFormatter.fullReport(data, Month.AUGUST, 30.0);
/// System.out.println(report);
/// ```
public interface ReportFormatter {

    /// The complete report shown in the GUI "Report" tab and printed by `--console`.
    ///
    /// ```java
    /// System.out.println(ReportFormatter.fullReport(data, Month.JULY, 28));
    /// ```
    ///
    /// @param data      the records to report on
    /// @param month     month used for the average and rainy-day count
    /// @param threshold temperature threshold in °C
    /// @return a multi-line report
    static String fullReport(List<WeatherRecord> data, Month month, double threshold) {
        if (data.isEmpty()) {
            return """
                    No weather data loaded.
                    Open a CSV file with the columns: Date,Temperature,Humidity,Precipitation
                    """;
        }
        var stats = WeatherAnalyzer.temperatureStats(data);
        var hotDays = WeatherAnalyzer.daysAboveThreshold(data, threshold);
        String monthName = monthName(month);
        String average = WeatherAnalyzer.averageTemperature(data, month)
                .stream()
                .mapToObj(a -> "%.1f °C (%s)".formatted(a, WeatherCategory.of(a).label()))
                .findFirst()
                .orElse("no data for this month");

        String hotList = hotDays.stream()
                .limit(15)
                .map(r -> "    • %s  %5.1f °C  %s".formatted(r.date(), r.temperature(), r.category().label()))
                .collect(Collectors.joining("\n"));
        if (hotDays.size() > 15) {
            hotList += "\n    … and %d more".formatted(hotDays.size() - 15);
        }

        String categories = WeatherAnalyzer.categoryCounts(data).entrySet().stream()
                .map(e -> "    %-5s %4d days".formatted(e.getKey(), e.getValue()))
                .collect(Collectors.joining("\n"));

        return """
                ==============================================
                           WEATHER DATA ANALYSIS
                ==============================================
                Records analysed : %d  (%s to %s)
                Temperature      : min %.1f °C | avg %.1f °C | max %.1f °C
                Total rainfall   : %.1f mm

                --- %s ---
                Average temperature : %s
                Rainy days          : %d

                --- Days above %.1f °C : %d ---
                %s

                --- Rainy days (whole file) : %d ---

                --- Weather categories ---
                %s
                ==============================================
                """.formatted(
                data.size(), data.getFirst().date(), data.getLast().date(),
                stats.getMin(), stats.getAverage(), stats.getMax(),
                WeatherAnalyzer.totalPrecipitation(data),
                monthName,
                average,
                WeatherAnalyzer.countRainyDays(data, month),
                threshold, hotDays.size(),
                hotList.isEmpty() ? "    (none)" : hotList,
                WeatherAnalyzer.countRainyDays(data),
                categories);
    }

    /// Full English month name, e.g. `AUGUST` becomes `"August"`.
    ///
    /// @param month the month
    /// @return the display name
    static String monthName(Month month) {
        return month.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }
}
