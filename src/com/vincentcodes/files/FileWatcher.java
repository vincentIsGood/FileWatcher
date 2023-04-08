package com.vincentcodes.files;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A high level wrapper for the WatchService API
 * <p>
 * A better implementation with similar functionality 
 * can be found in Apache Commons-IO.
 * <p>
 * Note: This project is used to sharpen my skill in Java.
 * 
 * @author Vincent Ko
 */
public class FileWatcher {
    private enum FileEventTypes {
        CREATED, DELETED, MODIFIED
    }

    private final List<WatchKey> registeredDirectories;
    private final WatchService dirWatcher;
    private final ExecutorService executorService;

    private final Map<FileEventTypes, List<FileEventListener>> listeners;

    private boolean shutdownRequested = false;

    /**
     * At least one directory is needed
     * @param directory a directory to be watched
     */
    public FileWatcher(String directory) throws IOException {
        this(new File(directory));
    }

    /**
     * At least one directory is needed
     * @param directory a directory to be watched
     */
    public FileWatcher(File directory) throws IOException{
        registeredDirectories = new ArrayList<>();

        dirWatcher = FileSystems.getDefault().newWatchService();
        addDirToWatchList(directory);

        executorService = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(null, runnable, "FileWatcher", 0, false);
            thread.setDaemon(true);
            return thread;
        });

        listeners = new EnumMap<>(FileEventTypes.class);
        listeners.put(FileEventTypes.CREATED, new ArrayList<>());
        listeners.put(FileEventTypes.DELETED, new ArrayList<>());
        listeners.put(FileEventTypes.MODIFIED, new ArrayList<>());
    }

    /**
     * Watch an additional directory
     * @return successful or not
     */
    public boolean addDirToWatchList(File directory){
        if(!directory.isDirectory())
            return false;
        try {
            Path directoryPath = directory.getCanonicalFile().toPath();
            WatchKey key = directoryPath.register(dirWatcher, 
                StandardWatchEventKinds.ENTRY_CREATE, 
                StandardWatchEventKinds.ENTRY_DELETE, 
                StandardWatchEventKinds.ENTRY_MODIFY);
            registeredDirectories.add(key);
        } catch (IOException e) {
            return false;
        }
        return true;
    }
    public void addDirsToWatchList(List<File> directories){
        directories.forEach(this::addDirToWatchList);
    }

    /**
     * <p>
     * To turn {@link WatchKey} back to {@link Path}, you are required
     * to cast a {@link WatchKey#watchable()} object back to path like 
     * this {@code (Path) key.watchable()}.
     * <p>
     * To unregister a directory, do {@link WatchKey#cancel()}
     * @return a list of watch keys registered to the watch service
     */
    public List<WatchKey> getRegisteredDirectories(){
        return registeredDirectories;
    }

    /**
     * Register a listener to allow it to listen for file events
     * @param listener an object that implements one or a maximum of 3
     *                 interfaces listed below
     * @see OnFileCreated
     * @see OnFileDeleted
     * @see OnFileModified
     */
    public void registerListener(FileEventListener listener){
        if(listener instanceof OnFileCreated)
            listeners.get(FileEventTypes.CREATED).add(listener);
        if(listener instanceof OnFileDeleted)
            listeners.get(FileEventTypes.DELETED).add(listener);
        if(listener instanceof OnFileModified)
            listeners.get(FileEventTypes.MODIFIED).add(listener);
    }

    /**
     * Start the FileWatcher immediately
     */
    public void start(){
        start(0);
    }

    public void stop(){
        shutdownRequested = true;
        executorService.shutdown();
        try{
            if(executorService.awaitTermination(60, TimeUnit.SECONDS)){
                executorService.shutdownNow();
            }
        }catch (InterruptedException ex){
            executorService.shutdown();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * @param delay in seconds
     */
    public void start(int delay){
        executorService.submit(()-> {
            while(true){
                WatchKey key;
                try {
                    key = dirWatcher.take();

                    // Let the file fully update (eg. update file content and timestamp)
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                    if(shutdownRequested)
                        return;
                    continue;
                }
                if(shutdownRequested)
                    return;

                for(WatchEvent<?> event : key.pollEvents()){
                    if(event.kind() == StandardWatchEventKinds.OVERFLOW)
                        continue;

                    Path directory = (Path) key.watchable();
                    Path fileChanged = directory.resolve((Path)event.context());
                    if(event.kind() == StandardWatchEventKinds.ENTRY_CREATE)
                        dispatchEventToListeners(FileEventTypes.CREATED, fileChanged);
                    else if(event.kind() == StandardWatchEventKinds.ENTRY_DELETE)
                        dispatchEventToListeners(FileEventTypes.DELETED, fileChanged);
                    else if(event.kind() == StandardWatchEventKinds.ENTRY_MODIFY)
                        dispatchEventToListeners(FileEventTypes.MODIFIED, fileChanged);
                }

                // no longer valid, just leave
                if(!key.reset())
                    return;
            }
        }, delay);
    }

    private void dispatchEventToListeners(FileEventTypes type, Path fileChanged){
        List<FileEventListener> typedListeners = listeners.get(type);
        if(type.equals(FileEventTypes.CREATED)){
            for (FileEventListener listener : typedListeners) {
                if(!(listener instanceof SpecificFileListener) || isFilePathsTheSame(fileChanged, ((SpecificFileListener)listener).getFileListeningFor()))
                    ((OnFileCreated) listener).handleOnFileCreated(fileChanged);
            }
        }else if(type.equals(FileEventTypes.DELETED)){
            for(FileEventListener listener : typedListeners) {
                if(!(listener instanceof SpecificFileListener) || isFilePathsTheSame(fileChanged, ((SpecificFileListener)listener).getFileListeningFor()))
                    ((OnFileDeleted) listener).handleOnFileDeleted(fileChanged);
            }
        }else if(type.equals(FileEventTypes.MODIFIED)){
            for(FileEventListener listener : typedListeners) {
                if(!(listener instanceof SpecificFileListener) || isFilePathsTheSame(fileChanged, ((SpecificFileListener)listener).getFileListeningFor()))
                    ((OnFileModified) listener).handleOnFileModified(fileChanged);
            }
        }
    }
    private boolean isFilePathsTheSame(Path path, File file){
        try {
            return path.toFile().getCanonicalFile().equals(file.getCanonicalFile());
        } catch (IOException e) {
            return false;
        }
    }
}
