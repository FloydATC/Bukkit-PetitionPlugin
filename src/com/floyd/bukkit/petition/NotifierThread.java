package com.floyd.bukkit.petition;

import java.io.File;

import java.util.HashMap;
import java.util.regex.Matcher;

import org.bukkit.entity.Player;
import java.sql.*;


public class NotifierThread extends Thread {
	private Boolean stop = false;
	private Long interval = 300000L; // 5 minutes
	private HashMap<String, Integer> count = new HashMap<String, Integer>();
	private PetitionPlugin plugin = null;
    private String baseDir = "plugins/PetitionPlugin";
    private Player player = null;

    public NotifierThread(PetitionPlugin owner) {
    	plugin = owner;
    }

	// This method is called when the thread runs
    public void run() {
		System.out.println("[Pe] NotifierThread started");

    	while (stop == false) {
    		// Go to sleep
    		try {
				sleep(interval);
			} catch (InterruptedException e) {
				System.out.println("[Pe] NotifierThread sleep interrupted");
			}
			// Count open petitions per player
			Connection dbh = null;
			if (plugin.dbpool != null) { dbh = plugin.dbpool.getConnection(); }
			count.clear();
			File dir = new File(baseDir);
			for (String filename : dir.list()) {
				if (stop) { return; }
				if (filename.endsWith(".ticket")) {
					String[] parts = filename.split("['.']");
            		Integer id = Integer.valueOf(parts[0]);
            		try {
            	    	while (!plugin.SetPetitionLock(id, "*NotifierThread*", false)) {
            	    		try {
            	    			Thread.sleep(100);
            	    		}
            	    		catch (InterruptedException e) {
            	    		}
            	    	}
    					PetitionObject petition = new PetitionObject(id, dbh);
    					if (petition.isValid()) {
    						Integer found = count.get(petition.Owner());
    						if (found == null) { found = 0; }
    						count.put(petition.Owner(), found + 1);
    					}
            		}
            		finally {
            	   		plugin.SetPetitionLock(id, "*NotifierThread*", true);
            		}
				}
			}

			// Notify each player and get total
			Integer total = 0;
			for (String name : count.keySet()) {
				Integer found = count.get(name);
				total = total + found;
				Player p = plugin.getServer().getPlayer(name);
				if (p != null) {
					if (found == 1) {
						p.sendMessage("[Pe] §7You have 1 open " + plugin.settings.get("single").toLowerCase() + " waiting, use '/pe list' to review");
					} else {
						p.sendMessage("[Pe] §7You have " + found + " open " + plugin.settings.get("plural").toLowerCase() + " waiting, use '/pe list' to review");
					}
				}
			}
			
			// Notify admins about the total
			String[] except = new String[0];
			if (total > 0) {
				if (total == 1) {
					plugin.notifyModerators("[Pe] §7There is 1 open " + plugin.settings.get("single").toLowerCase() + " waiting, use '/pe list' to review", except);
				} else {
					plugin.notifyModerators("[Pe] §7There are " + total + " open " + plugin.settings.get("plural").toLowerCase() + " waiting, use '/pe list' to review", except);
				}
			}
    	
    	}
		System.out.println("[Pe] NotifierThread stopped");
    }
    
    public void signalStop() {
    	stop = true;
		System.out.println("[Pe] NotifierThread set to stop");
    }

    public void setInterval(Integer sec) {
    	Long ms = sec * 1000L;    	
    	if (ms < 30000) { ms = 30000L; }
    	interval = ms;
		System.out.println("[Pe] NotifierThread interval set to " + sec + " seconds");
    }
    
}
