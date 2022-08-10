package controller;

import com.jfoenix.controls.JFXButton;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.*;
import java.text.NumberFormat;
import java.util.Optional;

public class MainFormController {
    public Label lblProgress;
    public Label lblSize;
    public JFXButton btnSelectFile;
    public Label lblFile;
    public JFXButton btnSelectDir;
    public Label lblFolder;
    public Rectangle pgbContainer;
    public Rectangle pgbBar;
    public JFXButton btnCopy;
    public JFXButton btnSelectDirectory;
    public Label lblDirectory;

    private File srcFile;
    private File destDir;

    public void initialize() {
        btnCopy.setDisable(true);

    }
    // ======================================================== Source =====================================================================================================

    public void btnSelectFileOnAction(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        File file = new File(System.getProperty("user.home"));
        fileChooser.setInitialDirectory(file);
        fileChooser.setTitle("Select a file to copy");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files (*.*)", "*.*"));
        srcFile = fileChooser.showOpenDialog(lblFolder.getScene().getWindow());

        if (srcFile != null) {
            lblFile.setText(srcFile.getName() + ". " + (srcFile.length() / 1024.0) + "Kb");
        } else {
            lblFile.setText("No file selected");
        }

        btnCopy.setDisable(srcFile == null || destDir == null);

    }

    public void btnSelectDirectoryOnAction(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File file = new File(System.getProperty("user.home"));
        directoryChooser.setTitle("Select a source folder");
        directoryChooser.setInitialDirectory(file);
        srcFile = directoryChooser.showDialog(lblDirectory.getScene().getWindow());
        if (srcFile != null) {
            lblDirectory.setText(srcFile.getName() + ". " + (srcFile.length() / 1024.0) + "Kb");
        } else {
            lblDirectory.setText("No Directory selected");
        }

        btnCopy.setDisable(srcFile == null || destDir == null);


    }

    // ====================================================================== Destination ========================================================================
    public void btnSelectDirOnAction(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select a destination folder");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        destDir = directoryChooser.showDialog(lblFolder.getScene().getWindow());

        if (destDir != null) {
            lblFolder.setText(destDir.getAbsolutePath());
        } else {
            lblFolder.setText("No folder selected");
        }

        btnCopy.setDisable(srcFile == null || destDir == null);
    }

    public void btnCopyOnAction(ActionEvent actionEvent) throws IOException {
        File destFile = new File(destDir, srcFile.getName());
        if (!destFile.exists()) {
            destFile.createNewFile();
        } else {
            Optional<ButtonType> result = new Alert(Alert.AlertType.INFORMATION,
                    "File already exists. Do you want to overwrite?",
                    ButtonType.YES, ButtonType.NO).showAndWait();
            if (result.get() == ButtonType.NO) {
                return;
            }
        }
        var task = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
                FileInputStream fis = new FileInputStream(srcFile);
                FileOutputStream fos = new FileOutputStream(destFile);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fis);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fos);


                long fileSize = srcFile.length();
                int totalRead = 0;

                while (true) {
                    byte[] buffer = new byte[1024 * 10];
                    int read = bufferedInputStream.read(buffer);
                    totalRead += read;
                    if (read == -1) break;
                    bufferedOutputStream.write(buffer, 0, read);
                    updateProgress(totalRead, fileSize);

                }

                bufferedInputStream.close();
                bufferedOutputStream.close();
                return null;
            }
        };
        task.workDoneProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number prevWork, Number currentWork) {
                pgbBar.setWidth(pgbContainer.getWidth() / task.getTotalWork() * currentWork.doubleValue());
                lblProgress.setText("Progress :" + formatNumbers(task.getProgress() * 100) + "%");
                lblSize.setText(formatNumbers(task.getWorkDone() / 1024.0) + " / " + formatNumbers(task.getTotalWork() / 1024.0) + " Kb");
            }
        });
        task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                pgbBar.setWidth(pgbContainer.getWidth());
                new Alert(Alert.AlertType.INFORMATION, "File has been copied successfully").show();
                lblFolder.setText("No folder selected");
                lblFile.setText("No file selected");
                btnCopy.setDisable(true);
                pgbBar.setWidth(0);
                lblProgress.setText("Progress : 0%");
                srcFile = null;
                destDir = null;
            }
        });
        new Thread(task).start();
    }

    private String formatNumbers(double input) {
        NumberFormat ni = NumberFormat.getNumberInstance();
        ni.setGroupingUsed(true);
        ni.setMaximumFractionDigits(2);
        ni.setMinimumFractionDigits(2);
        return ni.format(input);
    }
}
