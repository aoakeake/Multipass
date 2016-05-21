/*
    Multipass, Nukkit permissions plugin
    Copyright (C) 2016  fromgate, nukkit.ru

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ru.nukkit.multipass.data;

import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import ru.nukkit.multipass.MultipassPlugin;
import ru.nukkit.multipass.permissions.Group;
import ru.nukkit.multipass.permissions.Pass;
import ru.nukkit.multipass.permissions.User;
import ru.nukkit.multipass.util.Message;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class YamlSource implements DataSource {

    private File userDir;
    private File groupFile;

    public YamlSource() {
        this.userDir = new File(MultipassPlugin.getPlugin().getDataFolder() + File.separator + "users");
        this.userDir.mkdirs();
        this.groupFile = new File(MultipassPlugin.getPlugin().getDataFolder() + File.separator + "groups.yml");
    }

    private File getUserFile(String user) {
        return new File(userDir + File.separator + user.toLowerCase() + ".yml");
    }

    @Override
    public User loadUser(String playerName) {
        Message.debugMessage("Loading permissions",playerName);
        File file = getUserFile(playerName);
        User user = new User(playerName);
        Config cfg = new Config(Config.YAML);
        if (file.exists()) {
            cfg.load(file.toString());
            Pass pass = sectionToPass(cfg.getRootSection());
            user = new User(playerName, pass);

            ConfigSection worlds = cfg.getSection("worlds");
            for (String world: worlds.getKeys(false)){
                ConfigSection ws = worlds.getSection(world);
                if (ws.isEmpty()) continue;
                Pass wpass = sectionToPass(ws);
                user.setWorldPass(world,wpass);
            }
        }
        return user;
    }

    @Override
    public void saveUser(User user) {
        File file = getUserFile(user.getName());
        Message.debugMessage("Save user file: ", file.toString());
        if (user.isEmpty()) {
            if (file.exists()) file.delete();
            Message.debugMessage(user.getName(), "data is empty. File removed");
        } else {
            Config cfg = new Config(file, Config.YAML);
            ConfigSection cfgSection = passToSection(user);
            cfg.setAll(cfgSection);
            user.getWorldPass().entrySet().forEach(e -> {
                cfg.set(e.getKey(),passToSection(e.getValue()));
                cfg.set(e.getKey(),e.getValue().getPermissionList());
            });
            Message.debugMessage(user.getName(), " saved.");
            cfg.save(file);
        }
    }

    @Override
    public void saveGroups(Collection<Group> groups) {
        Config cfg = new Config();
        groups.forEach(g -> {
            ConfigSection section = passToSection(g);
            cfg.set(g.getName(), section);
            g.getWorldPass().entrySet().forEach(e ->{
                ConfigSection ws = passToSection(e.getValue());
                section.set(e.getKey(),ws);
            });
            cfg.set(g.getName(),section);
        });
        cfg.save(this.groupFile);
    }

    @Override
    public Map<String, Group> loadGroups() {
        Map<String, Group> groups = new TreeMap<>();
        if (!groupFile.exists()) return groups;
        Config cfg = new Config(groupFile, Config.YAML);
        cfg.getSections().entrySet().forEach(e -> {
            Pass pass = sectionToPass((ConfigSection) e.getValue());
            Group group = new Group(e.getKey(),pass);
            ConfigSection worlds = cfg.getSection("worlds");
            for (String world: worlds.getKeys(false)){
                ConfigSection ws = worlds.getSection(world);
                if (ws.isEmpty()) continue;
                Pass wpass = sectionToPass(ws);
                group.setWorldPass(world,wpass);
            }
            groups.put(e.getKey(), group);
        });
        Message.debugMessage(groups.size(), "groups loaded");
        return groups;
    }

    @Override
    public boolean isStored(String userName) {
        return getUserFile(userName).exists();
    }

    private ConfigSection passToSection(Pass pass){
        ConfigSection section = new ConfigSection();
        section.set("groups", pass.getGroupList());
        section.set("permissions", pass.getPermissionList());
        section.set("prefix",pass.getPrefix());
        section.set("suffix",pass.getSuffix());
        section.set("priority",pass.getPriority());
        return section;
    }

    private Pass sectionToPass(ConfigSection section){
        Pass pass = new Pass();
        pass.setGroupList(section.getStringList("groups"));
        pass.setPermissionsList(section.getStringList("permissions"));
        pass.setPrefix(section.getString("prefix",""));
        pass.setSuffix(section.getString("suffix",""));
        pass.setPriority(section.getInt("priority",0));
        return pass;
    }
}