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

/*
 * Copyright 2007 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package gedi.app.classpath;

import java.util.SortedSet;
import java.util.TreeSet;

public class ResourceFinder {

  private final ClassPath classPath;

  public ResourceFinder(ClassPath classPath) {
    this.classPath = classPath;
  }

  public String[] findResources(String rootPackageName, ResourceFilter resourceFilter) {
    SortedSet<String> resources = new TreeSet<String>();
    findResources(resources, rootPackageName, resourceFilter, new TreeSet<String>());
    return (String[]) resources.toArray(new String[resources.size()]);
  }

  private void findResources(SortedSet<String> resources, String rootPackageName,
      ResourceFilter resourceFilter, TreeSet<String> visitedPackages) {
    if (rootPackageName.startsWith("/")) {
      rootPackageName = rootPackageName.substring(1);
    }
    if (visitedPackages.contains(rootPackageName)) {
      return;
    }
    visitedPackages.add(rootPackageName);
    for (String packageName : classPath.listPackages(rootPackageName)) {
      findResources(resources, rootPackageName + "/" + packageName,
          resourceFilter, visitedPackages);
    }
    for (String resourceName : classPath.listResources(rootPackageName)) {
      if (resourceFilter.match(rootPackageName, resourceName)) {
        String name = rootPackageName + "/" + resourceName;
        if (name.startsWith("/")) {
          name = name.substring(1);
        }
        resources.add(name);
      }
    }
  }

}
