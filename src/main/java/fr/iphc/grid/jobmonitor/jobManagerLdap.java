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

package fr.iphc.grid.jobmonitor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import fr.iphc.grid.Config;
import fr.iphc.grid.Global;
import fr.iphc.grid.MySQLAccess;
import fr.iphc.grid.RangeCe;

public class jobManagerLdap {

	public static DirContext entryUpdateInLdap() {
		DirContext dirContext = null;
		try {
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(Context.PROVIDER_URL, Config.ldapUrl());
			String AdminDn = Config.ldapDN();
			String password = Config.ldapPassword();
			env.put(Context.SECURITY_PRINCIPAL, AdminDn);
			env.put(Context.SECURITY_CREDENTIALS, password);
			dirContext = new InitialDirContext(env);
			return (dirContext);
		} catch (Exception ee) {
			ee.printStackTrace();
			System.exit(0);
		}
		return dirContext;
	}

	public static void updateLdapCe() throws NumberFormatException, NamingException {

		SimpleDateFormat formater = null;
		Date aujourdhui = new Date();
		formater = new SimpleDateFormat("yyyyMMddHHmm");
		String date = formater.format(aujourdhui);
		DirContext dirContent = entryUpdateInLdap();
		MySQLAccess sql = new MySQLAccess();
		sql.connectDataBase();
		ArrayList<RangeCe> CeList = sql.RangeBestCeWithLastOKandTime(Global.DAYRANGE, Global.TIMEOUTWAIT);
		Iterator<RangeCe> k = CeList.iterator();
		Integer index = 1;
		while (k.hasNext()) {
			RangeCe ce = k.next();
			if (((float) ce.getNb_ok() / (float) ce.getNb_req()) >= Global.SEUILCEOK) {
				String line = "cream://" + ce.getCe() + ":8443" + ce.getPath();
				String filter = "uri=" + line;
				String[] attribut = { "uri" };
				NamingEnumeration<SearchResult> answer = searchLdapCe(filter, attribut);
				if (answer.hasMore()) {
//					SearchResult result = (SearchResult) answer.next();
//					Attributes attrs = result.getAttributes();
//					Attribute uri = attrs.get("uri");
					ModificationItem[] modItems = new ModificationItem[3];
					modItems[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
							new BasicAttribute("r", index.toString()));
					modItems[1] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("date", date));
					modItems[2] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
							new BasicAttribute("avg", Float.toString((float) ce.getNb_ok() / (float) ce.getNb_req())));
					String entryDN = "uri=" + line + "," + Config.ldapOU();
					Name composite = new CompositeName().add(entryDN);
					dirContent.modifyAttributes(composite, modItems);
					index++;
				} else { // add new CE
					Attribute uri = new BasicAttribute("uri", line);
					Attribute rank = new BasicAttribute("rank", index.toString());
					Attribute dateCe = new BasicAttribute("date", date);
					Attribute celdap = new BasicAttribute("ce", ce.getCe());
					Attribute pathldap = new BasicAttribute("path", new CompositeName().add(ce.getPath()).toString());
					Attribute impl = new BasicAttribute("impl", "cream");
					Attribute avg = new BasicAttribute("avg",
							Float.toString((float) ce.getNb_ok() / (float) ce.getNb_req()));
					String entryDN = "uri=" + line + "," + Config.ldapOU();
					Name composite = new CompositeName().add(entryDN);
					Attribute oc = new BasicAttribute("objectClass");
					oc.add("ceRanking");
					Attributes entry = new BasicAttributes();
					entry.put(oc);
					entry.put(uri);
					entry.put(rank);
					entry.put(celdap);
					entry.put(pathldap);
					entry.put(impl);
					entry.put(dateCe);
					entry.put(avg);
					dirContent.createSubcontext(composite, entry);
					index++;
				}
			}
		} // END while
		// remove entry Ce
		String filter = "!(date=" + date + ")";
		String[] attribut = { "uri" };
		NamingEnumeration<SearchResult> answer = searchLdapCe(filter, attribut);
		while (answer.hasMore()) {
			SearchResult result = (SearchResult) answer.next();
//			Attributes attrs = result.getAttributes();
//			Attribute uri = attrs.get("uri");
			String dn = result.getName().replace("\"", "") + "," + Config.ldapOU();
			Name entryDn = new CompositeName().add(dn);
			dirContent.destroySubcontext(entryDn);
		}
		dirContent.close();
	}

	public static NamingEnumeration<SearchResult> searchLdapCe(String addFilter, String[] attributIDs) {
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, "ldap://134.158.151.250");
		NamingEnumeration<SearchResult> answerfailed = null;
		try {
			// Create initial context
			DirContext ctx = new InitialDirContext(env);
			SearchControls contraints = new SearchControls();
			contraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
			contraints.setReturningAttributes(attributIDs);
			String BASE_SEARCH = Config.ldapOU();
			String filter = "(&(objectClass=ceRanking)(" + addFilter + "))";
			NamingEnumeration<SearchResult> answer = ctx.search(BASE_SEARCH, filter, contraints);
			ctx.close();
			return (answer);
		} catch (Exception e) {
			e.printStackTrace();
		}
		;
		return (answerfailed);
	}

}
