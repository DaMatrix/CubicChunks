/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.cubicChunks;

import cuchaz.cubicChunks.world.Cube;
import net.minecraft.world.chunk.IChunkProvider;

public interface CubeProvider extends IChunkProvider
{
	boolean cubeExists( int cubeX, int cubeY, int cubeZ );
	Cube provideCube( int cubeX, int cubeY, int cubeZ );
}
