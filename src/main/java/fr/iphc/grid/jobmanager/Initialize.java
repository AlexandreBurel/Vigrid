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

package fr.iphc.grid.jobmanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.job.JobDescription;
import org.ogf.saga.job.JobFactory;
import org.ogf.saga.url.URL;
import org.ogf.saga.url.URLFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import fr.iphc.grid.Global;

public class Initialize {

	public Initialize() {

	}

	public ArrayList<File> InitJdl(File file) throws IOException {
		ArrayList<File> Jdl = new ArrayList<File>();
		// Read jdl file
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			String line = null;

			while ((line = br.readLine()) != null) {
				Jdl.add(new File(line));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			br.close();
		} // Close the BufferedReader
		return Jdl;
	}

	public void InitJob(ArrayList<File> JdlList) {

		try {
			Iterator<File> i = JdlList.iterator();
			Integer index = 0;
			while (i.hasNext()) {
				File file = i.next();
				Properties prop = new Properties();
				FileInputStream jdl = new FileInputStream(file);
				prop.load(jdl);
				JobDescription desc = JobFactory.createJobDescription();
				setRequired(desc, prop, JobDescription.EXECUTABLE);
				setOptMulti(desc, prop, JobDescription.ARGUMENTS);
				setOptional(desc, prop, JobDescription.SPMDVARIATION);
				setOptional(desc, prop, JobDescription.TOTALCPUCOUNT);
				setOptional(desc, prop, JobDescription.NUMBEROFPROCESSES);
				setOptional(desc, prop, JobDescription.PROCESSESPERHOST);
				setOptional(desc, prop, JobDescription.THREADSPERPROCESS);
				setOptMulti(desc, prop, JobDescription.ENVIRONMENT);
				setOptional(desc, prop, JobDescription.WORKINGDIRECTORY);
				setOptional(desc, prop, JobDescription.INTERACTIVE);
				setOptional(desc, prop, JobDescription.INPUT);
				setRequired(desc, prop, JobDescription.OUTPUT);
				setRequired(desc, prop, JobDescription.ERROR);
				setReqMulti(desc, prop, JobDescription.FILETRANSFER);
				setOptional(desc, prop, JobDescription.CLEANUP);
				setOptional(desc, prop, JobDescription.JOBSTARTTIME);
				setOptional(desc, prop, JobDescription.WALLTIMELIMIT);
				setOptional(desc, prop, JobDescription.TOTALCPUTIME);
				setOptional(desc, prop, JobDescription.TOTALPHYSICALMEMORY);
				setOptional(desc, prop, JobDescription.CPUARCHITECTURE);
				setOptional(desc, prop, JobDescription.OPERATINGSYSTEMTYPE);
				setOptMulti(desc, prop, JobDescription.CANDIDATEHOSTS);
				setOptional(desc, prop, JobDescription.QUEUE);
				setOptional(desc, prop, JobDescription.JOBPROJECT);
				setOptMulti(desc, prop, JobDescription.JOBCONTACT);
				// Remove output file else exception
				rmFiletransfer(prop, JobDescription.FILETRANSFER);
				// Add ListJob
				Jdl job = new Jdl(index);
				index++;
				job.setName(file.getName());
				job.setDesc(desc);
				Global.ListJob.add(job);
				jdl.close();
			}
		} catch (Exception e) {
			System.err.println("Erreur InitJob\n" + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}

	}

	public void GlobalSetup(String SetupFile) {

		try {
			File setupXML = new File(SetupFile);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.parse(setupXML);
			Element root = dom.getDocumentElement();
			NodeList nodes = root.getChildNodes();
			for (int i = 0; i < nodes.getLength(); i++) {
				if ((nodes.item(i).getNodeName().compareTo("#text") != 0)
						&& (nodes.item(i).getNodeName().compareTo("#comment") != 0)) {
					if (nodes.item(i).getNodeName().compareTo("SEUILCEOK") == 0) {
						Global.SEUILCEOK = (Float.parseFloat(nodes.item(i).getTextContent()));
					}
					if (nodes.item(i).getNodeName().compareTo("SEUILDISPLAYLOG") == 0) {
						Global.SEUILDISPLAYLOG = (Float.parseFloat(nodes.item(i).getTextContent()));
					}
					if (nodes.item(i).getNodeName().compareTo("MAXTHREAD") == 0) {
						Global.MAXTHREAD = (Integer.parseInt(nodes.item(i).getTextContent()));
					}
					if (nodes.item(i).getNodeName().compareTo("TIMEOUTWAIT") == 0) {
						Global.TIMEOUTWAIT = (Integer.parseInt(nodes.item(i).getTextContent()));
					}
					if (nodes.item(i).getNodeName().compareTo("TIMEOUTRUN") == 0) {
						Global.TIMEOUTRUN = (Integer.parseInt(nodes.item(i).getTextContent()));
					}
					if (nodes.item(i).getNodeName().compareTo("TIMEOUTEND") == 0) {
						Global.TIMEOUTEND = (Integer.parseInt(nodes.item(i).getTextContent()));
					}
					if (nodes.item(i).getNodeName().compareTo("DAYRANGE") == 0) {
						Global.DAYRANGE = (Integer.parseInt(nodes.item(i).getTextContent()));
					}
					if (nodes.item(i).getNodeName().compareTo("OPTTIMEOUTRUN") == 0) {
						Global.OPTTIMEOUTRUN = (Boolean.parseBoolean(nodes.item(i).getTextContent()));
					}
				}

			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void PrintGlobalSetup() {

		System.out.println("Global parameters settings");
		System.out.println("  TIMEOUTWAIT:" + Global.TIMEOUTWAIT + " s");
		System.out.println("  TIMEOUTRUN:" + Global.TIMEOUTRUN + " s");
		System.out.println("  TIMEOUTEND:" + Global.TIMEOUTEND + " min");
		System.out.println("  MAXTHREAD:" + Global.MAXTHREAD);
		System.out.println("  SEUILDISPLAYLOG:" + Global.SEUILDISPLAYLOG);
		System.out.println("  SEUILCEOK:" + Global.SEUILCEOK + " Best:0.7");
		System.out.println("  DAYRANGE:" + Global.DAYRANGE + " day(s)");
		System.out.println("  BadCe:" + Global.BadCe);
		System.out.println("  Cwd:" + Global.Cwd);
		System.out.println("  OPTTIMEOUTRUN:" + Global.OPTTIMEOUTRUN + " [true/false]");
	}

	public void InitSosCe() {

		// Iterator<File> i = JdlList.iterator();
		try {
			URL uri = URLFactory.createURL("cream://sbgce2.in2p3.fr:8443/cream-pbs-biomed+?delegationId=iphc");
			// uri =
			// URLFactory.createURL("cream://creamce2.gina.sara.nl:8443/cream-pbs-short");
			Url url = new Url(uri);
			Boolean sbgce2Exist = false;
			for (Url urlList : Global.ListUrl) {
				if (urlList.getUrl().equals(url.getUrl())) {
					sbgce2Exist = true;
					break;
				}
			}
			Integer idx = 0;
			if (sbgce2Exist) {
				url.setTimeoutWait(Global.TIMEOUTWAIT);
				url.setTimeoutRun(Global.TIMEOUTRUN);
				Global.SosCe.add(url);
			} else {
				url = new Url(Global.ListUrl.get(idx).getUrl());
				url.setTimeoutWait(Global.TIMEOUTWAIT);
				url.setTimeoutRun(Global.TIMEOUTRUN);
				Global.SosCe.add(url);
				idx++;
			}
			// uri =
			// URLFactory.createURL("cream://prabi-ce3.ibcp.fr:8443/cream-pbs-biomed");
			url = new Url(Global.ListUrl.get(idx).getUrl());
			url.setTimeoutWait(Global.TIMEOUTWAIT);
			url.setTimeoutRun(Global.TIMEOUTRUN);
			Global.SosCe.add(url);
			uri = URLFactory.createURL("local://localhost");
			url = new Url(uri);
			url.setTimeoutWait(Global.TIMEOUTWAIT);
			url.setTimeoutRun(Global.TIMEOUTRUN);
			// Global.SosCe.add(url);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// Init ldap URL Ce
	public ArrayList<String> InitLdapUrl() throws Exception {
		ArrayList<String> ldapUrl = new ArrayList<String>();
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, "ldap://cclcgtopbdii01.in2p3.fr:2170");
		boolean success = false;
		int count = 0, MAX_TRIES = 5;
		while (!success && count < MAX_TRIES) {
			try {
				// Create initial context
				DirContext ctx = new InitialDirContext(env);
				SearchControls contraints = new SearchControls();
				contraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
				String[] attributIDs = { "GlueCEUniqueID", "GlueCeStateFreeJobSlots", "GlueForeignKey" };
				contraints.setReturningAttributes(attributIDs);
				String BASE_SEARCH = "Mds-Vo-name=local,o=grid";
				String filter = "(&" + "(GlueCEPolicyMaxCPUTime>=120)"
						+ "(objectClass=GlueCE)(GlueCEImplementationName=CREAM)(GlueCEAccessControlBaseRule=VO:biomed))";
				NamingEnumeration<SearchResult> answer = ctx.search(BASE_SEARCH, filter, contraints);

//				int index = 0;
				Map<String, Integer> tMap = new HashMap<String, Integer>();
				while (answer.hasMore()) {
//					index++;
					SearchResult result = answer.next();
					tMap.put(result.getAttributes().get("GlueCEUniqueID").get().toString(),
							Integer.parseInt(result.getAttributes().get("GlueCeStateFreeJobSlots").get().toString()));
					// System.out.println("cream://"+result.getAttributes().get("GlueCEUniqueID").get()+"
					// Free: "+
					// result.getAttributes().get("GlueCeStateFreeJobSlots").get());
				}
				// Close the context when we're done
				ctx.close();
				// Permet le tri sur valeur max de slot de libre
				for (Iterator<String> i = sortByValue(tMap).iterator(); i.hasNext();) {
					ldapUrl.add("cream://" + i.next());
				}
				success = true;
			} catch (Exception e) {
				e.printStackTrace();
				count++;
			}
		}
		if (!success || ldapUrl.size() == 0) {
			System.err.println("LDAP Error Server unavailable or List Ldap Empty");
			System.exit(-1);
		}
		return ldapUrl;
	}

	public static List<String> sortByValue(final Map<String, Integer> m) {
		List<String> keys = new ArrayList<String>();
		keys.addAll(m.keySet());
		Collections.sort(keys, new Comparator<Object>() {
			@SuppressWarnings("unchecked")
			public int compare(Object o1, Object o2) {
				Object v1 = m.get(o1);
				Object v2 = m.get(o2);
				if (v1 == null) {
					return (v2 == null) ? 0 : 1;
				} else if (v2 instanceof Comparable) {
					return ((Comparable<Object>) v2).compareTo(v1);
				} else {
					return 0;
				}
			}
		});
		return keys;
	}

	// Init URL Ce
	public void InitUrl(File file) throws Exception {
		Random rand = new Random();
		if (file == null) { // Requete via Sql
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(Context.PROVIDER_URL, "ldap://134.158.151.250");
			HashMap<Integer, String> CeList = new HashMap<Integer, String>();
			DirContext ctx = null;
			try {
				// Create initial context
				ctx = new InitialDirContext(env);
				SearchControls contraints = new SearchControls();
				contraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
				String[] attributIDs = { "uri", "rank", "avg" };
				contraints.setReturningAttributes(attributIDs);
				String BASE_SEARCH = "ou=egi,dc=iphc,dc=fr";
				String filter = "(objectClass=ceRanking)";
				NamingEnumeration<SearchResult> answer = ctx.search(BASE_SEARCH, filter, contraints);

				while (answer.hasMore()) {
					SearchResult result = (SearchResult) answer.next();
					Attributes attrs = result.getAttributes();
					String uri = attrs.get("uri").get().toString();
					String rank = attrs.get("r").get().toString();
					Float avg = Float.parseFloat(attrs.get("avg").get().toString());
					if (avg >= Global.SEUILCEOK) {
						CeList.put(Integer.parseInt(rank), uri);
					}
				}
			} catch (Exception e) {
				System.err.println("Could not connect to the local ldap server(bistro (" + e.getMessage() + ")");
				CeList = null;
			} finally {
				ctx.close();
			} // Close the BufferedReader
			ArrayList<String> BadCe = new ArrayList<String>();
			// Appel ldap pour CE avec temps CPU > 2H
			ArrayList<String> LdapUrl = InitLdapUrl();
			// Load BadCE
			if (Global.BadCe != null) {
				BufferedReader br = new BufferedReader(new FileReader(Global.BadCe));
				String input = null;
				System.out.println("List Bad Ce");
				try {
					while ((input = br.readLine()) != null) {
						if (input.contains("#")) {
							continue;
						}
						;
						BadCe.add(input);
						System.out.println("  " + input);
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					br.close();
				} // Close the BufferedReader
			}
			// CeList.retainAll(objArray2);
			if (CeList != null) {
				System.out.println("\nList CE Cream");
				for (Map.Entry<Integer, String> m : CeList.entrySet()) {
					String line = m.getValue().toString();
					if ((BadCe.contains(line)) || BadCe.contains(URLFactory.createURL(line).getHost())) {
						continue;
					}
					if (LdapUrl.contains(line)) {
						line = line + "?delegationId=" + rand.nextLong();
						URL uri = URLFactory.createURL(line);
						Url url = new Url(uri);
						url.setTimeoutWait(Global.TIMEOUTWAIT);
						url.setTimeoutRun(Global.TIMEOUTRUN);
						Global.ListUrl.add(url);
						System.out.println("  CE: " + line);
					} else {
						System.out.println("FAILEDLDAP: " + line);
					}
				}
			} else { // CeList != null
				for (String line : LdapUrl) {
					line = line + "?delegationId=" + rand.nextLong();
					URL uri = URLFactory.createURL(line);
					Url url = new Url(uri);
					url.setTimeoutWait(Global.TIMEOUTWAIT);
					url.setTimeoutRun(Global.TIMEOUTRUN);
					Global.ListUrl.add(url);
					System.out.println("  CE: " + line);
				}
			}
		} else { // else file not null
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = null;
			try {
				while ((line = br.readLine()) != null) {
					// Process the data, here we just print it out
					line = line + "?delegationId=" + rand.nextLong();
					URL uri = URLFactory.createURL(line);
					Url url = new Url(uri);
					url.setTimeoutWait(Global.TIMEOUTWAIT);
					url.setTimeoutRun(Global.TIMEOUTRUN);
					Global.ListUrl.add(url);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				br.close(); // Close the BufferedReader
			}
		}
		return;
	}

	private static void setRequired(JobDescription desc, Properties prop, String name) throws Exception {
		String value = prop.getProperty(name);
		if (value != null) {
			desc.setAttribute(name, value);
		} else {
			throw new BadParameterException("Missing required attribute: " + name);
		}
	}

	private static void setOptional(JobDescription desc, Properties prop, String name) throws Exception {
		String value = prop.getProperty(name);
		if (value != null) {
			desc.setAttribute(name, value);
		}
	}

	private static void setOptMulti(JobDescription desc, Properties prop, String name) throws Exception {
		String values = prop.getProperty(name);
		if (values != null) {
			desc.setVectorAttribute(name, values.split(","));
		}
	}

	private static void setReqMulti(JobDescription desc, Properties prop, String name) throws Exception {
		String values = prop.getProperty(name);
		if (values != null) {
			desc.setVectorAttribute(name, values.split(","));
		} else {
			throw new BadParameterException("Missing required attribute: " + name);
		}
	}

//	private static void copyStream(InputStream in, OutputStream out) throws IOException {
//		byte[] buffer = new byte[1024];
//		for (int len; (len = in.read(buffer)) > 0;) {
//			out.write(buffer, 0, len);
//		}
//	}

	private static void rmFiletransfer(Properties prop, String name) throws Exception {
		String values = prop.getProperty(name);
		if (values != null) {
			String[] file;
			file = values.split(",");
			for (String s : file) {
				String[] inout;
				inout = s.split("<");
				if (inout.length == 2) {
					try {
						// Construct a File object for the file to be deleted.
						File target = new File(inout[0]);
						if (target.exists()) {
							target.delete();
						}
					} catch (SecurityException e) {
						System.err.println("Unable to delete " + inout[0] + e.getMessage());
					}
				}
			}
		} else {
			throw new BadParameterException("Missing required attribute: " + name);
		}
	}

	public static void rmLoadFailed(String loadfailed) throws Exception {
		File target = new File(loadfailed);
		if (target.exists()) {
			target.delete();
		}
	}
}
