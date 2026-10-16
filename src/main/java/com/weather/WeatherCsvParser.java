package com.weather;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;

/// Reads `weatherdata.csv` into a list of [WeatherRecord]s.
///
/// The parser is an interface containing only `static` methods, so no class
/// is needed. Expected file format:
///
/// ```text
/// Date,Temperature,Humidity,Precipitation
/// 2023-08-01,32.5,65,0.0
/// 2023-08-02,35.0,60,0.2
/// ```
///
/// Blank lines, the header row, and malformed rows are skipped instead of
/// crashing the program.
public interface WeatherCsvParser {

    /// Parses an entire CSV file with a stream pipeline.
    ///
    /// ```java
    /// List<WeatherRecord> data = WeatherCsvParser.parse(Path.of("weatherdata.csv"));
    /// System.out.println(data.size() + " days loaded");
    /// ```
    ///
    /// @param path location of the CSV file
    /// @return an unmodifiable list of valid records, in file order
    /// @throws IOException if the file cannot be read
    static List<WeatherRecord> parse(Path path) throws IOException {
        try (Stream<String> lines = Files.lines(path)) {
            return parse(lines);
        }
    }

    /// Parses CSV rows that are already in memory. Useful for testing.
    ///
    /// ```java
    /// var data = WeatherCsvParser.parse(Stream.of(
    ///         "Date,Temperature,Humidity,Precipitation",
    ///         "2023-08-01,32.5,65,0.0"));
    /// // data.size() == 1
    /// ```
    ///
    /// @param lines the raw lines, header included or not
    /// @return the valid records
    static List<WeatherRecord> parse(Stream<String> lines) {
        return lines
                .map(String::strip)
                .filter(Predicate.not(String::isEmpty))
                .filter(Predicate.not(WeatherCsvParser::isHeader))
                .map(WeatherCsvParser::parseLine)
                .flatMap(Optional::stream)
                .toList();
    }

    /// Parses a file on a **virtual thread** (Java 21) so the JavaFX UI thread
    /// never blocks while reading from disk.
    ///
    /// ```java
    /// WeatherCsvParser.parseAsync(Path.of("weatherdata.csv"))
    ///         .thenAccept(data -> System.out.println(data.size()));
    /// ```
    ///
    /// @param path location of the CSV file
    /// @return a future completed with the parsed records, or exceptionally on I/O failure
    static CompletableFuture<List<WeatherRecord>> parseAsync(Path path) {
        var future = new CompletableFuture<List<WeatherRecord>>();
        Thread.ofVirtual().name("csv-loader").start(() -> {
            try {
                future.complete(parse(path));
            } catch (IOException | RuntimeException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /// Converts one CSV row into a record.
    ///
    /// ```java
    /// WeatherCsvParser.parseLine("2023-08-02,35.0,60,0.2"); // Optional[WeatherRecord[...]]
    /// WeatherCsvParser.parseLine("not,a,valid,row");        // Optional.empty
    /// ```
    ///
    /// @param line a single data row with four comma-separated values
    /// @return the record, or `Optional.empty()` if the row is malformed
    static Optional<WeatherRecord> parseLine(String line) {
        String[] parts = line.split(",");
        if (parts.length != 4) {
            return Optional.empty();
        }
        try {
            return Optional.of(new WeatherRecord(
                    LocalDate.parse(parts[0].strip()),
                    Double.parseDouble(parts[1].strip()),
                    Double.parseDouble(parts[2].strip()),
                    Double.parseDouble(parts[3].strip())));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            // NumberFormatException is a subclass of IllegalArgumentException
            return Optional.empty();
        }
    }

    /// Detects the header row (`Date,Temperature,...`).
    ///
    /// @param line a trimmed line from the file
    /// @return `true` if the line is the column header
    static boolean isHeader(String line) {
        return line.regionMatches(true, 0, "date", 0, 4);
    }
}
