package de.codeinfection.quickwango.gmChat;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.data.GroupVariables;
import org.anjocaido.groupmanager.data.User;
import org.anjocaido.groupmanager.data.UserVariables;
import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder;
import org.anjocaido.groupmanager.dataholder.worlds.WorldsHolder;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author CodeInfection
 */
public class gmChat extends JavaPlugin implements Listener
{
    private Server server;
    private PluginManager pm;
    private File dataFolder;
    private PluginDescriptionFile pdf;
    private Logger logger;

    private WorldsHolder worldsHolder;

    public boolean allowColors;
    public boolean allowUserData;
    public String format;

    private static final String[] placeHolders = new String[] {
        "NAME",
        "DISPLAYNAME",
        "GROUP",
        "PREFIX",
        "SUFFIX",
        "WORLD",
        "MESSAGE"
    };

    @Override
    public void onEnable()
    {
        // basic setup
        this.server = this.getServer();
        this.pm = this.server.getPluginManager();
        this.dataFolder = this.getDataFolder();
        this.pdf = this.getDescription();
        this.logger = this.getLogger();

        // GroupManager hook
        GroupManager gm = (GroupManager)this.pm.getPlugin("GroupManager");
        if (gm != null)
        {
            this.worldsHolder = gm.getWorldsHolder();
        }
        if (this.worldsHolder == null)
        {
            error("Failed to hook into GroupManager: Plugin not found!");
            return;
        }

        // Config
        Configuration config = this.getConfig();
        config.options().copyDefaults(true);

        this.format = config.getString("format", this.format);
        this.allowUserData = config.getBoolean("allowUserData", this.allowUserData);
        this.allowColors = config.getBoolean("allowColors", this.allowColors);

        this.dataFolder.mkdirs();
        this.saveConfig();

        // Event registration
        this.pm.registerEvents(this, this);

        // Command registration
        this.getCommand("reloadgmchat").setExecutor(new CommandExecutor() {
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
            {
                if (sender instanceof Permissible)
                {
                    Permissible permissableSender = (Permissible)sender;
                    if (!permissableSender.hasPermission("gmChat.reload"))
                    {
                        sender.sendMessage(ChatColor.RED + "Permission denied!");
                        return true;
                    }
                }

                onDisable();
                onEnable();

                sender.sendMessage(ChatColor.GREEN + "gmChat reloaded!");
                
                return true;
            }
        });

        log("Version " + this.pdf.getVersion() + " enabled");
    }

    @Override
    public void onDisable()
    {
        log("Version " + this.pdf.getVersion() + " disabled");
    }

    public String formatMessage(final Player player, final String message)
    {
        String prefix = "";
        String suffix = "";
        String formatString = "";

        OverloadedWorldHolder worldHolder = this.worldsHolder.getWorldData(player);
        User user = worldHolder.getUser(player.getName());
        if (this.allowUserData)
        {
            UserVariables userVariables = user.getVariables();
            prefix = userVariables.getVarString("prefix");
            suffix = userVariables.getVarString("suffix");
            formatString = userVariables.getVarString("format");
        }
        Group group = user.getGroup();
        GroupVariables groupVariables = group.getVariables();
        if (prefix.length() == 0)
        {
            prefix = groupVariables.getVarString("prefix");
        }
        if (suffix.length() == 0)
        {
            suffix = groupVariables.getVarString("suffix");
        }
        if (formatString.length() == 0)
        {
            formatString = groupVariables.getVarString("format");
        }
        
        if (formatString.length() == 0)
        {
            formatString = this.format;
        }
        final String[] values = new String[] {
            player.getName(),
            player.getDisplayName(),
            group.getName(),
            prefix,
            suffix,
            player.getWorld().getName(),
            message
        };

        for (int i = 0; i < placeHolders.length; ++i)
        {
            formatString = formatString.replace("{" + placeHolders[i] + "}", values[i]);
        }
        return formatString.replaceAll("&([a-f0-9])", (this.allowColors ? "\u00A7$1" : ""));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerChat(PlayerChatEvent event)
    {
        event.setFormat(this.formatMessage(event.getPlayer(), event.getMessage()).replace("%", "%%"));
    }

    public void log(Level logLevel, String msg, Throwable t)
    {
        this.logger.log(logLevel, msg, t);
    }

    public void log(String msg)
    {
         this.log(Level.INFO, msg, null);
    }

    public void error(String msg)
    {
         this.log(Level.SEVERE, msg, null);
    }

    public void error(String msg, Throwable t)
    {
         this.log(Level.SEVERE, msg, t);
    }
}
