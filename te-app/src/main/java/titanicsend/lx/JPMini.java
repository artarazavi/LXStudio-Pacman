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
 * <p>The JP-Mini banks select patterns on matching Chromatik channels. Pressing a pad raises that
 * channel fader without muting other channels, so patterns can be layered. The top buttons control
 * whichever JP-Mini bank/channel was most recently selected.
 */
@LXMidiSurface.Name("JP-Mini Pacman")
@LXMidiSurface.DeviceName(MidiNames.JPMINI)
public class JPMini extends LXMidiSurface {

  private static final String PACMAN_CHANNEL_LABEL = "Pacman";
  private static final String AUTOPLAY_CHANNEL_LABEL = "Autoplay";
  private static final int GREEN_BANK_CHANNEL = 9;
  private static final int TOP_BUTTON_CHANNEL = 0;
  private static final int AUTOPLAY_ON_CC = 22;
  private static final int AUTOPLAY_OFF_CC = 23;
  private static final int SOLO_ACTIVE_CHANNEL_CC = 24;
  private static final int TOP_BUTTON_TRIGGER_VALUE = 127;
  private static final double AUTOPLAY_INTERVAL_SECONDS = 30;

  private String activeChannelLabel = PACMAN_CHANNEL_LABEL;

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
    TE.log(
        "JP-Mini note on channel="
            + note.getChannel()
            + " pitch="
            + note.getPitch()
            + " velocity="
            + note.getVelocity());

    int pitch = note.getPitch();
    if (pitch >= 1 && pitch <= 16 && note.getVelocity() > 0) {
      selectChannelPattern(PACMAN_CHANNEL_LABEL, pitch - 1, "Pacman pad " + pitch);
      return;
    }

    int greenPatternIndex = getGreenBankPatternIndex(note);
    if (greenPatternIndex >= 0 && note.getVelocity() > 0) {
      selectChannelPattern(
          AUTOPLAY_CHANNEL_LABEL, greenPatternIndex, "green pad " + (greenPatternIndex + 1));
    }
  }

  private int getGreenBankPatternIndex(MidiNoteOn note) {
    if (note.getChannel() != GREEN_BANK_CHANNEL) {
      return -1;
    }

    int pitch = note.getPitch();
    if (pitch < 52 || pitch > 67) {
      return -1;
    }

    int row = (67 - pitch) / 4;
    int col = pitch % 4;
    return row * 4 + col;
  }

  private void selectChannelPattern(String channelLabel, int patternIndex, String controlLabel) {
    LXChannel channel = findChannel(channelLabel);
    if (channel == null) {
      TE.log("JP-Mini could not find channel named " + channelLabel);
      return;
    }

    this.activeChannelLabel = channelLabel;
    channel.fader.setValue(1);

    if (patternIndex >= channel.patterns.size()) {
      TE.log(
          "JP-Mini "
              + controlLabel
              + " has no "
              + channelLabel
              + " pattern; channel only has "
              + channel.patterns.size());
      return;
    }

    LXPattern pattern = channel.patterns.get(patternIndex);
    channel.goPatternIndex(patternIndex);
    TE.log(
        "JP-Mini selected "
            + channelLabel
            + " pattern "
            + (patternIndex + 1)
            + ": "
            + pattern.getLabel());
  }

  private LXChannel findChannel(String channelLabel) {
    for (LXAbstractChannel channel : this.lx.engine.mixer.channels) {
      if (channel instanceof LXChannel && channelLabel.equals(channel.getLabel())) {
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
        enableActiveChannelAutoplay();
        break;
      case AUTOPLAY_OFF_CC:
        disableActiveChannelAutoplay();
        break;
      case SOLO_ACTIVE_CHANNEL_CC:
        soloActiveChannel();
        break;
      default:
        break;
    }
  }

  private void enableActiveChannelAutoplay() {
    LXChannel channel = findChannel(this.activeChannelLabel);
    if (channel == null) {
      TE.log("JP-Mini could not enable autoplay; no channel named " + this.activeChannelLabel);
      return;
    }

    channel.fader.setValue(1);
    channel.patternEngine.autoCycleMode.setValue(AutoCycleMode.NEXT);
    channel.enableAutoCycle(AUTOPLAY_INTERVAL_SECONDS);
    TE.log(
        "JP-Mini enabled "
            + this.activeChannelLabel
            + " autoplay at "
            + AUTOPLAY_INTERVAL_SECONDS
            + "s");
  }

  private void disableActiveChannelAutoplay() {
    LXChannel channel = findChannel(this.activeChannelLabel);
    if (channel == null) {
      TE.log("JP-Mini could not disable autoplay; no channel named " + this.activeChannelLabel);
      return;
    }

    channel.disableAutoCycle();
    TE.log("JP-Mini disabled " + this.activeChannelLabel + " autoplay");
  }

  private void soloActiveChannel() {
    LXChannel activeChannel = findChannel(this.activeChannelLabel);
    if (activeChannel == null) {
      TE.log("JP-Mini could not solo; no channel named " + this.activeChannelLabel);
      return;
    }

    for (LXAbstractChannel channel : this.lx.engine.mixer.channels) {
      channel.fader.setValue(channel == activeChannel ? 1 : 0);
    }
    TE.log("JP-Mini soloed " + this.activeChannelLabel + " channel");
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
