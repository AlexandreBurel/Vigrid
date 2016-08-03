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

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import fr.iphc.grid.jobmanager.Jdl;
import fr.iphc.grid.jobmanager.Url;

public class Global {
//	private static final Boolean TRUE = null;
	public static Integer TIMEOUTWAIT=900;  //en secondes
	public static Integer TIMEOUTRUN=1800*2; //30mn 1/2h*N secondes
	public static Integer TIMEOUTEND=60*21; //en minutes
	public static Integer MAXTHREAD=10;	//max job request launch via execute -> jobthread
	public static Integer NBTHREAD=0;
	public static Float SEUILDISPLAYLOG=(float) 0.8;
	public static Float SEUILCEOK=(float)0.7;//seuil inferieur rapport best CE en 75 %
	public static Integer DAYRANGE=3;	//Intervalle jour 
	public static Boolean END=false;	// Flag end thread
	public static File file=null; // necessaire pour relancer url si liste vide
	public static Integer LASTCALLBACK=0;
	public static ExecutorService getOutputexecutor=null;
	public static ArrayList<Jdl> ListJob= new ArrayList<Jdl>();
	public static ArrayList<Url> ListUrl= new ArrayList<Url>();
	public static ArrayList<Url> SosCe= new ArrayList<Url>();  // liste de secours CE fiable sbgce2
	public static File BadCe= null;
	public static String Cwd= null; //current working direct for mon (grid.log jobmon)
	public static Boolean OPTTIMEOUTRUN=false;	// optimize run timeout
	public static Integer WAITLOAD=5*1000; //10s en ms
	public static String proxy="/home/dsa/.globus/biomed.txt";
}

