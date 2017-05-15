/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */

package gedi.core.workspace;

import java.io.File;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import gedi.core.workspace.WorkspaceItemChangeEvent.ChangeType;
import gedi.core.workspace.file.FileWorkspaceItem;
import gedi.core.workspace.loader.WorkspaceItemLoader;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.util.StringUtils;
import gedi.util.mutable.MutablePair;

public class Workspace {

	
	public static final Logger log = Logger.getLogger( Workspace.class.getName() );

	
	public static final String GEDI_FOLDER = "/.gedi";
	public static final String WORKSPACE_TABLE = GEDI_FOLDER+"/project.tables";

	private static Workspace current;
	public static Workspace open(String path) throws IOException {
		if (current!=null) throw new RuntimeException("Close open workspace first!");
		current = new Workspace(path);
		WorkspaceEvent e = workspaceListeners.size()>0?new WorkspaceEvent(current, true):null;
		for (Consumer<WorkspaceEvent> l : workspaceListeners)
			l.accept(e);
		return current;
	}
	
	public static <T> T loadItem(String path) throws IOException {
		Path ppath = Paths.get(path);
		WorkspaceItemLoader<T,?> loader = WorkspaceItemLoaderExtensionPoint.getInstance().get(ppath);
		if (loader==null) throw new IOException("No loader for "+path);
		return loader.load(ppath);
	}

	public static Workspace getCurrent() {
		return current;
	}
	
	private static ArrayList<Consumer<WorkspaceEvent>> workspaceListeners = new ArrayList<Consumer<WorkspaceEvent>>();
	public static void addWorkspaceListener(Consumer<WorkspaceEvent> l) {
		workspaceListeners.add(l);
	}
	
	public static void removeWorkspaceListener(Consumer<WorkspaceEvent> l) {
		workspaceListeners.remove(l);
	}
	
	public static class WorkspaceEvent {
		private Workspace workspace;
		private boolean opened;
		public WorkspaceEvent(Workspace workspace, boolean opened) {
			super();
			this.workspace = workspace;
			this.opened = opened;
		}
		
		public Workspace getWorkspace() {
			return workspace;
		}

		public boolean isOpened() {
			return opened;
		}

		@Override
		public String toString() {
			return "WorkspaceEvent [workspace=" + workspace + ", opened="
					+ opened + "]";
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (opened ? 1231 : 1237);
			result = prime * result
					+ ((workspace == null) ? 0 : workspace.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			WorkspaceEvent other = (WorkspaceEvent) obj;
			if (opened != other.opened)
				return false;
			if (workspace == null) {
				if (other.workspace != null)
					return false;
			} else if (!workspace.equals(other.workspace))
				return false;
			return true;
		}
	}

	private String path;
	private Thread watcherThread;
	
	private Workspace(String path) {
		if (path.endsWith("/")) path = path.substring(0, path.length()-1);
		this.path = path;

		
		
		watcherThread = new Thread(this::watchLoop);
		watcherThread.setDaemon(true);
		watcherThread.setName("FileSystemWatcher");
		watcherThread.start();
	}
	
	 /**
     * Register the given directory with the WatchService
     */
    private void watch(Path dir, boolean logit, WatchService watcher, HashMap<WatchKey, Path> watchKeys) throws IOException {
        WatchKey key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        Path prev = watchKeys.get(key);
        if (logit) {
	        if (prev == null) {
	        	log.log(Level.FINE, "Register file system watcher for "+dir);
	        } else {
	            if (!dir.equals(prev)) {
	            	log.log(Level.FINE, "Update file system watcher for "+dir);
	            }
	        }
        }
        watchKeys.put(key, dir);
    }
 
    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     * @param watchKeys 
     * @param watcher 
     */
    private void watchAll(final Path start, WatchService watcher, HashMap<WatchKey, Path> watchKeys) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException
            {
                watch(dir,false,watcher,watchKeys);
                return FileVisitResult.CONTINUE;
            }
        });
    }
 
    
    private void watchLoop() {
    	
    	HashMap<WatchKey, Path> watchKeys = new HashMap<WatchKey,Path>();
    	try {
	    	WatchService watcher = FileSystems.getDefault().newWatchService();
			
			watchAll(Paths.get(path),watcher,watchKeys);
	    	for (;;) {
	    		 
	            // wait for key to be signalled
	            WatchKey key;
	            try {
	                key = watcher.take();
	            } catch (InterruptedException x) {
	                return;
	            }
	 
	            Path dir = watchKeys.get(key);
	            if (dir == null) {
	            	log.log(Level.SEVERE, "Watch key unknown: "+key);
	                continue;
	            }
	 
	            for (WatchEvent<?> event: key.pollEvents()) {
	                WatchEvent.Kind kind = event.kind();
	 
	                // TBD - provide example of how OVERFLOW event is handled
	                if (kind == StandardWatchEventKinds.OVERFLOW) {
	                    continue;
	                }
	 
	                // Context for directory entry event is the file name of entry
	                WatchEvent<Path> ev = (WatchEvent<Path>) event;
	                Path name = ev.context();
	                Path child = dir.resolve(name);
	 
	                // print out event
	                log.log(Level.FINE, "File system watcher reports :"+event.kind().name()+" on "+child);
	                fireListeners(new FileWorkspaceItem(child), ChangeType.fromWatchKey(event.kind()));
	                
	                // if directory is created, and watching recursively, then
	                // register it and its sub-directories
	                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
	                    try {
	                        if (Files.isDirectory(child)) {
	                        	watchAll(child,watcher,watchKeys);
	                        }
	                    } catch (IOException x) {
	                        // ignore to keep sample readbale
	                    }
	                }
	            }
	 
	            // reset key and remove from set if directory no longer accessible
	            boolean valid = key.reset();
	            if (!valid) {
	                watchKeys.remove(key);
	 
	                // all directories are inaccessible
	                if (watchKeys.isEmpty()) {
	                    break;
	                }
	            }
	        }
    	} catch (IOException e) {
    		log.log(Level.SEVERE, "File system watcher caught exception, watch is terminated!",e);
    	} finally {
    		Iterator<WatchKey> it = watchKeys.keySet().iterator();
    		while (it.hasNext()) {
    			WatchKey wk = it.next();
    			wk.cancel();
    			it.remove();
    		}
    	}
    }


	

	public String getPathName() {
		return path;
	}

	public Path getPath() {
		return Paths.get(path);
	}

	public Path getGediDirectory() {
		return Paths.get(path+GEDI_FOLDER);
	}

	public Path getWorkspaceTableFile() {
		return Paths.get(path+WORKSPACE_TABLE);
	}

	public void close() {
		Workspace.current = null;
		watcherThread.interrupt();
		loadedItems.clear();
		
		WorkspaceEvent e = workspaceListeners.size()>0?new WorkspaceEvent(current, false):null;
		for (Consumer<WorkspaceEvent> l : workspaceListeners)
			l.accept(e);
	}


	public WorkspaceItem getRoot() {
		return new FileWorkspaceItem(getPath());
	}


	public WorkspaceItem getItem(String path) {
		return new FileWorkspaceItem(Paths.get(this.path,path));
	}

	
	private Map<String, MutablePair<Pattern,List<Consumer<WorkspaceItemChangeEvent>>>> listeners = Collections.synchronizedMap(new HashMap<>());

	private void fireListeners(WorkspaceItem wi, ChangeType type) {
		String path = wi.toString();
		path = StringUtils.removeHeader(path, this.path);
		WorkspaceItemChangeEvent event = null;
		
		for (String k : listeners.keySet()) {
			MutablePair<Pattern, List<Consumer<WorkspaceItemChangeEvent>>> p = listeners.get(k);
			if (p.Item1.matcher(path).find())
				for (Consumer<WorkspaceItemChangeEvent> c : p.Item2) {
					if (event==null) event = new WorkspaceItemChangeEvent(wi, type);
					c.accept(event);
				}
		}
			
	}
	
	/**
	 * Don't perform any heavy task in the consumer, rather use the job system to launch a local job for it!
	 * @param l
	 */
	public void addChangeListener(String filterRegex, Consumer<WorkspaceItemChangeEvent> l) {
		MutablePair<Pattern, List<Consumer<WorkspaceItemChangeEvent>>> list = listeners.computeIfAbsent(filterRegex, p->new MutablePair(Pattern.compile(filterRegex), Collections.synchronizedList(new ArrayList<>())));
		list.Item2.add(l);
	}
	
	public void removeChangeListener(Consumer<WorkspaceItemChangeEvent> l){
		Iterator<String> it = listeners.keySet().iterator();
		while (it.hasNext()) {
			String f = it.next();
			MutablePair<Pattern, List<Consumer<WorkspaceItemChangeEvent>>> list = listeners.get(f);
			if (list!=null) {
				list.Item2.remove(l);
				if (list.Item2.isEmpty())
					it.remove();
			}
		}
	}
	public void addChangeListener(Consumer<WorkspaceItemChangeEvent> l) {
		addChangeListener(".*",l);
	}

	
	// Due to the weak reference, items are garbage-collected, when no strong reference somewhere else points to it!
	private HashMap<WorkspaceItem,WeakReference<?>> loadedItems = new HashMap<WorkspaceItem, WeakReference<?>>();
	
	public <T> T getItem(WorkspaceItem wi) throws IOException {
		WeakReference<?> ref = loadedItems.get(wi);
		if (ref==null || ref.get()==null){
			log.log(Level.INFO, "Loading "+wi);
			T loaded = wi.load();
			loadedItems.put(wi, new WeakReference<T>(loaded));
			return loaded;
		} else {
			log.log(Level.INFO, "From cache: "+wi);
		}
		return (T) ref.get();
	}
	

}
