package model;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class Cuboid implements Iterable<Block> {

    private final World world;
    private final int xMin, xMax, yMin, yMax, zMin, zMax;

    public Cuboid(Location loc1, Location loc2) {
        if (!loc1.getWorld().equals(loc2.getWorld()))
            throw new IllegalArgumentException("Locations must be in the same world!");

        this.world = loc1.getWorld();

        this.xMin = Math.min(loc1.getBlockX(), loc2.getBlockX());
        this.xMax = Math.max(loc1.getBlockX(), loc2.getBlockX());

        this.yMin = Math.min(loc1.getBlockY(), loc2.getBlockY());
        this.yMax = Math.max(loc1.getBlockY(), loc2.getBlockY());

        this.zMin = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        this.zMax = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
    }

    @NotNull
    @Override
    public Iterator<Block> iterator() {
        return new Iterator<Block>() {
            private int x = xMin;
            private int y = yMin;
            private int z = zMin;

            @Override
            public boolean hasNext() {
                return x <= xMax && y <= yMax && z <= zMax;
            }

            @Override
            public Block next() {
                Block block = world.getBlockAt(x, y, z);

                // перемещаем "курсор"
                if (++x > xMax) {
                    x = xMin;
                    if (++y > yMax) {
                        y = yMin;
                        z++;
                    }
                }

                return block;
            }
        };
    }

    public boolean isPlayerMemberOrOwnerInAllProtections(UUID playerId) {

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        Set<String> seenRegionIds = new HashSet<>();

        for (Block b : this) {
            Location loc = b.getLocation();

            ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(loc));
            for (ProtectedRegion region : set) {
                String id = region.getId();
                if (!seenRegionIds.add(id)) continue;

                // Проверка владельца/членства:
                DefaultDomain owners = region.getOwners();
                DefaultDomain members = region.getMembers();
                if (owners.contains(playerId) || members.contains(playerId)) continue;
                else return false;
            }
        }

        return true;
    }

    public boolean isWithinWorldBorder() {
        if (world == null) return false;

        // Получаем границы WorldBorder
        double borderSize = world.getWorldBorder().getSize() / 2.0;
        double centerX = world.getWorldBorder().getCenter().getX();
        double centerZ = world.getWorldBorder().getCenter().getZ();
        int worldMinX = (int) Math.floor(centerX - borderSize);
        int worldMaxX = (int) Math.ceil(centerX + borderSize);
        int worldMinZ = (int) Math.floor(centerZ - borderSize);
        int worldMaxZ = (int) Math.ceil(centerZ + borderSize);

        int worldMinY = world.getMinHeight();
        int worldMaxY = world.getMaxHeight() - 1;

        // Проверяем границы
        if (xMin < worldMinX || xMax > worldMaxX) return false;
        if (zMin < worldMinZ || zMax > worldMaxZ) return false;
        if (yMin < worldMinY || yMax > worldMaxY) return false;

        return true;
    }

    public Block[][] to2DArray(int y) {
        int width = xMax - xMin + 1;   // по X
        int depth = zMax - zMin + 1;   // по Z

        Block[][] arr = new Block[width][depth];

        for (int xi = 0; xi < width; xi++) {
            int x = xMin + xi;
            for (int zi = 0; zi < depth; zi++) {
                int z = zMin + zi;
                arr[xi][zi] = world.getBlockAt(x, y, z);
            }
        }
        return arr;
    }

    public int getXMin() {
        return xMin;
    }

    public int getXMax() {
        return xMax;
    }

    public int getZMin() {
        return zMin;
    }

    public int getZMax() {
        return zMax;
    }
}
