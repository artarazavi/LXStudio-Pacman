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
public class EyePattern extends TEAudioPattern {
    public final CompoundParameter size =
            new CompoundParameter("EyeSize", 0.8f, 0.2f, 2.0f)
                    .setDescription("Size of the eye");
    
    public final DiscreteParameter eyeColor =
            new DiscreteParameter("EyeColor", 0, 0, 6)
                    .setDescription("Eye color (0=Blue, 1=Green, 2=Red, 3=Purple, 4=Orange, 5=Pink, 6=Yellow)")
                    .setOptions(new String[]{"Blue", "Green", "Red", "Purple", "Orange", "Pink", "Yellow"});
    
    public final BooleanParameter colorSwap =
            new BooleanParameter("ColorSwap", false)
                    .setDescription("Automatically cycle through eye colors");
    
    public final CompoundParameter colorSwapSpeed =
            new CompoundParameter("SwapSpeed", 2.0f, 0.1f, 5.0f)
                    .setDescription("Speed of color swapping (seconds per color)");
    
    public final BooleanParameter blinkAnimation =
            new BooleanParameter("BlinkAnim", true)
                    .setDescription("Enable blinking animation");
    
    public final CompoundParameter blinkSpeed =
            new CompoundParameter("BlinkSpeed", 2.0f, 0.5f, 8.0f)
                    .setDescription("Speed of blinking (seconds between blinks)");
    
    public final CompoundParameter pupilSize =
            new CompoundParameter("PupilSize", 0.47f, 0.1f, 0.8f)
                    .setDescription("Size of the pupil relative to eye");
    
    public final CompoundParameter pupilX =
            new CompoundParameter("PupilX", 0.13f, -1.0f, 1.0f)
                    .setDescription("Pupil X position offset");
    
    public final CompoundParameter pupilY =
            new CompoundParameter("PupilY", -0.15f, -1.0f, 1.0f)
                    .setDescription("Pupil Y position offset");
    
    // Sparkle 1 (Large) controls
    public final CompoundParameter sparkle1X =
            new CompoundParameter("S1X", -0.35f, -1.0f, 1.0f)
                    .setDescription("Sparkle 1 X position");
    
    public final CompoundParameter sparkle1Y =
            new CompoundParameter("S1Y", 0.33f, -1.0f, 1.0f)
                    .setDescription("Sparkle 1 Y position");
    
    public final CompoundParameter sparkle1Size =
            new CompoundParameter("S1Size", 0.25f, 0.05f, 0.5f)
                    .setDescription("Sparkle 1 size");
    
    // Sparkle 2 (Medium) controls  
    public final CompoundParameter sparkle2X =
            new CompoundParameter("S2X", 0.33f, -1.0f, 1.0f)
                    .setDescription("Sparkle 2 X position");
    
    public final CompoundParameter sparkle2Y =
            new CompoundParameter("S2Y", -0.62f, -1.0f, 1.0f)
                    .setDescription("Sparkle 2 Y position");
    
    public final CompoundParameter sparkle2Size =
            new CompoundParameter("S2Size", 0.17f, 0.05f, 0.5f)
                    .setDescription("Sparkle 2 size");
    
    public final CompoundParameter twist =
            new CompoundParameter("Twist", 0.0f, 0.0f, 360.0f)
                    .setDescription("Rotate the entire eye");
    
    public final BooleanParameter panic =
            new BooleanParameter("PANIC", false)
                    .setDescription("Reset all parameters to defaults")
                    .setMode(BooleanParameter.Mode.MOMENTARY);

    // Animation variables
    private double animationTime = 0.0;
    private double lastBlinkTime = 0.0;
    private boolean isBlinking = false;
    private double blinkProgress = 0.0;
    
    // Panic listener
    private final LXParameterListener panicListener = (p) -> {
        if (((BooleanParameter) p).getValueb()) {
            onPanic();
        }
    };
    
    // Method to get eye color based on choice
    private int getEyeColor() {
        switch (eyeColor.getValuei()) {
            case 0: return LXColor.hsb(200, 80, 90);   // Blue
            case 1: return LXColor.hsb(120, 80, 90);   // Green
            case 2: return LXColor.hsb(0, 80, 90);     // Red
            case 3: return LXColor.hsb(270, 80, 90);   // Purple
            case 4: return TEColor.ORANGE;             // Orange
            case 5: return LXColor.hsb(330, 80, 90);   // Pink
            case 6: return TEColor.YELLOW;             // Yellow
            default: return LXColor.hsb(200, 80, 90);  // Default blue
        }
    }

    public EyePattern(LX lx) {
        super(lx);
        addParameter("EyeSize", size);
        addParameter("EyeColor", eyeColor);
        addParameter("ColorSwap", colorSwap);
        addParameter("SwapSpeed", colorSwapSpeed);
        addParameter("BlinkAnim", blinkAnimation);
        addParameter("BlinkSpeed", blinkSpeed);
        addParameter("PupilSize", pupilSize);
        addParameter("PupilX", pupilX);
        addParameter("PupilY", pupilY);
        addParameter("S1X", sparkle1X);
        addParameter("S1Y", sparkle1Y);
        addParameter("S1Size", sparkle1Size);
        addParameter("S2X", sparkle2X);
        addParameter("S2Y", sparkle2Y);
        addParameter("S2Size", sparkle2Size);
        addParameter("Twist", twist);
        addParameter("PANIC", panic);
        
        // Add panic listener
        panic.addListener(panicListener);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        // Update animation time
        animationTime += deltaMs * 0.001; // Convert to seconds
        
        // Handle color swapping
        if (colorSwap.isOn()) {
            // Cycle through all available colors (0-6)
            float colorTime = (float) (animationTime / colorSwapSpeed.getValuef());
            int colorIndex = (int) (colorTime % 7); // 7 colors total (0-6)
            eyeColor.setValue(colorIndex);
        }
        
        // Handle blinking animation
        if (blinkAnimation.isOn()) {
            double blinkInterval = blinkSpeed.getValuef();
            
            // Check if it's time for a new blink
            if (animationTime - lastBlinkTime >= blinkInterval) {
                isBlinking = true;
                lastBlinkTime = animationTime;
                blinkProgress = 0.0;
            }
            
            // Update blink progress
            if (isBlinking) {
                blinkProgress += deltaMs * 0.003; // Blink duration ~333ms (slower)
                if (blinkProgress >= 2.0) { // Full cycle: 0-1 = close, 1-2 = open
                    isBlinking = false;
                    blinkProgress = 0.0;
                }
            }
        } else {
            isBlinking = false;
            blinkProgress = 0.0;
        }
        
        // Calculate the center of the model
        float centerX = (model.xMax + model.xMin) / 2.0f;
        float centerY = (model.yMax + model.yMin) / 2.0f;
        
        // Calculate the radius based on model size and size parameter
        float maxDimension = Math.max(model.xMax - model.xMin, model.yMax - model.yMin);
        float eyeRadius = (maxDimension / 2.0f) * size.getValuef();
        
        // Get twist angle in radians
        float twistAngle = (float) Math.toRadians(twist.getValuef());
        
        // Clear all colors first
        for (int i = 0; i < colors.length; i++) {
            colors[i] = LXColor.BLACK;
        }
        
        // Draw the main eye circle
        drawEyeCircle(centerX, centerY, eyeRadius, twistAngle);
        
        // Draw the black pupil (oval shape, taller in Y direction)
        drawPupil(centerX, centerY, eyeRadius, twistAngle);
        
        // Draw the sparkles
        drawSparkles(centerX, centerY, eyeRadius, twistAngle);
        
        // Draw blink overlay if blinking
        if (isBlinking) {
            // Calculate proper blink animation: 0-1 = closing, 1-2 = opening
            float blinkAmount;
            if (blinkProgress <= 1.0) {
                // Closing phase (0 to 1)
                blinkAmount = (float) blinkProgress;
            } else {
                // Opening phase (1 to 0)
                blinkAmount = 2.0f - (float) blinkProgress;
            }
            drawBlinkOverlay(centerX, centerY, eyeRadius, twistAngle, blinkAmount);
        }
    }
    
    private void drawEyeCircle(float centerX, float centerY, float radius, float twistAngle) {
        int eyeColorValue = getEyeColor();
        
        for (int i = 0; i < model.points.length; i++) {
            LXVector point = new LXVector(model.points[i]);
            
            // Apply twist rotation to the point
            float relX = point.x - centerX;
            float relY = point.y - centerY;
            float rotatedX = (float) (relX * Math.cos(twistAngle) - relY * Math.sin(twistAngle));
            float rotatedY = (float) (relX * Math.sin(twistAngle) + relY * Math.cos(twistAngle));
            
            // Calculate distance from center
            float distance = (float) Math.sqrt(rotatedX * rotatedX + rotatedY * rotatedY);
            
            // If point is within the eye radius, color it
            if (distance <= radius) {
                colors[point.index] = eyeColorValue;
            }
        }
    }
    
    private void drawPupil(float centerX, float centerY, float eyeRadius, float twistAngle) {
        // Calculate pupil dimensions - oval shape (taller in Y direction)
        float basePupilSize = eyeRadius * pupilSize.getValuef();
        float pupilRadiusX = basePupilSize;      // Horizontal radius
        float pupilRadiusY = basePupilSize * 1.3f; // Vertical radius (30% taller)
        
        // Use parameter-controlled pupil position
        float pupilOffsetX = basePupilSize * pupilX.getValuef();
        float pupilOffsetY = basePupilSize * pupilY.getValuef();
        
        for (int i = 0; i < model.points.length; i++) {
            LXVector point = new LXVector(model.points[i]);
            
            // Apply twist rotation to the point
            float relX = point.x - centerX;
            float relY = point.y - centerY;
            float rotatedX = (float) (relX * Math.cos(twistAngle) - relY * Math.sin(twistAngle));
            float rotatedY = (float) (relX * Math.sin(twistAngle) + relY * Math.cos(twistAngle));
            
            // Apply offset for pupil position
            float pupilX = rotatedX - pupilOffsetX;
            float pupilY = rotatedY - pupilOffsetY;
            
            // Calculate elliptical distance (oval shape)
            float normalizedX = pupilX / pupilRadiusX;
            float normalizedY = pupilY / pupilRadiusY;
            float ellipticalDistance = (float) Math.sqrt(normalizedX * normalizedX + normalizedY * normalizedY);
            
            // If point is within the oval pupil and within the main eye, make it black
            if (ellipticalDistance <= 1.0f && colors[point.index] != LXColor.BLACK) {
                colors[point.index] = LXColor.BLACK;
            }
        }
    }
    
    private void drawSparkles(float centerX, float centerY, float eyeRadius, float twistAngle) {
        // Draw 2 sparkles using parameter controls (sparkle layer draws on top of pupil layer)
        
        // Sparkle 1 (Large)
        float sparkle1XPos = eyeRadius * sparkle1X.getValuef();
        float sparkle1YPos = eyeRadius * sparkle1Y.getValuef();
        float sparkle1SizeValue = eyeRadius * sparkle1Size.getValuef();
        drawSparkle(centerX, centerY, sparkle1XPos, sparkle1YPos, sparkle1SizeValue, twistAngle);
        
        // Sparkle 2 (Medium)
        float sparkle2XPos = eyeRadius * sparkle2X.getValuef();
        float sparkle2YPos = eyeRadius * sparkle2Y.getValuef();
        float sparkle2SizeValue = eyeRadius * sparkle2Size.getValuef();
        drawSparkle(centerX, centerY, sparkle2XPos, sparkle2YPos, sparkle2SizeValue, twistAngle);
    }
    
    private void drawSparkle(float centerX, float centerY, float offsetX, float offsetY, float sparkleSize, float twistAngle) {
        for (int i = 0; i < model.points.length; i++) {
            LXVector point = new LXVector(model.points[i]);
            
            // Apply twist rotation to the point
            float relX = point.x - centerX;
            float relY = point.y - centerY;
            float rotatedX = (float) (relX * Math.cos(twistAngle) - relY * Math.sin(twistAngle));
            float rotatedY = (float) (relX * Math.sin(twistAngle) + relY * Math.cos(twistAngle));
            
            // Calculate distance from sparkle position
            float sparkleX = rotatedX - offsetX;
            float sparkleY = rotatedY - offsetY;
            float distance = (float) Math.sqrt(sparkleX * sparkleX + sparkleY * sparkleY);
            
            // If point is within sparkle size and within the main eye, make it white (can draw over pupil)
            float eyeDistance = (float) Math.sqrt(rotatedX * rotatedX + rotatedY * rotatedY);
            float eyeRadius = (model.xMax - model.xMin) / 2.0f * size.getValuef();
            
            if (distance <= sparkleSize && eyeDistance <= eyeRadius) {
                colors[point.index] = LXColor.WHITE;
            }
        }
    }
    
    private void drawBlinkOverlay(float centerX, float centerY, float eyeRadius, float twistAngle, float blinkProgress) {
        // Create a blink effect that covers from top to bottom
        float blinkHeight = eyeRadius * 2.0f * blinkProgress;
        float blinkTop = centerY + eyeRadius - blinkHeight;
        
        for (int i = 0; i < model.points.length; i++) {
            LXVector point = new LXVector(model.points[i]);
            
            // Apply twist rotation to the point
            float relX = point.x - centerX;
            float relY = point.y - centerY;
            float rotatedX = (float) (relX * Math.cos(twistAngle) - relY * Math.sin(twistAngle));
            float rotatedY = (float) (relX * Math.sin(twistAngle) + relY * Math.cos(twistAngle));
            
            // Check if point is within the eye and above the blink line
            float distance = (float) Math.sqrt(rotatedX * rotatedX + rotatedY * rotatedY);
            float actualY = centerY + rotatedY;
            
            if (distance <= eyeRadius && actualY >= blinkTop) {
                colors[point.index] = LXColor.BLACK; // Eyelid color (black)
            }
        }
    }
    
    /**
     * Called when the momentary PANIC button is pressed. Resets all parameters to defaults.
     */
    protected void onPanic() {
        size.reset();
        eyeColor.reset();
        colorSwap.reset();
        colorSwapSpeed.reset();
        blinkAnimation.reset();
        blinkSpeed.reset();
        pupilSize.reset();
        pupilX.reset();
        pupilY.reset();
        sparkle1X.reset();
        sparkle1Y.reset();
        sparkle1Size.reset();
        sparkle2X.reset();
        sparkle2Y.reset();
        sparkle2Size.reset();
        twist.reset();
    }
    
    @Override
    public void dispose() {
        // Remove the panic listener to prevent memory leaks
        panic.removeListener(panicListener);
        super.dispose();
    }
}
