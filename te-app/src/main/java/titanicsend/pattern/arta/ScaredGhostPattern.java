package titanicsend.pattern.arta;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.transform.LXVector;
import titanicsend.pattern.TEAudioPattern;

@LXCategory("Arta")
public class ScaredGhostPattern extends TEAudioPattern {
    public final CompoundParameter size =
            new CompoundParameter("GSize", 0.6f, 0.1f, 2.0f)
                    .setDescription("Size of the ghost");
    
    public final CompoundParameter floatSpeed =
            new CompoundParameter("FloatSpeed", 1.0f, 0.1f, 5.0f)
                    .setDescription("Speed of floating animation");
    
    public final CompoundParameter floatAmount =
            new CompoundParameter("FloatAmount", 0.3f, 0.0f, 1.0f)
                    .setDescription("Amount of floating movement");
    
    public final BooleanParameter enableFloat =
            new BooleanParameter("Float", true)
                    .setDescription("Enable floating animation");
    
    public final BooleanParameter showEyes =
            new BooleanParameter("Eyes", true)
                    .setDescription("Show ghost's eyes");
    
    public final CompoundParameter twist =
            new CompoundParameter("Twist", 0.0f, 0.0f, 360.0f)
                    .setDescription("Rotate the entire ghost");
    
    public final DiscreteParameter colorChoice =
            new DiscreteParameter("Color", 0, 0, 1)
                    .setDescription("Ghost color (0=White, 1=Dark Blue)")
                    .setOptions(new String[]{"White", "Dark Blue"});
    
    public final BooleanParameter colorSwap =
            new BooleanParameter("ColorSwap", true)
                    .setDescription("Automatically swap between white and dark blue");
    
    public final CompoundParameter colorSwapSpeed =
            new CompoundParameter("SwapSpeed", 0.8f, 0.1f, 5.0f)
                    .setDescription("Speed of color swapping (seconds per color)");
    
    public final BooleanParameter panic =
            new BooleanParameter("PANIC", false)
                    .setDescription("Reset all parameters to defaults")
                    .setMode(BooleanParameter.Mode.MOMENTARY);

    // Animation variables
    private double animationTime = 0.0;
    
    // Ghost shape coordinates (same as GhostPattern)
    private final int[] ghostPolyX = {20,52,54,85,87,118,119,150,152,220,220,284,286,352,354,385,387,418,419,451,452,486,485,452,452,420,419,386,387,319,319,186,184,120,118,85,84,52,52,20};
    private final int[] ghostPolyY = {486,486,455,452,422,422,453,455,486,487,420,422,485,486,454,453,422,421,453,456,484,486,217,217,121,119,85,83,51,50,21,20,51,52,85,88,117,118,216,219};
    
    // Scared ghost mouth coordinates from polygon tracing
    // coords="56,219,56,236,75,238,76,220,76,200,114,201,115,219,115,238,153,239,155,219,153,200,193,200,192,218,192,239,230,218,231,237,231,200,270,200,270,218,288,219,289,238,269,239"
    private final int[] mouthPolyX = {56,56,75,76,76,114,115,115,115,153,155,153,193,192,192,230,231,231,270,270,288,289,269};
    private final int[] mouthPolyY = {219,236,238,220,200,201,219,238,238,239,219,200,200,218,239,218,237,200,200,218,219,238,239};
    
    // Panic listener
    private final LXParameterListener panicListener = (p) -> {
        if (((BooleanParameter) p).getValueb()) {
            onPanic();
        }
    };
    
    // Method to get ghost color based on choice
    private int getGhostColor() {
        switch (colorChoice.getValuei()) {
            case 0: return LXColor.hsb(0, 0, 95);      // White
            case 1: return LXColor.hsb(240, 100, 80); // Dark Blue (scared)
            default: return LXColor.hsb(0, 0, 95);    // Default white
        }
    }

    public ScaredGhostPattern(LX lx) {
        super(lx);
        addParameter("GSize", size);
        addParameter("FloatSpeed", floatSpeed);
        addParameter("FloatAmount", floatAmount);
        addParameter("Float", enableFloat);
        addParameter("Eyes", showEyes);
        addParameter("Twist", twist);
        addParameter("Color", colorChoice);
        addParameter("ColorSwap", colorSwap);
        addParameter("SwapSpeed", colorSwapSpeed);
        addParameter("PANIC", panic);
        
        // Add panic listener
        panic.addListener(panicListener);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        // Update animation time
        animationTime += deltaMs * floatSpeed.getValuef() * 0.001; // Convert to seconds and apply speed
        
        // Update color swap if enabled
        if (colorSwap.isOn()) {
            // Swap between colors based on speed parameter
            float colorTime = (float) (animationTime / colorSwapSpeed.getValuef());
            int colorIndex = (int) (colorTime % 2); // 2 colors total (0-1)
            colorChoice.setValue(colorIndex);
        }
        
        // Calculate the center of the model
        float centerX = (model.xMax + model.xMin) / 2.0f;
        float centerY = (model.yMax + model.yMin) / 2.0f;
        
        // Calculate floating offset
        float floatOffset = 0.0f;
        if (enableFloat.isOn()) {
            floatOffset = (float) Math.sin(animationTime * 2 * Math.PI) * 
                         floatAmount.getValuef() * 
                         Math.max(model.xMax - model.xMin, model.yMax - model.yMin) * 0.1f;
        }
        
        // Apply floating to center
        centerY += floatOffset;
        
        // Get twist angle in radians
        float twistAngle = (float) Math.toRadians(twist.getValuef());
        
        // Clear all colors first
        for (int i = 0; i < colors.length; i++) {
            colors[i] = LXColor.BLACK;
        }
        
        // Normalize ghost coordinates to fit the model
        float[] normalizedPolyX = new float[ghostPolyX.length];
        float[] normalizedPolyY = new float[ghostPolyY.length];
        
        // Find bounds of original polygon
        int minPolyX = Integer.MAX_VALUE, maxPolyX = Integer.MIN_VALUE;
        int minPolyY = Integer.MAX_VALUE, maxPolyY = Integer.MIN_VALUE;
        
        for (int i = 0; i < ghostPolyX.length; i++) {
            minPolyX = Math.min(minPolyX, ghostPolyX[i]);
            maxPolyX = Math.max(maxPolyX, ghostPolyX[i]);
            minPolyY = Math.min(minPolyY, ghostPolyY[i]);
            maxPolyY = Math.max(maxPolyY, ghostPolyY[i]);
        }
        
        // Calculate scaling factor
        float polyWidth = maxPolyX - minPolyX;
        float polyHeight = maxPolyY - minPolyY;
        float modelWidth = model.xMax - model.xMin;
        float modelHeight = model.yMax - model.yMin;
        
        float scale = Math.min(modelWidth / polyWidth, modelHeight / polyHeight) * size.getValuef();
        
        // Normalize and scale coordinates (flip Y to fix upside-down orientation)
        for (int i = 0; i < ghostPolyX.length; i++) {
            normalizedPolyX[i] = centerX + (ghostPolyX[i] - minPolyX - polyWidth/2) * scale;
            // Flip Y coordinate by subtracting from max instead of min
            normalizedPolyY[i] = centerY - (ghostPolyY[i] - minPolyY - polyHeight/2) * scale;
        }
        
        // Draw the ghost shape
        for (int i = 0; i < model.points.length; i++) {
            LXVector point = new LXVector(model.points[i]);
            
            // Apply twist rotation to the point
            float relX = point.x - centerX;
            float relY = point.y - centerY;
            float rotatedX = (float) (relX * Math.cos(twistAngle) - relY * Math.sin(twistAngle));
            float rotatedY = (float) (relX * Math.sin(twistAngle) + relY * Math.cos(twistAngle));
            
            // Check if point is inside the ghost polygon using ray casting algorithm
            if (isPointInPolygon(centerX + rotatedX, centerY + rotatedY, normalizedPolyX, normalizedPolyY)) {
                colors[point.index] = getGhostColor();
            }
        }
        
        // Add simple square eyes if enabled
        if (showEyes.isOn()) {
            addSimpleSquareEyes(centerX, centerY, scale, twistAngle);
        }
        
        // Add the scared ghost mouth (simple square segments)
        addSimpleScaredMouth(centerX, centerY, scale, twistAngle);
    }
    
    /**
     * Ray casting algorithm to determine if a point is inside a polygon
     */
    private boolean isPointInPolygon(float x, float y, float[] polyX, float[] polyY) {
        int nvert = polyX.length;
        boolean inside = false;
        
        for (int i = 0, j = nvert - 1; i < nvert; j = i++) {
            float xi = polyX[i];
            float yi = polyY[i];
            float xj = polyX[j];
            float yj = polyY[j];
            
            // Check if ray crosses this edge
            if (((yi > y) != (yj > y)) && 
                (x < (xj - xi) * (y - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }
        }
        
        return inside;
    }
    
    /**
     * Add simple square eyes in the center (no shifting)
     */
    private void addSimpleSquareEyes(float centerX, float centerY, float scale, float twistAngle) {
        // Eye parameters - simple squares
        float eyeSize = Math.max(scale * 32.0f, 12.0f);   // Square eye size (smaller)
        float eyeOffsetX = Math.max(scale * 28.0f, 12.0f); // Distance from center horizontally (closer together)
        float eyeOffsetY = Math.max(scale * 20.0f, 10.0f); // Distance from center vertically
        
        // Eye positions - centered, no shifting
        float leftEyeX = centerX - eyeOffsetX;
        float leftEyeY = centerY + eyeOffsetY;
        float rightEyeX = centerX + eyeOffsetX;
        float rightEyeY = centerY + eyeOffsetY;
        
        // Draw simple square eyes
        drawSimpleSquareEye(leftEyeX, leftEyeY, eyeSize, centerX, centerY, twistAngle);
        drawSimpleSquareEye(rightEyeX, rightEyeY, eyeSize, centerX, centerY, twistAngle);
    }
    
    private void drawSimpleSquareEye(float eyeX, float eyeY, float eyeSize, float centerX, float centerY, float twistAngle) {
        for (int i = 0; i < model.points.length; i++) {
            LXVector point = new LXVector(model.points[i]);
            
            // Only modify pixels that are already ghost-colored (don't touch black pixels)
            if (colors[point.index] == LXColor.BLACK) {
                continue; // Skip black pixels - don't interfere with ghost shape
            }
            
            // Apply twist rotation to the point
            float relX = point.x - centerX;
            float relY = point.y - centerY;
            float rotatedX = (float) (relX * Math.cos(twistAngle) - relY * Math.sin(twistAngle));
            float rotatedY = (float) (relX * Math.sin(twistAngle) + relY * Math.cos(twistAngle));
            
            // Apply the opposite rotation to the eye position
            float eyeRelX = eyeX - centerX;
            float eyeRelY = eyeY - centerY;
            float rotatedEyeX = centerX + (float) (eyeRelX * Math.cos(-twistAngle) - eyeRelY * Math.sin(-twistAngle));
            float rotatedEyeY = centerY + (float) (eyeRelX * Math.sin(-twistAngle) + eyeRelY * Math.cos(-twistAngle));
            
            // Check if point is within the square eye area
            float deltaX = Math.abs(point.x - rotatedEyeX);
            float deltaY = Math.abs(point.y - rotatedEyeY);
            
            // Draw simple square eye (black)
            if (deltaX <= eyeSize/2 && deltaY <= eyeSize/2) {
                colors[point.index] = LXColor.BLACK;
            }
        }
    }
    
    /**
     * Add simple scared ghost mouth made of square segments (like the reference image)
     */
    private void addSimpleScaredMouth(float centerX, float centerY, float scale, float twistAngle) {
        // Mouth parameters based on reference image analysis
        // Ghost width is roughly 16 units, mouth spans about 12 units (75% of ghost width)
        float mouthSegmentSize = Math.max(scale * 16.0f, 6.0f);  // Each segment is 1 square
        float mouthY = centerY - scale * 60.0f; // Position below eyes (moved down)
        
        // Mouth pattern: continuous zigzag pattern
        // Pattern: 1 bottom, 2 top, 2 bottom, 2 top, 2 bottom, 2 top, 1 bottom (12 total segments)
        float segmentSpacing = scale * 24.0f; // Space between each segment
        float startX = centerX - scale * 132.0f; // Start position (6 segments left of center)
        
        float[] segmentX = new float[12];
        float[] segmentY = new float[12];
        
        // Generate the zigzag pattern
        for (int i = 0; i < 12; i++) {
            segmentX[i] = startX + (i * segmentSpacing);
        }
        
        // Y positions for zigzag: 1 top, 2 bottom, 2 top, 2 bottom, 2 top, 2 bottom, 1 top (flipped for scared look)
        segmentY[0] = mouthY - scale * 8.0f;   // 1 top
        segmentY[1] = mouthY + scale * 8.0f;   // 2 bottom (1st)
        segmentY[2] = mouthY + scale * 8.0f;   // 2 bottom (2nd)
        segmentY[3] = mouthY - scale * 8.0f;   // 2 top (1st)
        segmentY[4] = mouthY - scale * 8.0f;   // 2 top (2nd)
        segmentY[5] = mouthY + scale * 8.0f;   // 2 bottom (1st)
        segmentY[6] = mouthY + scale * 8.0f;   // 2 bottom (2nd)
        segmentY[7] = mouthY - scale * 8.0f;   // 2 top (1st)
        segmentY[8] = mouthY - scale * 8.0f;   // 2 top (2nd)
        segmentY[9] = mouthY + scale * 8.0f;   // 2 bottom (1st)
        segmentY[10] = mouthY + scale * 8.0f;  // 2 bottom (2nd)
        segmentY[11] = mouthY - scale * 8.0f;  // 1 top
        
        // Draw each mouth segment
        for (int seg = 0; seg < segmentX.length; seg++) {
            drawMouthSegment(segmentX[seg], segmentY[seg], mouthSegmentSize, centerX, centerY, twistAngle);
        }
    }
    
    private void drawMouthSegment(float segX, float segY, float segmentSize, float centerX, float centerY, float twistAngle) {
        for (int i = 0; i < model.points.length; i++) {
            LXVector point = new LXVector(model.points[i]);
            
            // Only modify pixels that are already ghost-colored (don't touch black pixels)
            if (colors[point.index] == LXColor.BLACK) {
                continue; // Skip black pixels - don't interfere with ghost shape
            }
            
            // Apply twist rotation to the point
            float relX = point.x - centerX;
            float relY = point.y - centerY;
            float rotatedX = (float) (relX * Math.cos(twistAngle) - relY * Math.sin(twistAngle));
            float rotatedY = (float) (relX * Math.sin(twistAngle) + relY * Math.cos(twistAngle));
            
            // Apply the opposite rotation to the segment position
            float segRelX = segX - centerX;
            float segRelY = segY - centerY;
            float rotatedSegX = centerX + (float) (segRelX * Math.cos(-twistAngle) - segRelY * Math.sin(-twistAngle));
            float rotatedSegY = centerY + (float) (segRelX * Math.sin(-twistAngle) + segRelY * Math.cos(-twistAngle));
            
            // Check if point is within the segment square
            float deltaX = Math.abs(point.x - rotatedSegX);
            float deltaY = Math.abs(point.y - rotatedSegY);
            
            // Draw square segment (black)
            if (deltaX <= segmentSize/2 && deltaY <= segmentSize/2) {
                colors[point.index] = LXColor.BLACK;
            }
        }
    }
    
    /**
     * Called when the momentary PANIC button is pressed. Resets all parameters to defaults.
     */
    protected void onPanic() {
        size.reset();
        floatSpeed.reset();
        floatAmount.reset();
        enableFloat.reset();
        showEyes.reset();
        twist.reset();
        colorChoice.reset();
        colorSwap.reset();
        colorSwapSpeed.reset();
    }
    
    @Override
    public void dispose() {
        // Remove the panic listener to prevent memory leaks
        panic.removeListener(panicListener);
        super.dispose();
    }
}
