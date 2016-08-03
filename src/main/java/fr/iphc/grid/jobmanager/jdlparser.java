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

import org.glite.jdl.JobAd;
import org.glite.wms.wmproxy.StringAndLongList;
import org.glite.wms.wmproxy.StringAndLongType;
import org.glite.wms.wmproxy.WMProxyAPI;
import org.ogf.saga.context.Context;
import org.ogf.saga.session.Session;
import org.ogf.saga.session.SessionFactory;

import fr.in2p3.jsaga.impl.context.ConfigurableContextFactory;
import fr.in2p3.jsaga.impl.context.ConfiguredContext;

public class jdlparser {
	public jdlparser() {
	};

	private static String getTab(String s) {
		int t = 50 - s.length();
		String ws = "";
		for (int i = 0; i < t; i++) {
			ws += " ";
		}
		return ws;
	}

	public static void runTest(String url, String jdlFile, String delegationID, String proxyFile, String certsPath)
			throws java.lang.Exception {
		WMProxyAPI client = null;
		String jdlString = "";
		StringAndLongList result = null;
		StringAndLongType[] list = null;
		// Prints out the input parameters
		System.out.println("TEST : JobListMatch");
		System.out.println(
				"************************************************************************************************************************************");
		System.out.println("WS URL	 		= [" + url + "]");
		System.out.println(
				"--------------------------------------------------------------------------------------------------------------------------------");
		System.out.println("JDL-FILE		= [" + jdlFile + "]");
		System.out.println(
				"--------------------------------------------------------------------------------------------------------------------------------");

		System.out.println("delegationID		= [" + delegationID + "]");
		System.out.println(
				"--------------------------------------------------------------------------------------------------------------------------------");
		System.out.println("proxy			= [" + proxyFile + "]");
		System.out.println(
				"--------------------------------------------------------------------------------------------------------------------------------");
		// Reads JDL
		JobAd jad = new JobAd();
		jad.fromFile(jdlFile);
		jdlString = jad.toString();
		// jdlString="[Type=\"Job\";Executable=\"/bin/date\";VirtualOrganisation=\"biomed\";"+
		// "requirements=((other.GlueCeStateFreeJobSlots>=10)&&
		// (other.GlueCEStateStatus==\"Production\")&&(other.GlueCEImplementationName==\"CREAM\"));"+
		// "rank=(other.GlueCeStateFreeJobSlots)]";
		System.out.println("jdlString		= [" + jdlString + "]");
		System.out.println(
				"--------------------------------------------------------------------------------------------------------------------------------");
		if (certsPath.length() > 0) {
			System.out.println("CAs path		= [" + certsPath + "]");
			System.out.println(
					"--------------------------------------------------------------------------------------------------------------------------------");
			client = new WMProxyAPI(url, proxyFile, certsPath);
		} else {
			client = new WMProxyAPI(url, proxyFile);
		}

		String proxy = client.getProxyReq(delegationID);
		client.grstPutProxy(delegationID, proxy);
		// Test
		System.out.println("Testing ....");
		result = client.jobListMatch(jdlString, delegationID);
		System.out.println("End of the test.\n");
		// Results
		if (result != null) {
			System.out.println("Result:");
			System.out.println("=======================================================================");
			// list of CE's+their ranks
			list = (StringAndLongType[]) result.getFile();
			if (list != null) {
				int size = list.length;
				for (int i = 0; i < size; i++) {
					String ce = list[i].getName();
					System.out.println("- " + ce + getTab(ce) + list[i].getSize());
				}
			} else {
				System.out.println("No Computing Element matching your job requirements has been found!");
			}
			System.out.println("=======================================================================");
		}
	}

	/**
	 * main
	 */
	public static void main(String[] args) throws Exception {
		String url = "";
		String jdlFile = "";
		String proxyFile = "";
		String delegationID = "";
		String certsPath = "";
		// Reads the input arguments
		// if ((args == null) || (args.length < 4)) {
		// throw new Exception ("error: some mandatory input parameters are
		// missing (<WebServices URL> <delegationID> <proxyFile> <JDL-FIlePath>
		// [CAs paths (optional)])");
		// } else if (args.length > 5) {
		// throw new Exception ("error: too many parameters\nUsage: java
		// <package>.<class> <WebServices URL> <delegationID> <proxyFile>
		// <JDL-FIlePath> [CAs paths (optional)]");
		// }
		url = "https://sbgwms1.in2p3.fr:7443/glite_wms_wmproxy_server";
		delegationID = "iphcID";
		// proxyFile = "/home/dsa/.globus/biomed.txt";
		jdlFile = "/home/dsa/jsaga_iphc/file.jdl";
		// certsPath = "/home/dsa/.globus/certificates";
		Session session = SessionFactory.createSession(false);
		ConfiguredContext[] configContexts = ConfigurableContextFactory.listConfiguredContext();
		Context context = ConfigurableContextFactory.createContext(configContexts[0]);
		System.out.println("Prefix:" + context.getAttribute(Context.USERPROXY));
		certsPath = context.getAttribute(Context.CERTREPOSITORY);
		proxyFile = context.getAttribute(Context.USERPROXY);
		session.close();

		// url = args[0];
		// delegationID = args[1];
		// proxyFile = args[2];
		// jdlFile = args[3];

		// if (args.length == 5) {
		// certsPath = args[4];
		// } else {
		// certsPath = "";
		// }
		// Launches the test
		runTest(url, jdlFile, delegationID, proxyFile, certsPath);
	}

}
