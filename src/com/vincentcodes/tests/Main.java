package com.vincentcodes.tests;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import com.vincentcodes.files.FileWatcher;
import com.vincentcodes.files.OnFileCreated;
import com.vincentcodes.files.OnFileModified;
import com.vincentcodes.files.SpecificFileListener;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        FileWatcher fileWatcher = new FileWatcher("./0_testme");
        fileWatcher.addDirToWatchList(new File("./"));
        fileWatcher.addDirsToWatchList(List.of(new File("./lib"), new File("./src")));

        fileWatcher.registerListener((OnFileModified) fileChanged -> System.out.println("Dir Mod: " + fileChanged.toAbsolutePath()));
        fileWatcher.registerListener((OnFileCreated) fileChanged -> System.out.println("Dir Create: " + fileChanged.toAbsolutePath()));

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
                return new File("./0_testme/test.md");
            }
        }

        fileWatcher.registerListener(new ReadmeWatcher());
        fileWatcher.start();

        System.out.println("Watching these directories for 30 s: " + fileWatcher.getRegisteredDirectories()
            .stream().map(key -> (Path)key.watchable()).collect(Collectors.toList()));
        
        createTestFiles();

        Thread.sleep(30*1000);

        fileWatcher.stop();
    }

    private static void createTestFiles(){
        new Thread(){
            public void run(){
                try {
                    Thread.sleep(2000);
                    createFile("./0_testme/");
                    createFile("./asd.txt");
                    createFile("./0_testme/test.txt");
                    createFile("./0_testme/test.md");
                    createFile("./lib/test.txt");
                    createFile("./src/dsa.txt");
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }

            private void createFile(String pathname) throws IOException{
                File file = new File(pathname);
                file.createNewFile();
                file.deleteOnExit();
            }
        }.start();
    }
}