package com.vincentcodes.files;

import java.nio.file.Path;

public interface OnFileDeleted extends FileEventListener{
    void handleOnFileDeleted(Path fileChanged);
}
