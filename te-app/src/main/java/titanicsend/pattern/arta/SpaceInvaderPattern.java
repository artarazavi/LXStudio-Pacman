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
import titanicsend.util.TEColor;

@LXCategory("Arta")
public class SpaceInvaderPattern extends TEAudioPattern {
    public final CompoundParameter size =
            new CompoundParameter("SISize", 0.6f, 0.1f, 2.0f)
                    .setDescription("Size of the space invader");
    
    public final CompoundParameter floatSpeed =
            new CompoundParameter("FloatSpeed", 1.0f, 0.1f, 5.0f)
                    .setDescription("Speed of floating animation");
    
    public final CompoundParameter floatAmount =
            new CompoundParameter("FloatAmount", 0.3f, 0.0f, 1.0f)
                    .setDescription("Amount of floating movement");
    
    public final BooleanParameter enableFloat =
            new BooleanParameter("Float", true)
                    .setDescription("Enable floating animation");
    
    public final CompoundParameter twist =
            new CompoundParameter("Twist", 0.0f, 0.0f, 360.0f)
                    .setDescription("Rotate the entire space invader");
    
    public final DiscreteParameter colorChoice =
            new DiscreteParameter("Color", 0, 0, 8)
                    .setDescription("Space Invader color (0=Pink, 1=Green, 2=Blue, 3=Red, 4=Purple, 5=Orange, 6=Yellow, 7=Cyan, 8=White)")
                    .setOptions(new String[]{"Pink", "Green", "Blue", "Red", "Purple", "Orange", "Yellow", "Cyan", "White"});
    
    public final BooleanParameter colorShift =
            new BooleanParameter("ColorShift", false)
                    .setDescription("Automatically cycle through colors");
    
    public final CompoundParameter colorShiftSpeed =
            new CompoundParameter("ColorSpeed", 2.0f, 0.1f, 5.0f)
                    .setDescription("Speed of color cycling (seconds per color)");
    


    public final BooleanParameter panic =
            new BooleanParameter("PANIC", false)
                    .setDescription("Reset all parameters to defaults")
                    .setMode(BooleanParameter.Mode.MOMENTARY);

    // Animation variables
    private double animationTime = 0.0;
    
    // Space Invader shape - 12x8 pixel grid matching classic arcade sprite
    // Exact pattern provided by user
    private final boolean[][] invaderShape = {
        {false,false,false,true, true, false,true, true, false,false,false},  // Row 7: 000110110000
        {true, false,true, false,false,false,false,false,true, false,true }, // Row 6: 101000000101  
        {true, false,true, true, true, true, true, true, true, false,true }, // Row 5: 101111111010
        {true, true, true, true, true, true, true, true, true, true, true }, // Row 4: 111111111111
        {false,true, true, false,true, true, true, false,true, true, false}, // Row 3: 011011110110
        {false,false,true, true, true, true, true, true, true, false,false}, // Row 2: 001111111000
        {false,false,false,true, false,false,false,true, false,false,false}, // Row 1: 000010001000 
        {false,false,true, false,false,false,false,false,true, false,false} // Row 0: 001000001000
    };
    
    // Panic listener
    private final LXParameterListener panicListener = (p) -> {
        if (((BooleanParameter) p).getValueb()) {
            onPanic();
        }
    };
    
    // Method to get space invader color based on choice
    private int getInvaderColor() {
        switch (colorChoice.getValuei()) {
            case 0: return LXColor.hsb(330, 100, 100); // Pink
            case 1: return LXColor.hsb(120, 100, 100); // Green
            case 2: return LXColor.hsb(240, 100, 100); // Blue
            case 3: return LXColor.hsb(0, 100, 100);   // Red
            case 4: return LXColor.hsb(270, 100, 100); // Purple
            case 5: return TEColor.ORANGE;             // Orange
            case 6: return TEColor.YELLOW;             // Yellow
            case 7: return LXColor.hsb(180, 100, 100); // Cyan
            case 8: return LXColor.hsb(0, 0, 95);      // White
            default: return LXColor.hsb(330, 100, 100); // Default pink
        }
    }

    public SpaceInvaderPattern(LX lx) {
        super(lx);
        addParameter("SISize", size);
        addParameter("FloatSpeed", floatSpeed);
        addParameter("FloatAmount", floatAmount);
        addParameter("Float", enableFloat);
        addParameter("Twist", twist);
        addParameter("Color", colorChoice);
        addParameter("ColorShift", colorShift);
        addParameter("ColorSpeed", colorShiftSpeed);
        addParameter("PANIC", panic);
        
        // Add panic listener
        panic.addListener(panicListener);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        // Update animation time
        animationTime += deltaMs * floatSpeed.getValuef() * 0.001; // Convert to seconds and apply speed
        
        // Update color shift if enabled
        if (colorShift.isOn()) {
            // Cycle through colors based on speed parameter
            float colorTime = (float) (animationTime / colorShiftSpeed.getValuef());
            int colorIndex = (int) (colorTime % 9); // 9 colors total (0-8)
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
        
        // Calculate scaling factor based on model size
        float modelWidth = model.xMax - model.xMin;
        float modelHeight = model.yMax - model.yMin;
        
        // Space invader is 11 pixels wide x 8 pixels tall  
        int gridWidth = 11;
        int gridHeight = 8;
        
        float scale = Math.min(modelWidth / gridWidth, modelHeight / gridHeight) * size.getValuef();
        float pixelSize = scale;
        
        // Draw the space invader shape using the pixel grid
        for (int i = 0; i < model.points.length; i++) {
            LXVector point = new LXVector(model.points[i]);
            
            // Apply twist rotation to the point
            float relX = point.x - centerX;
            float relY = point.y - centerY;
            float rotatedX = (float) (relX * Math.cos(twistAngle) - relY * Math.sin(twistAngle));
            float rotatedY = (float) (relX * Math.sin(twistAngle) + relY * Math.cos(twistAngle));
            
            // Convert to grid coordinates
            float adjustedX = centerX + rotatedX;
            float adjustedY = centerY + rotatedY;
            
            // Check if point falls within any filled pixel of the space invader
            if (isPointInSpaceInvader(adjustedX, adjustedY, centerX, centerY, pixelSize)) {
                colors[point.index] = getInvaderColor();
            }
        }
    }
    


    /**
     * Check if a point falls within the space invader grid shape
     */
    private boolean isPointInSpaceInvader(float x, float y, float centerX, float centerY, float pixelSize) {
        // Calculate which grid cell this point falls into
        float startX = centerX - (11 * pixelSize) / 2.0f; // 11 pixels wide
        float startY = centerY - (8 * pixelSize) / 2.0f;  // 8 pixels tall
        
        // Convert to grid coordinates
        int gridX = (int) Math.floor((x - startX) / pixelSize);
        int gridY = (int) Math.floor((y - startY) / pixelSize);
        
        // Check bounds
        if (gridX < 0 || gridX >= 11 || gridY < 0 || gridY >= 8) {
            return false;
        }
        
        // Check if this pixel is filled in our shape
        return invaderShape[gridY][gridX];
    }
    
    /**
     * Called when the momentary PANIC button is pressed. Resets all parameters to defaults.
     */
    protected void onPanic() {
        size.reset();
        floatSpeed.reset();
        floatAmount.reset();
        enableFloat.reset();
        twist.reset();
        colorChoice.reset();
        colorShift.reset();
        colorShiftSpeed.reset();
    }
    
    @Override
    public void dispose() {
        // Remove the panic listener to prevent memory leaks
        panic.removeListener(panicListener);
        super.dispose();
    }
}
