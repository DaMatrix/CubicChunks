package cubicchunks.worldgen.generator.custom.test;

import com.flowpowered.noise.NoiseQuality;
import com.flowpowered.noise.module.source.Perlin;

public class NoiseTest {
    public static void main(String... args) {
        Perlin groundNoise = new Perlin();
        groundNoise.setNoiseQuality(NoiseQuality.STANDARD);
        groundNoise.setOctaveCount(4);
        groundNoise.setFrequency(0.008);
        groundNoise.setSeed((int) (System.currentTimeMillis() / (Math.pow(2, 32))));

        double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
        for (int x = 0; x < 900000; x++)    {
            double value = groundNoise.getValue(x, 0,0);
            //value *= (400 - 3) / 2;
            //value += (400 + 3) / 2;
            value *= 256;
            if (value > max)    {
                max = value;
            } else if (value < min) {
                min = value;
            }
            System.out.println(value);
        }
        System.out.println(min + " " + max);
    }
}
