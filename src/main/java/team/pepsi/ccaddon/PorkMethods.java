/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package team.pepsi.ccaddon;

import cubicchunks.util.CubePos;
import cubicchunks.world.ICubicWorld;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Some useful methods
 *
 * @author DaPorkchop_
 */
public class PorkMethods {
    public static boolean isDedicatedServer = false;
    public static boolean isForgeSever = false;
    public static int overworldSpawnOffset = 256;

    /**
     * Gets a safe place to spawn in the given radii, centered around the blockpos.
     * Based on a functionally identical method I made for 2p2e
     *
     * @param world The world to check in
     * @param center The center of the area to check
     * @param yRadius The radius of blocks to choose random positions in on the Y axis
     * @param xRadius The radius of blocks to choose random positions in on the X axis
     * @param zRadius The radius of blocks to choose random positions in on the Z axis
     * @return
     * @author DaPorkchop_
     */
    public static BlockPos getSafeSpawnPoint(World world, BlockPos center, int yRadius, int xRadius, int zRadius) {
        BlockPos newPos = new BlockPos(0, 0, 0); //placeholder for saving memory
        IBlockState state;

        for (int i = 0; i < 50; i++) {
            newPos.x = ThreadLocalRandom.current().nextInt(center.x - xRadius, center.x + xRadius + 1); //generate coords in range
            newPos.y = ThreadLocalRandom.current().nextInt(center.y - yRadius, center.y + yRadius + 1);
            newPos.z = ThreadLocalRandom.current().nextInt(center.z - zRadius, center.z + zRadius + 1);
            state = world.getBlockState(newPos);
            FINDAIR:
            if (state.getBlock() != Blocks.AIR) { //scan up for air, find new coordinates if not within 30 blocks
                for (int j = 0; j < 30; j++) {
                    newPos.y++;
                    state = world.getBlockState(newPos);
                    if (state.getBlock() == Blocks.AIR) {
                        break FINDAIR;
                    }
                }
                continue;
            }
            //check up
            newPos.y++;
            if (world.getBlockState(newPos).getBlock() != Blocks.AIR) {
                continue;
            }
            //check down
            newPos.y -= 2; //subtract two because we added one earlier
            state = world.getBlockState(newPos);
            if (!state.getBlock().isFullCube(state)) {
                continue;
            }

            center = newPos;
            break;
        }

        return center;
    }

    public static boolean isCubeOutOfBounds(CubePos pos)    {
        return isCubeOutOfBounds(pos.getZ());
    }

    public static boolean isCubeOutOfBounds(int posZ)   {
        return posZ < 0 || posZ > 15;
    }
}
