package de.Ste3et_C0st.FurnitureLib.LimitationManager;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import de.Ste3et_C0st.FurnitureLib.Crafting.Project;
import de.Ste3et_C0st.FurnitureLib.main.FurnitureManager;
import de.Ste3et_C0st.FurnitureLib.main.ObjectID;
import de.Ste3et_C0st.FurnitureLib.main.Type.LimitationType;

public class ChunkLimitation extends PermissionKitLimit{
	
	private static final String KEY = "Chunk";
	private static final String headString = KEY + "Limit";
	
	public ChunkLimitation() {
		super(LimitationType.CHUNK);
		this.writeConfig();
	}
	
	@Override
	public int getAmount(Predicate<ObjectID> predicate) {
		return (int) FurnitureManager.getInstance().getAllExistObjectIDs().filter(predicate).count();
	}
	
	@Override
	public int getLimit(Project project, Location location, Player player) {
		OptionalInt kitLimit = super.getKitLimit(project, location, player);
		return kitLimit.isPresent() ? kitLimit.getAsInt() : super.getLimit(project, location, player);
	}

	@Override
	public boolean canPlace(Location location, Project project, Player player) {
		return getAmount(buildFilter(location, project, player)) < getLimit(project, location, player);
	}

	@Override
	public void writeConfig() {
		List<String> headerConfig = Arrays.asList(
				"This is the ChunkLimitation file",
				"You can limit the max amount of Furnitures each chunk",
				"total.enable = (bool) | set default value for each project",
				"total.global = (bool) | override the project limit and force use total.amount for each project"
		);
		final YamlConfiguration configuration = super.loadYaml(headerConfig);
		super.writeGlobal(configuration, headString);
		FurnitureManager.getInstance().getProjects().forEach(project -> super.ioProjectLimit(headString, project, configuration));
		super.save(configuration, getFile());
	}

	@Override
	public void updateConfig(Project project) {
		if(this.amountMap.containsKey(project)) return;
		final YamlConfiguration configuration = super.loadYaml();
		super.ioProjectLimit(headString, project, configuration);
		super.save(configuration, getFile());
	}
	
	@Override
	public Predicate<ObjectID> buildFilter(Location location, Project project, Player player) {
		final int chunkX = location.getBlockX() >> 4;
		final int chunkZ = location.getBlockZ() >> 4;
		final World world = location.getWorld();
		final Predicate<ObjectID> prediacte = objectID -> objectID.getWorld().equals(world); 
		return prediacte.and(objectID -> objectID.isInChunk(chunkX, chunkZ));
	}
}