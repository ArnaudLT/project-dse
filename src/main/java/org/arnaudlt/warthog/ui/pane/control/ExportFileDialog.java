package org.arnaudlt.warthog.ui.pane.control;

import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.arnaudlt.warthog.model.util.PoolService;
import org.arnaudlt.warthog.model.dataset.NamedDataset;
import org.arnaudlt.warthog.model.dataset.NamedDatasetManager;
import org.arnaudlt.warthog.model.setting.ExportFileSettings;
import org.arnaudlt.warthog.model.util.Format;
import org.arnaudlt.warthog.ui.pane.transform.TransformPane;
import org.arnaudlt.warthog.ui.service.NamedDatasetExportToFileService;
import org.arnaudlt.warthog.ui.service.SqlExportToFileService;
import org.arnaudlt.warthog.ui.util.AlertFactory;
import org.arnaudlt.warthog.ui.util.GridFactory;
import org.arnaudlt.warthog.ui.util.StageFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;


@Slf4j
@Component
public class ExportFileDialog {


    private final NamedDatasetManager namedDatasetManager;

    private final PoolService poolService;

    private final TransformPane transformPane;

    private Stage owner;

    private Stage dialog;


    @Autowired
    public ExportFileDialog(NamedDatasetManager namedDatasetManager, PoolService poolService, TransformPane transformPane) {
        this.namedDatasetManager = namedDatasetManager;
        this.poolService = poolService;
        this.transformPane = transformPane;
    }


    public void buildExportFileDialog(Stage owner) {

        this.owner = owner;
        this.dialog = StageFactory.buildModalStage(owner, "Export to file");

        // Basic settings
        GridPane basicGrid = GridFactory.buildGrid();
        int rowIndex = 0;

        Label outputLabel = new Label("Output directory :");
        TextField output = new TextField();

        Button outputButton = new Button("...");
        outputButton.setOnAction(event -> {

            FileChooser fc = new FileChooser();
            File exportFile = fc.showSaveDialog(this.dialog);

            if (exportFile == null) return;
            output.setText(exportFile.getAbsolutePath());
        });

        basicGrid.add(outputLabel, 0, rowIndex);
        basicGrid.add(output, 1, rowIndex, 3, 1);
        basicGrid.add(outputButton, 4, rowIndex);
        rowIndex++;

        Label formatLabel = new Label("Format :");
        ComboBox<Format> format = new ComboBox<>(FXCollections.observableArrayList(Format.values()));
        format.setValue(Format.CSV);
        format.setMinWidth(100);

        Label modeLabel = new Label("Mode :");
        ComboBox<String> saveMode = new ComboBox<>(FXCollections.observableArrayList("Overwrite", "Append"));
        saveMode.setValue("Overwrite");
        saveMode.setMinWidth(100);
        basicGrid.addRow(rowIndex++, formatLabel, format, modeLabel, saveMode);

        // Start conditional display
        BooleanBinding csvSelected = format.valueProperty().isEqualTo(Format.CSV);
        Separator s = new Separator(Orientation.HORIZONTAL);
        s.visibleProperty().bind(csvSelected);
        basicGrid.add(s, 0, rowIndex++, 4, 1);

        Label separatorLabel = new Label("Separator :");
        separatorLabel.visibleProperty().bind(csvSelected);
        TextField separator = new TextField(";");
        separator.setMaxWidth(50);
        separator.visibleProperty().bind(csvSelected);

        Label headerLabel = new Label("Header :");
        headerLabel.visibleProperty().bind(csvSelected);
        CheckBox header = new CheckBox();
        header.setSelected(true);
        header.visibleProperty().bind(csvSelected);
        basicGrid.addRow(rowIndex++, separatorLabel, separator, headerLabel, header);
        // End conditional display

        Tab basicSettingsTab = new Tab("Settings", basicGrid);

        // Advanced settings
        GridPane advancedGrid = GridFactory.buildGrid();
        rowIndex = 0;

        Label partitionByLabel = new Label("Partitions :");
        TextField partitionBy = new TextField();
        partitionBy.setMinWidth(300);
        partitionBy.setMaxWidth(300);
        partitionBy.setTooltip(new Tooltip("Comma separated list of attributs"));

        advancedGrid.addRow(rowIndex++, partitionByLabel, partitionBy);

        Tab advancedSettingsTab = new Tab("Advanced", advancedGrid);

        TabPane tabPane = new TabPane(basicSettingsTab, advancedSettingsTab);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        GridPane bottomGrid = GridFactory.buildGrid();
        Button exportButton = new Button("Export");
        exportButton.setOnAction(event -> {

            ExportFileSettings exportFileSettings = new ExportFileSettings(output.getText(), format.getValue(),
                    saveMode.getValue(), partitionBy.getText(), separator.getText(), header.isSelected());
            exportToFile(exportFileSettings);
            dialog.close();
        });
        bottomGrid.addRow(0, exportButton);

        Scene dialogScene = StageFactory.buildScene(new VBox(tabPane, bottomGrid));
        dialog.setScene(dialogScene);
    }


    public void showExportFileDialog() {

        dialog.show();
    }


    private void exportToFile(ExportFileSettings exportFileSettings) {

        NamedDataset selectedNamedDataset = this.transformPane.getSelectedNamedDataset();
        if (selectedNamedDataset == null) {

            final String sqlQuery = this.transformPane.getSqlQuery();
            SqlExportToFileService exportService = new SqlExportToFileService(poolService, namedDatasetManager, sqlQuery, exportFileSettings);
            exportService.setOnSucceeded(success -> log.info("Export succeeded"));
            exportService.setOnFailed(fail -> AlertFactory.showFailureAlert(owner, fail, "Not able to generate the export"));
            exportService.start();
        } else {

            NamedDatasetExportToFileService exportService = new NamedDatasetExportToFileService(poolService, namedDatasetManager, selectedNamedDataset, exportFileSettings);
            exportService.setOnSucceeded(success -> log.info("Export succeeded"));
            exportService.setOnFailed(fail -> AlertFactory.showFailureAlert(owner, fail, "Not able to generate the export"));
            exportService.start();
        }
    }

}
