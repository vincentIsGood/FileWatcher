# File Watcher for Java
The aim of this project is to create a library to watch changes of a file (directory OR a file). 
A simple API is created to make the file watching process easy to implement.

## Simple Usage
To use the library, a simple example is shown in the class `com.vincentcodes.tests.Main`.
The basic idea is to create `FileWatcher` and add custom-made listeners to it. Once a 
matching file event is found. It will run the code inside your listener.

To listen files (including directory) for their modified events in the whole directory `./`:
```java
FileWatcher fileWatcher = new FileWatcher("./");
fileWatcher.registerListener((OnFileModified) fileChanged -> 
    System.out.println("Dir: " + fileChanged.toAbsolutePath()));
```

To add other directories to the watch list:
```java
fileWatcher.addDirToWatchList(new File("./0_testme/"));
```

To watch **a** specific file for its creation and modification:
```java
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
```

Finally, start the watcher.
```java
fileWatcher.start();

// Stop it if you want
fileWatcher.stop();
```