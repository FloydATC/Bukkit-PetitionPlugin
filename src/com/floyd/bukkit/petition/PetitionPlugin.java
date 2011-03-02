package com.floyd.bukkit.petition;


import java.io.*;

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
import java.util.UUID;

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
    	if (player == null || Permissions.Security.permission(player, "petition.moderate")) {
    		moderator = true;
    	}
		try {
    		getLock(id, player);
    		PetitionObject petition = new PetitionObject(id);
    		if (petition.isValid()) {
    			if (petition.Owner().equalsIgnoreCase(player.getName()) || moderator) {
					player.sendMessage("[Pe] §7" + petition.Header(getServer()) );
					player.sendMessage("[Pe] §7Teleporting you to where the " + settings.get("single").toLowerCase() + " was opened" );
					player.teleportTo(petition.getLocation(getServer()));
					
					logger.info(player.getName() + " teleported to petition " + id);
    			} else {
    				logger.info("[Pe] Access to warp to #" + id + " denied for " + player.getName());
    			}
    		} else {
				player.sendMessage("§4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found." );
    		}
		}
		finally {
			releaseLock(id, player);
		}
    	
    }
    
    private void performOpen(Player player, String[] args) {
		Integer id = IssueUniqueTicketID();
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
    			player.sendMessage("[Pe] §7Thank you, your ticket is §6#" + petition.ID() + "§7. (Use '/petition' to manage it)");
    			notifyModerators("[Pe] §7" + settings.get("single") + " §6#" + petition.ID() + "§7 opened by " + player.getName() + ": " + title);
				logger.info(player.getName() + " opened petition " + id + ". " + title);
    		} else {
    			player.sendMessage("§4[Pe] There was an error creating your ticket, please try again later.");
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
		try {
    		getLock(id, player);
    		PetitionObject petition = new PetitionObject(id);
    		if (petition.isValid()) {
    			if (petition.Owner().equalsIgnoreCase(player.getName()) || moderator) {
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
					notifyNamedPlayer(petition.Assignee(), "[Pe] §7" + settings.get("single") + " §6#" + id + "§7 has been updated by " + player.getName() + ".");
	    			notifyModerators("[Pe] §7" + settings.get("single") + " §6#" + id + "§7 comment added by " + player.getName() + ".");
	        		petition.Comment(player, message);
					logger.info(player.getName() + " commented petition " + id + ". " + message);
    			} else {
    				logger.info("[Pe] Access to comment on #" + id + " denied for " + player.getName());
    			}
    		} else {
				player.sendMessage("§4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found." );
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
		try {
    		getLock(id, player);
    		PetitionObject petition = new PetitionObject(id);
    		if (petition.isValid()) {
    			if (petition.Owner().equalsIgnoreCase(player.getName()) || moderator) {
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
					notifyNamedPlayer(petition.Assignee(), "[Pe] §7" + settings.get("single") + " §6#" + id + "§7 was closed by " + player.getName() + ".");
	        		notifyModerators("[Pe] §7" + settings.get("single") + " §6#" + id + "§7 was closed. " + message );
	        		petition.Close(player, message);
					logger.info(player.getName() + " closed petition " + id + ". " + message);
    			} else {
    				logger.info("[Pe] Access to close #" + id + " denied for " + player.getName());
    			}
    		} else {
				player.sendMessage("§4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found." );
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
    	if (moderator == false) {
    		logger.info("[Pe] Access to unassign #" + id + " denied for " + player.getName());
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
				notifyNamedPlayer(petition.Assignee(), "[Pe] §7" + settings.get("single") + " §6#" + id + "§7 has been unassigned from you by " + player.getName() + ".");
				notifyModerators("[Pe] §7" + settings.get("single") + " §6#" + id + "§7 unassigned by " + player.getName() + "." );
				logger.info(player.getName() + " unassigned petition " + id);
    		} else {
				player.sendMessage("§4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found." );
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
    	if (moderator == false) {
    		logger.info("[Pe] Access to assign #" + id + " denied for " + player.getName());
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
        			petition.Assign(player, player.getName());
    			}
        		// Notify
    			if (Boolean.parseBoolean(settings.get("notify-owner-on-assign"))) {
    				notifyNamedPlayer(petition.Owner(), "[Pe] §7Your " + settings.get("single").toLowerCase() + " §6#" + id + "§7 assigned to " + petition.Assignee() + ".");
    			}
				notifyNamedPlayer(petition.Assignee(), "[Pe] §7" + settings.get("single") + " §6#" + id + "§7 has been assigned to you by " + player.getName() + ".");
				notifyModerators("[Pe] §7" + settings.get("single") + " §6#" + id + "§7 has been assigned to " + petition.Assignee() + ".");
				logger.info(player.getName() + " assigned petition " + id + " to " + petition.Assignee());
    		} else {
				player.sendMessage("§4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found." );
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
		Integer id = Integer.valueOf(args[1]);
		try {
    		getLock(id, player);
    		PetitionObject petition = new PetitionObject(id);
    		if (petition.isValid()) {
    			if (petition.Owner().equalsIgnoreCase(player.getName()) || moderator) {
					player.sendMessage("[Pe] §7" + petition.Header(getServer()) );
					for ( String line : petition.Log()) {
						player.sendMessage("[Pe] §6#" + petition.ID() + " §7" + line );
					}
    			} else {
    				logger.info("[Pe] Access to view #" + id + " denied for " + player.getName());
    			}
    		} else {
				player.sendMessage("§4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found." );
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
    					if (petition.isValid() && (petition.Owner().equalsIgnoreCase(player.getName()) || Permissions.Security.permission(player, "petition.moderate") )) {
    						if (count < limit) {
    							player.sendMessage( "[Pe] " + petition.Header(getServer()) );
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
		player.sendMessage("[Pe] §7Open " + settings.get("plural").toLowerCase() + ": " + count + " (Showing " + showing + ")" );
    }
    
    private void performHelp(Player player) {
    	String cmd = "pe";
    	Boolean moderator = false;
    	if (player == null || Permissions.Security.permission(player, "petition.moderate")) {
    		moderator = true;
    	}
    	
        player.sendMessage("[Pe] §7" + settings.get("single") + " usage:");
    	player.sendMessage("[Pe] §7/" + cmd + " open|create|new <Message>");
    	player.sendMessage("[Pe] §7/" + cmd + " comment|log <#> <Message>");
    	player.sendMessage("[Pe] §7/" + cmd + " close <#> [<Message>]");
    	player.sendMessage("[Pe] §7/" + cmd + " list [<count>]");
    	player.sendMessage("[Pe] §7/" + cmd + " view <#>");
    	player.sendMessage("[Pe] §7/" + cmd + " warp|goto <#>");
        if (moderator) {
        	player.sendMessage("[Pe] §7/" + cmd + " assign <#> [<Operator>]");
        	player.sendMessage("[Pe] §7/" + cmd + " unassign <#>");
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
    	while (!SetPetitionLock(id, player.getName(), false)) {
    		try {
    			Thread.sleep(1000);
    		}
    		catch (InterruptedException e) {
    			logger.warning("[Pe] Sleep interrupted while waiting for lock");
    		}
    	}
    }
    
    private void releaseLock(Integer id, Player player) {
    	SetPetitionLock(id, player.getName(), true);
    }
    
    private void notifyNamedPlayer(String name, String message) {
    	// Ignore broken filenames -- should probably be improved
    	if (name.equals("") || name.equals("*")) {
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
    
    private void notifyModerators(String message) {
    	Player[] players = getServer().getOnlinePlayers();
    	for (Player player: players) {
    		if (Permissions.Security.permission(player, "petition.moderate")) {
    			player.sendMessage(message);
    		}
    	}
    }
    
    public String[] getMessages(Player player) {
    	String[] messages = null;
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
}

