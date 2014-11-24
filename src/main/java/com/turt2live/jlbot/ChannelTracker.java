package com.turt2live.jlbot;

import org.pircbotx.PircBotX;

import java.io.*;
import java.util.Map;

public class ChannelTracker {

    /*
    TODO: Fix known bugs

    Confirmed Bugs:
        1) Person A joins
           Person B joins
           Person A talks
           Person A leaves
           > Person B is still being tracked when A should have (possibly) set a record
        2) Inaccurate consideration of "question answered". Assumes that *ANYONE* else who speaks during the
           person's time in the channel is a response to the question - intended but could do with some work.

           Eg:
           Person A joins
           Person B talks  // Already in channel
           Person A talks
           Person A leaves
     */

    private long record = Long.MAX_VALUE;
    private String lastRecord = null;
    private PircBotX bot;
    private String channel;
    private Map<String, userInChannel> nicks;

    public ChannelTracker(String channel, PircBotX bot) {
        this.bot = bot;
        this.channel = channel;
        load();
    }

    public void checkNicks() {
        for(String nick: nicks.keySet()) {
            if(nicks.get(nick).getTimeInChannel()>record) {
                nicks.remove(nick);
            }
        }
    }

    public void onJoin(String nick) {
        nicks.put(nick, new userInChannel(nick, System.currentTimeMillis()));
        checkNicks();
    }

    public void onLeave(String nick) {
        if(nicks.containsKey(nick)) {
            if((nicks.get(nick).getTalked()) && (nicks.get(nick).getTimeInChannel()<record)) {
                    if (lastRecord != null)
                        bot.sendIRC().message(channel, nick + " beat the record for shortest time in the channel! Last record was " + toHuman(record) + " and is now " + toHuman(nicks.get(nick).getTimeInChannel()));
                    else
                        bot.sendIRC().message(channel, nick + " has set the record for the shortest time in the channel at " + toHuman(nicks.get(nick).getTimeInChannel()));
                    record = nicks.get(nick).getTimeInChannel();
                    lastRecord = nick;
                    save();
            }
        }
    }


    public void onMessage(String nick) {
        if (nicks.containsKey(nick)) {
            nicks.get(nick).setTalked(true);
        }
        checkNicks();
    }

    public void save() {
        try {
            File f = new File("ch-record-" + channel + ".dat");
            DataOutputStream os = new DataOutputStream(new FileOutputStream(f, false));
            os.writeUTF(lastRecord);
            os.writeLong(record);
            os.flush();
            os.close();
        } catch (Exception e) {
            bot.sendIRC().message(channel, "Warning: Error writing record information. " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public void load() {
        try {
            File f = new File("ch-record-" + channel + ".dat");
            if (!f.exists()) return;

            DataInputStream is = new DataInputStream(new FileInputStream(f));
            lastRecord = is.readUTF();
            record = is.readLong();
            is.close();
        } catch (Exception e) {
            bot.sendIRC().message(channel, "Warning: Error loading record information. " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public long getRecord() {
        return record;
    }

    public String getRecordHolder() {
        return lastRecord;
    }

    public static String toHuman(long time) {
        return time + "ms";
    }
}

class userInChannel {
    private String nick;
    private boolean talked = false;
    private long joinTime;

    public userInChannel(String nick, long joinTime) {
        this.nick = nick;
        this.joinTime = joinTime;
    }

    public long getTimeInChannel() {
        return System.currentTimeMillis() - joinTime;
    }

    public void setTalked(boolean talked) {
        this.talked = talked;
    }

    public boolean getTalked() {
        return talked;
    }
}
