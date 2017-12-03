package cubicchunks.worldgen.generator.custom.test;

import cubicchunks.worldgen.generator.custom.builder.IBuilder;
import cubicchunks.worldgen.generator.custom.builder.NoiseSource;

public class NoiseTest {
    public static void main(String... args) {
        IBuilder builder = NoiseSource.perlin()
                .frequency(0.06)
                .octaves(6)
                .normalizeTo(-1, 1)
                .seed(System.currentTimeMillis())
                .create();

        double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
        for (int x = -100000; x < 900000; x++)    {
            double valueX = builder.get(x, 0, 0) * 256;
            double valueY = builder.get(0, x, 0) * 256;
            double valueZ = builder.get(0, 0, x) * 256;
            double value = (valueX + valueY + valueZ) / 3;
            if (value < min)    {
                min = value;
            } else if (value > max) {
                max = value;
            }
            System.out.println((value > 0) + " " + (valueX > 0) + " " + (valueY > 0) + " " + (valueZ > 0));
        }
        System.out.println(min + " " + max);
    }
}
