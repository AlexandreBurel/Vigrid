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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.log4j.chainsaw.Main;

public class Config {
	
	private static String propertiesFileName = "application.conf";
	private static Properties properties = null;

	private static void initializeIfNeeded() {
		if(properties == null) {
			// lazy loading
			properties = new Properties();
			try(InputStream input = Main.class.getClassLoader().getResourceAsStream(propertiesFileName)) {
				if(input == null) {
					System.out.println("Can't find properties");
				}
				properties.load(input);
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	// example: Config.get("max.file.size")
	private static Object _get(String key) {
		initializeIfNeeded();
		return properties.getProperty(key);
	}
	
	public static Integer getInteger(String key) {
		Object value = _get(key);
		if(value == null)
			return null;
		return new Integer(value.toString());
	}
	
	public static String get(String key) {
		Object value = _get(key);
		if(value == null)
			return null;
		return value.toString();
	}
	
	public static ArrayList<String> getPropertyKeys(String regex) {
		initializeIfNeeded();
		ArrayList<String> keys = new ArrayList<String>();
		for(String key: properties.stringPropertyNames()) {
			if(key.matches(regex))
				keys.add(key);
		}
		return keys;
	}
	
	public static String mySqlUrl() {
		return "jdbc:mysql://"+_get("host")+":"+_get("port")+"/"+_get("database")+"";
	}
	
	public static String mySqlUser() {
		return get("user");
	}
	
	public static String mySqlPassword() {
		return get("password");
	}
	
	public static String ldapUrl() {
		return "ldap://"+_get("ldap-host");
	}
	
	public static String ldapDN() {
		return get("ldap-dn");
	}
	
	public static String ldapOU() {
		return get("ldap-ou");
	}
	
	public static String ldapPassword() {
		return get("ldap-password");
	}
}
