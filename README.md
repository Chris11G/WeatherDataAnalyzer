# 🌦️ Weather Data Analyzer

A JavaFX desktop application that reads daily weather observations from a CSV file and analyses them with **modern Java** (Java 15 – 23): records, sealed interfaces, enhanced `switch`, pattern matching, text blocks, lambdas, streams, virtual threads, and Markdown Javadoc.

The whole project is written **without a single explicit class**. Every type is a `record` or an `interface`, and all behaviour is expressed with static methods and lambdas.

> **Course:** CSC311 – Advanced Programming · Module 6 Assignment
> **Author:** _Your Name_ · **Date:** October 2026

---

## Table of contents

1. [Features](#features)
2. [Screenshots](#screenshots)
3. [Requirements](#requirements)
4. [Getting started in IntelliJ IDEA](#getting-started-in-intellij-idea)
5. [Running from the command line](#running-from-the-command-line)
6. [CSV file format](#csv-file-format)
7. [Project structure](#project-structure)
8. [Design: no explicit classes](#design-no-explicit-classes)
9. [Modern Java features used](#modern-java-features-used)
10. [Documentation (Markdown Javadoc)](#documentation-markdown-javadoc)
11. [Assignment checklist](#assignment-checklist)

---

## Features

### Analysis
| Feature | Where it lives |
|---|---|
| Parse temperature, humidity and precipitation from `weatherdata.csv` | `WeatherCsvParser.parse(Path)` |
| **Average temperature for a specific month** | `WeatherAnalyzer.averageTemperature(data, Month)` |
| **Days with temperatures above a given threshold** | `WeatherAnalyzer.daysAboveThreshold(data, double)` |
| **Count of rainy days** (whole file and per month) | `WeatherAnalyzer.countRainyDays(...)` |
| Categorise each day as **Hot / Warm / Mild / Cold** with an enhanced `switch` | `WeatherCategory.of(double)` |
| Monthly averages, category counts, min/max, hottest/coldest day, total rainfall | `WeatherAnalyzer` |
| Rain description (`Dry`, `Light rain`, `Rain`, `Heavy rain`) using record patterns | `WeatherAnalyzer.describeRain(...)` |
| Full text report built with a text block | `ReportFormatter.fullReport(...)` |

### GUI (JavaFX + CSS) — extra credit
- Gradient header, stat **cards** that update live when you change the month or threshold
- **Data tab:** sortable table, rows coloured by category (🔥 Hot · ☀️ Warm · ⛅ Mild · ❄️ Cold), plus a "only days above threshold" filter
- **Charts tab:** bar chart of monthly averages (bars coloured by category), pie chart of category counts, and a daily temperature line with a dashed threshold line
- **Report tab:** the text-block report in a dark monospace panel with a *Copy report* button
- **Open CSV…** button and **drag-and-drop** of any CSV file onto the window
- File loading runs on a **virtual thread**, so the window never freezes
- All styling is in [`styles.css`](src/main/resources/com/weather/styles.css)

---

## Screenshots

> Add your own screenshots after running the app (e.g. save them to a `docs/` folder):
>
> `![Dashboard](docs/dashboard.png)`
> `![Charts](docs/charts.png)`

---

## Requirements

| Tool | Version |
|---|---|
| JDK | **23 or newer** (needed for Markdown `///` Javadoc; the code itself compiles on 21+) |
| JavaFX | 23.0.2 (downloaded automatically by Maven) |
| Maven | 3.8+ (bundled with IntelliJ) |
| IDE | IntelliJ IDEA (Community or Ultimate) |

> If you only have JDK 21 installed, change `<maven.compiler.release>23</maven.compiler.release>` in `pom.xml` to `21`. The program runs the same; only the Markdown Javadoc rendering requires JDK 23.

---

## Getting started in IntelliJ IDEA

1. **Clone** the repository: *File → New → Project from Version Control…* and paste the GitHub URL.
   (Or *File → Open…* and select the folder containing `pom.xml`.)
2. IntelliJ detects the **Maven** project and downloads JavaFX automatically. If not, open the Maven tool window and click **Reload All Maven Projects** (🔄).
3. Set the SDK: *File → Project Structure → Project → SDK →* a JDK 23+ (IntelliJ can download one for you).
4. Run it, either way:
   - Open the Maven tool window → **Plugins → javafx → javafx:run** (recommended), or
   - Open `src/main/java/com/weather/WeatherApp.java` and click the green ▶ next to `static void main`.
5. The window opens and loads `weatherdata.csv` from the project folder. Use **Open CSV…** to load a different file.

> **Tip:** if ▶ reports *"weatherdata.csv not found"*, make sure the run configuration's *Working directory* is the project folder (the one with `pom.xml`).

---

## Running from the command line

```bash
# GUI
mvn javafx:run

# Console-only text report (August, threshold 30 °C)
mvn compile exec:java -Dexec.args="--console"

# Console report for July with a 25 °C threshold and a different file
mvn compile exec:java -Dexec.args="--console --month=7 --threshold=25 data/other.csv"
```

Sample console output:

```text
==============================================
           WEATHER DATA ANALYSIS
==============================================
Records analysed : 365  (2023-01-01 to 2023-12-31)
Temperature      : min -5.0 °C | avg 13.6 °C | max 35.0 °C
Total rainfall   : 754.8 mm

--- August ---
Average temperature : 25.3 °C (Warm)
Rainy days          : 9

--- Days above 30.0 °C : 5 ---
    • 2023-06-26   30.3 °C  Hot
    • 2023-06-29   32.8 °C  Hot
    • 2023-08-01   32.5 °C  Hot
    • 2023-08-02   35.0 °C  Hot
    • 2023-08-07   31.7 °C  Hot

--- Rainy days (whole file) : 132 ---

--- Weather categories ---
    Cold   154 days
    Hot      5 days
    Mild    91 days
    Warm   115 days
==============================================
```

---

## CSV file format

`weatherdata.csv` (in the project root) contains one row per day for all of 2023:

```csv
Date,Temperature,Humidity,Precipitation
2023-08-01,32.5,65,0.0
2023-08-02,35.0,60,0.2
```

| Column | Type | Unit |
|---|---|---|
| `Date` | ISO date `yyyy-MM-dd` | – |
| `Temperature` | decimal | °C |
| `Humidity` | decimal `0–100` | % |
| `Precipitation` | decimal `>= 0` | mm |

A day is **rainy** when `Precipitation > 0`. The header row, blank lines, and malformed rows (bad dates, non-numbers, humidity over 100, etc.) are skipped safely instead of crashing the program.

---

## Project structure

```text
WeatherDataAnalyzer/
├── pom.xml                       Maven build (JavaFX, javafx:run, javadoc, exec)
├── weatherdata.csv               365 days of sample data
├── README.md
└── src/main/
    ├── java/com/weather/
    │   ├── WeatherApp.java         interface – main() entry point, GUI or --console
    │   ├── WeatherRecord.java      record    – one day of data + validation
    │   ├── WeatherCategory.java    sealed interface + 4 records (Hot/Warm/Mild/Cold)
    │   ├── WeatherCsvParser.java   interface – CSV parsing with streams + virtual thread
    │   ├── WeatherAnalyzer.java    interface – all calculations (lambdas & streams)
    │   ├── ReportFormatter.java    interface – text-block report
    │   └── WeatherDashboard.java   interface – JavaFX GUI built from lambdas
    └── resources/com/weather/
        └── styles.css              JavaFX CSS theme
```

---

## Design: no explicit classes

| Usual approach | What this project does instead |
|---|---|
| `class WeatherData { private fields, getters… }` | `record WeatherRecord(...)` |
| `enum` or class hierarchy for categories | `sealed interface WeatherCategory` with `record Hot`, `Warm`, `Mild`, `Cold` |
| Utility / service classes | `interface`s that contain only `static` methods |
| `class Main extends Application` | `interface WeatherApp { static void main(...) }` + `Platform.startup(...)` |
| Anonymous inner classes for event handlers, table cells, row factories | Lambdas and method references |

`grep -rn "class " src/main/java` only finds the word inside comments and `.class` literals; there is no `class` declaration anywhere.

---

## Modern Java features used

### Records (Java 16)
```java
public record WeatherRecord(LocalDate date, double temperature,
                            double humidity, double precipitation) {
    public WeatherRecord {                     // compact constructor validation
        if (humidity < 0 || humidity > 100) throw new IllegalArgumentException(...);
    }
    public boolean isRainy() { return precipitation > 0.0; }
}
```

### Enhanced `switch` with guarded patterns (Java 14 / 21)
```java
static WeatherCategory of(double temperature) {
    return switch (Double.valueOf(temperature)) {
        case Double t when t >= HOT_MIN  -> new Hot(t);
        case Double t when t >= WARM_MIN -> new Warm(t);
        case Double t when t >= MILD_MIN -> new Mild(t);
        case Double t                    -> new Cold(t);
    };
}
```

### Sealed interfaces + exhaustive pattern matching for `switch` (Java 17 / 21)
No `default` branch is needed, because the compiler knows all four categories:
```java
default String label() {
    return switch (this) {
        case Hot h  -> "Hot";
        case Warm w -> "Warm";
        case Mild m -> "Mild";
        case Cold c -> "Cold";
    };
}
```

### Record deconstruction patterns (Java 21)
```java
return switch (day) {
    case WeatherRecord(var date, var temp, var humidity, var rain) when rain >= 10.0 -> "Heavy rain";
    case WeatherRecord(var date, var temp, var humidity, var rain) when rain > 0.0   -> "Light rain";
    case WeatherRecord r -> "Dry";
};
```

### Text blocks (Java 15)
```java
return """
        --- %s ---
        Average temperature : %s
        Rainy days          : %d
        """.formatted(monthName, average, rainyDays);
```

### Lambdas and streams
```java
static Map<Month, Double> monthlyAverages(List<WeatherRecord> data) {
    return data.stream()
            .collect(Collectors.groupingBy(WeatherRecord::month, TreeMap::new,
                     Collectors.averagingDouble(WeatherRecord::temperature)));
}
```

### Virtual threads (Java 21)
```java
Thread.ofVirtual().name("csv-loader").start(() -> future.complete(parse(path)));
```

### Other modern APIs
`Stream.toList()` (16), `List.getFirst()/getLast()` (21, sequenced collections), `String.formatted` (15), `Predicate.not` (11), `Optional.stream`.

---

## Documentation (Markdown Javadoc)

Every type and method is documented with **Markdown doc comments** (`///`), including tables and fenced code examples:

```java
/// Average temperature for one month.
///
/// ```java
/// OptionalDouble avg = WeatherAnalyzer.averageTemperature(data, Month.AUGUST);
/// ```
///
/// @param data  the records to analyse
/// @param month the month to average
/// @return the average in °C, or empty if the month has no data
```

> Note on versions: code examples in Javadoc via `{@snippet}` arrived in **JDK 18** (JEP 413), and full **Markdown** documentation comments with `///` arrived in **JDK 23** (JEP 467). This project uses the Markdown style, which is why JDK 23 is recommended.

Generate the HTML documentation with:

```bash
mvn javadoc:javadoc
# open target/reports/apidocs/index.html
```

---

## Assignment checklist

- [x] Parse temperature, humidity, precipitation from a CSV file
- [x] Average temperature for a specific month
- [x] Days with temperature above a threshold
- [x] Count of rainy days
- [x] Enhanced `switch` for Hot / Warm / (Mild) / Cold categories
- [x] Lambdas and streams throughout
- [x] Java records for weather data
- [x] Text blocks for output
- [x] Pattern matching for `switch`, record patterns, sealed interfaces, virtual threads
- [x] **No explicit classes** (records and interfaces only)
- [x] Markdown Javadoc with code examples for all methods
- [x] Extra credit: JavaFX GUI styled with CSS
- [x] README.md
