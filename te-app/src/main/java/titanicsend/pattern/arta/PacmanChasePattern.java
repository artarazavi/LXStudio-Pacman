package titanicsend.pattern.arta;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.transform.LXVector;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.util.TEColor;

@LXCategory("Arta")
public class PacmanChasePattern extends TEAudioPattern {
    public final CompoundParameter size =
            new CompoundParameter("Size", 1.34f, 0.7f, 1.7f)
                    .setDescription("Overall size of the chase animation");

    public final CompoundParameter speed =
            new CompoundParameter("Speed", 0.52f, 0.2f, 1.5f)
                    .setDescription("Playback speed of the chase animation");

    private static final int GRID_WIDTH = 32;
    private static final int GRID_HEIGHT = 8;
    private static final double FRAME_MS = 100.0;

    // Reverse-engineered from the actual GIF frames at:
    // https://media.tenor.com/llSAvv3PxdAAAAAM/pacman-namco.gif
    // The source frames were cropped to the active sprite strip and quantized
    // into a 32x8 pixel-art grid so the motion follows the GIF instead of a
    // hand-approximated sequence.
    private static final String[][] FRAMES = {
            {
                    "..........................YY....",
                    ".........................YYYYY..",
                    "........................YWWYDW..",
                    ".D...D....D...D...D...DDYWBYDBP.",
                    ".D...D....D...D...D...DDYYDYOWY.",
                    ".......................YYYYYYYY.",
                    ".......................YYYYYYYY.",
                    "........................Y..Y.Y..",
            },
            {
                    "YY............................OO",
                    "YYYD........................DYYY",
                    "YYY.........................YWWY",
                    ".....D....D...D...D...D....YYWBY",
                    "Y....D....D...D...D...D....YYDDY",
                    "YYYD.......................YYYYY",
                    "YYY........................YYYYY",
                    "...........................Y.Y.Y",
            },
            {
                    "...YY...........................",
                    ".YYYYYY........................O",
                    ".YYYYYY........................O",
                    ".YYYYYY...D...D...D...D....D..DO",
                    ".YYYYYY...D...D...D...D....D..DO",
                    ".YYYYYY.......................DO",
                    "..YYYY........................DO",
                    "...............................O",
            },
            {
                    "......YYD.......................",
                    "....DYYYYY......................",
                    "....YYYYY.......................",
                    "....YY....D...D...D...D....D...D",
                    "....YYYY..D...D...D...D....D...D",
                    "....YYYYYY......................",
                    ".....YYYYY......................",
                    "................................",
            },
            {
                    ".........YYY....................",
                    ".......YYYY.....................",
                    ".......YYY......................",
                    ".......YY.....D...D...D....D...D",
                    ".......YYY....D...D...D....D...D",
                    ".......DYYD.....................",
                    "........YYYY....................",
                    "................................",
            },
            {
                    "............DYY.................",
                    "...........YYYYYY...............",
                    "..........YYYYY.................",
                    "..........YY......D...D....D...D",
                    "..........YYYY....D...D....D...D",
                    "...........YYYYYY...............",
                    "...........YYYYY................",
                    "................................",
            },
            {
                    "...............YYY..............",
                    "..............YYYYYD............",
                    ".............DYYYYYY............",
                    ".............DYYYYYY..D....D...D",
                    ".............DYYYYYY..D....D...D",
                    "..............YYYYYD............",
                    "..............DYYYY.............",
                    "................................",
            },
            {
                    "...................YY...........",
                    ".................YYYYYY.........",
                    ".................YYYYY..........",
                    ".................YY...D....D...D",
                    ".................YYY..D....D...D",
                    ".................YYYYYY.........",
                    "..................YYYY..........",
                    "................................",
            },
            {
                    "......................YYO.......",
                    "D...................YYYY........",
                    "W...................YYY.........",
                    "BP..................YY.....D...D",
                    "WP..................YY.....D...D",
                    "PP..................YYY.........",
                    "PP...................YYYO.......",
                    ".P..............................",
            },
            {
                    "PP.......................YYY....",
                    "PPPP...................OYYYYY...",
                    "WPWW...................YYYYY....",
                    "BPWBP..................YY......D",
                    "WPPWP..................YYYD....D",
                    "PPPPP..................OYYYYY...",
                    "PPPPP...................YYYYY...",
                    ".P.P............................",
            },
            {
                    "...PP.......................DYY.",
                    "..PPPPP....................YYYYY",
                    ".PWWPWW...................YYYYYY",
                    ".PWBPWBB..................YYYYYY",
                    ".PPWPPWP..................YYYYYY",
                    ".PPPPPPP...................YYYYY",
                    ".PPPPPPP...................YYYYY",
                    "...P.P.D........................",
            },
            {
                    "......PPD......................O",
                    "CC...PPPPP....................YY",
                    "WW..PWWWPWW..................DYY",
                    "WBC.PWBBPPB..................DY.",
                    "CWC.PPWPPWP..................DYY",
                    "CCC.PPPPPPP...................YY",
                    "CCC.PPPPPPP...................DY",
                    ".C..P..P..P.....................",
            },
            {
                    ".CC.......PP....................",
                    "CCCCC...PPPPP...................",
                    "WWCWW..DPWWPWW..................",
                    "WBCWBC.PPPBPWB..................",
                    "CWCCWC.PPWPPWP..................",
                    "CCCCCC.PPPPPPP..................",
                    "CCCCCC.PPPPPPP..................",
                    "..C.C..DD.P..P..................",
            },
            {
                    "....CC.......PP.................",
                    "...CCCCC...PPPPPD...............",
                    "..CWWCWWD..PWWPWW...............",
                    "R.CWBCWBB.PPWBPWBP..............",
                    "R.CCWCCWC.PPWPPPWP..............",
                    "R.CCCCCCC.PPPPPPPP..............",
                    "R.CCCCCCC.PPPPPPPP..............",
                    "R...C.C...P.P.DB.P..............",
            },
            {
                    "R......CCC......PP..............",
                    "RRR...CCCCC...DPPPPP............",
                    "RWW..CWWCCWW..PWWPWW............",
                    "RWBR.CWBBCWB.PPWBPWBP...........",
                    "RRWR.CCWCCWC.PPPWPPWP...........",
                    "RRRR.CCCCCCC.PPPPPPPP...........",
                    "RRRR.CCCCCCC.PPPPPPPP...........",
                    "R.R..C..C..C..P..P.P............",
            },
            {
                    "..RR.......CC......PP...........",
                    ".RRRRR...CCCCC....PPPPP.........",
                    "RWWRWW...CWWCWW..PWWPWW.........",
                    "RWBRWBR.CCWBCWB..PWBPWBB........",
                    "RRWRRWR.CCWCCWWB.PPWPPWP........",
                    "RRRRRRR.CCCCCCCB.PPPPPPP........",
                    "RRRRRRR.CCCCCCCB.PPPPPPP........",
                    "..R.R.R.C.C.C..B...P.P.D........",
            },
            {
                    ".....RRR......CC......PPD.......",
                    "O...RRRRR...WCCCCC...PPPPP......",
                    "W..RWWRDWD..CWWCWW..PWWWPWW.....",
                    "BO.RWBRDPB.CCWBCWBC.PWBBPPB.....",
                    "DO.RRWRRWR.CCWWCCWC.PPWPPWP.....",
                    "OO.RRRRRRR.CCCCCCCC.PPPPPPP.....",
                    "OO.RRRRRRR.CCCCCCCC.PPPPPPP.....",
                    ".O...R.R...C.C.CC.C...P.P.......",
            },
            {
                    ".OO......RR.......CC......PP....",
                    "OOOO....RRRRR...CCCCC....PPPPP..",
                    "WWOWW..RWWRWW..CCWWCWW..PWWPWW..",
                    "DBOWB.RRWBRW.R.CCWBCWB.DPWBPWBP.",
                    "WOOWO.RROWRRWR.CCWCCWC.DPPWPPWP.",
                    "OOOOO.RRRRRRRR.CCCCCCC.DPPPPPPP.",
                    "OOOOO.RRRRRRRR.CCCCCCC.DPPPPPPP.",
                    ".O..O..R..R.R..CC.C..C..P..P.P..",
            },
            {
                    "....OO......RRR......CC......PPB",
                    "..OOOOO....RRRRR...CCCCCC...PPPP",
                    "..OWWOWW..RWWRDWD..CWWCWW..PWWDW",
                    ".OOWBOWBO.RW.RDBB.CCWBCWBC.PWBPW",
                    ".OOWOODDO.RRWRRWR.CCWCCCCC.PPWPP",
                    ".OOOOOOOO.RRRRRRR.CCCCCCCC.PPPPP",
                    ".OOOOOOOO.RRRRRRR.CCCCCCCC.PPPPP",
                    ".O.O.D..O...R.R...C.C.CB.C...P.P",
            },
            {
                    ".......OO......RRR......CC......",
                    ".....DOOOOO...RRRRR...CCCCCC...P",
                    ".....OWWOWW..RDWDRWW..CWWCWW..DW",
                    ".D..OOWBOWBO.RDBBRWB.CCWBCWBC.PW",
                    ".D..OODDOODO.RRWRRWR.CCCCCCWC.PP",
                    "....OOOOOOOO.RRRRRRR.CCCCCCCC.PP",
                    "....OOOOOOOO.RRRRRRR.CCCCCCCC.PP",
                    ".....O.OO.O..RR.R..R..C..C.C..DB",
            },
            {
                    "..........OO.......RR......CC...",
                    ".........OOOOO...RRRRR....CCCCC.",
                    "........OWWODW...RWWRWW..CWWCWW.",
                    ".D...D.OOWBODBP.RRWBRWBR.CWBCW.B",
                    ".D...D.OOOWOOWO.RRWRRWRR.CCWCCWC",
                    ".......OOOOOOOO.RRRRRRRR.CCCCCCC",
                    ".......OOOOOOOO.RRRRRRRR.CCCCCCC",
                    "........O..O.O...R.R..R..C..C.D.",
            },
            {
                    ".............OO.......RR......DC",
                    "............OOOOO...RRRRRR...CCC",
                    "...........ODWDDWD..RWWRWW..CWWW",
                    ".D...D....DODBPDDB.RRWBRWBR.CW.B",
                    ".D...D....DOOWOOWO.RROWRRWR.CCWC",
                    "...........OOOOOOO.RRRRRRRR.CCCC",
                    "...........OOOOOOO.RRRRRRRR.CCCC",
                    ".............O.O...R.R..R.R...C.",
            },
            {
                    ".................OO......RR.....",
                    "...............OOOOO....RRRRR...",
                    "..............DDWDOWW..RWWRWW..C",
                    ".D...D....D...ODDBOWB.RRWBRW.R.C",
                    ".D...D....D...OOWOOWO.RROWRRWR.C",
                    "..............OOOOOOO.RRRRRRRR.C",
                    "..............OOOOOOO.RRRRRRRR.C",
                    "..............D..O..O..R..R.R..C",
            },
            {
                    "....................OO......RRR.",
                    "..................OOOOO....RRRRR",
                    "..................OWWOWW..RWWRDW",
                    ".D...D....D...D..OOWBOWBO.RW.RDB",
                    ".D...D....D...D..OOWOODDO.RRWRRW",
                    ".................OOOOOOOO.RRRRRR",
                    ".................OOOOOOOO.RRRRRR",
                    ".................O.O.D..O...R.R.",
            },
            {
                    ".......................OO......R",
                    ".....................DOOOOO...RR",
                    ".....................OWWOWW..RDW",
                    ".D...D....D...D...D.OOWBOWBO.RDB",
                    ".D...D....D...D...D.OODDOODO.RRW",
                    "....................OOOOOOOO.RRR",
                    "....................OOOOOOOO.RRR",
                    "....................O.O..O.O...R",
            },
            {
                    "..........................YY....",
                    ".........................YYYYY..",
                    "........................YWWYDW..",
                    ".D...D....D...D...D...DDYWBYDBP.",
                    ".D...D....D...D...D...DYYYDYOWY.",
                    ".......................YYYYYYYY.",
                    ".......................YYYYYYYY.",
                    "........................Y..Y.Y..",
            },
            {
                    "YD...........................OO.",
                    "YYY.........................OOOO",
                    "YY.........................YDWDD",
                    ".....D....D...D...D...D....YDBPD",
                    "Y....D....D...D...D...D....YYWYY",
                    "YYY........................YYYYY",
                    "YYY........................YYYYY",
                    ".............................Y.Y",
            },
            {
                    "..YYY...........................",
                    ".YYYYY.........................O",
                    "YYYYYYY.......................DD",
                    "YYYYYYY...D...D...D...D....D..OD",
                    "YYYYYYY...D...D...D...D....D..OO",
                    ".YYYYY........................OO",
                    ".YYYYY........................OO",
                    "..............................D.",
            },
            {
                    ".....DYY........................",
                    "....YYYYYY......................",
                    "...YYYYYY.......................",
                    "...YY.....D...D...D...D....D...D",
                    "...YYYY...D...D...D...D....D...D",
                    "....YYYYYY......................",
                    "....YYYYY.......................",
                    "................................",
            },
            {
                    ".........YY.....................",
                    ".......YYYD.....................",
                    "......YYYY......................",
                    "......YYY.....D...D...D....D...D",
                    "......YYY.....D...D...D....D...D",
                    ".......YYY......................",
                    ".......YYYY.....................",
                    "................................",
            },
            {
                    "............YY..................",
                    "..........YYYYYY................",
                    "..........YYYYY.................",
                    "..........YY......D...D....D...D",
                    "..........YYY.....D...D....D...D",
                    "..........YYYYYY................",
                    "...........YYYY.................",
                    "................................",
            },
            {
                    "...............YYD..............",
                    ".............DYYYYY.............",
                    ".............YYYYYYD............",
                    ".............YYYYYYD..D....D...D",
                    ".............YYYYYYY..D....D...D",
                    ".............DYYYYY.............",
                    "..............YYYYY.............",
                    "................................",
            },
            {
                    "..................YYY...........",
                    ".................YYYYY..........",
                    "................YYYYY...........",
                    "................YY....D....D...D",
                    "................YYYY..D....D...D",
                    ".................YYYYY..........",
                    ".................YYYYY..........",
                    "................................",
            },
            {
                    ".....................DYY........",
                    "....................YYY.........",
                    "D..................YYY..........",
                    "B..................YY......D...D",
                    "P..................YYY.....D...D",
                    "P...................YYY.........",
                    "P...................YYYY........",
                    "................................",
            },
            {
                    "PP.......................YYY....",
                    "PPPP...................OYYYYY...",
                    "WPWW...................YYYYY....",
                    "BPWBP..................YY......D",
                    "WPPWP..................YYYD....D",
                    "PPPPP..................OYYYYY...",
                    "PPPPP...................YYYYY...",
                    ".P.P............................",
            },
            {
                    "...PP.......................DYY.",
                    "..PPPPP....................YYYYY",
                    ".PWWPWW...................YYYYYY",
                    ".PWBPWBB..................YYYYYY",
                    ".PPWPPWP..................YYYYYY",
                    ".PPPPPPP...................YYYYY",
                    ".PPPPPPP...................YYYYY",
                    "...P.P.D........................",
            },
            {
                    "......PPD......................O",
                    "CC...PPPPP....................YY",
                    "WW..PWWWPWW..................DYY",
                    "WBC.PWBBPPB..................DY.",
                    "CWC.PPWPPWP..................DYY",
                    "CCC.PPPPPPP...................YY",
                    "CCC.PPPPPPP...................DY",
                    ".C..P..P..P.....................",
            },
            {
                    ".CC.......PP....................",
                    "CCCCC...PPPPP...................",
                    "WWCWW..DPWWPWW..................",
                    "WBCWBC.PPPBPWB..................",
                    "CWCCWC.PPWPPWP..................",
                    "CCCCCC.PPPPPPP..................",
                    "CCCCCC.PPPPPPP..................",
                    "..C.C..DD.P..P..................",
            },
            {
                    "....CC.......PP.................",
                    "...CCCCC...PPPPPD...............",
                    "..CWWCWWD..PWWPWW...............",
                    "R.CWBCWBB.PPWBPWBP..............",
                    "R.CCWCCWC.PPWPPPWP..............",
                    "R.CCCCCCC.PPPPPPPP..............",
                    "R.CCCCCCC.PPPPPPPP..............",
                    "R...C.C...P.P.DB.D..............",
            },
            {
                    "R......BCC......PP..............",
                    "RRR...CCCCC...DPPPPP............",
                    "RWW..CWWCCWW..PWWPWW............",
                    "RWBR.CWBBCWB.PPWBPWBP...........",
                    "RRWR.CCWCCWC.PPPWPPWP...........",
                    "RRRR.CCCCCCC.PPPPPPPP...........",
                    "RRRR.CCCCCCC.PPPPPPPP...........",
                    "R.R..C..C..C..P..P.P............",
            },
            {
                    "..RR.......CC......PP...........",
                    ".RRRRR...CCCCC....PPPPP.........",
                    "RWWRWW...CWWCWW..PWWPWW.........",
                    "RWBRWBR.CCWBCWB..PWBPWBB........",
                    "RRWRRWR.CCWCCWW..PPWPPWP........",
                    "RRRRRRR.CCCCCCCB.PPPPPPP........",
                    "RRRRRRR.CCCCCCCB.PPPPPPP........",
                    "..R.R.R.C.C.W..B...P.P.D........",
            },
            {
                    ".....RRR......CC......PPD.......",
                    "O...RRRRR...CCCCCC...PPPPP......",
                    "W..RWWRDWD..CWWCWW..PWWWPWW.....",
                    "BO.RWBRDPB.CCWBCWBC.PWBBPPB.....",
                    "DO.RRWRRWR.CCWWCCWC.PPWPPWP.....",
                    "OO.RRRRRRR.CCCCCCCC.PPPPPPP.....",
                    "OO.RRRRRRR.CCCCCCCC.PPPPPPP.....",
                    ".O...R.R...C.C.CC.C...P.P.......",
            },
            {
                    "OO......RRR......CC.......PP....",
                    "OOOO...RRRRR...CCCCCC...PPPPP...",
                    "WOWW..RDWWRWW..CWWCWW..DPWWPWW..",
                    "BOWBD.RDPBRWB.DCWBCWBC.PPPBPWB..",
                    "DOOWO.RRWRRWR.DCCWCCWC.PPWPPWP..",
                    "OOOOO.RRRRRRR.DCCCCCCC.PPPPPPP..",
                    "OOOOO.RRRRRRR.DCCCCCCC.PPPPPPP..",
                    ".O.O..RR.R..R..C..C.C..DD.P..P..",
            },
            {
                    "...OO.......RR......CC.......PP.",
                    "..OOOOO...RRRRRR...CCCCC...PPPPP",
                    ".OWWODW...RWWRWW..CWWCWWD..PWWPW",
                    ".OWBODBB.RRWBRWBR.CWBCWBB.PPWBPW",
                    ".OOWOOWO.RRWRRDOR.CCWCCWC.PPWPPP",
                    ".OOOOOOO.RRRRRRRR.CCCCCCC.PPPPPP",
                    ".OOOOOOO.RRRRRRRR.CCCCCCC.PPPPPP",
                    "...O.O.O.R.R.RR.R...C.C...P.P.DB",
            },
            {
                    "......OOO......RR......BCC......",
                    ".....OOOOO...RRRRRR...CCCCC...DP",
                    "....ODWDDWW..RWWRWW..CWWWCWW..PW",
                    ".D..ODBBDPB.RRWBRWBR.CWBBCWB.PPW",
                    ".D..OOWOOWO.RRDORRWR.CCWCCWC.PPP",
                    "....OOOOOOO.RRRRRRRR.CCCCCCC.PPP",
                    "....OOOOOOO.RRRRRRRR.CCCCCCC.PPP",
                    "....O..O..O..R.RR.R..C..C..C..P.",
            },
            {
                    "..........OO......RR.......CC...",
                    "........OOOOO....RRRRR...CCCCC..",
                    ".......ODWWOWW..RWWRWW...CWWCWW.",
                    ".D...D.ODPBOWB..RWBRW.R.CCWBCWB.",
                    ".D...D.OOWOODO..RRWRRWR.CCWCCWWB",
                    ".......OOOOOOO..RRRRRRR.CCCCCCCB",
                    ".......OOOOOOO..RRRRRRR.CCCCCCC.",
                    ".......OO.O..O..R..R.R...C.C..C.",
            },
            {
                    ".............OO......RRR......CC",
                    "...........OOOOOO...RRRRR...WCCC",
                    "...........OWWOWW..RWWRDWD..CWWC",
                    ".D...D....OOWBOWBO.RWBRDPB.CCWBC",
                    ".D...D....OODOODDO.RRWRRWR.CCWWC",
                    "..........OOOOOOOO.RRRRRRR.CCCCC",
                    "..........OOOOOOOO.RRRRRRR.CCCCC",
                    "..........O.D.D..O...R.R...C.C.C",
            },
            {
                    "................OO......RRR.....",
                    "..............DOOOOO...RRRRR...C",
                    "..............OWWOWW..RDWWRWW..C",
                    ".D...D....D..OOWBOWBD.RDPBRWB.DC",
                    ".D...D....D..OODDOOWO.RRWRRWR.DC",
                    ".............OOOOOOOO.RRRRRRR.DC",
                    ".............OOOOOOOO.RRRRRRR.DC",
                    "..............O..O.O..RR.R..R..C",
            },
            {
                    "...................OO.......RR..",
                    "..................OOOOO...RRRRR.",
                    ".................OWWODW...RWWRWW",
                    ".D...D....D...D..OWBODBB.RRWBRWB",
                    ".D...D....D...D..OOWOOWO.RRWRRDO",
                    ".................OOOOOOO.RRRRRRR",
                    ".................OOOOOOO.RRRRRRR",
                    "...................O.O.O.R.R.RR.",
            },
            {
                    "......................OOO......R",
                    ".....................OOOOO...RRR",
                    "....................ODWDDWW..RWW",
                    ".D...D....D...D...D.ODBBDPB.RRWB",
                    ".D...D....D...D...D.OOWOOWO.RRDO",
                    "....................OOOOOOO.RRRR",
                    "....................OOOOOOO.RRRR",
                    "......................O.O...R..R",
            },
            {
                    "..........................OO....",
                    ".........................OOOOO..",
                    "........................OWWODW..",
                    ".D...D....D...D...D...DDOWBODBP.",
                    ".D...D....D...D...D...DDOODOOWO.",
                    ".......................OOOOOOOO.",
                    ".......................OOOOOOOO.",
                    "........................O..O.O..",
            },
    };

    private double animationTimeMs = 0.0;

    public PacmanChasePattern(LX lx) {
        super(lx);
        addParameter("Size", size);
        addParameter("Speed", speed);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        animationTimeMs += deltaMs * speed.getValuef();

        int frameIndex = (int) Math.floor(animationTimeMs / FRAME_MS) % FRAMES.length;
        String[] sprite = FRAMES[frameIndex];

        float centerX = (model.xMax + model.xMin) / 2.0f;
        float centerY = (model.yMax + model.yMin) / 2.0f;
        float modelWidth = model.xMax - model.xMin;
        float modelHeight = model.yMax - model.yMin;
        float pixelSize = Math.min(modelWidth / GRID_WIDTH, modelHeight / GRID_HEIGHT) * size.getValuef();
        float spriteWidth = GRID_WIDTH * pixelSize;
        float spriteHeight = GRID_HEIGHT * pixelSize;
        float startX = centerX - spriteWidth / 2.0f;
        float startY = centerY - spriteHeight / 2.0f;

        for (int i = 0; i < colors.length; i++) {
            colors[i] = LXColor.BLACK;
        }

        for (int i = 0; i < model.points.length; i++) {
            LXVector point = new LXVector(model.points[i]);
            int gridX = (int) Math.floor((point.x - startX) / pixelSize);
            int gridY = (int) Math.floor((point.y - startY) / pixelSize);

            if (gridX < 0 || gridX >= GRID_WIDTH || gridY < 0 || gridY >= GRID_HEIGHT) {
                continue;
            }

            int spriteY = GRID_HEIGHT - 1 - gridY;
            char pixel = sprite[spriteY].charAt(gridX);
            int color = getPixelColor(sprite, gridX, spriteY, pixel);
            if (color != -1) {
                colors[point.index] = color;
            }
        }
    }

    private int getPixelColor(String[] sprite, int x, int y, char pixel) {
        switch (pixel) {
            case 'Y':
                return TEColor.YELLOW;
            case 'O':
                return shouldTreatOrangeAsYellow(sprite, x, y) ? TEColor.YELLOW : TEColor.ORANGE;
            case 'P':
                return LXColor.hsb(320, 40, 100);
            case 'R':
                return LXColor.hsb(0, 100, 100);
            case 'C':
                return LXColor.hsb(180, 100, 100);
            case 'W':
                return LXColor.WHITE;
            case 'B':
                return LXColor.hsb(230, 100, 100);
            case 'D':
                return isPelletPixel(sprite, x, y)
                        ? LXColor.rgb(244, 206, 168)
                        : getDominantBodyColor(sprite, x, y);
            default:
                return -1;
        }
    }

    private boolean isPelletPixel(String[] sprite, int x, int y) {
        // Pellet dots appear as isolated warm pixels or short horizontal pairs.
        // Flesh-tone anti-aliasing pixels sit embedded inside a sprite next to
        // eyes/body colors. Distinguishing them here keeps the dots from
        // visually blending into Pac-Man and ghost faces.
        int nonBackgroundNeighbors = 0;
        int spriteNeighbors = 0;

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }

                int nx = x + dx;
                int ny = y + dy;
                if (ny < 0 || ny >= sprite.length || nx < 0 || nx >= sprite[ny].length()) {
                    continue;
                }

                char neighbor = sprite[ny].charAt(nx);
                if (neighbor != '.') {
                    nonBackgroundNeighbors++;
                }
                if (neighbor == 'Y' || neighbor == 'O' || neighbor == 'P' || neighbor == 'R'
                        || neighbor == 'C' || neighbor == 'W' || neighbor == 'B') {
                    spriteNeighbors++;
                }
            }
        }

        return spriteNeighbors == 0 && nonBackgroundNeighbors <= 2;
    }

    private int getDominantBodyColor(String[] sprite, int x, int y) {
        int yellow = 0;
        int orange = 0;
        int pink = 0;
        int red = 0;
        int cyan = 0;

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }

                int nx = x + dx;
                int ny = y + dy;
                if (ny < 0 || ny >= sprite.length || nx < 0 || nx >= sprite[ny].length()) {
                    continue;
                }

                switch (sprite[ny].charAt(nx)) {
                    case 'Y':
                        yellow++;
                        break;
                    case 'O':
                        orange++;
                        break;
                    case 'P':
                        pink++;
                        break;
                    case 'R':
                        red++;
                        break;
                    case 'C':
                        cyan++;
                        break;
                    default:
                        break;
                }
            }
        }

        if (yellow >= orange && yellow >= pink && yellow >= red && yellow >= cyan && yellow > 0) {
            return TEColor.YELLOW;
        }
        if (orange >= pink && orange >= red && orange >= cyan && orange > 0) {
            return TEColor.ORANGE;
        }
        if (pink >= red && pink >= cyan && pink > 0) {
            return LXColor.hsb(320, 40, 100);
        }
        if (red >= cyan && red > 0) {
            return LXColor.hsb(0, 100, 100);
        }
        if (cyan > 0) {
            return LXColor.hsb(180, 100, 100);
        }

        return LXColor.rgb(244, 206, 168);
    }

    private boolean shouldTreatOrangeAsYellow(String[] sprite, int x, int y) {
        int yellow = 0;
        int warmGhost = 0;

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }

                int nx = x + dx;
                int ny = y + dy;
                if (ny < 0 || ny >= sprite.length || nx < 0 || nx >= sprite[ny].length()) {
                    continue;
                }

                char neighbor = sprite[ny].charAt(nx);
                if (neighbor == 'Y') {
                    yellow++;
                } else if (neighbor == 'R' || neighbor == 'P' || neighbor == 'C') {
                    warmGhost++;
                }
            }
        }

        return yellow >= 2 && warmGhost == 0;
    }
}
