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

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
//import javax.naming.directory.Attribute;
//import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

public class SearchCEfromTopBDII {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, "ldap://cclcgtopbdii01.in2p3.fr:2170");

		try {
			// Create initial context
			DirContext ctx = new InitialDirContext(env);
			SearchControls contraints = new SearchControls();
			contraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
			String[] attributIDs = { "GlueCEUniqueID" };
			contraints.setReturningAttributes(attributIDs);
			String BASE_SEARCH = "Mds-Vo-name=local,o=grid";

			// Attributes matchAttrs = new BasicAttributes(true);
			// matchAttrs.put(new BasicAttribute("GlueCEUniqueID"));
			// matchAttrs.put(new BasicAttribute("mail"));

			// Search for objects that have those matching attributes

			String filter = "(&(objectClass=GlueCE)(GlueCEImplementationName=CREAM)(GlueCEAccessControlBaseRule=VO:biomed))";
			;
			NamingEnumeration<SearchResult> answer = ctx.search(BASE_SEARCH, filter, contraints);
//			int index = 0;
			while (answer.hasMore()) {
//				index++;
				SearchResult result = answer.next();
//				Attributes attrs = result.getAttributes();
//				NamingEnumeration f = attrs.getAll();
//				Attribute attr = (Attribute) f.next();
				// SearchResult sr = (SearchResult)answer.next();
				// System.out.println(">>>" + sr.getName());
				System.out.println("cream://" + result.getAttributes().get("GlueCEUniqueID").get());
				// System.out.println(index+"-"+attr.get());
			}
			// Close the context when we're done
			ctx.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
