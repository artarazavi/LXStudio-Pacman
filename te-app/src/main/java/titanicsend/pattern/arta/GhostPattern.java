package titanicsend.pattern.arta;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.transform.LXVector;
import titanicsend.color.TEColorType;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.util.TEColor;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@LXCategory("Arta")
public class GhostPattern extends TEAudioPattern {
    public final CompoundParameter size =
            new CompoundParameter("GSize", 1.0f, 0.1f, 1.0f)
                    .setDescription("Size of the ghost");
    
    public final LinkedColorParameter ghostColor =
            registerColor("Color", "color", TEColorType.PRIMARY, "Color of the ghost");
    
    public final CompoundParameter waveSpeed =
            new CompoundParameter("WaveSpeed", 2.0f, 0.1f, 10.0f)
                    .setDescription("Speed of the wavy bottom animation");
    
    public final BooleanParameter showEyes =
            new BooleanParameter("Eyes", true)
                    .setDescription("Show ghost's eyes");

    // Animation variables
    private double animationTime = 0.0;
    
    // Ghost pixel data loaded from CSV
    private Map<String, String> ghostPixels = new HashMap<>();
    
    // Load ghost pixel data from CSV
    private void loadGhostPixels() {
        try {
            // Load from the same directory as the Java file
            String csvPath = "src/main/java/titanicsend/pattern/arta/ghost_pixels.csv";
            BufferedReader reader = new BufferedReader(new java.io.FileReader(csvPath));
            
            String line;
            boolean firstLine = true;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false; // Skip header
                    continue;
                }
                
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());
                    String type = parts[2].trim();
                    
                    String key = x + "," + y;
                    ghostPixels.put(key, type);
                    count++;
                }
            }
            reader.close();
            System.out.println("Loaded " + count + " ghost pixels from " + csvPath);
        } catch (Exception e) {
            System.err.println("Error loading ghost pixels: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Get pixel type from loaded data
    private String getGhostPixelType(int x, int y) {
        String key = x + "," + y;
        String result = ghostPixels.getOrDefault(key, "none");
        // Debug: print eye pixels
        if (result.equals("eye_white") || result.equals("pupil")) {
            System.out.println("Eye pixel at " + x + "," + y + " = " + result);
        }
        return result;
    }

    public GhostPattern(LX lx) {
        super(lx);
        addParameter("GSize", size);
        addParameter("WaveSpeed", waveSpeed);
        addParameter("Eyes", showEyes);
        
        // Load ghost pixel data from CSV
        loadGhostPixels();
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        // Update animation time
        animationTime += deltaMs * waveSpeed.getValuef() * 0.001;
        
        // Calculate the center of the model
        float centerX = (model.xMax + model.xMin) / 2.0f;
        float centerY = (model.yMax + model.yMin) / 2.0f;
        
        // Calculate the radius based on model size and size parameter
        float maxDimension = Math.max(model.xMax - model.xMin, model.yMax - model.yMin);
        float radius = (maxDimension / 2.0f) * size.getValuef();
        
        // Clear all colors first
        for (int i = 0; i < colors.length; i++) {
            colors[i] = LXColor.BLACK;
        }
        
        // Draw the ghost body using pixel-perfect mapping
        float pixelSize = radius / 8f; // Scale factor to map 16x16 pixel grid to radius
        
        for (int i = 0; i < model.points.length; i++) {
            LXVector point = new LXVector(model.points[i]);
            
            // Convert point to pixel coordinates (flip Y-axis)
            int pixelX = (int) Math.round((point.x - centerX) / pixelSize) + 8; // Center at pixel 8
            int pixelY = (int) Math.round(-(point.y - centerY) / pixelSize) + 8; // Flip Y and center at pixel 8
            
            // Check if point maps to a ghost pixel
            String pixelType = getGhostPixelType(pixelX, pixelY);
            
            if (pixelType.equals("body")) {
                colors[point.index] = ghostColor.getColor();
            } else if (pixelType.equals("eye_white")) {
                colors[point.index] = LXColor.WHITE;
            } else if (pixelType.equals("pupil")) {
                colors[point.index] = LXColor.BLUE;
            }
        }
        
        // Eyes are now handled by pixel mapping - showEyes parameter controls visibility
        if (!showEyes.isOn()) {
            // If eyes are disabled, override eye pixels to be ghost color
            for (int i = 0; i < model.points.length; i++) {
                LXVector point = new LXVector(model.points[i]);
                int pixelX = (int) Math.round((point.x - centerX) / pixelSize) + 8;
                int pixelY = (int) Math.round(-(point.y - centerY) / pixelSize) + 8; // Flip Y
                
                String pixelType = getGhostPixelType(pixelX, pixelY);
                if (pixelType.equals("eye_white") || pixelType.equals("pupil")) {
                    colors[point.index] = ghostColor.getColor();
                }
            }
        }
    }
}
