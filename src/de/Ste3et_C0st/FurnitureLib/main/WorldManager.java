package de.Ste3et_C0st.FurnitureLib.main;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import de.Ste3et_C0st.FurnitureLib.main.Type.SQLAction;
import de.Ste3et_C0st.FurnitureLib.main.entity.fEntity;

public class WorldManager {

	//World List<ObjectID>
	private final static HashMap<String, HashSet<ObjectID>> objectList = new HashMap<String, HashSet<ObjectID>>();
	private final static Predicate<ObjectID> predicate = objectID -> SQLAction.REMOVE != objectID.getSQLAction();
	
	public void loadWorld(World world) {
		HashSet<ObjectID> objectSet = FurnitureLib.getInstance().getSQLManager().getDatabase().loadWorld(SQLAction.NOTHING, world);
		if(!objectSet.isEmpty()) {
			objectList.put(world.getName(), objectSet);
		}
	}
	
	public HashSet<ObjectID> getObjectSet(World world){
		return getObjectSet(world.getName());
	}
	
	public HashSet<ObjectID> getObjectSet(String worldName){
		HashSet<ObjectID> hashSet = objectList.getOrDefault(worldName, new HashSet<ObjectID>());
		if(!objectList.containsKey(worldName)) {
			objectList.put(worldName, hashSet);
		}
		return hashSet;
	}
	
	public ObjectID getObjectID(Location location) {
		return this.getObjectID(location.getWorld(), location.toVector());
	}
	
	public ObjectID getObjectID(World world, Vector vector) {
		return getObjectStreamFromWorld(world).filter(entry -> entry.getStartLocation().toVector().equals(vector)).findFirst().orElse(null);
	}
	
	public ObjectID getObjectID(String worldName, Vector vector) {
		return getObjectStreamFromWorld(worldName).filter(entry -> entry.getStartLocation().toVector().equals(vector)).findFirst().orElse(null);
	}
	
	public ObjectID getObjectID(World world, String string) {
		return getObjectStreamFromWorld(world).filter(entry -> entry.getSerial().equalsIgnoreCase(string)).findFirst().orElse(null);
	}
	
	public ObjectID getObjectID(String worldName, String string) {
		return getObjectStreamFromWorld(worldName).filter(entry -> entry.getSerial().equalsIgnoreCase(string)).findFirst().orElse(null);
	}
	
	public List<fEntity> getObjectIDByPassanger(Player player) {
		Integer entityID = player.getEntityId();
		return getObjectStreamFromWorld(player.getWorld()).flatMap(entry -> entry.getPacketList().stream()).filter(entry -> entry.getPassenger().contains(entityID)).collect(Collectors.toList());
	}
	
	public List<fEntity> getArmorStandFromPassenger(Player p) {
		return getObjectIDByPassanger(p);
	}
	
	public fEntity getByArmorStandID(World world, int entityID) {
		Optional<ObjectID> objectID = getObjectStreamFromWorld(world).filter(entry -> entry.containsEntity(entityID)).findFirst();
		return objectID.isPresent() ? objectID.get().getByID(entityID) : null;
	}
	
	public Stream<ObjectID> getObjectStreamFromWorld(World world){
		return getObjectSet(world).stream().filter(predicate);
	}
	
	public Stream<ObjectID> getObjectStreamFromWorld(String worldName){
		return getObjectSet(worldName).stream().filter(predicate);
	}
	
	public List<ObjectID> getInWorld(World world) {
		return getObjectStreamFromWorld(world).collect(Collectors.toList());
	}
	
	public List<ObjectID> getInWorld(String worldName) {
		return getObjectStreamFromWorld(worldName).collect(Collectors.toList());
	}

	public void updatePlayerView(Player player) {
		if(player.isOnline()) {
			World world = player.getWorld();
			getObjectStreamFromWorld(world.getName()).filter(entry -> entry.canSee(player)).forEach(entry -> entry.updatePlayerView(player));
			this.removePlayerView(player);
		}
	}
	
	public void removePlayerView(Player player) {
		getAllExistObjectIDs().filter(entry -> entry.getPlayerList().contains(player) && !entry.canSee(player)).forEach(entry -> entry.updatePlayerView(player));
	}
	
	public HashSet<ObjectID> getFromPlayer(UUID uuid) {
		return new HashSet<ObjectID>(getAllExistObjectIDs().filter(entry -> entry.getUUID().equals(uuid)).collect(Collectors.toList()));
	}
	
	public ObjectID getObjectIDByString(String objID) {
        return getAllExistObjectIDs().filter(entry -> entry.getID().equalsIgnoreCase(objID)).findFirst().orElse(null);
    }
	
	public HashSet<ObjectID> getInChunk(Chunk c) {
		int x = c.getX();
		int z = c.getZ();
		return new HashSet<ObjectID>(getObjectStreamFromWorld(c.getWorld()).filter(entry -> entry.getBlockX() >> 4 == x && entry.getBlockZ() >> 4 == z).collect(Collectors.toList()));
	}
	
	public HashSet<fEntity> getfArmorStandByObjectID(ObjectID id) {
        return id.getPacketList();
    }
	
	public ObjectID getObjectIDByEntityID(int entityID) {
		return getAllExistObjectIDs().filter(entry -> entry.containsEntity(entityID)).findFirst().orElse(null);
	}
	
	public ObjectID getfArmorStandByID(int entityID) {
		return getObjectIDByEntityID(entityID);
	}
	
	public Stream<ObjectID> getAllExistObjectIDs(){
		return objectList.values().stream().flatMap(entry -> entry.stream()).filter(predicate);
	}
	
	public Stream<ObjectID> getAllObjectIDs(){
		return objectList.values().stream().flatMap(entry -> entry.stream()).filter(Objects::nonNull);
	}
	
	public List<ObjectID> getObjectList(){
		return getAllObjectIDs().collect(Collectors.toList());
	}
	
	public void remove(ObjectID objectID) {
		if(Objects.nonNull(objectID)) {
			objectID.setSQLAction(SQLAction.REMOVE);
			
			if(!objectID.getBlockList().isEmpty()) {
				FurnitureLib.getInstance().getBlockManager().destroy(objectID.getBlockList(), false);
				objectID.getBlockList().clear();
			}
			
			objectID.getPacketList().stream().forEach(fEntity::kill);
			objectID.getPacketList().clear();
		}
	}
	
	public void deleteObjectID(ObjectID id) {
		getObjectSet(id.getWorldName()).remove(id);
	}
	
	public void remove(fEntity armorStandPacket) {
		ObjectID objectID = armorStandPacket.getObjID();
		objectID.getPacketList().remove(armorStandPacket);
	}
	
	public void removeFurniture(Player player) {
		getAllExistObjectIDs().forEach(entry -> entry.removePacket(player));
	}
	
	public void updateFurniture(ObjectID obj) {
		if (obj.isFromDatabase()) {
            obj.setSQLAction(SQLAction.UPDATE);
        }
        obj.update();
	}
	
	public void addObjectIDs(ObjectID... objArray) {
		for(ObjectID objectID : objArray) {
			this.addObjectID(objectID);
		}
	}
	
	public boolean addObjectID(ObjectID obj) {
        return getObjectSet(obj.getWorldName()).add(obj);
    }
	
	public void addObjectID(Iterable<ObjectID> objI) {
	    objI.forEach(this::addObjectID);
	}
	
	public void send(ObjectID id) {
	   if (Objects.nonNull(id)) {
	       return;
	   }
	   id.sendAll();
	}
	
	public void sendAll() {
        this.getAllExistObjectIDs().forEach(this::send);
    }
	

    public static HashMap<String, HashSet<ObjectID>> getObjectWorldHashMap(){
    	return objectList;
    }
    
}
