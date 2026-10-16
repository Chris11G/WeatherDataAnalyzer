package com.weather;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Clipboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

/// The JavaFX user interface, built entirely from lambdas and `static` methods.
///
/// JavaFX normally asks you to `extend Application`, which would be an explicit
/// class. Instead, [WeatherApp] starts the toolkit with `Platform.startup(...)`
/// and calls [#show(Stage, Path)]. Every event handler, table column, and row
/// style below is a lambda, so the GUI needs no classes or anonymous classes.
///
/// Styling lives in `src/main/resources/com/weather/styles.css`.
///
/// ```java
/// Platform.startup(() -> WeatherDashboard.show(new Stage(), Path.of("weatherdata.csv")));
/// ```
public interface WeatherDashboard {

    /// CSS classes that are swapped on rows and labels depending on the category.
    List<String> CATEGORY_CLASSES = List.of("cat-hot", "cat-warm", "cat-mild", "cat-cold");

    /// Builds the window, wires up all controls, and loads `initialFile` if it exists.
    ///
    /// @param stage       the stage to fill (created on the JavaFX thread)
    /// @param initialFile CSV file to load at start-up
    static void show(Stage stage, Path initialFile) {
        ObservableList<WeatherRecord> data = FXCollections.observableArrayList();
        FilteredList<WeatherRecord> tableItems = new FilteredList<>(data);

        // ---------- header & toolbar ----------
        Label title = new Label("Weather Data Analyzer");
        title.getStyleClass().add("title");
        Label subtitle = new Label("Records • Streams • Lambdas • Pattern-matching switch");
        subtitle.getStyleClass().add("subtitle");
        Label fileLabel = new Label("No file loaded");
        fileLabel.getStyleClass().add("file-label");
        Button openButton = new Button("Open CSV…");
        openButton.getStyleClass().add("primary-button");
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(16, new VBox(2, title, subtitle), headerSpacer, fileLabel, openButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");

        ComboBox<String> monthBox = new ComboBox<>(FXCollections.observableArrayList(
                Arrays.stream(Month.values()).map(ReportFormatter::monthName).toList()));
        monthBox.getSelectionModel().select(Month.AUGUST.ordinal());
        Spinner<Double> thresholdSpinner = new Spinner<>(-40.0, 50.0, 30.0, 0.5);
        thresholdSpinner.setEditable(true);
        thresholdSpinner.setPrefWidth(100);
        // commit typed text when the spinner loses focus
        thresholdSpinner.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) {
                thresholdSpinner.increment(0);
            }
        });
        CheckBox onlyAbove = new CheckBox("Table: only days above threshold");

        HBox toolbar = new HBox(12,
                label("Month"), monthBox,
                label("Threshold (°C)"), thresholdSpinner,
                onlyAbove);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("toolbar");

        // ---------- stat cards ----------
        Label avgValue = new Label("–");
        Label avgCaption = new Label();
        Label aboveValue = new Label("–");
        Label aboveCaption = new Label();
        Label rainyValue = new Label("–");
        Label rainyCaption = new Label();
        Label rangeValue = new Label("–");
        Label rangeCaption = new Label();
        HBox cards = new HBox(16,
                card("Average temperature", avgValue, avgCaption, "card-avg"),
                card("Days above threshold", aboveValue, aboveCaption, "card-above"),
                card("Rainy days", rainyValue, rainyCaption, "card-rain"),
                card("Temperature range", rangeValue, rangeCaption, "card-range"));
        cards.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        // ---------- table ----------
        TableView<WeatherRecord> table = new TableView<>(tableItems);
        table.getColumns().addAll(List.of(
                column("Date", WeatherRecord::date),
                column("Temp (°C)", WeatherRecord::temperature),
                column("Humidity (%)", WeatherRecord::humidity),
                column("Rain (mm)", WeatherRecord::precipitation),
                column("Category", r -> r.category().icon() + "  " + r.category().label()),
                column("Conditions", WeatherAnalyzer::describeRain),
                column("Advice", r -> r.category().advice())));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Open a CSV file to see the weather data"));
        // colour each row by category: a plain TableRow plus a listener, no subclass needed
        table.setRowFactory(tv -> {
            TableRow<WeatherRecord> row = new TableRow<>();
            row.itemProperty().addListener((obs, old, item) -> {
                row.getStyleClass().removeAll(CATEGORY_CLASSES);
                if (item != null) {
                    row.getStyleClass().add(item.category().styleClass());
                }
            });
            return row;
        });

        // ---------- charts ----------
        BarChart<String, Number> monthlyChart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        monthlyChart.setTitle("Average temperature by month (°C)");
        monthlyChart.setLegendVisible(false);
        monthlyChart.setAnimated(false);

        PieChart categoryChart = new PieChart();
        categoryChart.setTitle("Days per category");
        categoryChart.setLabelsVisible(true);
        categoryChart.setLegendVisible(false);
        categoryChart.setAnimated(false);

        NumberAxis dayAxis = new NumberAxis();
        dayAxis.setLabel("Day #");
        LineChart<Number, Number> dailyChart = new LineChart<>(dayAxis, new NumberAxis());
        dailyChart.setTitle("Daily temperature (blue) vs. threshold (red dashed)");
        dailyChart.setCreateSymbols(false);
        dailyChart.setLegendVisible(false);
        dailyChart.setAnimated(false);

        GridPane chartGrid = new GridPane(16, 16);
        chartGrid.add(monthlyChart, 0, 0);
        chartGrid.add(categoryChart, 1, 0);
        chartGrid.add(dailyChart, 0, 1, 2, 1);
        chartGrid.setPadding(new Insets(12));
        chartGrid.getChildren().forEach(n -> {
            GridPane.setHgrow(n, Priority.ALWAYS);
            GridPane.setVgrow(n, Priority.ALWAYS);
        });

        // ---------- report ----------
        TextArea reportArea = new TextArea();
        reportArea.setEditable(false);
        reportArea.getStyleClass().add("report");
        Button copyButton = new Button("Copy report");
        copyButton.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(reportArea.getText());
            Clipboard.getSystemClipboard().setContent(content);
        });
        VBox reportPane = new VBox(10, reportArea, copyButton);
        reportPane.setPadding(new Insets(12));
        VBox.setVgrow(reportArea, Priority.ALWAYS);

        TabPane tabs = new TabPane(
                new Tab("Data", table),
                new Tab("Charts", chartGrid),
                new Tab("Report", reportPane));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        Label status = new Label("Ready");
        status.getStyleClass().add("status");

        // ---------- behaviour (all lambdas) ----------
        Runnable refreshStats = () -> {
            List<WeatherRecord> records = List.copyOf(data);
            Month month = Month.of(monthBox.getSelectionModel().getSelectedIndex() + 1);
            String monthShort = month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            double threshold = thresholdSpinner.getValue();

            avgValue.getStyleClass().removeAll(CATEGORY_CLASSES);
            WeatherAnalyzer.averageTemperature(records, month).ifPresentOrElse(avg -> {
                WeatherCategory category = WeatherCategory.of(avg);
                avgValue.setText("%.1f °C".formatted(avg));
                avgValue.getStyleClass().add(category.styleClass());
                avgCaption.setText("%s • %s %s".formatted(ReportFormatter.monthName(month),
                        category.icon(), category.label()));
            }, () -> {
                avgValue.setText("–");
                avgCaption.setText("No data for " + ReportFormatter.monthName(month));
            });

            aboveValue.setText(String.valueOf(WeatherAnalyzer.daysAboveThreshold(records, threshold).size()));
            aboveCaption.setText("days warmer than %.1f °C".formatted(threshold));

            rainyValue.setText(String.valueOf(WeatherAnalyzer.countRainyDays(records)));
            rainyCaption.setText("%d in %s • %.0f mm total".formatted(
                    WeatherAnalyzer.countRainyDays(records, month), monthShort,
                    WeatherAnalyzer.totalPrecipitation(records)));

            var stats = WeatherAnalyzer.temperatureStats(records);
            rangeValue.setText(records.isEmpty() ? "–"
                    : "%.1f° – %.1f°".formatted(stats.getMin(), stats.getMax()));
            rangeCaption.setText(WeatherAnalyzer.hottestDay(records)
                    .map(r -> "hottest: " + r.date())
                    .orElse(""));

            tableItems.setPredicate(onlyAbove.isSelected() ? WeatherAnalyzer.aboveThreshold(threshold) : null);
            reportArea.setText(ReportFormatter.fullReport(records, month, threshold));
            updateThresholdLine(dailyChart, records, threshold);
        };

        Runnable refreshCharts = () -> {
            List<WeatherRecord> records = List.copyOf(data);

            XYChart.Series<String, Number> monthly = new XYChart.Series<>();
            WeatherAnalyzer.monthlyAverages(records).forEach((month, avg) -> {
                XYChart.Data<String, Number> bar = new XYChart.Data<>(
                        month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), avg);
                styleWhenShown(bar.nodeProperty().get(), bar::nodeProperty, WeatherCategory.of(avg).styleClass());
                monthly.getData().add(bar);
            });
            monthlyChart.getData().setAll(List.of(monthly));

            categoryChart.getData().setAll(WeatherAnalyzer.categoryCounts(records).entrySet().stream()
                    .map(e -> {
                        PieChart.Data slice = new PieChart.Data("%s (%d)".formatted(e.getKey(), e.getValue()),
                                e.getValue());
                        styleWhenShown(slice.getNode(), slice::nodeProperty,
                                "cat-" + e.getKey().toLowerCase());
                        return slice;
                    })
                    .toList());

            XYChart.Series<Number, Number> daily = new XYChart.Series<>();
            daily.setName("Temperature");
            styleWhenShown(daily.getNode(), daily::nodeProperty, "temperature-line");
            daily.getData().setAll(java.util.stream.IntStream.range(0, records.size())
                    .mapToObj(i -> new XYChart.Data<Number, Number>(i + 1, records.get(i).temperature()))
                    .toList());
            dailyChart.getData().setAll(List.of(daily));
        };

        data.addListener((ListChangeListener<WeatherRecord>) change -> {
            refreshCharts.run();
            refreshStats.run();
        });
        monthBox.valueProperty().addListener((obs, old, now) -> refreshStats.run());
        thresholdSpinner.valueProperty().addListener((obs, old, now) -> refreshStats.run());
        onlyAbove.selectedProperty().addListener((obs, old, now) -> refreshStats.run());

        Consumer<Path> load = path -> {
            status.setText("Loading " + path.getFileName() + " on a virtual thread…");
            WeatherCsvParser.parseAsync(path).whenComplete((records, error) -> Platform.runLater(() -> {
                if (error != null) {
                    status.setText("Could not read " + path.getFileName());
                    new Alert(Alert.AlertType.ERROR, "Could not read the file:\n" + error.getMessage()).show();
                    return;
                }
                Month selected = Month.of(monthBox.getSelectionModel().getSelectedIndex() + 1);
                if (WeatherAnalyzer.averageTemperature(records, selected).isEmpty()) {
                    WeatherAnalyzer.mostRecordedMonth(records)
                            .ifPresent(m -> monthBox.getSelectionModel().select(m.ordinal()));
                }
                data.setAll(records);
                fileLabel.setText(path.getFileName().toString());
                status.setText("Loaded %d records from %s".formatted(records.size(), path.toAbsolutePath()));
            }));
        };

        openButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Open weather CSV");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            chooser.setInitialDirectory(new File(System.getProperty("user.dir")));
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                load.accept(file.toPath());
            }
        });

        // ---------- layout ----------
        VBox center = new VBox(16, cards, tabs);
        center.setPadding(new Insets(16));
        BorderPane root = new BorderPane(center, new VBox(header, toolbar), null, status, null);
        root.getStyleClass().add("root-pane");

        Scene scene = new Scene(root, 1180, 800);
        scene.getStylesheets().add(WeatherDashboard.class.getResource("styles.css").toExternalForm());
        // drag a CSV file onto the window to open it
        scene.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });
        scene.setOnDragDropped(e -> {
            e.getDragboard().getFiles().stream().findFirst().map(File::toPath).ifPresent(load);
            e.setDropCompleted(true);
            e.consume();
        });

        stage.setTitle("Weather Data Analyzer");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(640);
        stage.show();

        refreshStats.run();
        if (Files.isReadable(initialFile)) {
            load.accept(initialFile);
        } else {
            status.setText("weatherdata.csv not found — click \"Open CSV…\" or drop a file on the window");
        }
    }

    /// Creates a summary card: a small title, a big value, and a caption.
    ///
    /// ```java
    /// VBox c = WeatherDashboard.card("Rainy days", new Label("42"), new Label("in 2023"), "card-rain");
    /// ```
    ///
    /// @param title      text above the value
    /// @param value      the label holding the big number
    /// @param caption    the label holding the small explanation
    /// @param styleClass extra CSS class for the card's accent colour
    /// @return the card node
    static VBox card(String title, Label value, Label caption, String styleClass) {
        Label titleLabel = new Label(title.toUpperCase());
        titleLabel.getStyleClass().add("card-title");
        value.getStyleClass().add("card-value");
        caption.getStyleClass().add("card-caption");
        VBox box = new VBox(4, titleLabel, value, caption);
        box.getStyleClass().addAll("card", styleClass);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    /// Creates a read-only table column whose cell value comes from a lambda.
    ///
    /// ```java
    /// TableColumn<WeatherRecord, Double> temp = WeatherDashboard.column("Temp", WeatherRecord::temperature);
    /// ```
    ///
    /// @param title  column header
    /// @param getter function that extracts the cell value from a record
    /// @param <T>    the type of value shown in the column
    /// @return the configured column
    static <T> TableColumn<WeatherRecord, T> column(String title, Function<WeatherRecord, T> getter) {
        TableColumn<WeatherRecord, T> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(getter.apply(cell.getValue())));
        return column;
    }

    /// Small helper for toolbar labels.
    ///
    /// @param text the label text
    /// @return a styled label
    static Label label(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("toolbar-label");
        return label;
    }

    /// Adds a CSS class to a chart node as soon as JavaFX creates it.
    /// Chart nodes are created lazily, so we listen to the node property.
    ///
    /// @param current      the node if it already exists, otherwise `null`
    /// @param nodeProperty supplier of the chart item's node property
    /// @param styleClass   the class to add
    static void styleWhenShown(Node current,
                               java.util.function.Supplier<javafx.beans.value.ObservableValue<Node>> nodeProperty,
                               String styleClass) {
        if (current != null) {
            current.getStyleClass().add(styleClass);
        } else {
            nodeProperty.get().addListener((obs, old, node) -> {
                if (node != null) {
                    node.getStyleClass().add(styleClass);
                }
            });
        }
    }

    /// Draws (or redraws) the horizontal threshold line on the daily chart.
    ///
    /// @param chart     the daily temperature chart
    /// @param records   the current data
    /// @param threshold threshold in °C
    static void updateThresholdLine(LineChart<Number, Number> chart, List<WeatherRecord> records, double threshold) {
        chart.getData().removeIf(s -> "Threshold".equals(s.getName()));
        if (records.isEmpty()) {
            return;
        }
        XYChart.Series<Number, Number> line = new XYChart.Series<>();
        line.setName("Threshold");
        styleWhenShown(line.getNode(), line::nodeProperty, "threshold-line");
        line.getData().addAll(List.of(
                new XYChart.Data<>(1, threshold),
                new XYChart.Data<>(records.size(), threshold)));
        chart.getData().add(line);
    }
}
