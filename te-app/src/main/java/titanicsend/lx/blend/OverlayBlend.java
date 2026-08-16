package titanicsend.lx.blend;

import heronarts.lx.LX;
import heronarts.lx.blend.LXBlend;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;

/**
 * Overlay blend for sprite-style compositing.
 *
 * <p>Non-black pixels from the source buffer sit on top of the destination.
 * Black pixels in the source are treated as empty and allow the lower layer to
 * show through. Channel alpha is still respected so the fader behaves
 * normally.</p>
 */
public class OverlayBlend extends LXBlend {

  public OverlayBlend(LX lx) {
    super(lx);
    setName("Overlay");
  }

  @Override
  public void blend(int[] dst, int[] src, double alpha, int[] output, LXModel model) {
    int alphaMask = (int) (alpha * LXColor.BLEND_ALPHA_FULL);
    for (LXPoint p : model.points) {
      output[p.index] = overlay(dst[p.index], src[p.index], alphaMask);
    }
  }

  @Override
  public void blend(int[] dst, int[] src, double alpha, int[] output, int start, int num) {
    int alphaMask = (int) (alpha * LXColor.BLEND_ALPHA_FULL);
    for (int i = start; i < start + num; ++i) {
      output[i] = overlay(dst[i], src[i], alphaMask);
    }
  }

  private static int overlay(int dst, int src, int alphaMask) {
    if ((src & LXColor.RGB_MASK) == 0) {
      return dst;
    }
    return LXColor.lerp(dst, src, alphaMask);
  }
}
