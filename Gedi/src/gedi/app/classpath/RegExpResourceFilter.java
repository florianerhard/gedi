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

import java.util.regex.Pattern;

public class RegExpResourceFilter implements ResourceFilter {

  public static final String ENDS_WITH_CLASS = ".*\\.class";
  public static final String ANY = ".*";
  private final Pattern packagePattern;
  private final Pattern resourcePattern;

  public RegExpResourceFilter(String packageRegExp, String resourceRegExp) {
    this(Pattern.compile(packageRegExp), Pattern.compile(resourceRegExp));
  }

  public RegExpResourceFilter(Pattern packagePattern, Pattern resourcePattern) {
    this.packagePattern = packagePattern;
    this.resourcePattern = resourcePattern;
  }

  public boolean match(String packageName, String resourceName) {
    return packagePattern.matcher(packageName).matches()
        && resourcePattern.matcher(resourceName).matches();
  }

}
