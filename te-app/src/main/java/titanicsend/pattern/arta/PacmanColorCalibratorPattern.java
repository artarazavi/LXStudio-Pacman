package titanicsend.pattern.arta;

import heronarts.glx.ui.UI2dContainer;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import titanicsend.color.TEColorParameter;
import titanicsend.color.TEGradientSource;
import titanicsend.ui.UITEColorControl;

@LXCategory("Arta")
public class PacmanColorCalibratorPattern extends LXPattern
    implements UIDeviceControls<PacmanColorCalibratorPattern> {

  public final TEColorParameter color =
      new TEColorParameter(TEGradientSource.get(), "Color")
          .setDescription("Click to open the full LX color picker for output calibration");

  public PacmanColorCalibratorPattern(LX lx) {
    super(lx);
    this.color.colorSource.setValue(TEColorParameter.ColorSource.STATIC);
    this.color.hue.setValue(61);
    this.color.saturation.setValue(100);
    this.color.brightness.setValue(100);
    addParameter("color", this.color);
  }

  @Override
  protected void run(double deltaMs) {
    int color = this.color.calcColor();
    for (LXPoint point : model.points) {
      colors[point.index] = color;
    }
  }

  @Override
  public void buildDeviceControls(
      LXStudio.UI ui, UIDevice uiDevice, PacmanColorCalibratorPattern pattern) {
    uiDevice.setLayout(UI2dContainer.Layout.VERTICAL);
    uiDevice.setChildSpacing(6);
    uiDevice.setContentWidth(170);

    uiDevice.addChildren(
        controlLabel(ui, "Color"),
        new UITEColorControl(0, 0, this.color),
        controlLabel(ui, "Hue"),
        newDoubleBox(this.color.hue),
        controlLabel(ui, "Sat"),
        newDoubleBox(this.color.saturation),
        controlLabel(ui, "Bright"),
        newDoubleBox(this.color.brightness));
  }
}
