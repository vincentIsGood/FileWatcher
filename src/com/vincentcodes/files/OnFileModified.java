package com.vincentcodes.files;

import java.nio.file.Path;

public interface OnFileModified extends FileEventListener{
    void handleOnFileModified(Path fileChanged);
}
