package com.bukkit.floyd.petition;


import java.io.*;

import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.Server;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import java.util.logging.Logger;

import com.nijikokun.bukkit.Permissions.Permissions;

/**
* Petition plugin for Bukkit
*
* @author FloydATC
*/
public class PetitionPlugin extends JavaPlugin {
    private final PetitionPlayerListener playerListener = new PetitionPlayerListener(this);

    private final ConcurrentHashMap<Player, Boolean> debugees = new ConcurrentHashMap<Player, Boolean>();
    private final ConcurrentHashMap<Integer, String> semaphores = new ConcurrentHashMap<Integer, String>();
    public final ConcurrentHashMap<String, String> settings = new ConcurrentHashMap<String, String>();

    public static Permissions Permissions = null;
    
    String baseDir = "plugins/PetitionPlugin";
    String archiveDir = "archive";
    String ticketFile = "last_ticket_id.txt";
    String configFile = "settings.txt";

	public static final Logger logger = Logger.getLogger("Minecraft.PetitionPlugin");
    
    public PetitionPlugin(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
        super(pluginLoader, instance, desc, folder, plugin, cLoader);
        // TODO: Place any custom initialization code here

        // NOTE: Event registration should be done in onEnable not here as all events are unregistered when a plugin is disabled
    }

    public void onDisable() {
        // TODO: Place any custom disable code here

        // NOTE: All registered events are automatically unregistered when a plugin is disabled
    	
        // EXAMPLE: Custom code, here we just output some info so we can check all is well
    	PluginDescriptionFile pdfFile = this.getDescription();
		logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is disabled!" );
    }

    public void onEnable() {
        // TODO: Place any custom enable code here including the registration of any events

    	preFlightCheck();
    	setupPermissions();
    	loadSettings();
    	
        // Register our events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.PLAYER_COMMAND, playerListener, Priority.Normal, this);

        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
		logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
    }

    public boolean isDebugging(final Player player) {
        if (debugees.containsKey(player)) {
            return debugees.get(player);
        } else {
            return false;
        }
    }

    public void setDebugging(final Player player, final boolean value) {
        debugees.put(player, value);
    }
    
    // Code from author of Permissions.jar
    public void setupPermissions() {
    	Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");

    	if(this.Permissions == null) {
    	    if(test != null) {
    	    	this.Permissions = (Permissions)test;
    	    } else {
    			logger.info( "Permission system not enabled. Disabling plugin." );
    	    	this.getServer().getPluginManager().disablePlugin(this);
    	    }
    	}
    }
    
    public synchronized boolean SetPetitionLock(Integer id, String owner, Boolean release) {
    	if (release == false) {
    		// Check for lingering lock by the same player
    		if (semaphores.containsKey(id) && semaphores.get(id).equals(owner)) {
    			logger.severe( "INTERNAL ERROR! Petition #" + id + " is ALREADY locked by " + semaphores.get(id) );
    			logger.severe( "This was probably caused by a previous crash while accessing this petition." );
    			logger.severe( "Please report this issue to the plugin author." );
    			return true;
    		}
    		// Get lock
    		if (semaphores.containsKey(id)) {
    			logger.warning( "Denied " + owner + " lock on #" + id + "; currently locked by " + semaphores.get(id) );
    		} else {
    			semaphores.put(id, owner);
    			return true;
    		}
    	} else {
    		// Release lock
    		if (semaphores.containsKey(id) && semaphores.get(id) == owner) {
    			semaphores.remove(id);
    			return true;
    		}
    	}
    	return false;
    }
    
    public synchronized Integer IssueUniqueTicketID() {
    	String fname = baseDir + "/" + ticketFile;
		String line = null;

		// Read the current file (if it exists)
		try {
    		BufferedReader input =  new BufferedReader(new FileReader(fname));
    		if (( line = input.readLine()) != null) {
    			line = line.trim();
    		}
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}

    	// Unsuccessful? Assume the file is invalid or does not exist
    	if (line == null) {
    		line = "0";
    	}
    	
    	// Increment the counter
    	line = String.valueOf(Integer.parseInt(line) + 1);
    	
    	// Write the new last ticket id
   		BufferedWriter output;
		String newline = System.getProperty("line.separator");
   	    try {
       		output = new BufferedWriter(new FileWriter(fname));
       		output.write( line + newline );
   	    	output.close();
   	    }
   	    catch (Exception e) {
    		e.printStackTrace();
   	    }
    	
		logger.fine( "Issued ticket #" + line );
    	return Integer.valueOf(line);
    }
    
    private void loadSettings() {
    	String fname = baseDir + "/" + configFile;
		String line = null;

		// Load the settings hash with defaults
		settings.put("command", "petition");
		settings.put("commandalias", "pe");
		settings.put("single", "Petition");
		settings.put("plural", "Petitions");

		settings.put("notify-owner-on-assign", "false");
		settings.put("notify-owner-on-unassign", "false");

		// Read the current file (if it exists)
		try {
    		BufferedReader input =  new BufferedReader(new FileReader(fname));
    		while (( line = input.readLine()) != null) {
    			line = line.trim();
    			if (!line.startsWith("#") && line.contains("=")) {
    				String[] pair = line.split("=", 2);
    				settings.put(pair[0], pair[1]);
    			}
    		}
    	}
    	catch (FileNotFoundException e) {
			logger.warning( "Error reading " + e.getLocalizedMessage() + ", using defaults" );
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
    private void preFlightCheck() {
    	String fname = "";
    	File f;
    	
    	// Ensure that baseDir exists
    	fname = baseDir;
    	f = new File(fname);
    	if (!f.exists()) {
    		if (f.mkdir()) {
    			logger.info( "Created directory '" + fname + "'" );
    		}
    	}
    	// Ensure that archiveDir exists
    	fname = baseDir + "/" + archiveDir;
    	f = new File(fname);
    	if (!f.exists()) {
    		if (f.mkdir()) {
    			logger.info( "Created directory '" + fname + "'" );
    		}
    	}
    	// Ensure that configFile exists
    	fname = baseDir + "/" + configFile;
    	f = new File(fname);
    	if (!f.exists()) {
			// Ensure that configFile exists
			BufferedWriter output;
			String newline = System.getProperty("line.separator");
			try {
				output = new BufferedWriter(new FileWriter(fname));
				output.write("command=petition" + newline);
				output.write("commandalias=pe" + newline);
				output.write("single=Petition" + newline);
				output.write("plural=Petitions" + newline);
				output.write("notify-owner-on-assign=true" + newline);
				output.write("notify-owner-on-unassign=true" + newline);
				output.close();
    			logger.info( "Created config file '" + fname + "'" );
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
    	// Ensure that ticketFile exists
    	fname = baseDir + "/" + ticketFile;
    	f = new File(fname);
    	if (!f.exists()) {
			// Ensure that configFile exists
			String newline = System.getProperty("line.separator");
			BufferedWriter output;
			try {
				output = new BufferedWriter(new FileWriter(fname));
				output.write("0" + newline);
				output.close();
    			logger.info( "Created ticket file '" + fname + "'" );
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
    }
}

