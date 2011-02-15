package com.bukkit.floyd.petition;


import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;


import com.nijikokun.bukkit.Permissions.Permissions;
import java.io.*;


/**
 * Handle events for all Player related events
 * @author FloydATC
 */
public class PetitionPlayerListener extends PlayerListener {
    private final PetitionPlugin plugin;

    public PetitionPlayerListener(PetitionPlugin instance) {
        plugin = instance;
    }
    
    //Insert Player related code here
    @Override
    public void onPlayerCommand(PlayerChatEvent event) {
        String[] split = event.getMessage().split(" ");
        Player player = event.getPlayer();

        // TODO: Make the command/alias configurable
        //System.out.println("[Pe] intercepted command " + split[0]);
        if ( 
        	(split[0].equalsIgnoreCase("/"+plugin.settings.get("command")) 
        		|| split[0].equalsIgnoreCase("/"+plugin.settings.get("commandalias"))
        	) 
        	&& Permissions.Security.permission(player, "petition")
        ) {
            if (split.length == 1) {
            	// Help
                player.sendMessage("[Pe] §7" + plugin.settings.get("single") + " usage:");
                if (Permissions.Security.permission(player, "petition")) {
                	player.sendMessage("[Pe] §7/" + plugin.settings.get("commandalias") + " open <Message>");
                }
                if (Permissions.Security.permission(player, "petition")) {
                	player.sendMessage("[Pe] §7/" + plugin.settings.get("commandalias") + " comment|log <#> <Message>");
                }
                if (Permissions.Security.permission(player, "petition")) {
                	player.sendMessage("[Pe] §7/" + plugin.settings.get("commandalias") + " close <#> [<Message>]");
                }
                if (Permissions.Security.permission(player, "petition")) {
                	player.sendMessage("[Pe] §7/" + plugin.settings.get("commandalias") + " list [<count>]");
                }
                if (Permissions.Security.permission(player, "petition")) {
                	player.sendMessage("[Pe] §7/" + plugin.settings.get("commandalias") + " view <#>");
                }
                if (Permissions.Security.permission(player, "petition.moderate")) {
                	player.sendMessage("[Pe] §7/" + plugin.settings.get("commandalias") + " assign <#> [<Operator>]");
                }
                if (Permissions.Security.permission(player, "petition.moderate")) {
                	player.sendMessage("[Pe] §7/" + plugin.settings.get("commandalias") + " unassign <#>");
                }
                if (Permissions.Security.permission(player, "petition.moderate")) {
                	player.sendMessage("[Pe] §7/" + plugin.settings.get("commandalias") + " warp <#>");
                }
                event.setCancelled(true);
                return;
            }
            if (split.length >= 2) {
            	// List
            	if (split[1].equalsIgnoreCase("list") && Permissions.Security.permission(player, "petition")) {
            		File dir = new File("plugins/PetitionPlugin");
            		String[] filenames = dir.list();
            		Integer count = 0;
            		Integer showing = 0;
            		Integer limit = 10;
            		if (split.length == 3) {
            			limit = Integer.valueOf(split[2]);
            		}
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
	            							player.sendMessage( "[Pe] " + petition.Header(plugin.getServer()) );
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
					player.sendMessage("[Pe] §7Open " + plugin.settings.get("plural").toLowerCase() + ": " + count + " (Showing " + showing + ")" );
                    event.setCancelled(true);
                    return;
            	}
            }
            if (split.length >= 3) {
            	// Warp
            	if (split[1].equalsIgnoreCase("warp") && Permissions.Security.permission(player, "petition.moderate")) {
            		Integer id = Integer.valueOf(split[2]);
            		try {
                		getLock(id, player);
	            		PetitionObject petition = new PetitionObject(id);
	            		if (petition.isValid()) {
	    					player.sendMessage("[Pe] §7" + petition.Header(plugin.getServer()) );
	    					player.sendMessage("[Pe] §7Teleporting you to where the " + plugin.settings.get("single").toLowerCase() + " was opened" );
	    					player.teleportTo(petition.getLocation(plugin.getServer()));
	    					
	    					plugin.logger.info(player.getName() + " teleported to petition " + id);
	            		} else {
	    					player.sendMessage("§4[Pe] No open " + plugin.settings.get("single").toLowerCase() + " #" + split[2] + " found." );
	            		}
            		}
            		finally {
            			releaseLock(id, player);
            		}
            		event.setCancelled(true);
            		return;
            	}
            	// View
            	if (split[1].equalsIgnoreCase("view") && Permissions.Security.permission(player, "petition")) {
            		Integer id = Integer.valueOf(split[2]);
            		try {
	            		getLock(id, player);
	            		PetitionObject petition = new PetitionObject(id);
	            		if (petition.isValid() && (petition.Owner().equalsIgnoreCase(player.getName()) || Permissions.Security.permission(player, "petition.moderate") )) {
	    					player.sendMessage("[Pe] §7" + petition.Header(plugin.getServer()) );
	    					for ( String line : petition.Log()) {
	        					player.sendMessage("[Pe] §6#" + petition.ID() + " §7" + line );
	    					}
	            		} else {
	    					player.sendMessage("§4[Pe] No open " + plugin.settings.get("single").toLowerCase() + " #" + split[2] + " found." );
	            		}
            		}
            		finally {
            			releaseLock(id, player);
            		}
            		event.setCancelled(true);
            		return;
            	}
            	// Assign
            	if (split[1].equalsIgnoreCase("assign") && Permissions.Security.permission(player, "petition.moderate")) {
            		Integer id = Integer.valueOf(split[2]);
            		try {
	            		getLock(id, player);
	            		PetitionObject petition = new PetitionObject(id);
	            		if (petition.isValid()) {
	            			if (split.length == 4) {
	            				// Assign to named player
	            				petition.Assign(player, split[3]);
	            			} else {
	            				// Assign to self
	                			petition.Assign(player, player.getName());
	            			}
		            		// Notify
	            			if (Boolean.parseBoolean(plugin.settings.get("notify-owner-on-assign"))) {
	            				notifyNamedPlayer(petition.Owner(), "[Pe] §7Your " + plugin.settings.get("single").toLowerCase() + " §6#" + split[2] + "§7 assigned to " + petition.Assignee() + ".");
	            			}
	        				notifyModerators("[Pe] §7" + plugin.settings.get("single") + " §6#" + split[2] + "§7 has been assigned to " + petition.Assignee() + ".");
	    					plugin.logger.info(player.getName() + " assigned petition " + id + " to " + petition.Assignee());
	            		} else {
	    					player.sendMessage("§4[Pe] No open " + plugin.settings.get("single").toLowerCase() + " #" + split[2] + " found." );
	            		}
            		}
            		finally {
            			releaseLock(id, player);
            		}
            		event.setCancelled(true);
            		return;
            	}
            	// Unassign
            	if (split[1].equalsIgnoreCase("unassign") && Permissions.Security.permission(player, "petition.moderate")) {
            		Integer id = Integer.valueOf(split[2]);
            		try {
	            		getLock(id, player);
	            		PetitionObject petition = new PetitionObject(id);
	            		if (petition.isValid()) {
	            			petition.Unassign(player);
		            		// Notify
	            			if (Boolean.parseBoolean(plugin.settings.get("notify-owner-on-unassign"))) {
	            				notifyNamedPlayer(petition.Owner(), "[Pe] §7Your " + plugin.settings.get("single").toLowerCase() + " §6#" + split[2] + "§7 has been unassigned.");
	            			}
	    					notifyModerators("[Pe] §7" + plugin.settings.get("single") + " §6#" + split[2] + "§7 unassigned by " + player.getName() + "." );
	    					plugin.logger.info(player.getName() + " unassigned petition " + id);
	            		} else {
	    					player.sendMessage("§4[Pe] No open " + plugin.settings.get("single").toLowerCase() + " #" + split[2] + " found." );
	            		}
            		}
            		finally {
            			releaseLock(id, player);
            		}
            		event.setCancelled(true);
            		return;
            	}
            	// Close
            	if (split[1].equalsIgnoreCase("close") && Permissions.Security.permission(player, "petition")) {
            		Integer id = Integer.valueOf(split[2]);
            		try {
	            		getLock(id, player);
	            		PetitionObject petition = new PetitionObject(id);
	            		if (petition.isValid() && (petition.Owner().equalsIgnoreCase(player.getName()) || Permissions.Security.permission(player, "petition.moderate") )) {
		            		String message = "";
		            		Integer index = 3;
		            		while (index < split.length) {
		            			message = message.concat(" " + split[index]);
		            			index++;
		            		}
		            		if (message.length() > 0) {
		            			message = message.substring(1);
		            		}
		            		// Notify
		            		notifyNamedPlayer(petition.Owner(), "[Pe] §7Your " + plugin.settings.get("single").toLowerCase() + " §6#" + split[2] + "§7 was closed. " + message );
		            		notifyModerators("[Pe] §7" + plugin.settings.get("single") + " §6#" + split[2] + "§7 was closed. " + message );
		            		petition.Close(player, message);
	    					plugin.logger.info(player.getName() + " closed petition " + id + ". " + message);
	            		} else {
	    					player.sendMessage("§4[Pe] No open " + plugin.settings.get("single").toLowerCase() + " #" + split[2] + " found." );
	            		}
            		}
            		finally {
            			releaseLock(id, player);
            		}
            		event.setCancelled(true);
            		return;
            	}
            	// Comment
            	if ((split[1].equalsIgnoreCase("comment") || split[1].equalsIgnoreCase("log")) && Permissions.Security.permission(player, "petition")) {
            		Integer id = Integer.valueOf(split[2]);
            		try {
	            		getLock(id, player);
	            		PetitionObject petition = new PetitionObject(id);
	            		if (petition.isValid() && (petition.Owner().equalsIgnoreCase(player.getName()) || Permissions.Security.permission(player, "petition.moderate") )) {
		            		String message = "";
		            		Integer index = 3;
		            		while (index < split.length) {
		            			message = message.concat(" " + split[index]);
		            			index++;
		            		}
		            		if (message.length() > 0) {
		            			message = message.substring(1);
		            		}
		            		// Notify
		            		notifyNamedPlayer(petition.Owner(), "[Pe] §7Your " + plugin.settings.get("single").toLowerCase() + " §6#" + split[2] + "§7 was updated: " + message );
	            			notifyModerators("[Pe] §7" + plugin.settings.get("single") + " §6#" + split[2] + "§7 comment added by " + player.getName() + ".");
		            		petition.Comment(player, message);
	    					plugin.logger.info(player.getName() + " commented petition " + id + ". " + message);
	            		} else {
	    					player.sendMessage("§4[Pe] No open " + plugin.settings.get("single").toLowerCase() + " #" + split[2] + " found." );
	            		}
            		}
            		finally {
            			releaseLock(id, player);
            		}
            		event.setCancelled(true);
            		return;
            	}
            	
            	// Open
            	if (split[1].equalsIgnoreCase("open") && Permissions.Security.permission(player, "petition")) {
            		Integer id = plugin.IssueUniqueTicketID();
            		try {
	            		getLock(id, player);
	            		String title = "";
	            		Integer index = 2;
	            		while (index < split.length) {
	            			title = title.concat(" " + split[index]);
	            			index++;
	            		}
	            		if (title.length() > 0) {
	            			title = title.substring(1);
	            		}
	            		PetitionObject petition = new PetitionObject( id, player, title );
	            		releaseLock(id, player);
	            		if (petition.isValid()) {
	            			player.sendMessage("[Pe] §7Thank you, your ticket is §6#" + petition.ID() + "§7. (Use '/" + plugin.settings.get("commandalias") + "' to manage it)");
	            			notifyModerators("[Pe] §7" + plugin.settings.get("single") + " §6#" + petition.ID() + "§7 opened by " + player.getName() + ": " + title);
	    					plugin.logger.info(player.getName() + " opened petition " + id + ". " + title);
	            		} else {
	            			player.sendMessage("§4[Pe] There was an error creating your ticket, please try again later.");
	    			        System.out.println("[Pe] ERROR: PetitionPlugin failed to create a ticket, please check that plugins/PetitionPlugin exists and is writeable!" );
	            		}
            		}
            		finally {
            			event.setCancelled(true);
            		}
            		return;
            	}
            }
            player.sendMessage("§4[Pe] Command not understood, please type '/pe' for help.");
            event.setCancelled(true);
            return;
        }
        if (split[0].equalsIgnoreCase("/debug")) {
            plugin.setDebugging(player, !plugin.isDebugging(player));
            return;
        }
        if (plugin.isDebugging(player)) { 
        	System.out.println("[Pe] Ignored command: " + split[0]);
        }
    }
    
    private void getLock(Integer id, Player player) {	
    	while (!plugin.SetPetitionLock(id, player.getName(), false)) {
    		try {
    			Thread.sleep(1000);
    		}
    		catch (InterruptedException e) {
    			System.out.println("§4[Pe] Sleep interrupted while waiting for lock");
    		}
    	}
    }
    
    private void releaseLock(Integer id, Player player) {
    	plugin.SetPetitionLock(id, player.getName(), true);
    }
    
    private void notifyNamedPlayer(String name, String message) {
    	Player[] players = plugin.getServer().getOnlinePlayers();
    	for (Player player: players) {
    		if (player.getName().equalsIgnoreCase(name)) {
    			player.sendMessage(message);
    		}
    	}
    }
    
private void notifyModerators(String message) {
    	Player[] players = plugin.getServer().getOnlinePlayers();
    	for (Player player: players) {
    		if (Permissions.Security.permission(player, "petition.moderate")) {
    			player.sendMessage(message);
    		}
    	}
    }
    

}

