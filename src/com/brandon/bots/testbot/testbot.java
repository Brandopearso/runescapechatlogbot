package com.brandon.bots.testbot;

import com.runemate.game.api.script.framework.LoopingScript;
import com.runemate.game.api.script.framework.listeners.ChatboxListener;
import com.runemate.game.api.script.framework.listeners.events.MessageEvent;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.sql.Connection;
import java.sql.DriverManager;

public class testbot extends LoopingScript {

    public static final String DATABASE_NAME = "test";
    public static final String DATABASE_USERNAME = "test";
    public static final String DATABASE_PASSWORD = "test";

    // hash that contains a key of a player name, and a value of all the messages they've sent
    HashMap<String, ArrayList<String>> player_hash = new HashMap<String, ArrayList<String>>();

    // called over and over again by runemate
    @Override
    public void onLoop() {

        // placeholder for future default action
    }

    // called once at startup
    @Override
    public void onStart(String... args) {

        getEventDispatcher().addListener(chat);
    }

    // returns true if message contains spam.
    public boolean contains_character_spam(String message) {

        String[] character_blacklist = {"/", "\\" , ">", "<", "==", "[]", "~", "pokemon", "doubling", "double",  "@", "$"};

        for (int i = 0; i < character_blacklist.length; i++) {

            if (message.toLowerCase().contains(character_blacklist[i])) {

                return true;
            }
        }
        return false;
    }

    //returns true if a given player has been spamming the same message over and over.
    public boolean contains_duplicate_spam(String player_name, String player_message) {

        // immediately return false if the player hasn't said anything before.
        if (!player_hash.containsKey(player_name)) {

            return false;
        }
        else {

            ArrayList<String> player_messages = player_hash.get(player_name);
            if (player_messages.contains(player_message)) {

                return true;
            }
            else {

                return false;
            }
        }
    }

    // adds player to player hash if they're not in it already. used for spam detection.
    // only triggers when there's a new player. returns true if it adds a new player, false if the player
    // already exists, which means that we'll just add the message to the existing hash entry.
    public void add_to_player_hash(String player, String message) {

        // add new entry to player_hash, because the player doesn't exist yet
        if (!player_hash.containsKey(player)) {

            ArrayList<String> player_messages = new ArrayList<String>();
            player_messages.add(message);
            player_hash.put(player, player_messages);
        } else {

            player_hash.get(player).add(message);
        }
    }

    // this happens whenever a new message is sent to chat.
    ChatboxListener chat = new ChatboxListener() {

        @Override
        public void onMessageReceived(MessageEvent messageEvent) {

            Connection c;
            Statement statement;
            String message = messageEvent.getMessage();
            String sender = messageEvent.getSender();

            // check for spam that's usually live players repeating the same messsage over and over again
            if (contains_duplicate_spam(sender, message) || contains_character_spam(message)) {

                return;
            } else {

                add_to_player_hash(sender, message);
                String date = new SimpleDateFormat("YYYY-MM-dd HH:MM:ss").format(new Timestamp(System.currentTimeMillis()));
                String sql = "INSERT INTO log_table (player_name, message, time) "
                        + "VALUES ('" + sender + "', '" + message + "', '" + date + "');";
                try {

                    c = DriverManager.getConnection(DATABASE_NAME, DATABASE_USERNAME, DATABASE_PASSWORD);
                    c.setAutoCommit(false);
                    statement = c.createStatement();
                    statement.executeUpdate(sql);
                    statement.close();
                    c.commit();
                    c.close();
                } catch (Exception e) {

                    System.out.println("Failed to write to database.");
                }
            }
        }
    };
}
