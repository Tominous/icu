/*
 * Copyright (C) 2017 - 2018 Dennis Neufeld
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package space.npstr.icu.listeners;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import space.npstr.icu.db.entities.GuildSettings;
import space.npstr.sqlsauce.DatabaseWrapper;

import java.util.function.Supplier;

/**
 * Created by napster on 25.01.18.
 * <p>
 * Listens for messages containing everyone / here mentions and give the user using it that role to troll them back
 */
public class EveryoneHereListener extends ThreadedListener {

    private final Supplier<DatabaseWrapper> wrapperSupp;

    public EveryoneHereListener(Supplier<DatabaseWrapper> wrapperSupplier) {
        this.wrapperSupp = wrapperSupplier;
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        getExecutor(event.getGuild()).execute(() -> guildMessageReceived(event));
    }

    private void guildMessageReceived(GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        Member member = event.getMember();
        if (member == null) {
            return;
        }
        Guild guild = event.getGuild();
        GuildSettings guildSettings = wrapperSupp.get().getOrCreate(GuildSettings.key(guild));

        //dont troll admins
        if (CommandsListener.isAdmin(wrapperSupp.get(), member)) {
            return;
        }

        Message msg = event.getMessage();

        Long hereId = guildSettings.getHereRoleId();
        Role hereRole = hereId != null ? guild.getRoleById(hereId) : null;
        if (hereRole != null
                && (msg.mentionsEveryone() || msg.isMentioned(hereRole))
                && !member.getRoles().contains(hereRole)) {
            addRole(msg, member, hereRole);
        }

        Long everyoneId = guildSettings.getEveryoneRoleId();
        Role everyoneRole = everyoneId != null ? guild.getRoleById(everyoneId) : null;
        if (everyoneRole != null
                && (msg.mentionsEveryone() || msg.isMentioned(everyoneRole))
                && !member.getRoles().contains(everyoneRole)) {
            addRole(msg, member, everyoneRole);
        }
    }

    //msg needs to be from a guild
    private void addRole(Message msg, Member member, Role role) {
        if (role.getGuild().getSelfMember().canInteract(role)) {
            role.getGuild().addRoleToMember(member, role).queue(__ -> {
                if (role.getGuild().getSelfMember().hasPermission(msg.getTextChannel(), Permission.MESSAGE_ADD_REACTION)) {
                    msg.addReaction("👌").queue();
                    msg.addReaction("👌🏻").queue();
                    msg.addReaction("👌🏼").queue();
                    msg.addReaction("👌🏽").queue();
                    msg.addReaction("👌🏾").queue();
                    msg.addReaction("👌🏿").queue();
                }
            });
        }

    }
}
