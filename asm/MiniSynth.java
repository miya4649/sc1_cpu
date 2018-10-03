/*
  Copyright (c) 2018, miya
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

import java.lang.Math;

public class MiniSynth extends AsmLib
{
  private static final int TRUE = 1; // for program
  private static final int FALSE = 0;
  private static final int CODE_ROM_DEPTH = 10;
  private static final int DATA_ROM_DEPTH = 9;
  private static final int OSC_PARAM_SIZE_BITS = 2;
  private static final int OSC_PARAM_SIZE = (1 << OSC_PARAM_SIZE_BITS);
  private static final int CH_BITS = 2;
  private static final int CH = (1 << CH_BITS);
  private static final int NOTE_BITS = 2;
  private static final int NOTES = (1 << NOTE_BITS);
  private static final int NOTES_MASK = (NOTES - 1);
  private static final int CHORD_BITS = 3;
  private static final int CHORDS = (1 << CHORD_BITS);
  private static final int CHORDS_MASK = (CHORDS - 1);
  private static final int OCTAVES_MASK = 3;
  private static final int BEATS = 16;
  private static final int BARS = 4;
  private static final int SCALE_TABLE_SIZE = 16;
  private static final int SAMPLE_RATE = 48000;
  private static final int VOLUME = 4096;
  private static final int ADD_FREQUENCY_MASK = 63;
  private static final int DELETE_FREQUENCY = 48;
  private static final int FB_WIDTH = 128;
  private static final int FB_HEIGHT = 64;
  private static final int FB_SPAN = 128;
  private static final int FB_SIZE = (FB_HEIGHT * FB_SPAN);
  private static final int VIS_OBJ_SIZE = 64;
  private static final int VIS_STATE_CLEAR = 0;
  private static final int VIS_STATE_DRAW = 1;
  private static final int VIS_STATE_WAIT_VSYNC = 2;
  private static final int VIS_CLEAR_SIZE = 1024;
  private static final int VIS_CLEAR_END_ADDR = (SPRITE_ADDRESS + FB_SIZE);

  private StructHelper ctrlParam = new StructHelper();
  private StructHelper oscParam = new StructHelper();

  private void f_synth_init()
  {
    int Rac = R12;

    label("f_synth_init");

    // Rac = addr_struct("d_ctrl_param")
    lib_set_im32(addr_struct("d_ctrl_param"));
    lib_simple_mv(Rac, SP_REG_MVI);
    lib_wait_reg2addr();
    // d_addr=a_addr=b_addr=Rac
    as_nop(Rac,Rac,Rac, AM_SET,AM_SET,AM_SET);
    // tempoCount = 0
    as_add(ctrlParam.offset("tempoCount"),reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // bar = 0
    as_add(ctrlParam.offset("bar"),reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // seqPtr = &seq_data
    lib_set_im32(addr_abs("d_seq_data"));
    as_add(ctrlParam.offset("seqPtr"),SP_REG_MVI,reg_im(0), AM_OFFSET,AM_REG,AM_REG);

    lib_return();
  }

  private void f_synth_sequencer()
  {
    /*
    for (ch = 0; ch < CH; ch++)
    {
      data = *ctrlParam.seqPtr;
      if (data == 0)
      {
        osc_param[ch].levelL = 0;
        osc_param[ch].levelR = 0;
      }
      else
      {
        oct = data & 0xffff;
        note = (data >> 16) & 0xffff;
        pitch = scale_table[chord_table[(chord << NOTE_BITS) + note]] >> oct;
        osc_param[ch].levelL = VOLUME;
        osc_param[ch].levelR = VOLUME;
        osc_param[ch].pitch = pitch;
        visualizer_add();
      }
      if ((rand() & 0xff) < DELETE_FREQUENCY)
      {
        *ctrlParam.seqPtr = 0;
      }
      if ((rand() & 0xff) < ctrlParam.addFrequency)
      {
        *ctrlParam.seqPtr = ((rand() & NOTES_MASK) << 16) | (rand() & OCTAVES_MASK);
      }
      ctrlParam.seqPtr++;
    }
    ctrlParam.beat++;
    if (ctrlParam.beat == BEATS)
    {
      ctrlParam.beat = 0;
      ctrlParam.seqPtr = &seq_data;
      ctrlParam.bar++;
      if (ctrlParam.bar == BARS)
      {
        ctrlParam.bar = 0;
        ctrlParam.chord = rand() & CHORDS_MASK;
        ctrlParam.addFrequency = rand() & ADD_FREQUENCY_MASK;
      }
    }
    */

    int Rdata = R6;
    int Rt1 = R6;
    int Rnote = R7;
    int RscaleTableAddr = R7;
    int Rpitch = R7;
    int Roct = R8;
    int Rt2 = R8;
    int Rbar = R9;
    int Rbeat = R10;
    int RctrlParam = R11;
    int RoscParam = R12;
    int RseqPtr = R13;
    int Rch = R14;
    label("f_synth_sequencer");
    lib_push(SP_REG_LINK);
    lib_push_regs(R12, R14);

    // RctrlParam = addr_struct("d_ctrl_param")
    lib_set_im32(addr_struct("d_ctrl_param"));
    lib_simple_mv(RctrlParam, SP_REG_MVI);
    // RoscParam = addr_struct("d_osc_param")
    lib_set_im32(addr_struct("d_osc_param"));
    lib_simple_mv(RoscParam, SP_REG_MVI);
    // RseqPtr = ctrlParam.seqPtr
    lib_wait_reg2addr();
    as_nop(RctrlParam,RctrlParam,RctrlParam, AM_SET,AM_SET,AM_SET);
    as_add(RseqPtr,ctrlParam.offset("seqPtr"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    lib_wait_reg2addr();
    // for (ch = 0; ch < CH; ch++)
    // Rch = 0
    lib_set(Rch, 0);
    label("f_synth_sequencer_L_0");

    // data = *ctrlParam.seqPtr;
    as_add(Rdata,RseqPtr,reg_im(0), AM_REG,AM_SET,AM_REG);
    // if (data == 0) goto "f_synth_sequencer_L_1"
    as_ceq(Rdata,reg_im(0), AM_REG,AM_REG);
    lib_bc("f_synth_sequencer_L_1");
    // note = (data >> 16) & 0xffff;
    lib_set_im(16);
    as_sr(Rnote,Rdata,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    lib_set_im(0xffff);
    as_and(Rnote,Rnote,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    // oct = data & 0xffff;
    as_and(Roct,Rdata,SP_REG_MVI, AM_REG,AM_REG,AM_REG);

    // pitch = scale_table[chord_table[(chord << NOTE_BITS) + note]] >> oct;
    //   (chord << NOTE_BITS) + note
    as_nop(RctrlParam,RctrlParam,RctrlParam, AM_SET,AM_SET,AM_SET);
    lib_set_im(NOTE_BITS);
    as_sl(Rt1,ctrlParam.offset("chord"),SP_REG_MVI, AM_REG,AM_OFFSET,AM_REG);
    as_add(Rnote,Rt1,Rnote, AM_REG,AM_REG,AM_REG);
    //   chord_table[(chord << NOTE_BITS) + note]
    lib_set_im32(addr_abs("d_chord_table"));
    as_add(RscaleTableAddr,Rnote,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    lib_wait_reg2addr();
    as_add(Rnote,RscaleTableAddr,reg_im(0), AM_REG,AM_SET,AM_REG);
    //   pitch = scale_table[chord_table[(chord << NOTE_BITS) + note]] >> oct;
    lib_set_im32(addr_abs("d_scale_table"));
    as_add(RscaleTableAddr,Rnote,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    lib_wait_reg2addr();
    as_sr(Rpitch,RscaleTableAddr,Roct, AM_REG,AM_SET,AM_REG);
    // osc_param[ch].levelL = VOLUME;
    as_nop(RoscParam,0,0, AM_SET,AM_REG,AM_REG);
    lib_set_im32(VOLUME);
    as_add(oscParam.offset("levelL"),SP_REG_MVI,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // osc_param[ch].levelR = VOLUME;
    as_add(oscParam.offset("levelR"),SP_REG_MVI,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // osc_param[ch].pitch = pitch;
    as_add(oscParam.offset("pitch"),Rpitch,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // visualizer_add();
    lib_push_regs(R11, R14);
    lib_call("f_visualizer_add");
    lib_pop_regs(R14, R11);
    lib_ba("f_synth_sequencer_L_2");

    label("f_synth_sequencer_L_1");
    // osc_param[ch].levelL = 0;
    as_nop(RoscParam,0,0, AM_SET,AM_REG,AM_REG);
    as_add(oscParam.offset("levelL"),reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // osc_param[ch].levelR = 0;
    as_add(oscParam.offset("levelR"),reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    label("f_synth_sequencer_L_2");

    // if ((rand() & 0xff) < DELETE_FREQUENCY) {*ctrlParam.seqPtr = 0;}
    lib_call("f_rand");
    lib_set_im(0xff);
    as_and(R6,R6,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    lib_set_im(DELETE_FREQUENCY);
    as_cgt(SP_REG_MVI,R6, AM_REG,AM_REG);
    as_mv(RseqPtr,reg_im(0), AM_SET,AM_REG);

    // if ((rand() & 0xff) <= ctrlParam.addFrequency)
    // {
    //   *ctrlParam.seqPtr = ((rand() & NOTES_MASK) << 16) | (rand() & OCTAVES_MASK);
    // }
    //   (rand() & 0xff)
    lib_call("f_rand");
    lib_set_im(0xff);
    as_and(R6,R6,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    as_nop(RctrlParam,RctrlParam,RctrlParam, AM_SET,AM_SET,AM_SET);
    as_cgt(R6,ctrlParam.offset("addFrequency"), AM_REG,AM_OFFSET);
    lib_bc("f_synth_sequencer_L_3");
    //   (rand() & NOTES_MASK)
    lib_call("f_rand");
    as_and(R6,R6,reg_im(NOTES_MASK), AM_REG,AM_REG,AM_REG);
    //   ((rand() & NOTES_MASK) << 16)
    lib_set_im(16);
    as_sl(Rt2,R6,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    //   (rand() & OCTAVES_MASK)
    lib_call("f_rand");
    lib_set_im(OCTAVES_MASK);
    as_and(R6,R6,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    //   *ctrlParam.seqPtr = ((rand() & NOTES_MASK) << 16) | (rand() & OCTAVES_MASK);
    as_or(RseqPtr,Rt2,R6, AM_SET,AM_REG,AM_REG);
    label("f_synth_sequencer_L_3");

    // ctrlParam.seqPtr++;
    as_add(RseqPtr,RseqPtr,reg_im(1), AM_REG,AM_REG,AM_REG);

    // if Rch < CH goto "f_synth_sequencer_L0"
    // Rch++
    as_add(Rch,Rch,reg_im(1), AM_REG,AM_REG,AM_REG);
    lib_set_im(OSC_PARAM_SIZE);
    as_add(RoscParam,RoscParam,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    lib_set_im(CH);
    as_cgt(SP_REG_MVI,Rch, AM_REG,AM_REG);
    lib_bc("f_synth_sequencer_L_0");

    // ctrlParam.beat++;
    // Rbeat = ctrlParam.beat
    as_nop(RctrlParam,RctrlParam,RctrlParam, AM_SET,AM_SET,AM_SET);
    as_add(Rbeat,ctrlParam.offset("beat"),reg_im(1), AM_REG,AM_OFFSET,AM_REG);
    // Rbar = ctrlParam.bar
    as_add(Rbar,ctrlParam.offset("bar"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);

    // if (ctrlParam.beat == BEATS)
    lib_set_im32(BEATS);
    as_cgt(SP_REG_MVI,Rbeat, AM_REG,AM_REG);
    lib_bc("f_synth_sequencer_L_4");
    // {
    //   ctrlParam.beat = 0;
    as_add(Rbeat,reg_im(0),reg_im(0), AM_REG,AM_REG,AM_REG);
    //   ctrlParam.seqPtr = &seq_data;
    lib_set_im32(addr_abs("d_seq_data"));
    as_add(RseqPtr,SP_REG_MVI,reg_im(0), AM_REG,AM_REG,AM_REG);
    //   ctrlParam.bar++;
    as_add(Rbar,ctrlParam.offset("bar"),reg_im(1), AM_REG,AM_OFFSET,AM_REG);
    //   if (ctrlParam.bar == BARS)
    lib_set_im32(BARS);
    as_cgt(SP_REG_MVI,Rbar, AM_REG,AM_REG);
    lib_bc("f_synth_sequencer_L_4");
    //   {
    //     ctrlParam.bar = 0;
    as_add(Rbar,reg_im(0),reg_im(0), AM_REG,AM_REG,AM_REG);
    //     ctrlParam.chord = rand() & CHORDS_MASK;
    lib_call("f_rand");
    as_nop(RctrlParam,RctrlParam,RctrlParam, AM_SET,AM_SET,AM_SET);
    lib_set_im(CHORDS_MASK);
    as_and(ctrlParam.offset("chord"),R6,SP_REG_MVI, AM_OFFSET,AM_REG,AM_REG);
    //     ctrlParam.addFrequency = rand() & ADD_FREQUENCY_MASK;
    lib_call("f_rand");
    as_nop(RctrlParam,RctrlParam,RctrlParam, AM_SET,AM_SET,AM_SET);
    lib_set_im(ADD_FREQUENCY_MASK);
    as_and(ctrlParam.offset("addFrequency"),R6,SP_REG_MVI, AM_OFFSET,AM_REG,AM_REG);
    label("f_synth_sequencer_L_4");

    // ctrlParam.bar = Rbar;
    as_nop(RctrlParam,RctrlParam,RctrlParam, AM_SET,AM_SET,AM_SET);
    as_add(ctrlParam.offset("bar"),Rbar,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // ctrlParam.beat = Rbeat;
    as_add(ctrlParam.offset("beat"),Rbeat,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // ctrlParam.seqPtr = RseqPtr;
    as_add(ctrlParam.offset("seqPtr"),RseqPtr,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    lib_pop_regs(R14, R12);
    lib_pop(SP_REG_LINK);
    lib_return();
  }

  private void f_synth_render()
  {
    int Rt1 = R6;
    int Rt2 = R7;
    int Rt3 = R8;
    int Rt4 = R9;
    int Rt5 = R10;
    int Ri = R11;
    int Rac = R12;
    int Rao = R13;
    label("f_synth_render");
    lib_push(SP_REG_LINK);
    lib_push_regs(R12, R14);

    label("f_synth_render_L_0");
    // if audio_full then return
    lib_set_im32(AUDIO_FULL_ADDRESS);
    lib_wait_reg2addr();
    as_ceq(SP_REG_MVI,reg_im(1), AM_SET,AM_REG);
    lib_bc("f_synth_render_L_return");

    // Rac = addr_struct("d_ctrl_param")
    lib_set_im32(addr_struct("d_ctrl_param"));
    lib_simple_mv(Rac, SP_REG_MVI);
    // Rao = addr_struct("d_osc_param")
    lib_set_im32(addr_struct("d_osc_param"));
    lib_simple_mv(Rao, SP_REG_MVI);
    lib_wait_reg2addr();

    // mix = 0
    as_nop(Rac,Rac,Rac, AM_SET,AM_SET,AM_SET);
    as_add(ctrlParam.offset("mixL"),reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    as_add(ctrlParam.offset("mixR"),reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);

    // ch loop
    // Ri = 0
    lib_set(Ri, 0);
    label("f_synth_render_L_1");
    
    // render
    as_nop(Rao,Rao,Rao, AM_SET,AM_SET,AM_SET);
    // Rt1 = count + pitch
    as_add(Rt1,oscParam.offset("count"),oscParam.offset("pitch"), AM_REG,AM_OFFSET,AM_OFFSET);

    // count = Rt1
    as_add(oscParam.offset("count"),Rt1,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // Rt2 = -levelL
    as_sub(Rt2,reg_im(0),oscParam.offset("levelL"), AM_REG,AM_REG,AM_OFFSET);
    // Rt3 = -levelR
    as_sub(Rt3,reg_im(0),oscParam.offset("levelR"), AM_REG,AM_REG,AM_OFFSET);
    // if Rt1 > 0 then Rt2 = levelL, Rt3 = levelR
    as_cgta(Rt1,reg_im(0), AM_REG,AM_REG);
    as_mv(Rt2,oscParam.offset("levelL"), AM_REG,AM_OFFSET);
    as_mv(Rt3,oscParam.offset("levelR"), AM_REG,AM_OFFSET);

    // mixL += Rt2
    as_nop(Rac,Rac,Rac, AM_SET,AM_SET,AM_SET);
    as_add(ctrlParam.offset("mixL"),ctrlParam.offset("mixL"),Rt2, AM_OFFSET,AM_OFFSET,AM_REG);
    // mixR += Rt3
    as_add(ctrlParam.offset("mixR"),ctrlParam.offset("mixR"),Rt3, AM_OFFSET,AM_OFFSET,AM_REG);

    // ch loop end
    // Rao += OSC_PARAM_SIZE
    lib_set_im(OSC_PARAM_SIZE);
    as_add(Rao,Rao,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    // Ri++
    as_add(Ri,Ri,reg_im(1), AM_REG,AM_REG,AM_REG);
    // if Ri < CH goto "f_synth_render_L_1"
    lib_set_im(CH);
    as_cgt(SP_REG_MVI,Ri, AM_REG,AM_REG);
    lib_bc("f_synth_render_L_1");

    // if (mixR > VOLUME - 1) LED = 1; else LED = 0;
    lib_set_im32(VOLUME - 1);
    as_cgta(ctrlParam.offset("mixR"),SP_REG_MVI, AM_OFFSET,AM_REG);
    lib_set_im32(LED_ADDRESS);
    lib_wait_reg2addr();
    as_add(Rt1,reg_im(0),reg_im(0), AM_REG,AM_REG,AM_REG);
    as_mv(Rt1,reg_im(1), AM_REG,AM_REG);
    as_add(SP_REG_MVI,Rt1,reg_im(0), AM_SET,AM_REG,AM_REG);

    // 16bit R + 16bit L packing
    // Rt1 = mixR + 0x8000
    lib_set_im32(0x8000);
    as_add(Rt1,ctrlParam.offset("mixR"),SP_REG_MVI, AM_REG,AM_OFFSET,AM_REG);
    // Rt2 = mixL + 0x8000
    as_add(Rt2,ctrlParam.offset("mixL"),SP_REG_MVI, AM_REG,AM_OFFSET,AM_REG);
    // Rt1 = Rt1 & 0xffff
    lib_set_im(0xffff);
    as_and(Rt1,Rt1,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    // Rt2 = Rt2 & 0xffff
    as_and(Rt2,Rt2,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    // Rt1 = Rt1 << 16
    lib_set_im(16);
    as_sl(Rt1,Rt1,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    // Rt1 = Rt1 | Rt2
    as_or(Rt1,Rt1,Rt2, AM_REG,AM_REG,AM_REG);
    // *AUDIO_DATA_ADDRESS = Rt1
    lib_set_im32(AUDIO_DATA_ADDRESS);
    lib_wait_reg2addr();
    as_add(SP_REG_MVI,Rt1,reg_im(0), AM_SET,AM_REG,AM_REG);
    // *AUDIO_VALID_ADDRESS ^= *AUDIO_VALID_ADDRESS
    lib_set_im32(AUDIO_VALID_ADDRESS);
    lib_wait_reg2addr();
    as_xor(Rt1,ctrlParam.offset("audioValid"),reg_im(1), AM_REG,AM_OFFSET,AM_REG);
    as_add(SP_REG_MVI,Rt1,reg_im(0), AM_SET,AM_REG,AM_REG);
    as_nop(Rac,Rac,Rac, AM_SET,AM_SET,AM_SET);
    as_add(ctrlParam.offset("audioValid"),Rt1,reg_im(0), AM_OFFSET,AM_REG,AM_REG);

    // if tempoCount < tempo goto "f_synth_render_L_0"
    as_cgt(ctrlParam.offset("tempo"),ctrlParam.offset("tempoCount"), AM_OFFSET,AM_OFFSET);
    // tempoCount++
    as_add(ctrlParam.offset("tempoCount"),ctrlParam.offset("tempoCount"),reg_im(1), AM_OFFSET,AM_OFFSET,AM_REG);
    lib_bc("f_synth_render_L_0");

    // tempoCount = 0
    as_add(ctrlParam.offset("tempoCount"),reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);

    // call sequencer
    lib_call("f_synth_sequencer");

    // repeat until audio_full
    lib_ba("f_synth_render_L_0");

    label("f_synth_render_L_return");
    lib_pop_regs(R14, R12);
    lib_pop(SP_REG_LINK);
    lib_return();
  }

  private void f_visualizer_init()
  {
    label("f_visualizer_init");
    lib_push(SP_REG_LINK);
    // init vga
    lib_set_im32(SPRITE_X_ADDRESS);
    lib_wait_reg2addr();
    // x = 0
    as_add(SP_REG_MVI,reg_im(0),reg_im(0), AM_SET,AM_REG,AM_REG);
    // y = 128
    lib_set_im32(128);
    as_add(1,SP_REG_MVI,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // scale = 5
    as_add(2,reg_im(5),reg_im(0), AM_OFFSET,AM_REG,AM_REG);

    // init parameter
    lib_set_im32(addr_struct("d_ctrl_param"));
    lib_wait_reg2addr();
    as_nop(SP_REG_MVI,0,0, AM_SET,AM_REG,AM_REG);
    // vsyncState = FALSE;
    as_add(ctrlParam.offset("vsyncState"),reg_im(FALSE),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // visObjPtr = &d_visobj_data
    lib_set_im32(addr_abs("d_visobj_data"));
    as_add(ctrlParam.offset("visObjPtr"),SP_REG_MVI,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // visObjDrawPtr = &d_visobj_data
    lib_set_im32(addr_abs("d_visobj_data"));
    as_add(ctrlParam.offset("visObjDrawPtr"),SP_REG_MVI,reg_im(0), AM_OFFSET,AM_REG,AM_REG);

    // ctrlParam.clearAddr = SPRITE_ADDRESS
    lib_set_im32(SPRITE_ADDRESS);
    as_add(ctrlParam.offset("clearAddr"),SP_REG_MVI,reg_im(0), AM_OFFSET,AM_REG,AM_REG);

    // clear frame buffer
    lib_set_im32(SPRITE_ADDRESS);
    as_add(R6,SP_REG_MVI,reg_im(0), AM_REG,AM_REG,AM_REG);
    lib_set_im32(FB_SIZE);
    as_add(R7,SP_REG_MVI,reg_im(0), AM_REG,AM_REG,AM_REG);
    as_add(R8,reg_im(0),reg_im(0), AM_REG,AM_REG,AM_REG);
    lib_call("f_memory_fill");

    lib_pop(SP_REG_LINK);
    lib_return();
  }

  // dst_reg = (int8)src_reg[index_reg];
  // modify: SP_REG_MVI, dst_reg
  private void m_reg_load_byte(int dst_reg, int src_reg, int index_reg)
  {
    lib_simple_mv(dst_reg, src_reg);
    lib_simple_mv(SP_REG_MVI, index_reg);
    as_sl(SP_REG_MVI,SP_REG_MVI,reg_im(3), AM_REG,AM_REG,AM_REG);
    as_sl(dst_reg,dst_reg,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    lib_set_im(24);
    as_sra(dst_reg,dst_reg,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
  }

  // dst_reg[index_reg] = (int8)src_reg;
  // modify: SP_REG_MVI, dst_reg, src_reg
  // 0 <= index <= 3
  private void m_reg_store_byte(int dst_reg, int src_reg, int index_reg)
  {
    as_sl(index_reg,index_reg,reg_im(3), AM_REG,AM_REG,AM_REG);
    lib_set_im32(0xff000000);
    as_sr(SP_REG_MVI,SP_REG_MVI,index_reg, AM_REG,AM_REG,AM_REG);
    as_not(SP_REG_MVI,SP_REG_MVI, AM_REG,AM_REG);
    as_and(dst_reg,dst_reg,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    lib_set_im(24);
    as_sl(src_reg,src_reg,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    as_sr(src_reg,src_reg,index_reg, AM_REG,AM_REG,AM_REG);
    as_or(dst_reg,dst_reg,src_reg, AM_REG,AM_REG,AM_REG);
    as_sr(index_reg,index_reg,reg_im(3), AM_REG,AM_REG,AM_REG);
  }

  private void f_visualizer()
  {
    int Rvsync = R6;
    int Rcond = R7;
    int Rfbaddr = R8;
    int RctrlParam = R11;
    int Ri = R6;
    int Rx = R7;
    int Rone = R8;
    int Rdir = R9;
    int Rwidth = R12;
    int Rpoint = R13;
    int RdxPtr = R13;
    int Rt1 = R6;
    int Rldataaddr = R12;
    int Rcolor = R13;
    int RvisObjDrawPtr = R14;
    label("f_visualizer");
    lib_push(SP_REG_LINK);
    lib_push_regs(R12, R14);
    lib_set_im32(addr_struct("d_ctrl_param"));
    lib_simple_mv(RctrlParam, SP_REG_MVI);
    lib_wait_reg2addr();
    as_nop(RctrlParam,RctrlParam,RctrlParam, AM_SET,AM_SET,AM_SET);
    // if (ctrlParam.visState < VIS_STATE_WAIT_VSYNC) goto "f_visualizer_L_0"
    as_cgt(reg_im(VIS_STATE_WAIT_VSYNC),ctrlParam.offset("visState"), AM_REG,AM_OFFSET);
    lib_bc("f_visualizer_L_0");
    // vsync = *VSYNC;
    lib_set_im32(VSYNC_ADDRESS);
    lib_wait_reg2addr();
    as_add(Rvsync,SP_REG_MVI,reg_im(0), AM_REG,AM_SET,AM_REG);
    as_nop(RctrlParam,RctrlParam,RctrlParam, AM_SET,AM_SET,AM_SET);
    // cond = ((ctrlParam.vsyncState == FALSE) || (vsync == TRUE));
    as_ceq(ctrlParam.offset("vsyncState"),reg_im(FALSE), AM_OFFSET,AM_REG);
    lib_simple_mv(Rcond, SP_REG_CP);
    as_ceq(Rvsync,reg_im(TRUE), AM_REG,AM_REG);
    as_or(Rcond,Rcond,SP_REG_CP, AM_REG,AM_REG,AM_REG);
    // ctrlParam.vsyncState = vsync;
    as_add(ctrlParam.offset("vsyncState"),Rvsync,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // if (cond == TRUE) return
    as_add(SP_REG_CP,Rcond,reg_im(0), AM_REG,AM_REG,AM_REG);
    lib_bc("f_visualizer_L_return");
    // ctrlParam.visState = VIS_STATE_CLEAR;
    as_add(ctrlParam.offset("visState"),reg_im(VIS_STATE_CLEAR),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // ctrlParam.clearAddr = SPRITE_ADDRESS
    lib_set_im32(SPRITE_ADDRESS);
    as_add(ctrlParam.offset("clearAddr"),SP_REG_MVI,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    label("f_visualizer_L_0");

    // if (ctrlParam.visState == VIS_STATE_DRAW) goto "f_visualizer_L_1"
    as_ceq(ctrlParam.offset("visState"),reg_im(VIS_STATE_DRAW), AM_OFFSET,AM_REG);
    lib_bc("f_visualizer_L_1");
    // Clear frame buffer
    // if (ctrlParam.clearAddr == VIS_CLEAR_END_ADDR) ctrlParam.visState = VIS_STATE_DRAW; goto "f_visualizer_L_return"
    lib_set_im32(VIS_CLEAR_END_ADDR);
    as_ceq(ctrlParam.offset("clearAddr"),SP_REG_MVI, AM_OFFSET,AM_REG);
    as_mv(ctrlParam.offset("visState"),reg_im(VIS_STATE_DRAW), AM_OFFSET,AM_REG);
    lib_bc("f_visualizer_L_return");
    as_add(R6,ctrlParam.offset("clearAddr"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    lib_set_im32(VIS_CLEAR_SIZE);
    as_add(R7,SP_REG_MVI,reg_im(0), AM_REG,AM_REG,AM_REG);
    as_add(R8,reg_im(0),reg_im(0), AM_REG,AM_REG,AM_REG);
    lib_call("f_memory_fill");
    as_nop(RctrlParam,RctrlParam,RctrlParam, AM_SET,AM_SET,AM_SET);
    lib_set_im32(VIS_CLEAR_SIZE);
    as_add(ctrlParam.offset("clearAddr"),ctrlParam.offset("clearAddr"),SP_REG_MVI, AM_OFFSET,AM_OFFSET,AM_REG);
    // goto "f_visualizer_L_return"
    lib_ba("f_visualizer_L_return");

    label("f_visualizer_L_1");
    // RvisObjDrawPtr = ctrlParam.visObjDrawPtr;
    as_add(RvisObjDrawPtr,ctrlParam.offset("visObjDrawPtr"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    // RdxPtr = RvisObjDrawPtr + 4;
    as_add(RdxPtr,RvisObjDrawPtr,reg_im(4), AM_REG,AM_REG,AM_REG);
    lib_wait_reg2addr();
    as_nop(RvisObjDrawPtr,RvisObjDrawPtr,RdxPtr, AM_SET,AM_SET,AM_SET);
    // Rone = 1;
    as_add(Rone,reg_im(1),reg_im(0), AM_REG,AM_REG,AM_REG);
    // Ri = 0;
    as_add(Ri,reg_im(0),reg_im(0), AM_REG,AM_REG,AM_REG);

    // loop(4)
    lib_loop_start("f_visualizer_Loop_0", 3, LOOP_MODE_IM);
    // {
    //   Rx = *RvisObjDrawPtr + *RdxPtr; a_addr++; b_addr++;
    as_add(Rx,Rone,Rone, AM_REG,AM_INC,AM_INC);
    //   if (Rx < 0) Rx = 0;
    as_cgta(reg_im(0),Rx, AM_REG,AM_REG);
    as_mv(Rx,reg_im(0), AM_REG,AM_REG);
    //   if (Ri == 0) Rwidth = FB_WIDTH - 1 else Rwidth = FB_HEIGHT - 1;
    lib_set_im32(FB_HEIGHT - 1);
    lib_simple_mv(Rwidth, SP_REG_MVI);
    lib_set_im32(FB_WIDTH - 1);
    as_ceq(Ri,reg_im(0), AM_REG,AM_REG);
    as_mv(Rwidth,SP_REG_MVI, AM_REG,AM_REG);
    //   if (Rx > Rwidth) Rx = Rwidth;
    as_cgta(Rx,Rwidth, AM_REG,AM_REG);
    as_mv(Rx,Rwidth, AM_REG,AM_REG);
    // *RvisObjDrawPtr = Rx; d_addr++
    as_add(Rone,Rx,reg_im(0), AM_INC,AM_REG,AM_REG);
    //   Ri ^= 1;
    as_xor(Ri,Ri,reg_im(1), AM_REG,AM_REG,AM_REG);
    // }
    lib_loop_end("f_visualizer_Loop_0");

    // Rcolor = (RvisObjDrawPtr >> 3) & 7
    as_sr(Rcolor,RvisObjDrawPtr,reg_im(3), AM_REG,AM_REG,AM_REG);
    as_and(Rcolor,Rcolor,reg_im(7), AM_REG,AM_REG,AM_REG);
    // if (Rcolor == 0) Rcolor = 7
    as_ceq(Rcolor,reg_im(0), AM_REG,AM_REG);
    as_mv(Rcolor,reg_im(7), AM_REG,AM_REG);

    // draw line
    // Rldataaddr = addr_abs("d_line_data")
    lib_set_im32(addr_abs("d_line_data"));
    lib_simple_mv(Rldataaddr, SP_REG_MVI);
    lib_wait_reg2addr();
    // d_addr = Rldataaddr; a,b_addr = RvisObjDrawPtr;
    as_nop(Rldataaddr,RvisObjDrawPtr,RvisObjDrawPtr, AM_SET,AM_SET,AM_SET);
    // store x0,y0,x1,y1
    // *Rldataaddr = *RvisObjDrawPtr; d_addr++; a_addr++;
    as_add(Rone,Rone,reg_im(0), AM_INC,AM_INC,AM_REG);
    as_add(Rone,Rone,reg_im(0), AM_INC,AM_INC,AM_REG);
    as_add(Rone,Rone,reg_im(0), AM_INC,AM_INC,AM_REG);
    as_add(Rone,Rone,reg_im(0), AM_INC,AM_INC,AM_REG);
    // store color
    // *Rldataaddr = Rcolor; d_addr++;
    as_add(Rone,Rcolor,reg_im(0), AM_INC,AM_REG,AM_REG);
    // R6 = addr_abs("d_line_data")
    lib_set_im32(addr_abs("d_line_data"));
    lib_simple_mv(R6, SP_REG_MVI);
    // call f_line
    lib_call("f_line");

    // Rone = 1
    as_add(Rone,reg_im(1),reg_im(0), AM_REG,AM_REG,AM_REG);
    // d_addr = Rldataaddr; a,b_addr = RvisObjDrawPtr;
    as_nop(Rldataaddr,RvisObjDrawPtr,RvisObjDrawPtr, AM_SET,AM_SET,AM_SET);
    // store FB_WIDTH-1-x0,y0,FB_WIDTH-1-x1,y1
    // *Rldataaddr = *RvisObjDrawPtr; d_addr++; a_addr++;
    lib_set_im(FB_WIDTH-1);
    as_add(Rt1,Rone,reg_im(0), AM_REG,AM_INC,AM_REG);
    as_sub(Rone,SP_REG_MVI,Rt1, AM_INC,AM_REG,AM_REG);
    as_add(Rone,Rone,reg_im(0), AM_INC,AM_INC,AM_REG);
    as_add(Rt1,Rone,reg_im(0), AM_REG,AM_INC,AM_REG);
    as_sub(Rone,SP_REG_MVI,Rt1, AM_INC,AM_REG,AM_REG);
    as_add(Rone,Rone,reg_im(0), AM_INC,AM_INC,AM_REG);
    // store color
    // *Rldataaddr = Rcolor; d_addr++;
    as_add(Rone,Rcolor,reg_im(0), AM_INC,AM_REG,AM_REG);
    // R6 = addr_abs("d_line_data")
    lib_set_im32(addr_abs("d_line_data"));
    lib_simple_mv(R6, SP_REG_MVI);
    // call f_line
    lib_call("f_line");

    // RvisObjDrawPtr += 8;
    as_add(RvisObjDrawPtr,RvisObjDrawPtr,reg_im(8), AM_REG,AM_REG,AM_REG);
    // if (RvisObjDrawPtr == &d_visobj_data + VIS_OBJ_SIZE) RvisObjDrawPtr = &d_visobj_data; ctrlParam.visState = VIS_STATE_WAIT_VSYNC
    lib_set_im32(addr_abs("d_visobj_data"));
    lib_simple_mv(Rt1, SP_REG_MVI);
    lib_set_im32(VIS_OBJ_SIZE);
    as_add(Rt1,Rt1,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    as_ceq(RvisObjDrawPtr,Rt1, AM_REG,AM_REG);
    lib_set_im32(addr_abs("d_visobj_data"));
    as_mv(RvisObjDrawPtr,SP_REG_MVI, AM_REG,AM_REG);
    as_nop(RctrlParam,RctrlParam,RctrlParam, AM_SET,AM_SET,AM_SET);
    as_mv(ctrlParam.offset("visState"),reg_im(VIS_STATE_WAIT_VSYNC), AM_OFFSET,AM_REG);
    // ctrlParam.visObjDrawPtr = RvisObjDrawPtr;
    as_add(ctrlParam.offset("visObjDrawPtr"),RvisObjDrawPtr,reg_im(0), AM_OFFSET,AM_REG,AM_REG);

    label("f_visualizer_L_return");
    lib_pop_regs(R14, R12);
    lib_pop(SP_REG_LINK);
    lib_return();
  }

  private void f_visualizer_add()
  {
    int Rt1 = R6;
    int Rt2 = R7;
    int Rt3 = R8;
    int Rt4 = R9;
    int Rdata = R8;
    int RdirAddr = R9;
    int RvisObjPtr = R13;
    int RctrlParam = R14;
    label("f_visualizer_add");
    lib_push(SP_REG_LINK);

    // RctrlParam = &d_ctrl_param;
    lib_set_im32(addr_struct("d_ctrl_param"));
    lib_simple_mv(RctrlParam, SP_REG_MVI);
    lib_wait_reg2addr();

    // Rdata = rand() & 0x3f3f3f3f; (random y1,x1,y0,x0)
    lib_call("f_rand");
    as_nop(RctrlParam,RctrlParam,RctrlParam, AM_SET,AM_SET,AM_SET);
    lib_set_im32(0x3f3f3f3f);
    as_and(Rdata,R6,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    // loop(4)
    // {
    //   *visObjPtr = (char)(Rdata & 0xff);
    //   Rdata = Rdata >> 8;
    //   *vidObjPtr++;
    // }
    // RvisObjPtr += 4
    as_add(RvisObjPtr,ctrlParam.offset("visObjPtr"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    lib_wait_reg2addr();
    as_nop(RvisObjPtr,0,0, AM_SET,AM_REG,AM_REG);
    as_add(Rt1,reg_im(1),reg_im(0), AM_REG,AM_REG,AM_REG);
    lib_set_im(0xff);
    as_add(Rt2,SP_REG_MVI,reg_im(0), AM_REG,AM_REG,AM_REG);
    lib_loop_start("f_visualizer_add_Loop_0", 3, LOOP_MODE_IM);
    as_and(Rt4,Rdata,Rt2, AM_REG,AM_REG,AM_REG);
    as_add(Rt1,Rt4,reg_im(0), AM_INC,AM_REG,AM_REG);
    as_sr(Rdata,Rdata,reg_im(8), AM_REG,AM_REG,AM_REG);
    lib_loop_end("f_visualizer_add_Loop_0");
    as_add(RvisObjPtr,RvisObjPtr,reg_im(4), AM_REG,AM_REG,AM_REG);
    // loop(1)
    // {
    //   Rt2 = rand()
    //   Rt2 = (Rt2 & 7) << 1;
    //   RdirAddr = &direction_table[Rt2];
    //   *visObjPtr = *RdirAddr; d_addr++; a_addr++;
    //   *visObjPtr = *RdirAddr; d_addr++; a_addr++;
    //   RvisObjPtr += 2;
    // }
    lib_loop_start("f_visualizer_add_Loop_1", 1, LOOP_MODE_IM);
    lib_call("f_rand");
    as_and(Rt2,R6,reg_im(7), AM_REG,AM_REG,AM_REG);
    as_sl(Rt2,Rt2,reg_im(1), AM_REG,AM_REG,AM_REG);
    lib_set_im32(addr_abs("d_direction_table"));
    as_add(Rt2,Rt2,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    as_add(Rt1,reg_im(1),reg_im(0), AM_REG,AM_REG,AM_REG);
    lib_wait_reg2addr();
    as_nop(RvisObjPtr,Rt2,0, AM_SET,AM_SET,AM_REG);
    as_add(Rt1,Rt1,reg_im(0), AM_INC,AM_INC,AM_REG);
    as_add(Rt1,Rt1,reg_im(0), AM_INC,AM_INC,AM_REG);
    as_add(RvisObjPtr,RvisObjPtr,reg_im(2), AM_REG,AM_REG,AM_REG);
    lib_loop_end("f_visualizer_add_Loop_1");

    // if (RvisObjPtr == addr_abs("d_visobj_data") + VIS_OBJ_SIZE) RvisObjPtr = addr_abs("d_visobj_data");
    lib_set_im32(VIS_OBJ_SIZE);
    as_add(Rt1,SP_REG_MVI,reg_im(0), AM_REG,AM_REG,AM_REG);
    lib_set_im32(addr_abs("d_visobj_data"));
    as_add(Rt1,Rt1,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    as_ceq(RvisObjPtr,Rt1, AM_REG,AM_REG);
    lib_set_im32(addr_abs("d_visobj_data"));
    as_mv(RvisObjPtr,SP_REG_MVI, AM_REG,AM_REG);
    // ctrlParam.visObjPtr = RvisObjPtr;
    as_nop(RctrlParam,RctrlParam,RctrlParam, AM_SET,AM_SET,AM_SET);
    as_add(ctrlParam.offset("visObjPtr"),RvisObjPtr,reg_im(0), AM_OFFSET,AM_REG,AM_REG);

    lib_pop(SP_REG_LINK);
    lib_return();
  }

  @Override
  public void init()
  {
    set_rom_depth(CODE_ROM_DEPTH, DATA_ROM_DEPTH);
    set_stack_address((1 << DATA_ROM_DEPTH) - 1);
    spriteLineVoffsetInBits = 7;
    set_filename("mini_synth");

    ctrlParam.clear();
    ctrlParam.add("mixL", 0);
    ctrlParam.add("mixR", 0);
    ctrlParam.add("audioValid", 0);
    ctrlParam.add("tempo", 6400);
    ctrlParam.add("tempoCount", 0);
    ctrlParam.add("seqPtr", 0);
    ctrlParam.add("beat", 0);
    ctrlParam.add("bar", 0);
    ctrlParam.add("chord", 0);
    ctrlParam.add("addFrequency", 32);
    ctrlParam.add("vsyncState", FALSE);
    ctrlParam.add("visState", VIS_STATE_CLEAR);
    ctrlParam.add("visObjPtr", 0);
    ctrlParam.add("visObjDrawPtr", 0);
    ctrlParam.add("clearAddr", 0);

    oscParam.clear();
    oscParam.add("pitch", 0);
    oscParam.add("count", 0);
    oscParam.add("levelL", 0);
    oscParam.add("levelR", 0);
  }

  @Override
  public void program()
  {
    lib_init_stack();
    lib_call("f_synth_init");
    lib_call("f_visualizer_init");

    label("L_main_render");
    lib_call("f_synth_render");
    lib_call("f_visualizer");
    lib_ba("L_main_render");

    // link library
    f_synth_init();
    f_synth_render();
    f_synth_sequencer();
    f_visualizer_init();
    f_visualizer();
    f_visualizer_add();
    f_rand();
    f_memory_fill();
    f_line();
  }

  @Override
  public void data()
  {
    label("d_ctrl_param");
    store_struct(ctrlParam);

    label("d_osc_param");
    for (int i = 0; i < CH; i++)
    {
      store_struct(oscParam);
    }

    label("d_seq_data");
    for (int i = 0; i < BEATS; i++)
    {
      for (int j = 0; j < CH; j++)
      {
        // note[31:16] oct[15:0]
        d32(0);
      }
    }

    label("d_scale_table");
    for (int i = 0; i < SCALE_TABLE_SIZE; i++)
    {
      d32((int)(Math.floor(Math.pow(2.0, i / 12.0) * 523.2511306011972 * (0x100000000L / SAMPLE_RATE))));
    }

    label("d_chord_table");
    d32x4(0, 2, 4, 7);
    d32x4(0, 2, 4, 7);
    d32x4(0, 2, 5, 9);
    d32x4(2, 4, 7, 11);
    d32x4(0, 5, 7, 9);
    d32x4(2, 7, 9, 11);
    d32x4(0, 4, 7, 9);
    d32x4(2, 5, 7, 11);

    label("d_visobj_data");
    for (int i = 0; i < VIS_OBJ_SIZE; i++)
    {
      // x0, y0, x1, y1, dx0, dy0, dx1, dy1
      d32(0);
    }

    label("d_direction_table");
    d32(0); d32(-1); // dx=0, dy=-1
    d32(1); d32(-1); // dx=1, dy=-1
    d32(1); d32(0); // dx=1, dy=0
    d32(1); d32(1); // dx=1, dy=1
    d32(0); d32(1); // dx=0, dy=1
    d32(-1); d32(1); // dx=-1, dy=1
    d32(-1); d32(0); // dx=-1, dy=0
    d32(-1); d32(-1); // dx=-1, dy=-1

    label("d_line_data");
    for (int i = 0; i < 4; i++)
    {
      d32(0); d32(0); d32(0); d32(0); d32(0);
    }

    label("d_rand");
    d32(0x92d68ca1);
  }
}
