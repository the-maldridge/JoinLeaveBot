package com.turt2live.jlbot;

import org.pircbotx.PircBotX;

import java.io.*;

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

    private boolean sentMessage = false;
    private long joinTime = 0;
    private String lastUser = null;
    private long record = Long.MAX_VALUE;
    private String lastRecord = null;
    private PircBotX bot;
    private String channel;

    public ChannelTracker(String channel, PircBotX bot) {
        this.bot = bot;
        this.channel = channel;
        load();
    }

    public void onJoin(String nick) {
        lastUser = nick;
        joinTime = System.currentTimeMillis();
    }

    public void onLeave(String nick) {
        if (nick.equals(lastUser)) {
            if (sentMessage) {
                long totalTime = System.currentTimeMillis() - joinTime;
                if (totalTime < record) {
                    if (lastRecord != null)
                        bot.sendIRC().message(channel, nick + " beat the record for shortest time in the channel! Last record was " + toHuman(record) + " and is now " + toHuman(totalTime));
                    else
                        bot.sendIRC().message(channel, nick + " has set the record for the shortest time in the channel at " + toHuman(totalTime));
                    record = totalTime;
                    lastRecord = nick;
                    save();
                }
            }
            lastUser = null;
            sentMessage = false;
        }
    }

    public void onMessage(String nick) {
        if (!nick.equals(lastUser)) {
            lastUser = null;
        } else sentMessage = true;
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
