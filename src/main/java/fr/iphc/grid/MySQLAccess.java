/*
Copyright CNRS - IPHC - LSMBO 2016
Authors: 
- Alexandre Burel: alexandre.burel@unistra.fr
- Patrick Guterl: Patrick.Guterl@unistra.fr

This software is a computer program whose purpose is to submit jobs on a 
computing grid using the JSAGA API. It is composed of two modules, a JobMonitor 
meant to run as a service to check periodically the state of each nodes on the 
grid, and a JobManager that will send the jobs on the best nodes at the moment 
of the submission.

This software is governed by the CeCILL license under French law and
abiding by the rules of distribution of free software.  You can  use, 
modify and/ or redistribute the software under the terms of the CeCILL
license as circulated by CEA, CNRS and INRIA at the following URL
"http://www.cecill.info". 

As a counterpart to the access to the source code and  rights to copy,
modify and redistribute granted by the license, users are provided only
with a limited warranty  and the software's author,  the holder of the
economic rights,  and the successive licensors  have only  limited
liability. 

In this respect, the user's attention is drawn to the risks associated
with loading,  using,  modifying and/or developing or reproducing the
software by the user in light of its specific status of free software,
that may mean  that it is complicated to manipulate,  and  that  also
therefore means  that it is reserved for developers  and  experienced
professionals having in-depth computer knowledge. Users are therefore
encouraged to load and test the software's suitability as regards their
requirements in conditions enabling the security of their systems and/or 
data to be ensured and,  more generally, to use and operate it in the 
same conditions as regards security. 

The fact that you are presently reading this means that you have had
knowledge of the CeCILL license and that you accept its terms.
*/

package fr.iphc.grid;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class MySQLAccess {

	private Connection connect = null;

	private PreparedStatement preparedStatement = null;
	private ResultSet rs = null;

	public void connectDataBase() {
		try {
			try {
				Class.forName("com.mysql.jdbc.Driver");
			} catch (ClassNotFoundException e) {
				System.err.println("Driver jdbc Mysql not found\n Exit");
				System.exit(-1);
			}
			connect = DriverManager.getConnection(Config.mySqlUrl(), Config.mySqlUser(), Config.mySqlPassword());

		} catch (SQLException ex) {
			while (ex != null) {
				System.err.println("Message: " + ex.getMessage());
				System.err.println("SQLState: " + ex.getSQLState());
				System.err.println("ErrorCode: " + ex.getErrorCode());
				ex = ex.getNextException();
				System.err.println("");
			}
			System.err.println("Connection to the MySQL database has failed, check the error messages above to see what went wrong");
			System.err.println("If the database does not exist, use the script vigrid.sql in this jar to create it");
			System.exit(-1);
		}
	}
	
	public void closeDataBase() {
		try {
			connect.close();
		} catch (SQLException ex) {
			while (ex != null) {
				System.err.println("Message: " + ex.getMessage());
				System.err.println("SQLState: " + ex.getSQLState());
				System.err.println("ErrorCode: " + ex.getErrorCode());
				ex = ex.getNextException();
				System.err.println("");
			}
			System.exit(-1);
		}
	}
	
//	public void writeDataBase(CeDefinition ce) {
//		try {
//			String sql = "INSERT INTO monce(host,path,implementation,date,state,temps,msg)  VALUES(?,?,?,?,?,?,?)";
//			preparedStatement = connect.prepareStatement(sql);
//			preparedStatement.setString(1, ce.getCe());
//			preparedStatement.setString(2, ce.getPath());
//			preparedStatement.setString(3, ce.getImplementation());
//			java.sql.Timestamp dateSql = new java.sql.Timestamp(ce.getDate().getTime());
//			preparedStatement.setTimestamp(4, dateSql);
//			preparedStatement.setString(5, ce.getState());
//			preparedStatement.setInt(6, ce.getTemps());
//			preparedStatement.setString(7, ce.getMsg());
//			preparedStatement.execute();
//		} catch (SQLException ex) {
//			while (ex != null) {
//				System.out.println("Message: " + ex.getMessage());
//				System.out.println("SQLState: " + ex.getSQLState());
//				System.out.println("ErrorCode: " + ex.getErrorCode());
//				ex = ex.getNextException();
//				System.out.println("");
//			}
//			System.exit(-1);
//		}
//	}
	
	public void writeDataBase(CeDefinition ce, String TableSql) {
		try {
			connect.setAutoCommit(false);
			String sql = "INSERT INTO " + TableSql
					+ " (host,path,implementation,date,state,temps,msg,jobid)  VALUES(?,?,?,?,?,?,?,?)";
			preparedStatement = connect.prepareStatement(sql);
			preparedStatement.setString(1, ce.getCe());
			preparedStatement.setString(2, ce.getPath());
			preparedStatement.setString(3, ce.getImplementation());
			java.sql.Timestamp sqlDate = new java.sql.Timestamp(ce.getDate().getTime());
			preparedStatement.setTimestamp(4, sqlDate);
			preparedStatement.setString(5, ce.getState());
			preparedStatement.setInt(6, ce.getTemps());
			preparedStatement.setString(7, ce.getMsg());
			preparedStatement.setString(8, ce.getJobid());
			preparedStatement.execute();
			connect.commit();
		} catch (SQLException ex) {
			while (ex != null) {
				System.err.println("Message: " + ex.getMessage());
				System.err.println("SQLState: " + ex.getSQLState());
				System.err.println("ErrorCode: " + ex.getErrorCode());
				ex = ex.getNextException();
				System.err.println("");
			}
			if (connect != null) {
				try {
					System.err.print("Transaction is being rolled back");
					connect.rollback();
				} catch (SQLException excep) {
					System.err.println("ErrorCode rollback: " + excep.getErrorCode());
				}
			}
		}
	}
	
	public ArrayList<RangeCe> RangeBestCeWithLastOKandTime(int range, int thr_temps) {
		ArrayList<RangeCe> CeList = new ArrayList<RangeCe>();
		try {
			String sql = "call mongrid.RangeBestCeWithLastOKandTime(?,?)";
			preparedStatement = connect.prepareCall(sql);
			preparedStatement.setInt(1, range);
			preparedStatement.setInt(2, thr_temps);
			rs = preparedStatement.executeQuery();
			while (rs.next()) {
				RangeCe ce = new RangeCe();
				ce.setCe(rs.getString("host"));
				ce.setPath(rs.getString("path"));
				ce.setNb_ok(rs.getInt("Nb_ok"));
				ce.setTemps(rs.getInt("temps"));
				ce.setNb_req(rs.getInt("Nb_req"));
				ce.setLast_date(new java.util.Date(rs.getTimestamp("Last_req").getTime()));
				ce.setRange(range);
				CeList.add(ce);
			}
			return CeList;
		} catch (SQLException ex) {
			while (ex != null) {
				System.out.println("Message: " + ex.getMessage());
				System.out.println("SQLState: " + ex.getSQLState());
				System.out.println("ErrorCode: " + ex.getErrorCode());
				ex = ex.getNextException();
				System.out.println("");
			}
			return null;
		}
	}
	
	public ArrayList<CeDefinition> LastBestCe() {
		ArrayList<CeDefinition> CeList = new ArrayList<CeDefinition>();
		try {
			String sql = "call LastBestCe()";
			preparedStatement = connect.prepareCall(sql);
			rs = preparedStatement.executeQuery();
			while (rs.next()) {
				CeDefinition ce = new CeDefinition();
				ce.setCe(rs.getString("host"));
				ce.setPath(rs.getString("path"));
				// java.sql.Timestamp ts = rs.getTimestamp("date");
				java.util.Date dt = new java.util.Date(rs.getTimestamp("date").getTime());
				ce.setDate(dt);
				ce.setTemps(rs.getInt("temps"));
				ce.setState("OK");
				// int row = rs.getRow();
				// System.out.println(ce.getCe()+"\t"+ce.getPath()+"\t"+ce.getDate()+"\t"+ce.getState()+"\t"+ce.getTemps()+"\t");
				// System.out.println ("ROW: "+ row);
				CeList.add(ce);
			}
			return CeList;
		} catch (SQLException ex) {
			while (ex != null) {
				System.out.println("Message: " + ex.getMessage());
				System.out.println("SQLState: " + ex.getSQLState());
				System.out.println("ErrorCode: " + ex.getErrorCode());
				ex = ex.getNextException();
				System.out.println("");
			}
			return null;
		}
	}

	public ArrayList<RangeCe> RangeBestCe(int range) {
		ArrayList<RangeCe> CeList = new ArrayList<RangeCe>();
		try {
			String sql = "call mongrid.RangeBestCE(?)";
			preparedStatement = connect.prepareCall(sql);
			preparedStatement.setInt(1, range);
			rs = preparedStatement.executeQuery();
			while (rs.next()) {
				RangeCe ce = new RangeCe();
				ce.setCe(rs.getString("host"));
				ce.setPath(rs.getString("path"));
				ce.setNb_ok(rs.getInt("Nb_ok"));
				ce.setTemps(rs.getInt("temps"));
				ce.setNb_req(rs.getInt("Nb_req"));
				ce.setLast_date(new java.util.Date(rs.getTimestamp("Last_req").getTime()));
				ce.setRange(range);
				// int row = rs.getRow();
				//// System.out.println(ce.getCe()+"\t"+ce.getPath()+"\t"+ce.getDate()+"\t"+ce.getState()+"\t"+ce.getTemps()+"\t");
				//// System.out.println ("ROW: "+ row);
				CeList.add(ce);
			}
			return CeList;
		} catch (SQLException ex) {
			while (ex != null) {
				System.out.println("Message: " + ex.getMessage());
				System.out.println("SQLState: " + ex.getSQLState());
				System.out.println("ErrorCode: " + ex.getErrorCode());
				ex = ex.getNextException();
				System.out.println("");
			}
			return null;
		}
	}
}
