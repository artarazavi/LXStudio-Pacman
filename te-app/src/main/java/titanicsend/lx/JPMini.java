package titanicsend.lx;

import heronarts.lx.LX;
import heronarts.lx.midi.LXMidiInput;
import heronarts.lx.midi.LXMidiOutput;
import heronarts.lx.midi.LXSysexMessage;
import heronarts.lx.midi.MidiAftertouch;
import heronarts.lx.midi.MidiControlChange;
import heronarts.lx.midi.MidiNote;
import heronarts.lx.midi.MidiNoteOn;
import heronarts.lx.midi.MidiPitchBend;
import heronarts.lx.midi.MidiProgramChange;
import heronarts.lx.midi.surface.LXMidiSurface;
import heronarts.lx.mixer.LXAbstractChannel;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.mixer.LXPatternEngine.AutoCycleMode;
import heronarts.lx.pattern.LXPattern;
import titanicsend.midi.MidiNames;
import titanicsend.util.TE;

/**
 * JP-MINI Bluetooth MIDI surface.
 *
 * <p>The 4x4 pad grid selects patterns on the Pacman channel. Pressing a pad raises the Pacman
 * channel fader without muting other channels, so Pacman patterns can be layered.
 */
@LXMidiSurface.Name("JP-Mini Pacman")
@LXMidiSurface.DeviceName(MidiNames.JPMINI)
public class JPMini extends LXMidiSurface {

  private static final String PACMAN_CHANNEL_LABEL = "Pacman";
  private static final int TOP_BUTTON_CHANNEL = 0;
  private static final int AUTOPLAY_ON_CC = 22;
  private static final int AUTOPLAY_OFF_CC = 23;
  private static final int SOLO_PACMAN_CC = 24;
  private static final int TOP_BUTTON_TRIGGER_VALUE = 127;
  private static final double AUTOPLAY_INTERVAL_SECONDS = 30;

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

    pacmanChannel.fader.setValue(1);

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

    if (cc.getChannel() != TOP_BUTTON_CHANNEL || cc.getValue() != TOP_BUTTON_TRIGGER_VALUE) {
      return;
    }

    switch (cc.getCC()) {
      case AUTOPLAY_ON_CC:
        enablePacmanAutoplay();
        break;
      case AUTOPLAY_OFF_CC:
        disablePacmanAutoplay();
        break;
      case SOLO_PACMAN_CC:
        soloPacmanChannel();
        break;
      default:
        break;
    }
  }

  private void enablePacmanAutoplay() {
    LXChannel pacmanChannel = findPacmanChannel();
    if (pacmanChannel == null) {
      TE.log("JP-Mini could not enable autoplay; no channel named " + PACMAN_CHANNEL_LABEL);
      return;
    }

    pacmanChannel.fader.setValue(1);
    pacmanChannel.patternEngine.autoCycleMode.setValue(AutoCycleMode.NEXT);
    pacmanChannel.enableAutoCycle(AUTOPLAY_INTERVAL_SECONDS);
    TE.log("JP-Mini enabled Pacman autoplay at " + AUTOPLAY_INTERVAL_SECONDS + "s");
  }

  private void disablePacmanAutoplay() {
    LXChannel pacmanChannel = findPacmanChannel();
    if (pacmanChannel == null) {
      TE.log("JP-Mini could not disable autoplay; no channel named " + PACMAN_CHANNEL_LABEL);
      return;
    }

    pacmanChannel.disableAutoCycle();
    TE.log("JP-Mini disabled Pacman autoplay");
  }

  private void soloPacmanChannel() {
    LXChannel pacmanChannel = findPacmanChannel();
    if (pacmanChannel == null) {
      TE.log("JP-Mini could not solo; no channel named " + PACMAN_CHANNEL_LABEL);
      return;
    }

    for (LXAbstractChannel channel : this.lx.engine.mixer.channels) {
      channel.fader.setValue(channel == pacmanChannel ? 1 : 0);
    }
    TE.log("JP-Mini soloed Pacman channel");
  }

  @Override
  public void programChangeReceived(MidiProgramChange pc) {
    TE.log(
        "JP-Mini program change channel="
            + pc.getChannel()
            + " program="
            + pc.getProgram());
  }

  @Override
  public void pitchBendReceived(MidiPitchBend pitchBend) {
    TE.log(
        "JP-Mini pitch bend channel="
            + pitchBend.getChannel()
            + " bend="
            + pitchBend.getPitchBend()
            + " normalized="
            + pitchBend.getNormalized());
  }

  @Override
  public void aftertouchReceived(MidiAftertouch aftertouch) {
    TE.log(
        "JP-Mini aftertouch channel="
            + aftertouch.getChannel()
            + " pressure="
            + aftertouch.getAftertouch());
  }

  @Override
  public void midiPanicReceived() {
    TE.log("JP-Mini MIDI panic received");
  }

  @Override
  public void sysexReceived(LXSysexMessage sysex) {
    TE.log("JP-Mini sysex " + sysex);
  }
}
