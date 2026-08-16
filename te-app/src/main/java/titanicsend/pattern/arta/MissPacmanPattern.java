package titanicsend.pattern.arta;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.transform.LXVector;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.util.TEColor;

@LXCategory("Arta")
public class MissPacmanPattern extends TEAudioPattern {
    public final CompoundParameter size =
            new CompoundParameter("Size", 0.95f, 0.4f, 1.3f)
                    .setDescription("Overall size of Miss Pac-Man");

    private static final int GRID_WIDTH = 16;
    private static final int GRID_HEIGHT = 16;
    private static final double FRAME_MS = 100.0;

    // Reverse-engineered from the actual GIF frames at:
    // https://media.tenor.com/5IjXWp2X39QAAAAj/pac-man-pac-man.gif
    // Sequence is closed -> medium -> wide -> medium.
    private static final String[][] FRAMES = {
            {
                    "................",
                    "...........RR...",
                    "......YYYYRRR...",
                    "....YYYYYYRRBR..",
                    "...YYYYYYYYYRBRR",
                    "...YYYYYYYYYYRRR",
                    "..YYYYYY....YRR.",
                    ".RYYYYYYYYYYYYY.",
                    ".RRYYYYYYYYYYYY.",
                    ".RYYYYYYYYYYYYY.",
                    "..YYYYYYYYY.YYY.",
                    "...YYYYYYYYYYY..",
                    "...YYYYYYYYYYY..",
                    "....YYYYYYYYY...",
                    "......YYYYY.....",
                    "................"
            },
            {
                    "................",
                    "...........RR...",
                    "......YYYYRRR...",
                    "....YYYYYYRRBR..",
                    "...YYYYYYYYYRBRR",
                    "...RRYYY..YYYRRR",
                    ".......YYB.YYRR.",
                    ".........YYYYYY.",
                    "...........YYYY.",
                    ".........YYYYYY.",
                    ".......YYYY.YYY.",
                    "...RRYYYYYYYYY..",
                    "...YYYYYYYYYYY..",
                    "....YYYYYYYYY...",
                    "......YYYYY.....",
                    "................"
            },
            {
                    "................",
                    "...........RR...",
                    ".....RYYYYRRR...",
                    "......RYYYRRBR..",
                    ".......YY.YYRBRR",
                    "........Y..YYRRR",
                    ".........YB.YRR.",
                    "..........YYYYY.",
                    "...........YYYY.",
                    "..........YYYYY.",
                    ".........YY.YYY.",
                    "........YYYYYY..",
                    ".......YYYYYYY..",
                    "......RYYYYYY...",
                    ".....RYYYYY.....",
                    "................"
            },
            {
                    "................",
                    "...........RR...",
                    "......YYYYRRR...",
                    "....YYYYYYRRBR..",
                    "...YYYYYYYYYRBRR",
                    "...RRYYY..YYYRRR",
                    ".......YYB.YYRR.",
                    ".........YYYYYY.",
                    "...........YYYY.",
                    ".........YYYYYY.",
                    ".......YYYY.YYY.",
                    "...RRYYYYYYYYY..",
                    "...YYYYYYYYYYY..",
                    "....YYYYYYYYY...",
                    "......YYYYY.....",
                    "................"
            }
    };

    private double animationTimeMs = 0.0;

    public MissPacmanPattern(LX lx) {
        super(lx);
        addParameter("Size", size);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        animationTimeMs += deltaMs;

        int frameIndex = (int) Math.floor(animationTimeMs / FRAME_MS) % FRAMES.length;
        String[] sprite = FRAMES[frameIndex];

        float centerX = (model.xMax + model.xMin) / 2.0f;
        float centerY = (model.yMax + model.yMin) / 2.0f;
        float modelWidth = model.xMax - model.xMin;
        float modelHeight = model.yMax - model.yMin;
        float pixelSize = Math.min(modelWidth / GRID_WIDTH, modelHeight / GRID_HEIGHT) * size.getValuef();
        float spriteWidth = GRID_WIDTH * pixelSize;
        float spriteHeight = GRID_HEIGHT * pixelSize;
        float startX = centerX - spriteWidth / 2.0f;
        float startY = centerY - spriteHeight / 2.0f;

        for (int i = 0; i < colors.length; i++) {
            colors[i] = LXColor.BLACK;
        }

        for (int i = 0; i < model.points.length; i++) {
            LXVector point = new LXVector(model.points[i]);
            int gridX = (int) Math.floor((point.x - startX) / pixelSize);
            int gridY = (int) Math.floor((point.y - startY) / pixelSize);

            if (gridX < 0 || gridX >= GRID_WIDTH || gridY < 0 || gridY >= GRID_HEIGHT) {
                continue;
            }

            int spriteY = GRID_HEIGHT - 1 - gridY;
            int spriteX = GRID_WIDTH - 1 - gridX;
            char pixel = sprite[spriteY].charAt(spriteX);
            int color = getPixelColor(pixel);
            if (color != -1) {
                colors[point.index] = color;
            }
        }
    }

    private int getPixelColor(char pixel) {
        switch (pixel) {
            case 'Y':
                return TEColor.YELLOW;
            case 'R':
                return LXColor.hsb(0, 100, 100);
            case 'B':
                return LXColor.hsb(230, 100, 100);
            default:
                return -1;
        }
    }
}
