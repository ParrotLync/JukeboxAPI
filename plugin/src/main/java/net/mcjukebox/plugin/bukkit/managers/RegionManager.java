package net.mcjukebox.plugin.bukkit.managers;

import lombok.Getter;
import net.mcjukebox.plugin.bukkit.MCJukebox;
import net.mcjukebox.plugin.bukkit.api.JukeboxAPI;
import net.mcjukebox.plugin.bukkit.managers.shows.ShowManager;
import net.mcjukebox.plugin.bukkit.utils.DataUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

public class RegionManager implements Listener {

    @Getter
    private HashMap<String, String> regions;
    private String folder;

    public RegionManager(){
        folder = MCJukebox.getInstance().getDataFolder() + "";
        load();
    }

    private void load(){
        regions = DataUtils.loadObjectFromPath(folder + "/regions.data");
        if(regions == null) regions = new HashMap<>();

        // Import from the "shared.data" file we accidentally created
        HashMap<String, String> sharedFile = DataUtils.loadObjectFromPath(folder + "/shared.data");
        if (sharedFile != null) {
            MCJukebox.getInstance().getLogger().info("Running migration of shared.data regions...");
            for (String key : sharedFile.keySet()) regions.put(key, sharedFile.get(key));
            new File(folder + "/shared.data").delete();
            MCJukebox.getInstance().getLogger().info("Migration complete.");
        }
    }

    public int importFromOA() {
        try {
            File configFile = new File("plugins/OpenAudioMc/config.yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            ConfigurationSection regionConfig = config.getConfigurationSection("storage.regions");
            int added = 0;
            for (String region : regionConfig.getKeys(false)) {
                String url = regionConfig.getString(region + ".src");
                if (url.length() > 0 && !url.contains(" ")) {
                    regions.put(region.toLowerCase(), url);
                    added++;
                }
            }
            return added;
        } catch (Exception ex) {
            return 0;
        }
    }

    public void save(){
        DataUtils.saveObjectToPath(regions, folder + "/regions.data");
    }

    public void addRegion(String ID, String URL){
        regions.put(ID.toLowerCase(), URL);
        ShowManager showManager = MCJukebox.getInstance().getShowManager();
        HashMap<UUID, String> playersInRegion = MCJukebox.getInstance().getRegionListener().getPlayerInRegion();

        for (UUID uuid : playersInRegion.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                showManager.getShow(regions.get(ID)).addMember(player, true);
            }
        }
    }

    public void removeRegion(String ID){
        ShowManager showManager = MCJukebox.getInstance().getShowManager();
        HashMap<UUID, String> playersInRegion = MCJukebox.getInstance().getRegionListener().getPlayerInRegion();

        Iterator<UUID> keys = playersInRegion.keySet().iterator();

        while (keys.hasNext()) {
            UUID uuid = keys.next();
            String regionID = playersInRegion.get(uuid);

            if (regionID.equals(ID)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) continue;

                if (regions.get(ID).charAt(0) == '@') {
                    showManager.getShow(regions.get(ID)).removeMember(player);
                } else {
                    JukeboxAPI.stopMusic(player);
                    keys.remove();
                }
            }
        }

        regions.remove(ID);
    }

    public HashMap<String, String> getRegions() { return this.regions; }

    public boolean hasRegion(String ID){
        return regions.containsKey(ID);
    }

    public String getURL(String ID){
        return regions.get(ID);
    }

}
