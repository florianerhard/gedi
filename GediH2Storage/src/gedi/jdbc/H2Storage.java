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
package gedi.jdbc;

import gedi.util.io.randomaccess.serialization.BinarySerializable;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.h2.tools.DeleteDbFiles;


public class H2Storage<D extends BinarySerializable> extends JdbcStorage<D> {

	

	public H2Storage(String path, boolean deleteFirst, boolean server, Class<D> cls) throws SQLException {
		this(path,"","",deleteFirst,server,cls);
	}

	public H2Storage(String path,String user, String pw, boolean deleteFirst, boolean server, Class<D> cls) throws SQLException {
		super(createConnection(path,user,pw,deleteFirst,server),"",cls);
	}

	private static Connection createConnection(String path,String user, String pw, boolean deleteFirst, boolean server) throws SQLException {
		try {
			if (deleteFirst)
				DeleteDbFiles.execute(new File(path).getParent(), new File(path).getName(), true);
			Class.forName("org.h2.Driver");
		} catch (ClassNotFoundException e) {
		}
		return DriverManager.getConnection("jdbc:h2:"+path+(server?";AUTO_SERVER=TRUE":""), user, pw);
	}
	
	@Override
	protected String getTemporaryKeyword() {
		return "MEMORY";
	}
	
	@Override
	protected String getTemporaryModifier() {
		return "NOT PERSISTENT";
	}

	
}
