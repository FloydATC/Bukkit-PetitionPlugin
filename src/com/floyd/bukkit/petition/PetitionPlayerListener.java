package com.floyd.bukkit.petition;


import java.io.BufferedReader;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.HashMap;
import java.util.Date;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.*;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;

import com.nijikokun.bukkit.Permissions.Permissions;


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
    public void onPlayerJoin(PlayerEvent event) {
        Player player = event.getPlayer();
        
        // Play back messages stored in this player's maildir (if any)
        String[] messages = plugin.getMessages(player);
        if (messages.length > 0) {
        	for (String message : messages) {
        		player.sendMessage(message);
        	}
        	player.sendMessage("[Pe] §7Use /petition to view, comment or close");
        }
    }
    
    
}
