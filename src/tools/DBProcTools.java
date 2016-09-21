package tools;

import java.sql.*;

public class DBProcTools {

	public static final String DATABASE_NAME = "Skin_Lesions";
	private static Connection conn = null;

	public static Connection startConnection() throws ClassNotFoundException, SQLException {
		// create a mysql database connection
		String myDriver = "com.mysql.jdbc.Driver";
		String myUrl = "jdbc:mysql://localhost:3306";
		Class.forName(myDriver);
		conn = DriverManager.getConnection(myUrl, "root", "password");

		return conn;
	}

	public static Connection getConnection() throws ClassNotFoundException, SQLException {
		if (conn == null || conn.isClosed())
			return startConnection();
		return conn;
	}

	public static void closeConnection() throws SQLException {
		conn.close();
	}

	private static int getID(String fromTable, String keyName, String... params) throws Exception {
		int id = 0;
		try {
			Connection conn = DBProcTools.getConnection();

			StringBuilder queryBuilder = new StringBuilder();
			queryBuilder.append("SELECT ");
			queryBuilder.append(keyName);
			queryBuilder.append(" FROM ");
			queryBuilder.append(DATABASE_NAME);
			queryBuilder.append(".");
			queryBuilder.append(fromTable);
			queryBuilder.append(" WHERE ");

			if (params.length % 2 > 0)
				throw new Exception();

			for (int i = 0; i < params.length / 2; ++i) {
				if (i != 0)
					queryBuilder.append(" AND ");
				queryBuilder.append(params[i]);
				queryBuilder.append(" = ?");
			}

			String query = queryBuilder.toString();

			PreparedStatement prepStmt = conn.prepareStatement(query);

			for (int i = 1, j = params.length / 2; j < params.length; ++i, ++j) {
				prepStmt.setString(i, params[j]);
			}

			// System.out.println(prepStmt);

			boolean success = prepStmt.execute();

			ResultSet res = prepStmt.getResultSet();
			if (res.first()) {
				id = res.getInt(1);
				// System.out.println(success);
				// System.out.println(res.getFetchSize());
				// System.out.println(id);
			}

		} catch (Exception e) {
			System.err.println("Got an exception!");
			System.err.println(e.getMessage());
		}
		return id;
	}

	private static int getLastInsertID() throws Exception {
		int id = 0;
		try {
			Connection conn = DBProcTools.getConnection();

			String query = "SELECT LAST_INSERT_ID()";

			PreparedStatement prepStmt = conn.prepareStatement(query);

			boolean success = prepStmt.execute();

			ResultSet res = prepStmt.getResultSet();
			if (res.first()) {
				id = res.getInt(1);
			}
			conn.close();

		} catch (Exception e) {
			System.err.println("Got an exception!");
			System.err.println(e.getMessage());
		}
		return id;
	}

	public static int insertRecord(String fromTable, String... params) throws Exception {
		int id = 0;
		try {
			Connection conn = DBProcTools.getConnection();

			StringBuilder queryBuilder = new StringBuilder();
			queryBuilder.append("INSERT INTO ");
			queryBuilder.append(DATABASE_NAME);
			queryBuilder.append(".");
			queryBuilder.append(fromTable);
			queryBuilder.append(" (");

			if (params.length % 2 > 0)
				throw new Exception();

			for (int i = 0; i < params.length / 2; ++i) {
				if (i != 0)
					queryBuilder.append(",");
				queryBuilder.append(params[i]);
			}

			queryBuilder.append(") VALUES (");

			for (int i = 0; i < params.length / 2; ++i) {
				if (i == 0)
					queryBuilder.append("?");
				else
					queryBuilder.append(",?");
			}

			queryBuilder.append(")");

			String query = queryBuilder.toString();

			PreparedStatement prepStmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

			for (int i = params.length / 2, j = 1; i < params.length; ++i, ++j) {
				prepStmt.setString(j, params[i]);
			}

			// System.out.println(prepStmt);

			boolean success = prepStmt.execute();

			ResultSet keys = prepStmt.getGeneratedKeys();

			if (keys.first()) {
				id = keys.getInt(1);
				// System.out.println(success);
				// System.out.println(keys.getFetchSize());
				// System.out.println(id);
			}

		} catch (

		Exception e) {
			System.err.println("Got an exception!");
			System.err.println(e.getMessage());
		}
		return id;
	}

	public static int insertUniqueRecord(String fromTable, String keyName, String... params) throws Exception {
		int id = 0;

		id = getID(fromTable, keyName, params);

		if (id == 0)
			id = insertRecord(fromTable, params);

		return id;
	}

	public static void main(String[] args) throws Exception {

		DBProcTools.startConnection();

		int id;

		// getID Test
		//
		// id = getID("datasource", "datasourceid", new String[] { "datasourceid", "3" });
		//
		// System.out.println();
		// System.out.println(id);

		// getLastInsertID Test
		//
		// id = getLastInsertID();
		//
		// System.out.println();
		// System.out.println(id);
		//
		// DBProcTools.closeConnection();

		// insertRecord Test

		// id = insertRecord("datasource", new String[] { "name", " seedUrl", " directory", "1", "2", "3" });
		//
		// System.out.println();
		// System.out.println(id);

		// id = insertUniqueRecord("datasource", "datasourceid", new String[] { "name", "seedUrl", "directory", "1", "2", "3" });
		//
		// System.out.println();
		// System.out.println(id);

		DBProcTools.closeConnection();
	}
}
