package org.arnaudlt.warthog.ui.pane.control;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;
import org.arnaudlt.warthog.model.dataset.NamedDataset;

@Slf4j
public class NamedDatasetExportService extends Service<Void> {


    private final NamedDataset namedDataset;

    private final String filePath;


    public NamedDatasetExportService(NamedDataset namedDataset, String filePath) {
        this.namedDataset = namedDataset;
        this.filePath = filePath;
    }


    @Override
    protected Task<Void> createTask() {

        return new Task<>() {
            @Override
            protected Void call() {

                log.info("Start generating an export for {}", namedDataset.getName());
                namedDataset.export(filePath);
                return null;
            }
        };
    }
}
