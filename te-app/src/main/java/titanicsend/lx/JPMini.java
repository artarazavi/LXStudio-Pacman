package titanicsend.lx;

import heronarts.lx.LX;
import heronarts.lx.midi.LXMidiInput;
import heronarts.lx.midi.LXMidiOutput;
import heronarts.lx.midi.MidiAftertouch;
import heronarts.lx.midi.MidiControlChange;
import heronarts.lx.midi.MidiNote;
import heronarts.lx.midi.MidiNoteOn;
import heronarts.lx.midi.surface.LXMidiSurface;
import heronarts.lx.mixer.LXAbstractChannel;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.pattern.LXPattern;
import titanicsend.midi.MidiNames;
import titanicsend.util.TE;

/**
 * JP-MINI Bluetooth MIDI surface.
 *
 * <p>The 4x4 pad grid selects patterns on the Pacman channel. Pressing a pad solos the Pacman
 * channel by raising its fader and pulling the other channel faders down.
 */
@LXMidiSurface.Name("JP-Mini Pacman")
@LXMidiSurface.DeviceName(MidiNames.JPMINI)
public class JPMini extends LXMidiSurface {

  private static final String PACMAN_CHANNEL_LABEL = "Pacman";

  public JPMini(LX lx, LXMidiInput input, LXMidiOutput output) {
    super(lx, input, output);
    TE.log("JP-Mini surface initialized");
  }

  @Override
  protected void onEnable(boolean on) {
    TE.log("JP-Mini surface " + (on ? "enabled" : "disabled"));
  }

  @Override
  protected void onReconnect() {
    TE.log("JP-Mini surface reconnected");
  }

  @Override
  public void noteOnReceived(MidiNoteOn note) {
    int pitch = note.getPitch();
    if (pitch >= 1 && pitch <= 16 && note.getVelocity() > 0) {
      selectPacmanPattern(pitch - 1);
      return;
    }

    TE.log(
        "JP-Mini note on channel="
            + note.getChannel()
            + " pitch="
            + note.getPitch()
            + " velocity="
            + note.getVelocity());
  }

  private void selectPacmanPattern(int patternIndex) {
    LXChannel pacmanChannel = findPacmanChannel();
    if (pacmanChannel == null) {
      TE.log("JP-Mini could not find channel named " + PACMAN_CHANNEL_LABEL);
      return;
    }

    soloPacmanChannel(pacmanChannel);

    if (patternIndex >= pacmanChannel.patterns.size()) {
      TE.log(
          "JP-Mini pad "
              + (patternIndex + 1)
              + " has no Pacman pattern; channel only has "
              + pacmanChannel.patterns.size());
      return;
    }

    LXPattern pattern = pacmanChannel.patterns.get(patternIndex);
    pacmanChannel.goPatternIndex(patternIndex);
    TE.log("JP-Mini selected Pacman pattern " + (patternIndex + 1) + ": " + pattern.getLabel());
  }

  private LXChannel findPacmanChannel() {
    for (LXAbstractChannel channel : this.lx.engine.mixer.channels) {
      if (channel instanceof LXChannel && PACMAN_CHANNEL_LABEL.equals(channel.getLabel())) {
        return (LXChannel) channel;
      }
    }
    return null;
  }

  private void soloPacmanChannel(LXChannel pacmanChannel) {
    for (LXAbstractChannel channel : this.lx.engine.mixer.channels) {
      channel.fader.setValue(channel == pacmanChannel ? 1 : 0);
    }
  }

  @Override
  public void noteOffReceived(MidiNote note) {
    TE.log(
        "JP-Mini note off channel="
            + note.getChannel()
            + " pitch="
            + note.getPitch()
            + " velocity="
            + note.getVelocity());
  }

  @Override
  public void controlChangeReceived(MidiControlChange cc) {
    TE.log(
        "JP-Mini CC channel="
            + cc.getChannel()
            + " cc="
            + cc.getCC()
            + " value="
            + cc.getValue());
  }

  @Override
  public void aftertouchReceived(MidiAftertouch aftertouch) {
    TE.log(
        "JP-Mini aftertouch channel="
            + aftertouch.getChannel()
            + " pressure="
            + aftertouch.getAftertouch());
  }
}
