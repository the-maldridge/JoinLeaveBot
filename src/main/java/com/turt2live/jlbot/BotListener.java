/*******************************************************************************
 * Copyright (C) 2014 Travis Ralston (turt2live)
 *
 * This software is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.turt2live.jlbot;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.InviteEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PartEvent;

import java.util.HashMap;
import java.util.Map;

public class BotListener extends ListenerAdapter<PircBotX> {

    private Map<String, ChannelTracker> trackers = new HashMap<String, ChannelTracker>();

    @Override
    public void onInvite(InviteEvent<PircBotX> event) throws Exception {
        event.getBot().sendIRC().joinChannel(event.getChannel());
    }

    @Override
    public void onJoin(JoinEvent<PircBotX> event) throws Exception {
        ChannelTracker tracker = getTracker(event.getChannel(), event.getBot());
        tracker.onJoin(event.getUser().getNick());
    }

    @Override
    public void onMessage(MessageEvent<PircBotX> event) throws Exception {
        ChannelTracker tracker = getTracker(event.getChannel(), event.getBot());

        if (event.getMessage().startsWith(":")) {
            if (event.getMessage().trim().equalsIgnoreCase(":record")) {
                if (tracker.getRecordHolder() == null)
                    event.getBot().sendIRC().message(event.getChannel().getName(), "There is no current record");
                else
                    event.getBot().sendIRC().message(event.getChannel().getName(), tracker.getRecordHolder() + " holds the record at " + ChannelTracker.toHuman(tracker.getRecord()));
                return;
            } else if (event.getMessage().trim().equalsIgnoreCase(":ping")) {
                event.getBot().sendIRC().message(event.getChannel().getName(), "Pong");
                return;
            } else if (event.getMessage().trim().equalsIgnoreCase(":source")) {
                event.getBot().sendIRC().message(event.getChannel().getName(), "Source: https://github.com/turt2live/JoinLeaveBot");
                return;
            }
        }

        tracker.onMessage(event.getUser().getNick());
    }

    @Override
    public void onPart(PartEvent<PircBotX> event) throws Exception {
        ChannelTracker tracker = getTracker(event.getChannel(), event.getBot());
        tracker.onLeave(event.getUser().getNick());
    }

    private ChannelTracker getTracker(Channel channel, PircBotX bot) {
        if (!trackers.containsKey(channel.getName())) {
            trackers.put(channel.getName(), new ChannelTracker(channel.getName(), bot));
        }
        return trackers.get(channel.getName());
    }
}
