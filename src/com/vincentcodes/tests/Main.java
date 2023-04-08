package com.vincentcodes.tests;

import com.vincentcodes.files.FileWatcher;
import com.vincentcodes.files.OnFileCreated;
import com.vincentcodes.files.OnFileModified;
import com.vincentcodes.files.SpecificFileListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        FileWatcher fileWatcher = new FileWatcher("./");
        fileWatcher.registerListener((OnFileModified) fileChanged -> System.out.println("Dir: " + fileChanged.toAbsolutePath()));

        class ReadmeWatcher implements OnFileCreated, OnFileModified, SpecificFileListener {
            @Override
            public void handleOnFileCreated(Path fileChanged) {
                System.out.println("Specific (created): " + fileChanged.toAbsolutePath());
            }

            @Override
            public void handleOnFileModified(Path fileChanged) {
                System.out.println("Specific (modified): " + fileChanged.toAbsolutePath());
            }

            @Override
            public File getFileListeningFor() {
                return new File("./test.md");
            }
        }

        fileWatcher.registerListener(new ReadmeWatcher());
        fileWatcher.start();

        System.out.println("Watching directory for 10s: " + fileWatcher.getDirectoryPath().toAbsolutePath());
        Thread.sleep(10*1000);

        fileWatcher.stop();
    }
}