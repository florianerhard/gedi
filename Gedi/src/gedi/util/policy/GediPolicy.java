package gedi.util.policy;

import gedi.app.classpath.ClassPath;
import gedi.app.classpath.ClassPathCache;
import gedi.util.FileUtils;
import gedi.util.io.text.LineOrientedFile;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PropertyPermission;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GediPolicy extends Policy {
	
	private static final Logger log = Logger.getLogger( GediPolicy.class.getName() );


	private PermissionCollection general = new MyPermissionCollection();
	private HashMap<ClassPath,PermissionCollection> cp = new HashMap<ClassPath, PermissionCollection>();

    @Override
    public PermissionCollection getPermissions(CodeSource codesource) {
    	ClassPath cp = ClassPathCache.getInstance().getClassPathByURL(codesource.getLocation());
    	if (this.cp.containsKey(cp))
    		return this.cp.get(cp);
    	return general;
    }
    
    public void setup() {
    	Enumeration<Permission> en = general.elements();
    	while (en.hasMoreElements()) {
    		Permission n = en.nextElement();
    		for (PermissionCollection c : cp.values())
    			c.add(n);
    	}
    	
		Policy.setPolicy(this);
		System.setSecurityManager(new SecurityManager());
	}
    
    public GediPolicy addDefaults() {
    	general.add(new PropertyPermission("java.version", "read"));
    	general.add(new PropertyPermission("java.vendor", "read"));
    	general.add(new PropertyPermission("java.vendor.url", "read"));
    	general.add(new PropertyPermission("java.class.version", "read"));
    	general.add(new PropertyPermission("os.name", "read"));
    	general.add(new PropertyPermission("os.version", "read"));
    	general.add(new PropertyPermission("os.arch", "read"));
    	general.add(new PropertyPermission("file.separator", "read"));
    	general.add(new PropertyPermission("path.separator", "read"));
        general.add(new PropertyPermission("line.separator", "read"));

        general.add(new PropertyPermission("java.specification.version", "read"));
        general.add(new PropertyPermission("java.specification.vendor", "read"));
        general.add(new PropertyPermission("java.specification.name", "read"));

        general.add(new PropertyPermission("java.vm.specification.version", "read"));
        general.add(new PropertyPermission("java.vm.specification.vendor", "read"));
        general.add(new PropertyPermission("java.vm.specification.name", "read"));
        general.add(new PropertyPermission("java.vm.version", "read"));
        general.add(new PropertyPermission("java.vm.vendor", "read"));
        general.add(new PropertyPermission("java.vm.name", "read"));
    	
        general.add(new PropertyPermission("java.io.tmpdir", "read"));
        
    	return this;
    }
    
	public GediPolicy addFileReadPermissions(String... paths) throws IOException {
		for (String path : paths)
			new LineOrientedFile(path).lineIterator().forEachRemaining(l->{
				if (l.endsWith("**")) {
					FileUtils.applyRecursively(l.substring(0,l.length()-2),f->{
						addPermission(new FilePermission(f.getPath(),"read"));
					});
				}
				else if (l.endsWith("*")) {
					File r = new File(l.substring(0,l.length()-1));
					for (String f : r.list()) {
						addPermission(new FilePermission(new File(r,f).getPath(),"read"));
					}
				} else {
					addPermission(new FilePermission(l, "read"));
				}
				log.log(Level.INFO, "Adding read permissions: "+l);
			});
		return this;
	}

	
    public GediPolicy addAllPermissions(ClassPath... cps) {
    	AllPermission all = new AllPermission();
		for (ClassPath cp : cps)
			addPermission(cp, all);
		return this;
	}
    
    public GediPolicy addAllPermissions(Class... classes) {
    	AllPermission all = new AllPermission();
		for (Class cls : classes)
			addPermission(ClassPathCache.getInstance().getClassPathOfClass(cls), all);
		return this;
	}

    public GediPolicy addPermission(Permission permission) {
    	general.add(permission);
        return this;
    }

    public GediPolicy addPermission(ClassPath cp, Permission permission) {
    	PermissionCollection coll = this.cp.get(cp);
    	if (coll==null) this.cp.put(cp, coll = new MyPermissionCollection());
        coll.add(permission);
        return this;
    }
    
    private class MyPermissionCollection extends PermissionCollection {

        private static final long serialVersionUID = 614300921365729272L;

        HashSet<Permission> perms = new HashSet<Permission>();

        public void add(Permission p) {
            perms.add(p);
        }

        public boolean implies(Permission p) {
            for (Iterator<Permission> i = perms.iterator(); i.hasNext();) {
                if (((Permission) i.next()).implies(p)) {
                    return true;
                }
            }
            return false;
        }

        public Enumeration<Permission> elements() {
            return Collections.enumeration(perms);
        }

        public boolean isReadOnly() {
            return false;
        }

    }


	


	
}