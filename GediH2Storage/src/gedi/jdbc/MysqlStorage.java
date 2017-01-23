package gedi.jdbc;

import gedi.util.io.randomaccess.serialization.BinarySerializable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.h2.tools.DeleteDbFiles;


public class MysqlStorage<D extends BinarySerializable> extends JdbcStorage<D> {

	

	public MysqlStorage(String configPath, String prefix, Class<D> cls) throws SQLException, IOException {
		super(createConnection(configPath),prefix,cls);
	}

	public MysqlStorage(String host, String port, String db,String user, String pw, String prefix, Class<D> cls) throws SQLException {
		super(createConnection(host,port,db,user,pw),prefix,cls);
	}

	private static Connection createConnection(String host, String port, String db,String user, String pw) throws SQLException {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
		}
		return DriverManager.getConnection("jdbc:mysql://"+host+":"+port+"/"+db+"?" +
                                   "user="+user+"&password="+pw);
	}
	
	private static Connection createConnection(String configPath) throws IOException, SQLException {
		Properties properties = new Properties();
		properties.load(new FileInputStream(configPath));
		String host = properties.getProperty("host");
		String port = properties.getProperty("port","3306");
		String user = properties.getProperty("user");
		String db = properties.getProperty("db");
		String pw= properties.getProperty("password");
		return createConnection(host, port, db, user, pw);
	}

	
}
