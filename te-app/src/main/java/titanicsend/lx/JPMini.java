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
import java.util.HashSet;
import java.util.Set;
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

  private static final ChannelTarget PACMAN_CHANNEL = new ChannelTarget("Pacman");
  private static final ChannelTarget AUTOPLAY_CHANNEL = new ChannelTarget("Autoplay");
  private static final ChannelTarget HIGH_1_CHANNEL = new ChannelTarget("HIGH", 0, "HIGH 1");
  private static final ChannelTarget HIGH_2_CHANNEL = new ChannelTarget("HIGH", 1, "HIGH 2");
  private static final ChannelTarget LOW_1_CHANNEL = new ChannelTarget("LOW", 0, "LOW 1");
  private static final ChannelTarget LOW_2_CHANNEL = new ChannelTarget("LOW", 1, "LOW 2");
  private static final ChannelTarget OTHER_CHANNEL = new ChannelTarget("Other");
  private static final int GREEN_BANK_CHANNEL = 9;
  private static final int WHITE_BANK_CHANNEL = 9;
  private static final int TOP_BUTTON_CHANNEL = 0;
  private static final int AUTOPLAY_ON_CC = 22;
  private static final int AUTOPLAY_OFF_CC = 23;
  private static final int SOLO_ACTIVE_CHANNEL_CC = 24;
  private static final int TOP_BUTTON_TRIGGER_VALUE = 127;
  private static final double AUTOPLAY_INTERVAL_SECONDS = 30;
  private static final double MASTER_BRIGHTNESS_STEP = 0.10;
  private static final boolean LOG_RAW_MIDI_MESSAGES = false;

  private final Set<String> steppedChannels = new HashSet<>();
  private ChannelTarget activeChannel = PACMAN_CHANNEL;

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
    logRawMidi(
        "note on channel="
            + note.getChannel()
            + " pitch="
            + note.getPitch()
            + " velocity="
            + note.getVelocity());

    int pitch = note.getPitch();
    if (pitch >= 1 && pitch <= 16 && note.getVelocity() > 0) {
      selectChannelPattern(PACMAN_CHANNEL, pitch - 1, "Pacman pad " + pitch);
      return;
    }

    int greenPatternIndex = getGreenBankPatternIndex(note);
    if (greenPatternIndex >= 0 && note.getVelocity() > 0) {
      selectChannelPattern(
          AUTOPLAY_CHANNEL, greenPatternIndex, "green pad " + (greenPatternIndex + 1));
      return;
    }

    if (note.getVelocity() > 0 && handleWhiteBankPad(note)) {
      return;
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

  private boolean handleWhiteBankPad(MidiNoteOn note) {
    if (note.getChannel() != WHITE_BANK_CHANNEL) {
      return false;
    }

    switch (note.getPitch()) {
      case 80:
        setMasterBrightness(0.25, "white pad master 25 percent");
        return true;
      case 81:
        setMasterBrightness(0.50, "white pad master 50 percent");
        return true;
      case 82:
        setMasterBrightness(0.75, "white pad master 75 percent");
        return true;
      case 83:
        setMasterBrightness(1.00, "white pad master 100 percent");
        return true;
      case 76:
        stepChannelPattern(HIGH_1_CHANNEL, -1);
        return true;
      case 77:
        stepChannelPattern(HIGH_1_CHANNEL, 1);
        return true;
      case 78:
        stepChannelPattern(HIGH_2_CHANNEL, -1);
        return true;
      case 79:
        stepChannelPattern(HIGH_2_CHANNEL, 1);
        return true;
      case 72:
        stepChannelPattern(LOW_1_CHANNEL, -1);
        return true;
      case 73:
        stepChannelPattern(LOW_1_CHANNEL, 1);
        return true;
      case 74:
        stepChannelPattern(LOW_2_CHANNEL, -1);
        return true;
      case 75:
        stepChannelPattern(LOW_2_CHANNEL, 1);
        return true;
      case 68:
        stepChannelPattern(OTHER_CHANNEL, -1);
        return true;
      case 69:
        stepChannelPattern(OTHER_CHANNEL, 1);
        return true;
      case 70:
        nudgeMasterBrightness(-MASTER_BRIGHTNESS_STEP);
        return true;
      case 71:
        nudgeMasterBrightness(MASTER_BRIGHTNESS_STEP);
        return true;
      default:
        return false;
    }
  }

  private void selectChannelPattern(ChannelTarget target, int patternIndex, String controlLabel) {
    LXChannel channel = findChannel(target);
    if (channel == null) {
      TE.log("JP-Mini could not find channel " + target.displayLabel);
      return;
    }

    this.activeChannel = target;
    this.steppedChannels.add(target.key());
    channel.fader.setValue(1);

    if (patternIndex >= channel.patterns.size()) {
      TE.log(
          "JP-Mini "
              + controlLabel
              + " has no "
              + target.displayLabel
              + " pattern; channel only has "
              + channel.patterns.size());
      return;
    }

    LXPattern pattern = channel.patterns.get(patternIndex);
    channel.goPatternIndex(patternIndex);
    TE.log(
        "JP-Mini selected "
            + target.displayLabel
            + " pattern "
            + (patternIndex + 1)
            + ": "
            + pattern.getLabel());
  }

  private void stepChannelPattern(ChannelTarget target, int direction) {
    LXChannel channel = findChannel(target);
    if (channel == null) {
      TE.log("JP-Mini could not find channel " + target.displayLabel);
      return;
    }

    this.activeChannel = target;
    channel.fader.setValue(1);

    if (channel.patterns.isEmpty()) {
      TE.log("JP-Mini could not step " + target.displayLabel + "; channel has no patterns");
      return;
    }

    int nextIndex;
    if (!this.steppedChannels.contains(target.key())) {
      nextIndex = 0;
      this.steppedChannels.add(target.key());
    } else {
      int activeIndex = channel.getActivePatternIndex();
      nextIndex = Math.floorMod(activeIndex + direction, channel.patterns.size());
    }

    LXPattern pattern = channel.patterns.get(nextIndex);
    channel.goPatternIndex(nextIndex);
    TE.log(
        "JP-Mini selected "
            + target.displayLabel
            + " pattern "
            + (nextIndex + 1)
            + ": "
            + pattern.getLabel());
  }

  private void setMasterBrightness(double value, String controlLabel) {
    double constrained = Math.max(0, Math.min(1, value));
    this.lx.engine.mixer.masterBus.fader.setValue(constrained);
    this.activeChannel = PACMAN_CHANNEL;
    TE.log(
        "JP-Mini set master brightness to "
            + Math.round(constrained * 100)
            + " percent via "
            + controlLabel);
  }

  private void nudgeMasterBrightness(double delta) {
    double current = this.lx.engine.mixer.masterBus.fader.getValue();
    setMasterBrightness(
        current + delta, delta > 0 ? "white pad master up" : "white pad master down");
  }

  private LXChannel findChannel(ChannelTarget target) {
    int match = 0;
    for (LXAbstractChannel channel : this.lx.engine.mixer.channels) {
      if (channel instanceof LXChannel && target.label.equals(channel.getLabel())) {
        if (match == target.occurrence) {
          return (LXChannel) channel;
        }
        match++;
      }
    }
    return null;
  }

  @Override
  public void noteOffReceived(MidiNote note) {
    logRawMidi(
        "note off channel="
            + note.getChannel()
            + " pitch="
            + note.getPitch()
            + " velocity="
            + note.getVelocity());
  }

  @Override
  public void controlChangeReceived(MidiControlChange cc) {
    logRawMidi(
        "CC channel="
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
    LXChannel channel = findChannel(this.activeChannel);
    if (channel == null) {
      TE.log("JP-Mini could not enable autoplay; no channel " + this.activeChannel.displayLabel);
      return;
    }

    channel.fader.setValue(1);
    channel.patternEngine.autoCycleMode.setValue(AutoCycleMode.NEXT);
    channel.enableAutoCycle(AUTOPLAY_INTERVAL_SECONDS);
    TE.log(
        "JP-Mini enabled "
            + this.activeChannel.displayLabel
            + " autoplay at "
            + AUTOPLAY_INTERVAL_SECONDS
            + "s");
  }

  private void disableActiveChannelAutoplay() {
    LXChannel channel = findChannel(this.activeChannel);
    if (channel == null) {
      TE.log("JP-Mini could not disable autoplay; no channel " + this.activeChannel.displayLabel);
      return;
    }

    channel.disableAutoCycle();
    TE.log("JP-Mini disabled " + this.activeChannel.displayLabel + " autoplay");
  }

  private void soloActiveChannel() {
    LXChannel channelToSolo = findChannel(this.activeChannel);
    if (channelToSolo == null) {
      TE.log("JP-Mini could not solo; no channel " + this.activeChannel.displayLabel);
      return;
    }

    for (LXAbstractChannel channel : this.lx.engine.mixer.channels) {
      channel.fader.setValue(channel == channelToSolo ? 1 : 0);
    }
    TE.log("JP-Mini soloed " + this.activeChannel.displayLabel + " channel");
  }

  private static class ChannelTarget {
    private final String label;
    private final int occurrence;
    private final String displayLabel;

    private ChannelTarget(String label) {
      this(label, 0, label);
    }

    private ChannelTarget(String label, int occurrence, String displayLabel) {
      this.label = label;
      this.occurrence = occurrence;
      this.displayLabel = displayLabel;
    }

    private String key() {
      return this.label + "#" + this.occurrence;
    }
  }

  @Override
  public void programChangeReceived(MidiProgramChange pc) {
    logRawMidi("program change channel=" + pc.getChannel() + " program=" + pc.getProgram());
  }

  @Override
  public void pitchBendReceived(MidiPitchBend pitchBend) {
    logRawMidi(
        "pitch bend channel="
            + pitchBend.getChannel()
            + " bend="
            + pitchBend.getPitchBend()
            + " normalized="
            + pitchBend.getNormalized());
  }

  @Override
  public void aftertouchReceived(MidiAftertouch aftertouch) {
    logRawMidi(
        "aftertouch channel="
            + aftertouch.getChannel()
            + " pressure="
            + aftertouch.getAftertouch());
  }

  @Override
  public void midiPanicReceived() {
    logRawMidi("MIDI panic received");
  }

  @Override
  public void sysexReceived(LXSysexMessage sysex) {
    logRawMidi("sysex " + sysex);
  }

  private void logRawMidi(String message) {
    if (LOG_RAW_MIDI_MESSAGES) {
      TE.log("JP-Mini " + message);
    }
  }
}
