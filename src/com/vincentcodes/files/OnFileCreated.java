package com.vincentcodes.files;

import java.nio.file.Path;

public interface OnFileCreated extends FileEventListener {
    void handleOnFileCreated(Path fileChanged);
}
