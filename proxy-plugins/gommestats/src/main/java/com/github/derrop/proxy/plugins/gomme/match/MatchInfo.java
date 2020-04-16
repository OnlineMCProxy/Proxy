package com.github.derrop.proxy.plugins.gomme.match;

import com.github.derrop.proxy.api.connection.ServiceConnection;
import com.github.derrop.proxy.api.entity.PlayerInfo;
import com.github.derrop.proxy.plugins.gomme.GommeGameMode;
import com.github.derrop.proxy.plugins.gomme.match.event.MatchEvent;
import com.github.derrop.proxy.plugins.gomme.stats.PlayerStatistics;

import java.util.ArrayList;
import java.util.Collection;

public class MatchInfo {

    private final ServiceConnection invoker;
    private final GommeGameMode gameMode;
    private final String matchId;
    private boolean running;
    private long beginTimestamp;
    private long endTimestamp;
    private PlayerInfo[] playersOnBegin;
    private PlayerInfo[] playersOnEnd;

    private PlayerStatistics[] statisticsOnBegin;

    private Collection<MatchEvent> events = new ArrayList<>();

    public MatchInfo(ServiceConnection invoker, GommeGameMode gameMode, String matchId) {
        this.invoker = invoker;
        this.gameMode = gameMode;
        this.matchId = matchId;
    }

    public void start(PlayerInfo[] currentPlayers, PlayerStatistics[] statistics) {
        this.running = true;
        this.beginTimestamp = System.currentTimeMillis();
        this.playersOnBegin = currentPlayers;
        this.statisticsOnBegin = statistics;
    }

    public void end(PlayerInfo[] currentPlayers) {
        this.running = false;
        this.endTimestamp = System.currentTimeMillis();
        this.playersOnEnd = currentPlayers;
    }

    public void callEvent(MatchEvent event) {
        this.events.add(event);
    }

    public ServiceConnection getInvoker() {
        return invoker;
    }

    public GommeGameMode getGameMode() {
        return gameMode;
    }

    public String getMatchId() {
        return matchId;
    }

    public boolean isRunning() {
        return running;
    }

    public long getBeginTimestamp() {
        return beginTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public PlayerInfo[] getPlayersOnBegin() {
        return playersOnBegin;
    }

    public PlayerInfo[] getPlayersOnEnd() {
        return playersOnEnd;
    }
}
