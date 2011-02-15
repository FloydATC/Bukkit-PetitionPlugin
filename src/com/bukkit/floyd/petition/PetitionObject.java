package com.bukkit.floyd.petition;

import java.io.*;
import java.util.*;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.Server;

public class PetitionObject {
	String path = "plugins/PetitionPlugin";
	String archive = "plugins/PetitionPlugin/archive";
	
	Integer id = 0;
	String owner = "";
	String title = "";
	String world = "world";
	Double x = 0d;
	Double y = 0d;
	Double z = 0d;
	Float pitch = 0.0f;
	Float yaw = 0.0f;
	String assignee = "*";
	ArrayList<String> log = new ArrayList<String>();
		
	// Create a new petition
	public PetitionObject( Integer newid, Player player, String newtitle ) {
		id = newid;
		owner = player.getName();
		title = newtitle;
		world = player.getLocation().getWorld().getName();
		x = player.getLocation().getX();
		y = player.getLocation().getY();
		z = player.getLocation().getZ();
		pitch = player.getLocation().getPitch();
		yaw = player.getLocation().getYaw();
		Save();
	}

	
	// Load an existing petition
	public PetitionObject( Integer getid ) {
		String fname = path + "/" + String.valueOf(getid) + ".ticket";
		// TODO: Implement locking to prevent simultaneous updates
		// ...
    	try {
        	BufferedReader input =  new BufferedReader(new FileReader(fname));
    		String line = null;
    		while (( line = input.readLine()) != null) {
    			// File consists of key=value pairs, parse it 
    			String[] parts = line.split("=", 2);
    			if (parts[0].equals("id")) { id = Integer.parseInt(parts[1]); }
    			if (parts[0].equals("owner")) { owner = parts[1]; }
    			if (parts[0].equals("title")) { title = parts[1]; }
    			if (parts[0].equals("world")) { world = parts[1]; }
    			if (parts[0].equals("x")) { x = Double.parseDouble(parts[1]); }
    			if (parts[0].equals("y")) { y = Double.parseDouble(parts[1]); }
    			if (parts[0].equals("z")) { z = Double.parseDouble(parts[1]); }
    			if (parts[0].equals("pitch")) { pitch = Float.parseFloat(parts[1]); }
    			if (parts[0].equals("yaw")) { yaw = Float.parseFloat(parts[1]); }
    			if (parts[0].equals("assignee")) { assignee = parts[1]; }
    			if (parts[0].equals("log")) { log.add(parts[1]); }
    		}
    		input.close();
    	}
    	catch (FileNotFoundException e) {
    		System.out.println("[Pe] Error reading " + e.getLocalizedMessage());
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
	}

	
	// Save a new or updated petition
	public void Save() {
		String fname = path + "/" + String.valueOf(id) + ".ticket";
   		BufferedWriter output;
   		if (isValid() == false) {
   		  return;
   		}
   	    try {
       		output = new BufferedWriter(new FileWriter(fname));
       		output.write( "id=" + String.valueOf(id) + "\n" );
       		output.write( "owner=" + owner + "\n" );
       		output.write( "title=" + title + "\n" );
       		output.write( "world=" + world + "\n" );
       		output.write( "x=" + String.valueOf(x) + "\n" );
       		output.write( "y=" + String.valueOf(y) + "\n" );
       		output.write( "z=" + String.valueOf(z) + "\n" );
       		output.write( "pitch=" + String.valueOf(pitch) + "\n" );
       		output.write( "yaw=" + String.valueOf(yaw) + "\n" );
       		output.write( "assignee=" + assignee + "\n" );
       		for ( String entry : log ) {
       		  output.write( "log=" + entry + "\n" );
       		}
       		output.close();
   	    }
   	    catch (Exception e) {
    		e.printStackTrace();
   	    }
   	    
	}
	
	
	public void Assign(Player player, String name) {
		log.add("Assigned to " + name + " by " + player.getName());
		assignee = name;
		Save();
	}
	
	
	public void Unassign(Player player) {
		log.add("Unassigned by " + player.getName());
		assignee = "*";
		Save();
	}
	
	
	public void Close(Player player, String message) {
		if (message.equals("")) {
			log.add("Closed by " + player.getName());
		} else {
			log.add("Closed by " + player.getName() + ": " + message);
		}
		Save();
		File oldFile = new File(path + "/" + id + ".ticket");
		oldFile.renameTo(new File(archive + "/" + id + ".ticket"));
	}
	
	
	public void Comment(Player player, String message) {
		if (message.equals("")) {
			return;
		} else {
			log.add( player.getName() + ": " + message);
		}
		Save();
	}
	
	
	// Return 'true' if this is a valid petition object
	public boolean isValid() {
		return (id != 0);
	}
	
	
	public String Owner() {
		return owner;
	}
	public String Owner(Server server) {
		if (server.getPlayer(owner) == null) {
			return "§4ø§f" + owner;	// Offline
		} else {
			return "§2+§f" + owner;	// Online
		}
	}
	
	
	
	
	public String Title() {
		return title;
	}
	
	
	public String Assignee() {
		return assignee;
	}
	public String Assignee(Server server) {
		if (server.getPlayer(assignee) == null) {
			return "§4ø§f" + assignee;		// Offline
		} else {
			return "§2+§f" + assignee;		// Online
		}
	}
	
	
	public String ID() {
		return String.valueOf(id);
	}
	
	
	public String Header(Server server) {
		return "§6#" + ID() + " " + Owner(server) + "§7 -> " + Assignee(server) + "§7: " + Title() + " (" + Log().length + ")";
	}
	
	
	public String[] Log() {
		String[] lines = new String[log.size()];
		log.toArray(lines);
		return lines;
	}
	

	public String World() {
		return world;	// World name
	}
	
	
	public Location getLocation(Server server) {
		List<World> worlds = server.getWorlds();
		World normal = null;
		System.out.println("Examining worlds");
		for (World w : worlds ) {
			if (w.getName().equals(world)) {
				return new Location(w, x, y, z, yaw, pitch);
			}
			if (w.getName().equals("world")) {
				normal = w;
			}
		}
		// Use the first world if we can't find the right one
		return new Location(normal, x, y, z, yaw, pitch);
	}

}

