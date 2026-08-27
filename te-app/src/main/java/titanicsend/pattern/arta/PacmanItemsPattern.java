package titanicsend.pattern.arta;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.transform.LXVector;
import titanicsend.pattern.TEAudioPattern;

@LXCategory("Arta")
public class PacmanItemsPattern extends TEAudioPattern {
    public final CompoundParameter size =
            new CompoundParameter("ItemSize", 0.82f, 0.35f, 1.35f)
                    .setDescription("Overall size of the Pac-Man item");

    public final CompoundParameter floatSpeed =
            new CompoundParameter("FloatSpeed", 0.85f, 0.1f, 5.0f)
                    .setDescription("Speed of the floating bounce");

    public final CompoundParameter floatAmount =
            new CompoundParameter("FloatAmount", 0.26f, 0.0f, 1.0f)
                    .setDescription("Amount of floating movement");

    public final BooleanParameter enableFloat =
            new BooleanParameter("Float", true)
                    .setDescription("Enable floating animation");

    public final CompoundParameter switchTime =
            new CompoundParameter("SwitchSec", 1.25f, 0.35f, 4.0f)
                    .setDescription("Seconds each item stays visible");

    public final BooleanParameter panic =
            new BooleanParameter("PANIC", false)
                    .setDescription("Reset all parameters to defaults")
                    .setMode(BooleanParameter.Mode.MOMENTARY);

    private static final int GRID_WIDTH = 16;
    private static final int GRID_HEIGHT = 16;

    private static final char EMPTY = '.';
    private static final char RED = 'R';
    private static final char ORANGE = 'O';
    private static final char GREEN = 'G';
    private static final char WHITE = 'W';
    private static final char BROWN = 'N';

    private static final int ITEM_RED = LXColor.hsb(0, 100, 100);
    private static final int ITEM_ORANGE = LXColor.hsb(15.97, 100, 100);
    private static final int ITEM_GREEN = LXColor.hsb(115.07, 100, 100);
    private static final int ITEM_WHITE = LXColor.hsb(0, 0, 100);

    private static final String[] CHERRIES = {
            "................",
            "............NNN.",
            ".........NNNNNN.",
            "......N...NN....",
            ".......NNN.N....",
            "......N...N.....",
            "..RRRRN....N....",
            ".RRRRNRR..RR....",
            ".RRRRNRR.RR.....",
            ".RRRRRR.RNNRR...",
            ".RWWRR.RRNNRRR..",
            ".RRRWR.RRRRRRR..",
            "..NRRR.RWRRRRR..",
            ".......RRRWRRR..",
            ".......NRRWRRR..",
            "........RRRRR..."
    };

    private static final String[] STRAWBERRY = {
            "................",
            "................",
            ".......W........",
            "...GGGGWGGGG....",
            "..RRGGGGGGGGRR..",
            ".RRRRGGGRRRRRR..",
            ".RWRRRRRRWRRRR..",
            ".RRRWRWRRRWRRR..",
            ".RRRWRWRRRRWRR..",
            ".RRRRRRRRRRRRR..",
            "..RWWRRWRRRRR...",
            "..RRRRRRRRRRR...",
            "...RRWRRWW.R....",
            ".....RRRRRR.....",
            "......NRNN......",
            ".......RR......."
    };

    private static final String[] ORANGE_ITEM = {
            "................",
            ".........GGG....",
            ".........GGG....",
            ".......NGGGGG...",
            ".......N.GGG....",
            "...OOONNNOOOO....",
            "..OOOOONOOOOOO...",
            ".OOOOOOOOOOOOOO..",
            ".OOOOOOOOOOOOOO..",
            ".OOOOOOOOOOOOOO..",
            ".OOOOOOOOOOOOOO..",
            ".OOOOOOOOOOOOOO..",
            "..OOOOOOOOOOOO...",
            "..OOOOOOOOOOOO...",
            "...NNOOOOOOOO....",
            "....NOOOOOON....."
    };

    private static final String[] APPLE = {
            "................",
            "........N.......",
            ".......N.NNN....",
            "..RRRN.N.RNNN...",
            ".RRRRRNRRRRRRR..",
            ".RRRRNRRRRRRRRR.",
            ".RNNRRRRRRRRWWR.",
            ".RRNRRRRRRNRWRR.",
            ".RRRRNRRRRRWWRR.",
            ".RRRRRRRRRWWRRR.",
            ".RRRRRRRRRRWWRR.",
            "..RRRRRRRWWRRR..",
            "..RRRRRRRRRRRR..",
            "...RRRRRRRRRR...",
            "....RRR.RRR.....",
            "....NRR.RRR....."
    };

    private static final String[][] ITEMS = {
            CHERRIES,
            STRAWBERRY,
            ORANGE_ITEM,
            APPLE
    };

    private double elapsedMs = 0.0;

    private final LXParameterListener panicListener = (p) -> {
        if (((BooleanParameter) p).getValueb()) {
            onPanic();
        }
    };

    public PacmanItemsPattern(LX lx) {
        super(lx);
        addParameter("ItemSize", size);
        addParameter("FloatSpeed", floatSpeed);
        addParameter("FloatAmount", floatAmount);
        addParameter("Float", enableFloat);
        addParameter("SwitchSec", switchTime);
        addParameter("PANIC", panic);
        panic.addListener(panicListener);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        elapsedMs += deltaMs;

        int itemIndex = ((int) Math.floor((elapsedMs * 0.001) / switchTime.getValuef())) % ITEMS.length;
        String[] sprite = ITEMS[itemIndex];

        float centerX = (model.xMax + model.xMin) / 2.0f;
        float centerY = (model.yMax + model.yMin) / 2.0f;
        if (enableFloat.isOn()) {
            float floatOffset = (float) Math.sin(elapsedMs * 0.001 * floatSpeed.getValuef() * 2 * Math.PI)
                    * floatAmount.getValuef()
                    * Math.max(model.xMax - model.xMin, model.yMax - model.yMin)
                    * 0.1f;
            centerY += floatOffset;
        }

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
            char pixel = sprite[spriteY].charAt(gridX);
            int color = getPixelColor(pixel);
            if (color != -1) {
                colors[point.index] = color;
            }
        }
    }

    private int getPixelColor(char pixel) {
        switch (pixel) {
            case RED:
                return ITEM_RED;
            case ORANGE:
                return ITEM_ORANGE;
            case GREEN:
                return ITEM_GREEN;
            case WHITE:
                return ITEM_WHITE;
            case BROWN:
                return ITEM_ORANGE;
            case EMPTY:
            default:
                return -1;
        }
    }

    private void onPanic() {
        size.reset();
        floatSpeed.reset();
        floatAmount.reset();
        enableFloat.reset();
        switchTime.reset();
    }

    @Override
    public void dispose() {
        panic.removeListener(panicListener);
        super.dispose();
    }
}
