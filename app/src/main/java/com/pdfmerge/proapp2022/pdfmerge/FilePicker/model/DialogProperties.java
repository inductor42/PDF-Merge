package com.pdfmerge.proapp2022.pdfmerge.FilePicker.model;

import java.io.File;

/**
 * @author akshay sunil masram
 */
public class DialogProperties {
    public int selection_mode;
    public int selection_type;
    public File root;
    public File error_dir;
    public File offset;
    public String[] extensions;

    public DialogProperties() {
        selection_mode = DialogConfigs.SINGLE_MODE;
        selection_type = DialogConfigs.FILE_SELECT;
        root = new File(DialogConfigs.DEFAULT_DIR);
        error_dir = new File(DialogConfigs.DEFAULT_DIR);
        offset = new File(DialogConfigs.DEFAULT_DIR);
        extensions = null;
    }
}