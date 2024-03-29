package io.github.steveplays28.restartserver;

import io.github.steveplays28.restartserver.commands.RestartCommand;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.Instant;

public class RestartScheduler {
	public boolean isRestartScheduled = false;
	public long nextRestart = -1;

	public int lastWarning = -1;

	public boolean isRestartIfNoPlayersHaveBeenOnlineScheduled = false;
	public long nextRestartIfNoPlayersHaveBeenOnline = -1;

	public void onTick(MinecraftServer server) {
		restartIntervalCounter(server);
		RestartIfNoPlayersHaveBeenOnlineCounter(server);
	}

	private void restartIntervalCounter(MinecraftServer server) {
		if (RestartServer.config.restartInterval <= 0) {
			return;
		}

		// Get current epoch time
		long now = Instant.now().getEpochSecond();

		if (isRestartScheduled) {
			if (nextRestart <= now) {
				// Restart server
				RestartCommand.execute(server.getCommandSource());
			} else {
				long difference = nextRestart - now;
				if (difference % 60 != 0) return;
				int minutesUntilRestart = (int) difference / 60;
				if (minutesUntilRestart != lastWarning) {
					lastWarning = minutesUntilRestart;
					if (minutesUntilRestart > RestartServer.config.restartWarningCount) return;
					server.getPlayerManager().broadcast(Text.literal(String.format(RestartServer.config.restartWarningMessage, minutesUntilRestart)).formatted(Formatting.YELLOW), false);
				}
			}
		} else {
			// Schedule restart
			nextRestart = now + RestartServer.config.restartInterval;
			isRestartScheduled = true;
		}
	}

	private void RestartIfNoPlayersHaveBeenOnlineCounter(MinecraftServer server) {
		if (!RestartServer.config.restartIfNoPlayersHaveBeenOnline) {
			return;
		}

		// Get current epoch time
		long now = Instant.now().getEpochSecond();

		if (isRestartIfNoPlayersHaveBeenOnlineScheduled) {
			if (server.getCurrentPlayerCount() > 0) {
				// Unschedule no players restart
				nextRestartIfNoPlayersHaveBeenOnline = -1;
				isRestartIfNoPlayersHaveBeenOnlineScheduled = false;
			}

			if (nextRestartIfNoPlayersHaveBeenOnline <= now && isRestartIfNoPlayersHaveBeenOnlineScheduled) {
				// Restart server
				RestartCommand.execute(server.getCommandSource());
			}
		} else {
			if (server.getCurrentPlayerCount() <= 0) {
				// Schedule no players restart
				nextRestartIfNoPlayersHaveBeenOnline = now + RestartServer.config.noPlayersWaitTime;
				isRestartIfNoPlayersHaveBeenOnlineScheduled = true;
			}
		}
	}
}
