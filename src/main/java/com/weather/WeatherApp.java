package com.weather;

import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Month;
import java.util.List;

/// Entry point of the Weather Data Analyzer.
///
/// `main` lives in an **interface**, so the project has no explicit classes.
/// The GUI is started with `Platform.startup(...)` instead of extending
/// `javafx.application.Application`.
///
/// ## Running
///
/// ```shell
/// mvn javafx:run                               # GUI with weatherdata.csv
/// mvn compile exec:java -Dexec.args="--console" # text report only
/// ```
///
/// ## Command-line options
///
/// | Option            | Meaning                                 | Default            |
/// |-------------------|-----------------------------------------|--------------------|
/// | `path/to/file.csv`| CSV file to load                        | `weatherdata.csv`  |
/// | `--console`       | print the text-block report, no GUI     | off                |
/// | `--month=N`       | month number for the console report     | `8` (August)       |
/// | `--threshold=T`   | temperature threshold in °C             | `30`               |
public interface WeatherApp {

    /// Starts the GUI, or prints a console report when `--console` is passed.
    ///
    /// ```java
    /// WeatherApp.main(new String[] {"--console", "--month=7", "--threshold=28"});
    /// ```
    ///
    /// @param args command-line arguments (see the table above)
    static void main(String[] args) {
        List<String> arguments = List.of(args);
        Path csv = arguments.stream()
                .filter(a -> !a.startsWith("--"))
                .findFirst()
                .map(Path::of)
                .orElse(Path.of("weatherdata.csv"));

        if (arguments.contains("--console")) {
            Month month = Month.of((int) option(arguments, "--month=", 8));
            double threshold = option(arguments, "--threshold=", 30);
            printConsoleReport(csv, month, threshold);
        } else {
            Platform.startup(() -> WeatherDashboard.show(new Stage(), csv));
        }
    }

    /// Prints the full text report to standard output.
    ///
    /// @param csv       the CSV file to read
    /// @param month     the month to report on
    /// @param threshold the temperature threshold in °C
    static void printConsoleReport(Path csv, Month month, double threshold) {
        try {
            List<WeatherRecord> data = WeatherCsvParser.parse(csv);
            System.out.println(ReportFormatter.fullReport(data, month, threshold));
        } catch (IOException e) {
            System.err.println("""
                    Could not read "%s".
                    Make sure weatherdata.csv is in the project folder (next to pom.xml).
                    """.formatted(csv.toAbsolutePath()));
        }
    }

    /// Reads a numeric `--name=value` option, falling back to a default.
    ///
    /// ```java
    /// WeatherApp.option(List.of("--threshold=25"), "--threshold=", 30); // 25.0
    /// ```
    ///
    /// @param arguments    all command-line arguments
    /// @param prefix       the option prefix, including `=`
    /// @param defaultValue value used if the option is missing or invalid
    /// @return the parsed value
    static double option(List<String> arguments, String prefix, double defaultValue) {
        return arguments.stream()
                .filter(a -> a.startsWith(prefix))
                .map(a -> a.substring(prefix.length()))
                .flatMap(v -> {
                    try {
                        return java.util.stream.Stream.of(Double.parseDouble(v));
                    } catch (NumberFormatException e) {
                        return java.util.stream.Stream.empty();
                    }
                })
                .findFirst()
                .orElse(defaultValue);
    }
}
