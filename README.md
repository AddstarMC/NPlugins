NPlugins
=====
Project developed by Gael Ribes (https://ribesg.fr/) but abandoned in 2016 (https://github.com/Ribesg/NPlugins)

Now updated for Minecraft 1.9
Also supports 1.10.x

Addstar MC Jenkins server:  
* http://jenkins.addstar.com.au/


---
### Using NTheEndAgain

Place these two jar files in the server's plugins folder
* NCore.jar
* NTheEndAgain.jar

---
### NTheEndAgain Commands

| Command       | Explanation              |
| -------------------------- |-------------|
| /nend help                 | See commands
| /nend regen [world_name]   | Regenerate the chunks in the given End world.  If world_name is not specified, regenerate in the current world
| /nend respawn [world_name] | Respawn the Ender Dragon in the given End world.  If world_name is not specified, respawn in the current world
| /nend nb [world_name]      | Check whether any Ender Dragons are alive in the given End world
| /nend chunk info           | Check whether the current chunk is protected
| /nend chunk protect        | Protected a chunk, meaning to skip it when regenerating
| /nend chunk unprotect      | Unprotect a chunk
| /nend reload config        | Reload the configuration values from the .yml file
| /nend reload messages      | Reload messages from the messages.yml file
| /nend status               | Check status, including respawnType, regenType, and outer end regen time remaining
