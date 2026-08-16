package titanicsend.pattern.arta.v2;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import titanicsend.color.TEColorParameter;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.util.TEColor;

@LXCategory("Arta")
public class PacmanV2Pattern extends GLShaderPattern {
  public final CompoundParameter edgeFeather =
      new CompoundParameter("Edge", 0.0035f, 0.0005f, 0.02f)
          .setDescription("Softness of Pacman's circle edge");

  public final CompoundParameter mouthSize =
      new CompoundParameter("MSize", 0.48f, 0.1f, 0.8f)
          .setDescription("Size of Pacman's mouth");

  public final CompoundParameter mouthAnimation =
      new CompoundParameter("MAnim", 0.62f, 0.0f, 1.0f)
          .setDescription("Mouth animation amount");

  public final CompoundParameter mouthSpeed =
      new CompoundParameter("MSpeed", 1.55f, 0.1f, 6.0f)
          .setDescription("Speed of mouth animation");

  public final BooleanParameter mouthMove =
      new BooleanParameter("MouthMove", true)
          .setDescription("Enable mouth movement");

  public final BooleanParameter showEye =
      new BooleanParameter("Eyes", true).setDescription("Show Pacman's eye");

  public final CompoundParameter eyeSize =
      new CompoundParameter("EyeSize", 0.13f, 0.05f, 0.3f)
          .setDescription("Size of Pacman's eye");

  public final CompoundParameter eyeX =
      new CompoundParameter("EyeX", 0.1f, -0.5f, 0.5f)
          .setDescription("Horizontal eye offset");

  public final CompoundParameter eyeY =
      new CompoundParameter("EyeY", 0.4f, -0.5f, 0.5f)
          .setDescription("Vertical eye offset");

  public final BooleanParameter faceRight =
      new BooleanParameter("FaceRig", true).setDescription("Face right");

  public final BooleanParameter colorCycle =
      new BooleanParameter("ColorCy", false).setDescription("Cycle TE color over time");

  public final CompoundParameter colorCycleSpeed =
      new CompoundParameter("ColorSp", 0.12f, 0.01f, 0.75f)
          .setDescription("Speed of TE color cycling");

  public PacmanV2Pattern(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    controls.setLabel(TEControlTag.SIZE, "PSize");
    controls.setRange(TEControlTag.SIZE, 0.92, 0.35, 1.15);

    controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.SPEED));
    controls.markUnused(controls.getLXControl(TEControlTag.XPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.YPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.QUANTITY));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW1));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW2));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));

    addCommonControls();

    this.controls.color.colorSource.setValue(TEColorParameter.ColorSource.STATIC);
    this.controls.color.hue.setValue(LXColor.h(TEColor.YELLOW));
    this.controls.color.saturation.setValue(100);
    this.controls.color.brightness.setValue(100);

    addParameter("Edge", this.edgeFeather);
    addParameter("MSize", this.mouthSize);
    addParameter("MAnim", this.mouthAnimation);
    addParameter("MSpeed", this.mouthSpeed);
    addParameter("MouthMove", this.mouthMove);
    addParameter("Eyes", this.showEye);
    addParameter("EyeSize", this.eyeSize);
    addParameter("EyeX", this.eyeX);
    addParameter("EyeY", this.eyeY);
    addParameter("FaceRig", this.faceRight);
    addParameter("ColorCy", this.colorCycle);
    addParameter("ColorSp", this.colorCycleSpeed);

    addShader(
        GLShader.config(lx).withFilename("pacman_v2.fs").withUniformSource(this::setUniforms));
  }

  @Override
  public void runTEAudioPattern(double deltaMs) {
    if (this.colorCycle.isOn()) {
      double delta = deltaMs * 0.001 * this.colorCycleSpeed.getValue();
      this.controls.color.offset.incrementNormalized(delta, true);
    }
    super.runTEAudioPattern(deltaMs);
  }

  private void setUniforms(GLShader shader) {
    shader.setUniform("edgeFeather", this.edgeFeather.getValuef());
    shader.setUniform("mouthSize", this.mouthSize.getValuef());
    shader.setUniform("mouthOpen", computeMouthOpen());
    shader.setUniform("showEye", this.showEye.isOn());
    shader.setUniform("eyeSize", this.eyeSize.getValuef());
    shader.setUniform("eyeOffset", this.eyeX.getValuef(), this.eyeY.getValuef());
    shader.setUniform("faceRight", this.faceRight.isOn());
  }

  private float computeMouthOpen() {
    if (!this.mouthMove.isOn()) {
      return 1f;
    }
    float wave = (float) ((Math.sin(getTime() * this.mouthSpeed.getValue() * Math.PI * 2) + 1) * 0.5);
    return (0.2f + 0.8f * wave) * this.mouthAnimation.getValuef();
  }
}
