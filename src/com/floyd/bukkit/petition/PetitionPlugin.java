package com.floyd.bukkit.petition;


import java.io.*;
import java.util.Comparator;


import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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

import java.util.Arrays;
import java.util.UUID;
import java.util.regex.*;

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
    String mailDir = "mail";
    String ticketFile = "last_ticket_id.txt";
    String configFile = "settings.txt";

	public static final Logger logger = Logger.getLogger("Minecraft.PetitionPlugin");
    
//    public PetitionPlugin(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
//        super(pluginLoader, instance, desc, folder, plugin, cLoader);
//        // TODO: Place any custom initialization code here
//
//        // NOTE: Event registration should be done in onEnable not here as all events are unregistered when a plugin is disabled
//    }

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
        pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Normal, this);

        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
		logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args ) {
    	String cmdname = cmd.getName().toLowerCase();
        Player player = null;
        if (sender instanceof Player) {
        	player = (Player)sender;
        }
        
        if (cmdname.equalsIgnoreCase("pe") || cmdname.equalsIgnoreCase("petition")) {
        	if (player == null || Permissions.Security.permission(player, "petition")) {
	        	// Help
	        	if (args.length == 0) {
	        		performHelp(player);
	        		return true;
	        	}
	        	if (args.length >= 1) {
	        		// List
	        		if (args[0].equalsIgnoreCase("list")) {
	        			performList(player, args);
	        			return true;
	        		}
	        	}
	        	if (args.length >= 2) {
	        		// View
	        		if (args[0].equalsIgnoreCase("view")) {
	        			performView(player, args);
	        			return true;
	        		}
	        		// Assign
	        		if (args[0].equalsIgnoreCase("assign")) {
	        			performAssign(player, args);
	        			return true;
	        		}
	        		// Unassign
	        		if (args[0].equalsIgnoreCase("unassign")) {
	        			performAssign(player, args);
	        			return true;
	        		}
	        		// Close
	        		if (args[0].equalsIgnoreCase("close")) {
	        			performClose(player, args);
	        			return true;
	        		}
	        		// Comment
	        		if (args[0].equalsIgnoreCase("comment") || args[0].equalsIgnoreCase("log")) {
	        			performComment(player, args);
	        			return true;
	        		}
	        		// Open
	        		if (args[0].equalsIgnoreCase("open") || args[0].equalsIgnoreCase("new") || args[0].equalsIgnoreCase("create")) {
	        			performOpen(player, args);
	        			return true;
	        		}
	        		// Warp
	        		if (args[0].equalsIgnoreCase("warp") || args[0].equalsIgnoreCase("goto")) {
	        			performWarp(player, args);
	        			return true;
	        		}
	        	}
        	} else {
       			logger.info("[Pe] Access denied for " + player.getName());
        	}
        }

        return false;
    }

    private void performWarp(Player player, String[] args) {
		Integer id = Integer.valueOf(args[1]);
    	Boolean moderator = false;
    	String name = "(Console)";
    	if (player == null) {
    		respond(player, "[Pe] That would be a neat trick.");
    		return;
    	} else {
       		name = player.getName();
    	}
    	if (Permissions.Security.permission(player, "petition.moderate")) {
    		moderator = true;
    	}
		try {
    		getLock(id, player);
    		PetitionObject petition = new PetitionObject(id);
    		if (petition.isValid()) {
    			if (canWarpTo(player, petition)) {
    				respond(player, "[Pe] §7" + petition.Header(getServer()) );
    				respond(player, "[Pe] §7Teleporting you to where the " + settings.get("single").toLowerCase() + " was opened" );
					player.teleportTo(petition.getLocation(getServer()));
					
					logger.info(name + " teleported to petition " + id);
    			} else {
    				logger.info("[Pe] Access to warp to #" + id + " denied for " + name);
    				respond(player, "§4[Pe] Access denied.");
    			}
    		} else {
    			respond(player, "§4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found." );
    		}
		}
		finally {
			releaseLock(id, player);
		}
    	
    }
    
    private void performOpen(Player player, String[] args) {
		Integer id = IssueUniqueTicketID();
    	String name = "(Console)";
    	if (player != null) {
    		name = player.getName();
    	}
		try {
    		getLock(id, player);
    		String title = "";
    		Integer index = 1;
    		while (index < args.length) {
    			title = title.concat(" " + args[index]);
    			index++;
    		}
    		if (title.length() > 0) {
    			title = title.substring(1);
    		}
    		PetitionObject petition = new PetitionObject( id, player, title );
    		releaseLock(id, player);
    		if (petition.isValid()) {
    			respond(player, "[Pe] §7Thank you, your ticket is §6#" + petition.ID() + "§7. (Use '/petition' to manage it)");
				String[] except = { petition.Owner() };
    			notifyModerators("[Pe] §7" + settings.get("single") + " §6#" + petition.ID() + "§7 opened by " + name + ": " + title, except);
				logger.info(name + " opened petition " + id + ". " + title);
    		} else {
    			respond(player, "§4[Pe] There was an error creating your ticket, please try again later.");
		        System.out.println("[Pe] ERROR: PetitionPlugin failed to create a ticket, please check that plugins/PetitionPlugin exists and is writeable!" );
    		}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    private void performComment(Player player, String[] args) {
		Integer id = Integer.valueOf(args[1]);
    	Boolean moderator = false;
    	if (player == null || Permissions.Security.permission(player, "petition.moderate")) {
    		moderator = true;
    	}
    	String name = "(Console)";
    	if (player != null) {
    		name = player.getName();
    	}
		try {
    		getLock(id, player);
    		PetitionObject petition = new PetitionObject(id);
    		if (petition.isValid()) {
    			if (petition.ownedBy(player) || moderator) {
	        		String message = "";
	        		Integer index = 2;
	        		while (index < args.length) {
	        			message = message.concat(" " + args[index]);
	        			index++;
	        		}
	        		if (message.length() > 0) {
	        			message = message.substring(1);
	        		}
	        		// Notify
	        		notifyNamedPlayer(petition.Owner(), "[Pe] §7Your " + settings.get("single").toLowerCase() + " §6#" + id + "§7 was updated: " + message );
					notifyNamedPlayer(petition.Assignee(), "[Pe] §7" + settings.get("single") + " §6#" + id + "§7 has been updated by " + name + ".");
					String[] except = { petition.Owner(), petition.Assignee() };
	    			notifyModerators("[Pe] §7" + settings.get("single") + " §6#" + id + "§7 comment added by " + name + ".", except);
	        		petition.Comment(player, message);
					logger.info(name + " commented petition " + id + ". " + message);
    			} else {
    				logger.info("[Pe] Access to comment on #" + id + " denied for " + name);
    			}
    		} else {
    			respond(player, "§4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found." );
    		}
		}
		finally {
			releaseLock(id, player);
		}

    }
    
    private void performClose(Player player, String[] args) {
		Integer id = Integer.valueOf(args[1]);
    	Boolean moderator = false;
    	if (player == null || Permissions.Security.permission(player, "petition.moderate")) {
    		moderator = true;
    	}
    	String name = "(Console)";
    	if (player != null) {
    		name = player.getName();
    	}
		try {
    		getLock(id, player);
    		PetitionObject petition = new PetitionObject(id);
    		if (petition.isValid()) {
    			if (petition.ownedBy(player) || moderator) {
	        		String message = "";
	        		Integer index = 2;
	        		while (index < args.length) {
	        			message = message.concat(" " + args[index]);
	        			index++;
	        		}
	        		if (message.length() > 0) {
	        			message = message.substring(1);
	        		}
	        		// Notify
	        		notifyNamedPlayer(petition.Owner(), "[Pe] §7Your " + settings.get("single").toLowerCase() + " §6#" + id + "§7 was closed. " + message );
					notifyNamedPlayer(petition.Assignee(), "[Pe] §7" + settings.get("single") + " §6#" + id + "§7 was closed by " + name + ".");
					String[] except = { petition.Owner(), petition.Assignee() };
	        		notifyModerators("[Pe] §7" + settings.get("single") + " §6#" + id + "§7 was closed. " + message, except );
	        		petition.Close(player, message);
					logger.info(name + " closed petition " + id + ". " + message);
    			} else {
    				logger.info("[Pe] Access to close #" + id + " denied for " + name);
    			}
    		} else {
    			respond(player, "§4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found." );
    		}
		}
		finally {
			releaseLock(id, player);
		}

    }

    private void performUnassign(Player player, String[] args) {
		Integer id = Integer.valueOf(args[1]);
    	Boolean moderator = false;
    	if (player == null || Permissions.Security.permission(player, "petition.moderate")) {
    		moderator = true;
    	}
    	String name = "(Console)";
    	if (player != null) {
    		name = player.getName();
    	}
    	if (moderator == false) {
    		logger.info("[Pe] Access to unassign #" + id + " denied for " + name);
    	}
		try {
    		getLock(id, player);
    		PetitionObject petition = new PetitionObject(id);
    		if (petition.isValid()) {
    			petition.Unassign(player);
        		// Notify
    			if (Boolean.parseBoolean(settings.get("notify-owner-on-unassign"))) {
    				notifyNamedPlayer(petition.Owner(), "[Pe] §7Your " + settings.get("single").toLowerCase() + " §6#" + id + "§7 has been unassigned.");
    			}
				notifyNamedPlayer(petition.Assignee(), "[Pe] §7" + settings.get("single") + " §6#" + id + "§7 has been unassigned from you by " + name + ".");
				String[] except = { petition.Owner(), petition.Assignee() };
				notifyModerators("[Pe] §7" + settings.get("single") + " §6#" + id + "§7 unassigned by " + name + ".", except );
				logger.info(name + " unassigned petition " + id);
    		} else {
    			respond(player, "§4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found." );
    		}
		}
		finally {
			releaseLock(id, player);
		}
    }
    
    private void performAssign(Player player, String[] args) {
		Integer id = Integer.valueOf(args[1]);
    	Boolean moderator = false;
    	if (player == null || Permissions.Security.permission(player, "petition.moderate")) {
    		moderator = true;
    	}
    	String name = "(Console)";
    	if (player != null) {
    		name = player.getName();
    	}
    	if (moderator == false) {
    		logger.info("[Pe] Access to assign #" + id + " denied for " + name);
    	}
		try {
    		getLock(id, player);
    		PetitionObject petition = new PetitionObject(id);
    		if (petition.isValid()) {
    			if (args.length == 3) {
    				// Assign to named player
    				petition.Assign(player, args[2]);
    			} else {
    				// Assign to self
        			petition.Assign(player, name);
    			}
        		// Notify
    			if (Boolean.parseBoolean(settings.get("notify-owner-on-assign"))) {
    				notifyNamedPlayer(petition.Owner(), "[Pe] §7Your " + settings.get("single").toLowerCase() + " §6#" + id + "§7 assigned to " + petition.Assignee() + ".");
    			}
				notifyNamedPlayer(petition.Assignee(), "[Pe] §7" + settings.get("single") + " §6#" + id + "§7 has been assigned to you by " + name + ".");
				String[] except = { petition.Owner(), petition.Assignee() };
				notifyModerators("[Pe] §7" + settings.get("single") + " §6#" + id + "§7 has been assigned to " + petition.Assignee() + ".", except);
				logger.info(name + " assigned petition " + id + " to " + petition.Assignee());
    		} else {
    			respond(player, "§4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found." );
    		}
		}
		finally {
			releaseLock(id, player);
		}
    }
    
    private void performView(Player player, String[] args) {
    	Boolean moderator = false;
    	if (player == null || Permissions.Security.permission(player, "petition.moderate")) {
    		moderator = true;
    	}
    	String name = "(Console)";
    	if (player != null) {
    		name = player.getName();
    	}
		Integer id = Integer.valueOf(args[1]);
		try {
    		getLock(id, player);
    		PetitionObject petition = new PetitionObject(id);
    		if (petition.isValid()) {
    			if (petition.ownedBy(player) || moderator) {
    				respond(player, "[Pe] §7" + petition.Header(getServer()) );
					for ( String line : petition.Log()) {
						respond(player, "[Pe] §6#" + petition.ID() + " §7" + line );
					}
    			} else {
    				logger.info("[Pe] Access to view #" + id + " denied for " + name);
    			}
    		} else {
    			respond(player, "§4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found." );
    		}
		}
		finally {
			releaseLock(id, player);
		}
    }
    
    private void performList(Player player, String[] args) {
		Integer count = 0;
		Integer showing = 0;
		Integer limit = 10;
		if (args.length == 2) {
			limit = Integer.valueOf(args[1]);
		}
    	Boolean moderator = false;
    	if (player == null || Permissions.Security.permission(player, "petition.moderate")) {
    		moderator = true;
    	}

		File dir = new File("plugins/PetitionPlugin");
		String[] filenames = dir.list();
		// Sort the filenames in numerical order
		// OMG there _has_ to be a more efficient way to do this...!
		Comparator<String> numerical = new Comparator<String>() {
			public int compare(final String o1, final String o2) {
				// logger.info("Comparing " + o1 + " to " + o2);
				String[] parts1 = o1.split("\\.");
				String[] parts2 = o2.split("\\.");
				Integer int1 = 0;
				try {
					int1 = Integer.parseInt(parts1[0]);
				}
				catch (Exception e) {
				}
				Integer int2 = 0;
				try {
					int2 = Integer.parseInt(parts2[0]);
				}
				catch (Exception e) {
				}
				// logger.info("Stripped values are " + int1 + " and " + int2);
				if (int1 < int2) { return -1; }
				if (int1 > int2) { return  1; }
				return 0;
			}
		};
		Arrays.sort(filenames, numerical);
		
    	if (filenames == null) {
		    // Either dir does not exist or is not a directory
		} else {
			for (String filename : filenames) {
				if (filename.endsWith(".ticket")) {
					String[] parts = filename.split("['.']");
            		Integer id = Integer.valueOf(parts[0]);
            		try {
                		getLock(id, player);
    					PetitionObject petition = new PetitionObject(id);
    					if (petition.isValid() && (petition.ownedBy(player) || moderator) ) {
    						if (count < limit) {
    							respond(player, "[Pe] " + petition.Header(getServer()) );
    							showing++;
    						}
    						count++;
    					}
            		}
            		finally {
            			releaseLock(id, player);
            		}
				}
			}
		}
    	respond(player, "[Pe] §7Open " + settings.get("plural").toLowerCase() + ": " + count + " (Showing " + showing + ")" );
    }
    
    private void performHelp(Player player) {
    	String cmd = "pe";
    	Boolean moderator = false;
    	if (player == null || Permissions.Security.permission(player, "petition.moderate")) {
    		moderator = true;
    	}
    	
        respond(player, "[Pe] §7" + settings.get("single") + " usage:");
    	respond(player, "[Pe] §7/" + cmd + " open|create|new <Message>");
    	respond(player, "[Pe] §7/" + cmd + " comment|log <#> <Message>");
    	respond(player, "[Pe] §7/" + cmd + " close <#> [<Message>]");
    	respond(player, "[Pe] §7/" + cmd + " list [<count>]");
    	respond(player, "[Pe] §7/" + cmd + " view <#>");
    	if (canWarpAtAll(player)) {
    		respond(player, "[Pe] §7/" + cmd + " warp|goto <#>");
    	}
        if (moderator) {
        	respond(player, "[Pe] §7/" + cmd + " assign <#> [<Operator>]");
        	respond(player, "[Pe] §7/" + cmd + " unassign <#>");
        }
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
    			logger.info( "[Pe] Permission system not enabled. Disabling plugin." );
    	    	this.getServer().getPluginManager().disablePlugin(this);
    	    }
    	}
    }
    
    public synchronized boolean SetPetitionLock(Integer id, String owner, Boolean release) {
    	if (release == false) {
    		// Check for lingering lock by the same player
    		if (semaphores.containsKey(id) && semaphores.get(id).equals(owner)) {
    			logger.severe( "[Pe] INTERNAL ERROR! Petition #" + id + " is ALREADY locked by " + semaphores.get(id) );
    			logger.severe( "[Pe] This was probably caused by a previous crash while accessing this petition." );
    			logger.severe( "[Pe] Please report this issue to the plugin author." );
    			return true;
    		}
    		// Get lock
    		if (semaphores.containsKey(id)) {
    			logger.warning( "[Pe] Denied " + owner + " lock on #" + id + "; currently locked by " + semaphores.get(id) );
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
    	
		logger.fine( "[Pe] Issued ticket #" + line );
    	return Integer.valueOf(line);
    }
    
    private void loadSettings() {
    	String fname = baseDir + "/" + configFile;
		String line = null;

		// Load the settings hash with defaults
		settings.put("single", "Petition");
		settings.put("plural", "Petitions");

		settings.put("notify-owner-on-assign", "false");
		settings.put("notify-owner-on-unassign", "false");

		settings.put("warp-requires-permission", "false");
		// Read the current file (if it exists)
		try {
    		BufferedReader input =  new BufferedReader(new FileReader(fname));
    		while (( line = input.readLine()) != null) {
    			line = line.trim();
    			if (!line.startsWith("#") && line.contains("=")) {
    				String[] pair = line.split("=", 2);
    				settings.put(pair[0], pair[1]);
    				if (pair[0].equals("command") || pair[0].equals("commandalias")) {
    					logger.warning("[Pe] Warning: The '" + pair[0] + "' setting has been deprecated and no longer has any effect");
    				}
    			}
    		}
    	}
    	catch (FileNotFoundException e) {
			logger.warning( "[Pe] Error reading " + e.getLocalizedMessage() + ", using defaults" );
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
    			logger.info( "[Pe] Created directory '" + fname + "'" );
    		}
    	}
    	// Ensure that archiveDir exists
    	fname = baseDir + "/" + archiveDir;
    	f = new File(fname);
    	if (!f.exists()) {
    		if (f.mkdir()) {
    			logger.info( "[Pe] Created directory '" + fname + "'" );
    		}
    	}
    	// Ensure that mailDir exists
    	fname = baseDir + "/" + mailDir;
    	f = new File(fname);
    	if (!f.exists()) {
    		if (f.mkdir()) {
    			logger.info( "[Pe] Created directory '" + fname + "'" );
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
				output.write("single=Petition" + newline);
				output.write("plural=Petitions" + newline);
				output.write("notify-owner-on-assign=true" + newline);
				output.write("notify-owner-on-unassign=true" + newline);
				output.write("warp-requires-permission=false" + newline);
				output.close();
    			logger.info( "[Pe] Created config file '" + fname + "'" );
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
    			logger.info( "[Pe] Created ticket file '" + fname + "'" );
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
    }
    
    private void getLock(Integer id, Player player) {	
    	String name = "";
    	if (player != null) {
    		name = player.getName();
    	}
    	while (!SetPetitionLock(id, name, false)) {
    		try {
    			Thread.sleep(1000);
    		}
    		catch (InterruptedException e) {
    			logger.warning("[Pe] Sleep interrupted while waiting for lock");
    		}
    	}
    }
    
    private void releaseLock(Integer id, Player player) {
    	String name = "";
    	if (player != null) {
    		name = player.getName();
    	}
   		SetPetitionLock(id, name, true);
    }
    
    private void notifyNamedPlayer(String name, String message) {
    	// Ignore broken filenames -- should probably be improved
    	if (name.equals("") || name.equals("*") || name.equalsIgnoreCase("(Console)")) {
    		return;
    	}
    	Player[] players = getServer().getOnlinePlayers();
    	Boolean online = false;
    	for (Player player: players) {
    		if (player.getName().equalsIgnoreCase(name)) {
    			player.sendMessage(message);
    			online = true;
    		}
    	}
    	if (online == false) {
    		name = name.toLowerCase();
    		String fname;
    		File f;
        	// Ensure that player's mailDir exists
        	fname = baseDir + "/" + mailDir + "/" + name;
        	f = new File(fname);
        	if (!f.exists()) {
        		if (f.mkdir()) {
        			logger.info( "[Pe] Created directory '" + fname + "'" );
        		}
        	}
        	// Ensure that player's mailDir tmp exists
        	fname = baseDir + "/" + mailDir + "/" + name + "/tmp";
        	f = new File(fname);
        	if (!f.exists()) {
        		if (f.mkdir()) {
        			logger.info( "[Pe] Created directory '" + fname + "'" );
        		}
        	}
        	// Ensure that player's mailDir inbox exists
        	fname = baseDir + "/" + mailDir + "/" + name + "/inbox";
        	f = new File(fname);
        	if (!f.exists()) {
        		if (f.mkdir()) {
        			logger.info( "[Pe] Created directory '" + fname + "'" );
        		}
        	}
    		// Create a unique file in tmp
        	UUID uuid = UUID.randomUUID();
        	fname = baseDir + "/" + mailDir + "/" + name + "/tmp/" + uuid;
        	String fname_final = baseDir + "/" + mailDir + "/" + name + "/inbox/" + uuid;
			BufferedWriter output;
			String newline = System.getProperty("line.separator");
			try {
				output = new BufferedWriter(new FileWriter(fname));
				output.write(message + newline);
				output.close();
				// Move the file into player's inbox
				f = new File(fname);
				f.renameTo(new File(fname_final));
			} catch (Exception e) {
				e.printStackTrace();
			}
        	
    	}
    }
    
    private void notifyModerators(String message, String[] exceptlist) {
    	Player[] players = getServer().getOnlinePlayers();
    	for (Player player: players) {
    		if (Permissions.Security.permission(player, "petition.moderate")) {
    			Boolean skip = false;
    			for (String except: exceptlist) {
    				if (player.getName().toLowerCase().equals(except.toLowerCase())) {
    					skip = true;
    				}
    			}
    			if (skip == false) {
    				player.sendMessage(message);
    			}
    		}
    	}
    }
    
    public String[] getMessages(Player player) {
    	String[] messages = new String[0];
    	String name = player.getName().toLowerCase();
    	String pname = baseDir + "/" + mailDir + "/" + name + "/inbox";
		File dir = new File(pname);
		String[] filenames = dir.list();
    	if (filenames != null) {
    		messages = new String[filenames.length];
    		Integer index = 0;
    		for (String fname : filenames) {
    			try {
    	    		BufferedReader input =  new BufferedReader(new FileReader(pname + "/" + fname));
    	    		messages[index] = input.readLine();
    	    		input.close();
    	    		boolean success = (new File(pname + "/" + fname)).delete();
    	    		if (success == false) {
    	    			logger.warning("[Pe] Could not delete " + pname + "/" + fname);
    	    		}
    	    	}
    	    	catch (FileNotFoundException e) {
    				logger.warning( "[Pe] Unexpected error reading " + e.getLocalizedMessage());
    	    	}
    	    	catch (Exception e) {
    	    		e.printStackTrace();
    	    	}
    			index++;
    		}
    	}
    	return messages;
    }
    
    private void respond(Player player, String message) {
    	if (player == null) {
        	// Strip color codes
        	Pattern pattern = Pattern.compile("\\§[0-9a-f]");
        	Matcher matcher = pattern.matcher(message);
        	message = matcher.replaceAll("");
        	// Print message to console
    		System.out.println(message);
    	} else {
    		player.sendMessage(message);
    	}
    }
    
    // This method is invoked when showing help
    // Check if there are situations where this player can warp
    private Boolean canWarpAtAll(Player player) {
    	// Check if this limit is enabled at all
    	if (Boolean.parseBoolean(settings.get("warp-requires-permission")) == false) {
    		return true;
    	}
    	// Check who is asking
    	if (player == null) {
    		return true;	// Console
    	}
    	// Moderator?
    	if (Permissions.Security.permission(player, "petition.moderator")) {
    		return true;
    	}
    	
    	if (Permissions.Security.permission(player, "petition.warp-to-own-if-assigned")) {
    		return true;
    	}
    	if (Permissions.Security.permission(player, "petition.warp-to-own")) {
    		return true;
    	}
    	return false;
    }

    // This method is invoked ONLY when a player is attempting to warp to a petition location
    // Implements a set of rules for warp access
    private Boolean canWarpTo(Player player, PetitionObject petition) {
    	// Check who is asking
    	if (player == null) {
    		return true;	// Console
    	}
    	// Moderator?
    	if (Permissions.Security.permission(player, "petition.moderator")) {
    		return true;
    	}
    	// Player owns this petition?
    	if (petition.ownedBy(player) == false) {
    		return false;
    	}
    	// Check for limitations
    	if (Boolean.parseBoolean(settings.get("warp-requires-permission")) == false) {
    		return true;
    	}
    	// Player owns this petition, is that sufficient?
    	if (Permissions.Security.permission(player, "petition.warp-to-own")) {
    		return true;
    	}
    	// Our last chance is that the petition has been assigned
    	if (petition.Assignee().equals("*")) {
    		return false;
    	}
    	// It has been assigned, is that sufficient?
    	if (Permissions.Security.permission(player, "petition.warp-to-own-assigned")) {
    		return true;
    	}
		String[] except = { petition.Owner() };
    	notifyModerators("[Pe] " + player.getName() + " requested warp access to " + settings.get("single").toLowerCase() + " #" + petition.ID(), except);
    	return false;
    }

}

