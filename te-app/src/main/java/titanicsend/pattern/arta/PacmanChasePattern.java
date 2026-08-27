package titanicsend.pattern.arta;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.transform.LXVector;
import titanicsend.pattern.TEAudioPattern;

@LXCategory("Arta")
public class PacmanChasePattern extends TEAudioPattern {
    public final CompoundParameter size =
            new CompoundParameter("Size", 1.34f, 0.7f, 1.7f)
                    .setDescription("Overall size of the chase animation");

    public final CompoundParameter speed =
            new CompoundParameter("Speed", 0.52f, 0.2f, 1.5f)
                    .setDescription("Playback speed of the chase animation");

    private static final int GRID_WIDTH = 32;
    private static final int GRID_HEIGHT = 8;
    private static final double CYCLE_MS = 5000.0;

    private static final float SPRITE_CENTER_Y = 3.6f;
    private static final float PACMAN_RADIUS = 3.35f;
    private static final float GHOST_SPACING = 7.35f;

    private static final float PI = (float) Math.PI;
    private static final float TAU = PI * 2.0f;
    private static final float PACMAN_V2_SHADER_RADIUS = 0.48f;
    private static final float PACMAN_V2_MOUTH_SIZE = 0.48f;
    private static final float PACMAN_V2_MOUTH_ANIMATION = 0.62f;
    private static final float PACMAN_V2_MOUTH_SPEED = 1.55f;
    private static final float PACMAN_V2_EYE_SIZE = 0.13f;
    private static final float PACMAN_V2_EYE_X = 0.10f;
    private static final float PACMAN_V2_EYE_Y = 0.40f;
    private static final float PACMAN_V2_EDGE_FEATHER = 0.0035f;
    private static final float PACMAN_V2_ALPHA_THRESHOLD = 0.96f;

    private static final int PACMAN_YELLOW = LXColor.hsb(60, 100, 100);
    private static final int PELLET = LXColor.rgb(244, 206, 168);
    private static final int BLINKY_RED = LXColor.hsb(0, 100, 100);
    private static final int PINKY_PINK = LXColor.hsb(330, 100, 100);
    private static final int INKY_CYAN = LXColor.hsb(180, 100, 100);
    private static final int CLYDE_ORANGE = LXColor.hsb(15.97, 100, 100);

    private static final int[] GHOST_COLORS = {
            BLINKY_RED,
            PINKY_PINK,
            INKY_CYAN,
            CLYDE_ORANGE,
    };

    // Same traced silhouette used by GhostPattern, normalized into chase sprite space.
    // Chase sprite Y is up-positive, so source Y is mirrored to match GhostPattern's
    // model-space "flip Y to fix upside-down orientation" transform.
    private static final int[] GHOST_POLY_X = {
            20,52,54,85,87,118,119,150,152,220,220,284,286,352,354,385,387,418,419,451,
            452,486,485,452,452,420,419,386,387,319,319,186,184,120,118,85,84,52,52,20
    };
    private static final int[] GHOST_POLY_Y = {
            486,486,455,452,422,422,453,455,486,487,420,422,485,486,454,453,422,421,453,456,
            484,486,217,217,121,119,85,83,51,50,21,20,51,52,85,88,117,118,216,219
    };
    private static final float GHOST_POLY_MIN_X = 20.0f;
    private static final float GHOST_POLY_MIN_Y = 20.0f;
    private static final float GHOST_POLY_WIDTH = 466.0f;
    private static final float GHOST_POLY_HEIGHT = 467.0f;
    private static final float GHOST_RENDER_HEIGHT = 5.95f;
    private static final float GHOST_RENDER_SCALE = GHOST_RENDER_HEIGHT / GHOST_POLY_HEIGHT;
    private static final float GHOST_RENDER_WIDTH = GHOST_RENDER_SCALE * GHOST_POLY_WIDTH;
    private static final float GHOST_POLY_CENTER_X = GHOST_POLY_MIN_X + GHOST_POLY_WIDTH / 2.0f;
    private static final float GHOST_POLY_CENTER_Y = GHOST_POLY_MIN_Y + GHOST_POLY_HEIGHT / 2.0f;
    // GhostPattern uses oversized eye minimums so the face reads clearly on a
    // sparse model. The chase sprite is tiny, so we keep the same proportions
    // but also apply sprite-space floors to stop the right-looking eyes from
    // collapsing into a single squinty pixel cluster.
    private static final float GHOST_EYE_WIDTH = Math.max(GHOST_RENDER_SCALE * 70.0f, 1.45f);
    private static final float GHOST_EYE_HEIGHT = Math.max(GHOST_RENDER_SCALE * 90.0f, 1.90f);
    private static final float GHOST_PUPIL_SIZE = Math.max(GHOST_RENDER_SCALE * 32.0f, 0.72f);
    private static final float GHOST_EYE_OFFSET_X = Math.max(GHOST_RENDER_SCALE * 35.0f, 0.95f);
    private static final float GHOST_EYE_OFFSET_Y = Math.max(GHOST_RENDER_SCALE * 20.0f, 0.72f);
    private static final float GHOST_EYE_SHIFT_FACTOR = 0.22f;

    private static final float[] PELLET_X = {
            2.0f, 5.8f, 9.6f, 13.4f, 17.2f, 21.0f, 24.8f, 28.6f,
    };

    private double animationTimeMs = 0.0;

    public PacmanChasePattern(LX lx) {
        super(lx);
        addParameter("Size", size);
        addParameter("Speed", speed);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        animationTimeMs += deltaMs * speed.getValuef();

        float centerX = (model.xMax + model.xMin) / 2.0f;
        float centerY = (model.yMax + model.yMin) / 2.0f;
        float modelWidth = model.xMax - model.xMin;
        float modelHeight = model.yMax - model.yMin;
        float pixelSize = Math.min(modelWidth / GRID_WIDTH, modelHeight / GRID_HEIGHT) * size.getValuef();
        float spriteWidth = GRID_WIDTH * pixelSize;
        float spriteHeight = GRID_HEIGHT * pixelSize;
        float startX = centerX - spriteWidth / 2.0f;
        float startY = centerY - spriteHeight / 2.0f;

        double phase = (animationTimeMs % CYCLE_MS) / CYCLE_MS;

        for (int i = 0; i < colors.length; i++) {
            colors[i] = LXColor.BLACK;
        }

        for (int i = 0; i < model.points.length; i++) {
            LXVector point = new LXVector(model.points[i]);
            float spriteX = (point.x - startX) / pixelSize;
            float spriteY = GRID_HEIGHT - ((point.y - startY) / pixelSize);

            if (spriteX < 0 || spriteX >= GRID_WIDTH || spriteY < 0 || spriteY >= GRID_HEIGHT) {
                continue;
            }

            int color = getCellColor(spriteX, spriteY, phase);
            if (color != LXColor.BLACK) {
                colors[point.index] = color;
            }
        }
    }

    private int getCellColor(float x, float y, double phase) {
        int color = drawPellet(x, y, phase);

        int pacmanColor = drawPacman(x, y, phase);
        if (pacmanColor != LXColor.BLACK) {
            color = pacmanColor;
        }

        int ghostColor = drawGhosts(x, y, phase);
        if (ghostColor != LXColor.BLACK) {
            color = ghostColor;
        }

        return color;
    }

    private int drawPellet(float x, float y, double phase) {
        if (Math.abs(y - 3.5f) > 0.72f) {
            return LXColor.BLACK;
        }

        float pacmanX = getPacmanCenterX(phase);
        boolean pacmanIsEating = phase < 0.62;
        for (float pelletX : PELLET_X) {
            if (pacmanIsEating && pacmanX > pelletX - 0.55f) {
                continue;
            }
            if (Math.abs(x - pelletX) <= 0.48f) {
                return PELLET;
            }
        }

        return LXColor.BLACK;
    }

    private int drawPacman(float x, float y, double phase) {
        if (phase >= 0.66) {
            return LXColor.BLACK;
        }

        float cx = getPacmanCenterX(phase);
        float pX = (x - cx) / PACMAN_RADIUS * PACMAN_V2_SHADER_RADIUS;
        float pY = -(y - SPRITE_CENTER_Y) / PACMAN_RADIUS * PACMAN_V2_SHADER_RADIUS;
        return getPacmanV2Mask(pX, pY, getPacmanV2MouthOpen()) >= PACMAN_V2_ALPHA_THRESHOLD
                ? PACMAN_YELLOW
                : LXColor.BLACK;
    }

    private float getPacmanCenterX(double phase) {
        double t = Math.min(phase / 0.66, 1.0);
        return lerp(-PACMAN_RADIUS - 0.8f, GRID_WIDTH + PACMAN_RADIUS + 0.8f, smoothstep(t));
    }

    private float getPacmanV2MouthOpen() {
        double timeSeconds = animationTimeMs / 1000.0;
        float wave = (float) ((Math.sin(timeSeconds * PACMAN_V2_MOUTH_SPEED * Math.PI * 2.0) + 1.0) * 0.5);
        return (0.2f + 0.8f * wave) * PACMAN_V2_MOUTH_ANIMATION;
    }

    private float getPacmanV2Mask(float pX, float pY, float mouthOpen) {
        float radius = PACMAN_V2_SHADER_RADIUS;
        float feather = Math.max(PACMAN_V2_EDGE_FEATHER * 0.35f, 0.0006f);
        float cutoutFeather = Math.max(PACMAN_V2_EDGE_FEATHER * 0.08f, 0.00025f);

        float bodyMask = circleMask(pX, pY, radius, feather);

        float mouthHalfAngle = PACMAN_V2_MOUTH_SIZE * PI * mouthOpen;
        float angle = (float) Math.atan2(pY, pX);
        float mouthAngle = angleDiff(angle, 0.0f);
        float mouthAngularFeather = 0.012f + cutoutFeather * 1.2f;
        float mouthOvercut = Math.max(feather * 10.0f, 0.018f);
        float mouthRadialMask = circleMask(pX, pY, radius + mouthOvercut, cutoutFeather);
        float mouthMask = smoothstep(
                mouthHalfAngle + mouthAngularFeather,
                mouthHalfAngle - mouthAngularFeather,
                mouthAngle) * mouthRadialMask;

        float finalMask = bodyMask * (1.0f - mouthMask);

        float eyeCenterX = -PACMAN_V2_EYE_X * radius;
        float eyeCenterY = PACMAN_V2_EYE_Y * radius;
        float eyeRadius = PACMAN_V2_EYE_SIZE * radius;
        float eyeMask = circleMask(pX - eyeCenterX, pY - eyeCenterY, eyeRadius, cutoutFeather);
        finalMask *= (1.0f - eyeMask);

        return clamp01(finalMask);
    }

    private float circleMask(float x, float y, float radius, float feather) {
        float distance = (float) Math.sqrt(x * x + y * y);
        return smoothstep(radius + feather, radius - feather, distance);
    }

    private float angleDiff(float a, float b) {
        float d = Math.abs(a - b);
        return Math.min(d, TAU - d);
    }

    private int drawGhosts(float x, float y, double phase) {
        if (phase < 0.39) {
            return LXColor.BLACK;
        }

        double t = (phase - 0.39) / 0.61;
        float groupStartX = lerp(-31.0f, GRID_WIDTH + 5.5f, smoothstep(t));

        for (int i = GHOST_COLORS.length - 1; i >= 0; i--) {
            float ghostCenterX = groupStartX + i * GHOST_SPACING;
            int ghostColor = drawGhost(x, y, ghostCenterX, SPRITE_CENTER_Y + getGhostBounce(i), GHOST_COLORS[i]);
            if (ghostColor != LXColor.BLACK) {
                return ghostColor;
            }
        }

        return LXColor.BLACK;
    }

    private float getGhostBounce(int index) {
        double bounce = Math.sin(animationTimeMs * 0.0052 + index * 0.7);
        return (float) (bounce * 0.22);
    }

    private int drawGhost(float x, float y, float cx, float cy, int bodyColor) {
        float dx = x - cx;
        float dy = y - cy;

        if (!isInsideGhostPolygon(dx, dy)) {
            return LXColor.BLACK;
        }

        int eyeColor = drawGhostEyes(dx, dy);
        if (eyeColor != LXColor.BLACK) {
            return eyeColor;
        }

        return bodyColor;
    }

    private boolean isInsideGhostPolygon(float dx, float dy) {
        float ghostX = GHOST_POLY_CENTER_X + dx / GHOST_RENDER_SCALE;
        float ghostY = GHOST_POLY_CENTER_Y + dy / GHOST_RENDER_SCALE;
        boolean inside = false;

        for (int i = 0, j = GHOST_POLY_X.length - 1; i < GHOST_POLY_X.length; j = i++) {
            float xi = GHOST_POLY_X[i];
            float yi = GHOST_POLY_Y[i];
            float xj = GHOST_POLY_X[j];
            float yj = GHOST_POLY_Y[j];

            if (((yi > ghostY) != (yj > ghostY))
                    && (ghostX < (xj - xi) * (ghostY - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }
        }

        return inside;
    }

    private int drawGhostEyes(float dx, float dy) {
        float ghostMaxX = GHOST_RENDER_WIDTH / 2.0f;
        float ghostMinX = -GHOST_RENDER_WIDTH / 2.0f;
        float currentEyeShift = GHOST_EYE_SHIFT_FACTOR;
        float maxShiftLeft = -GHOST_EYE_OFFSET_X - (ghostMinX + GHOST_EYE_WIDTH / 2.0f);
        float maxShiftRight = (ghostMaxX - GHOST_EYE_WIDTH / 2.0f) - GHOST_EYE_OFFSET_X;
        float constrainedShift = currentEyeShift * Math.min(maxShiftLeft, maxShiftRight);

        int leftEye = drawGhostEye(dx, dy, -GHOST_EYE_OFFSET_X + constrainedShift, -GHOST_EYE_OFFSET_Y,
                GHOST_EYE_WIDTH, GHOST_EYE_HEIGHT, GHOST_PUPIL_SIZE);
        if (leftEye != LXColor.BLACK) {
            return leftEye;
        }

        return drawGhostEye(dx, dy, GHOST_EYE_OFFSET_X + constrainedShift, -GHOST_EYE_OFFSET_Y,
                GHOST_EYE_WIDTH, GHOST_EYE_HEIGHT, GHOST_PUPIL_SIZE);
    }

    private int drawGhostEye(
            float dx,
            float dy,
            float eyeCenterX,
            float eyeCenterY,
            float eyeWidth,
            float eyeHeight,
            float pupilSize) {
        float eyeDx = dx - eyeCenterX;
        float eyeDy = dy - eyeCenterY;

        float bottomRectWidth = eyeWidth / 1.5f;
        float bottomRectHeight = eyeHeight;
        float topRectWidth = eyeWidth;
        float topRectHeight = eyeHeight / 1.5f;

        boolean eyeWhite = (Math.abs(eyeDx) <= bottomRectWidth / 2.0f
                && Math.abs(eyeDy) <= bottomRectHeight / 2.0f)
                || (Math.abs(eyeDx) <= topRectWidth / 2.0f
                && Math.abs(eyeDy) <= topRectHeight / 2.0f);
        if (!eyeWhite) {
            return LXColor.BLACK;
        }

        float pupilX = eyeCenterX + topRectWidth / 4.0f;
        boolean pupil = Math.abs(dx - pupilX) <= pupilSize / 2.0f
                && Math.abs(dy - eyeCenterY) <= pupilSize / 2.0f;
        return pupil ? LXColor.BLUE : LXColor.WHITE;
    }

    private float lerp(double from, double to, double amount) {
        return (float) (from + (to - from) * amount);
    }

    private double smoothstep(double x) {
        double clamped = Math.max(0.0, Math.min(1.0, x));
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }

    private float smoothstep(float edge0, float edge1, float x) {
        float t = clamp01((x - edge0) / (edge1 - edge0));
        return t * t * (3.0f - 2.0f * t);
    }

    private float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
