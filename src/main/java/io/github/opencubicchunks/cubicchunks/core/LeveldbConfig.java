package io.github.opencubicchunks.cubicchunks.core;

import net.minecraftforge.common.config.Config;
import org.iq80.leveldb.CompressionType;

/**
 * @author DaPorkchop_
 */
@Config(modid = CubicChunks.MODID, name = "LevelDB", category = "leveldb")
public class LeveldbConfig {
    @Config.LangKey("cubicchunks.config.use_leveldb")
    @Config.Comment("Use leveldb-mcpe as the storage format. This can significantly reduce world sizes, but may not run correctly on all platforms.\n" +
            "Note that changing this value for an existing world will result in the existing world data being silently ignored and possibly overwritten.")
    @Config.RequiresWorldRestart
    public static boolean leveldb = true;

    @Config.RequiresMcRestart
    public static CompressionType columnCompressionType = CompressionType.ZLIB_RAW;

    @Config.RequiresMcRestart
    public static CompressionType cubeCompressionType = CompressionType.ZLIB_RAW;
}
