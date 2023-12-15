/*
  Copyright (c) 2017 miya
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

import java.lang.Math;

public class Synth extends AsmLib
{
  private static final int TRUE = 1; // for program
  private static final int FALSE = 0;
  private static final int CODE_ROM_DEPTH = 12;
  private static final int DATA_ROM_DEPTH = 12;
  private static final int AUDIO_DIVIDER = 374;

  private static final int STATE_ATTACK = 0;
  private static final int STATE_DECAY = 1;
  private static final int STATE_SUSTAIN = 2;
  private static final int STATE_RELEASE = 3;
  private static final int STATE_SLEEP = 4;

  private static final int ENVS = 4;
  private static final int OSCS = 4;
  private static final int FIXED_BITS = 14;
  private static final int FIXED_BITS_ENV = 8;
  private static final int ENV_VALUE_MAX = (1 << FIXED_BITS << FIXED_BITS_ENV);
  private static final int INT_BITS = 32;
  private static final int WAVE_TABLE_BITS = 8;
  private static final int WAVE_TABLE_SIZE = (1 << WAVE_TABLE_BITS);
  private static final int WAVE_TABLE_SIZE_M1 = (WAVE_TABLE_SIZE - 1);
  private static final int WAVE_ADDR_SHIFT = (INT_BITS - WAVE_TABLE_BITS);
  private static final int WAVE_ADDR_SHIFT_M = (WAVE_ADDR_SHIFT - FIXED_BITS);
  private static final int FIXED_SCALE = (1 << FIXED_BITS);
  private static final int FIXED_SCALE_M1 = (FIXED_SCALE - 1);
  private static final int MOD_LEVEL_MAX = ((int)(FIXED_SCALE * 0.52));
  private static final int SCALE_TABLE_SIZE = 16;
  private static final int SAMPLE_RATE = 48000;
  ///private static final int SAMPLE_RATE = 44100;
  private static final int SEQ_CH = 4;
  private static final int SEQ_LENGTH = 16;
  private static final int SEQ_INIT_NUM = 20;
  private static final int MAX_CHORD_LENGTH = 16;
  private static final int MAX_CHORD = 16;
  private static final int CHORD_LENGTH = 3;
  private static final int VIS_MAX_X = 40;
  private static final int VIS_MAX_Y = 30;
  /// for kv260
  ///private static final int VIS_MAX_Y = 22;
  private static final int VIS_NEXT_LINE = 64;
  private static final int DEFAULT_VOLUME = (int)(FIXED_SCALE / ((SEQ_CH / 2.0) + 2.0));

  private StructHelper envParam = new StructHelper();
  private StructHelper oscParam = new StructHelper();
  private StructHelper ctrlParam = new StructHelper();
  private StructHelper noteParam = new StructHelper();
  private StructHelper seqParam = new StructHelper();
  private StructHelper visParam = new StructHelper();

  // get seq_data address
  // return seq_data[beat][ch]
  // input: r6:beat r7:ch
  // output: r6:seq_data[beat][ch] address
  // modify: r6,r7
  private void m_get_seq_data_addr()
  {
    // return ("d_seq_data" + (SEQ_CH * beat + ch) * noteParam.size())
    lib_set_im(SEQ_CH);
    as_mul(R6,R6,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    as_add(R6,R6,R7, AM_REG,AM_REG,AM_REG);
    lib_set_im(noteParam.size());
    as_mul(R6,R6,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    lib_set_im32(addr_struct("d_seq_data"));
    as_add(R6,R6,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
  }

  // d_addr = &seq_data[nrand(length)][nrand(ch)]
  // input: none
  // output: none
  // modify: r6~r9, d_addr, a_addr
  private void m_set_seq_random_addr()
  {
    // nrand(seqParam.ch);
    as_add(R6,seqParam.offset("ch"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    lib_call("f_nrand");
    as_nop(R12,R12,R12, AM_SET,AM_SET,AM_SET);
    lib_simple_mv(R9, R6);
    // nrand(seqParam.length);
    as_add(R6,seqParam.offset("length"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    lib_call("f_nrand");
    as_nop(R12,R12,R12, AM_SET,AM_SET,AM_SET);
    lib_simple_mv(R7, R9);
    m_get_seq_data_addr();
    lib_wait_reg2addr();
    as_nop(R6,0,0, AM_SET,AM_REG,AM_REG);
  }

  // s = &seq_data[nrand(length)][nrand(ch)]
  // s.note = nrand(seqParam.CHORD_LENGTH);
  // s.oct = nrand(seqParam.octRange) + seqParam.octMin;
  // input: r12:addr_struct("d_seq_param")
  // output: none
  // modify: r6~r11, d_addr, a_addr
  private void m_seq_set_random_data()
  {
    as_nop(R12,R12,R12, AM_SET,AM_SET,AM_SET);
    //   r10=nrand(seqParam.CHORD_LENGTH);
    as_add(R6,seqParam.offset("CHORD_LENGTH"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    lib_call("f_nrand");
    lib_simple_mv(R10, R6);
    as_nop(R12,R12,R12, AM_SET,AM_SET,AM_SET);
    //   r11=nrand(seqParam.octRange) + seqParam.octMin;
    as_add(R6,seqParam.offset("octRange"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    lib_call("f_nrand");
    lib_simple_mv(R11, R6);
    as_nop(R12,R12,R12, AM_SET,AM_SET,AM_SET);
    as_add(R11,R11,seqParam.offset("octMin"), AM_REG,AM_REG,AM_OFFSET);
    //   ch = nrand(seqParam.ch);
    //   be = nrand(seqParam.length);
    //   seqData[be][ch].note = r10
    //   seqData[be][ch].oct = r11
    m_set_seq_random_addr();
    as_add(noteParam.offset("note"),R10,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    as_add(noteParam.offset("oct"),R11,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
  }

  private void m_sequencer()
  {
    label("m_sequencer");
    // r12 = addr_struct("d_seq_param");
    // r13 = addr_struct("d_env_param");
    // r14 = addr_struct("d_osc_param");
    // d,a,b addr = addr_struct("d_seq_param");
    lib_set_im32(addr_struct("d_seq_param"));
    lib_simple_mv(R12, SP_REG_MVI);
    lib_set_im32(addr_struct("d_env_param"));
    lib_simple_mv(R13, SP_REG_MVI);
    lib_set_im32(addr_struct("d_osc_param"));
    lib_simple_mv(R14, SP_REG_MVI);
    as_nop(R12,R12,R12, AM_SET,AM_SET,AM_SET);
    // if (seqParam.callbackCounter != 0) goto m_sequencer_L_end
    as_ceq(seqParam.offset("callbackCounter"), reg_im(0), AM_OFFSET,AM_REG);
    as_not(SP_REG_CP,SP_REG_CP, AM_REG,AM_REG);
    lib_bc("m_sequencer_L_end");

    /*
    ### SEQ_CH = OSC_CH = ENV_CH ###
    seqParam.led = 0;
    seqParam.currentCh = 0;
    for (i = 0; i < seqParam.ch; i++)
    {
      oct = d_seq_data[seqParam.beat].oct;
      note = d_seq_data[seqParam.beat].note;

      if (oct == 0)
      {
        if (envParam.state < STATE_RELEASE)
        {
          envParam.state = STATE_RELEASE;
        }
      }
      else
      {
        note2 = chordData[chord][note];
        oscParam.pitch = scaleTable[note2] << oct;
        envParam.state = STATE_ATTACK;
        seqParam.led = seqParam.led | (1 << seqParam.currentCh);
      }

      seqParam.beat += seqParam.sizeOfNoteParam;
      oscParam++;
      envParam++;
      seqParam.currentCh++;
    }
    */
    as_add(seqParam.offset("led"),reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    as_add(seqParam.offset("currentCh"),reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    as_sub(R9,seqParam.offset("ch"),reg_im(1), AM_REG,AM_OFFSET,AM_REG);
    lib_loop_start("loop_m_sequencer_0", R9, LOOP_MODE_REG);
    as_add(R9,seqParam.offset("beat"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    lib_wait_reg2addr();
    as_nop(0,0,R9, AM_REG,AM_REG,AM_SET);
    as_add(R9,reg_im(0),noteParam.offset("oct"), AM_REG,AM_REG,AM_OFFSET);
    as_add(R10,reg_im(0),noteParam.offset("note"), AM_REG,AM_REG,AM_OFFSET);
    // if (oct == 0)
    as_ceq(R9,reg_im(0), AM_REG,AM_REG);
    lib_bc("m_sequencer_L_0");
    // else
    // note2 = chordData[chord][note];
    lib_set_im(MAX_CHORD_LENGTH);
    as_mul(R6,seqParam.offset("chord"),SP_REG_MVI, AM_REG,AM_OFFSET,AM_REG);
    as_add(R6,R6,R10, AM_REG,AM_REG,AM_REG);
    lib_set_im32(addr_abs("d_chord_data"));
    as_add(R6,R6,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    lib_wait_reg2addr();
    as_add(R10,R6,reg_im(0), AM_REG,AM_SET,AM_REG);
    lib_set_im32(addr_abs("d_scale_table"));
    as_add(R11,SP_REG_MVI,R10, AM_REG,AM_REG,AM_REG);
    as_nop(R14,0,0, AM_SET,AM_REG,AM_REG);
    lib_wait_reg2addr();
    as_sl(oscParam.offset("pitch"),R11,R9, AM_OFFSET,AM_SET,AM_REG);
    as_nop(R13,0,0, AM_SET,AM_REG,AM_REG);
    as_add(envParam.offset("state"),reg_im(STATE_ATTACK),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // seqParam.led = seqParam.led | (1 << seqParam.currentCh);
    as_nop(R12,R12,R12, AM_SET,AM_SET,AM_SET);
    as_sl(R6,reg_im(1),seqParam.offset("currentCh"), AM_REG,AM_REG,AM_OFFSET);
    as_or(seqParam.offset("led"),seqParam.offset("led"),R6, AM_OFFSET,AM_OFFSET,AM_REG);
    lib_ba("m_sequencer_L_1");
    label("m_sequencer_L_0");
    // oct == 0
    as_nop(R13,R13,R13, AM_SET,AM_SET,AM_SET);
    as_cgt(reg_im(STATE_RELEASE),envParam.offset("state"), AM_REG,AM_OFFSET);
    as_mv(envParam.offset("state"),reg_im(STATE_RELEASE), AM_OFFSET,AM_REG);
    label("m_sequencer_L_1");
    as_nop(R12,R12,R12, AM_SET,AM_SET,AM_SET);
    as_add(seqParam.offset("beat"),seqParam.offset("beat"),seqParam.offset("sizeOfNoteParam"), AM_OFFSET,AM_OFFSET,AM_OFFSET);
    // envParam++;
    as_mvi(envParam.size());
    as_add(R13,R13,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    // oscParam++;
    as_mvi(oscParam.size());
    as_add(R14,R14,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    // seqParam.currentCh++
    as_add(seqParam.offset("currentCh"),seqParam.offset("currentCh"),reg_im(1), AM_OFFSET,AM_OFFSET,AM_REG);
    lib_loop_end("loop_m_sequencer_0");

    // *LED_ADDRESS = seqParam.led;
    lib_set_im32(LED_ADDRESS);
    lib_wait_reg2addr();
    as_add(SP_REG_MVI,seqParam.offset("led"),reg_im(0), AM_SET,AM_OFFSET,AM_REG);
    as_nop(R12,R12,R12, AM_SET,AM_SET,AM_SET);

    /*
    if (seqParam.beat < seqParam.seqDataEnd)
    {
    }
    else
    {
      seqParam.beat = seqParam.seqDataStart;
      seqParam.barCounter++;
      if (seqParam.barCounter >= seqParam.barRepeat)
      {
        seqParam.barCounter = 0;
        seqParam.deleteCounter = nrand(seqParam.deleteFrequency);
        chord = nrand(progressionData[chord][1]) + progressionData[chord][0];
        if (toneChange == TRUE)
        {
          for (i = 0; i < seqParam.ch; i++)
          {
            envParam[i].envelopeDiffA = ENV_VALUE_MAX >> (nrand(9) + 4);
            oscParam[i].modLevel0 = MOD_LEVEL_MAX * nrand(6);
          }
        }
      }
      for (i = 0; i < seqParam.deleteCounter+1; i++)
      {
        seqData[nrand(seqLength)][nrand(seqCh)].oct = 0;
      }
      for (i = 0; i < seqParam.addFrequency+1; i++)
      {
        int ch = nrand(seqParam.ch);
        int be = nrand(seqParam.length);
        seqData[be][ch].note = nrand(seqParam.CHORD_LENGTH);
        seqData[be][ch].oct = nrand(seqParam.octRange) + seqParam.octMin;
      }
    }
    */

    as_cgt(seqParam.offset("seqDataEnd"),seqParam.offset("beat"), AM_OFFSET,AM_OFFSET);
    lib_bc("m_sequencer_L_2");
    as_add(seqParam.offset("beat"),seqParam.offset("seqDataStart"),reg_im(0), AM_OFFSET,AM_OFFSET,AM_REG);

    // seqParam.barCounter++;
    as_add(seqParam.offset("barCounter"),seqParam.offset("barCounter"),reg_im(1), AM_OFFSET,AM_OFFSET,AM_REG);
    lib_wait_dependency();
    // if (seqParam.barCounter >= seqParam.barRepeat)
    as_cgt(seqParam.offset("barRepeat"),seqParam.offset("barCounter"), AM_OFFSET,AM_OFFSET);
    lib_bc("m_sequencer_L_3");
    // seqParam.barCounter = 0;
    as_add(seqParam.offset("barCounter"),reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // seqParam.deleteCounter = nrand(seqParam.deleteFrequency);
    as_add(R6,seqParam.offset("deleteFrequency"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    lib_call("f_nrand");
    as_nop(R12,R12,R12, AM_SET,AM_SET,AM_SET);
    as_add(seqParam.offset("deleteCounter"),R6,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // chord = nrand(progressionData[chord][1]) + progressionData[chord][0];
    //   r6=&progressionData[chord][0]
    as_mul(R6,seqParam.offset("chord"),reg_im(2), AM_REG,AM_OFFSET,AM_REG);
    lib_set_im32(addr_abs("d_progression_data"));
    as_add(R6,R6,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    lib_wait_reg2addr();
    as_nop(R6,R6,R6, AM_SET,AM_SET,AM_SET);
    //   r11=progressionData[chord][0]
    as_add(R11,0,reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    //   r6=progressionData[chord][1]
    as_add(R6,1,reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    //   r6=nrand(progressionData[chord][1])
    lib_call("f_nrand");
    as_nop(R12,R12,R12, AM_SET,AM_SET,AM_SET);
    as_add(seqParam.offset("chord"),R6,R11, AM_OFFSET,AM_REG,AM_REG);
    // if (toneChange == TRUE)
    as_cgt(reg_im(TRUE),seqParam.offset("toneChange"), AM_REG,AM_OFFSET);
    lib_bc("m_sequencer_L_4");
    lib_set_im32(addr_struct("d_ctrl_param"));
    lib_simple_mv(R11, SP_REG_MVI);
    lib_set_im32(addr_struct("d_env_param"));
    lib_simple_mv(R13, SP_REG_MVI);
    lib_set_im32(addr_struct("d_osc_param"));
    lib_simple_mv(R14, SP_REG_MVI);
    // for (i = 0; i < seqParam.ch; i++)
    as_sub(R6,seqParam.offset("ch"),reg_im(1), AM_REG,AM_OFFSET,AM_REG);
    lib_loop_start("loop_m_sequencer_1", R6, LOOP_MODE_REG);
    // envParam[i].envelopeDiffA = ENV_VALUE_MAX >> (nrand(9) + 4);
    //   nrand(9)
    lib_set(R6, 9);
    lib_call("f_nrand");
    //   (nrand(9) + 4)
    as_add(R6,R6,reg_im(4), AM_REG,AM_REG,AM_REG);
    as_nop(R13,R11,R12, AM_SET,AM_SET,AM_SET);
    as_sr(envParam.offset("envelopeDiffA"),ctrlParam.offset("ENV_VALUE_MAX"),R6, AM_OFFSET,AM_OFFSET,AM_REG);
    // oscParam[i].modLevel0 = MOD_LEVEL_MAX * nrand(6);
    //   r6=nrand(6)
    lib_set(R6, 6);
    lib_call("f_nrand");
    as_nop(R14,R11,R12, AM_SET,AM_SET,AM_SET);
    as_mul(oscParam.offset("modLevel0"),ctrlParam.offset("MOD_LEVEL_MAX"),R6, AM_OFFSET,AM_OFFSET,AM_REG);
    // envParam++;
    as_mvi(envParam.size());
    as_add(R13,R13,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    // oscParam++;
    as_mvi(oscParam.size());
    as_add(R14,R14,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    lib_loop_end("loop_m_sequencer_1");
    as_nop(R12,R12,R12, AM_SET,AM_SET,AM_SET);
    label("m_sequencer_L_4");
    label("m_sequencer_L_3");
    // for (i = 0; i < seqParam.deleteCounter+1; i++)
    as_sub(R6,seqParam.offset("deleteCounter"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    lib_loop_start("loop_m_sequencer_2", R6, LOOP_MODE_REG);
    // seqData[nrand(seqLength)][nrand(seqCh)].oct = 0;
    m_set_seq_random_addr();
    as_add(noteParam.offset("oct"),reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    lib_loop_end("loop_m_sequencer_2");
    as_nop(R12,R12,R12, AM_SET,AM_SET,AM_SET);

    // for (i = 0; i < seqParam.addFrequency+1; i++)
    as_sub(R6,seqParam.offset("addFrequency"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    lib_loop_start("loop_m_sequencer_3", R6, LOOP_MODE_REG);
    m_seq_set_random_data();
    lib_loop_end("loop_m_sequencer_3");
    as_nop(R12,R12,R12, AM_SET,AM_SET,AM_SET);
    label("m_sequencer_L_2");
    label("m_sequencer_L_end");
    // seqParam.callbackCounter++
    as_add(R6,seqParam.offset("callbackCounter"),reg_im(1), AM_REG,AM_OFFSET,AM_REG);
    // if (seqParam.callbackCounter == seqParam.callbackRate) {seqParam.callbackCounter = 0;}
    as_ceq(R6,seqParam.offset("callbackRate"), AM_REG,AM_OFFSET);
    as_mv(R6,reg_im(0), AM_REG,AM_REG);
    as_add(seqParam.offset("callbackCounter"),R6,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
  }

  private void m_envelope_generator()
  {
    // input: r6:envParam address
    // modify: r6 ~ r11, d,a,b_addr
    label("m_envelope_generator");

    // r9: addr_struct("d_env_param")
    // r10: addr_abs("d_switch_addr")
    // r11: sizeOfEnvParam

    /*
      loop(ENVS)
    */
    // r9 = addr_struct("d_env_param");
    lib_set_im32(addr_struct("d_env_param"));
    lib_simple_mv(R9, SP_REG_MVI);
    // r10 = addr_abs("d_switch_addr");
    lib_set_im32(addr_abs("d_switch_addr"));
    lib_simple_mv(R10, SP_REG_MVI);
    // r6 = ctrlParam.ENVS - 1;
    lib_set_im32(addr_struct("d_ctrl_param"));
    lib_wait_reg2addr();
    as_nop(SP_REG_MVI,SP_REG_MVI,SP_REG_MVI, AM_SET,AM_SET,AM_SET);
    as_sub(R6,ctrlParam.offset("ENVS"),reg_im(1), AM_REG,AM_OFFSET,AM_REG);
    // r11 = ctrlParam.sizeOfEnvParam;
    as_add(R11,ctrlParam.offset("sizeOfEnvParam"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);

    lib_loop_start("loop_m_envelope_generator_0", R6, LOOP_MODE_REG);

    // set struct pointer
    // d_addr = r9; a_addr = r9; b_addr = r10;
    as_nop(R9,R9,R10, AM_SET,AM_SET,AM_SET);

    /*
      switch (envParam.state)
      {
        case STATE_ATTACK: goto L_envelope_generator_s_a; break;
        case STATE_DECAY: goto L_envelope_generator_s_d; break;
        case STATE_SUSTAIN: goto L_envelope_generator_s_s; break;
        case STATE_RELEASE: goto L_envelope_generator_s_r; break;
        case STATE_SLEEP: goto L_envelope_generator_s_sl; break;
      }
    */
    as_add(R6,envParam.offset("state"),R10, AM_REG,AM_OFFSET,AM_REG);
    lib_wait_reg2addr();
    as_add(SP_REG_BA,R6,reg_im(0), AM_REG,AM_SET,AM_REG);
    // d_addr = r9; a_addr = r9; b_addr = r10;
    as_nop(R9,R9,R9, AM_SET,AM_SET,AM_SET);
    as_ba();
    lib_wait_delay_slot();

    /*
    case STATE_ATTACK:
    {
      envParams[i].currentLevel += envParams[i].envelopeDiffA;
      if (envParams[i].currentLevel > envParams[i].envelopeLevelA)
      {
        envParams[i].currentLevel = envParams[i].envelopeLevelA;
        envParams[i].state = STATE_DECAY;
      }
      break;
    }
    */
    label("L_envelope_generator_s_a");
    as_add(R6,envParam.offset("currentLevel"),envParam.offset("envelopeDiffA"), AM_REG,AM_OFFSET,AM_OFFSET);
    as_cgta(R6,envParam.offset("envelopeLevelA"), AM_REG,AM_OFFSET);
    as_mv(R6,envParam.offset("envelopeLevelA"), AM_REG,AM_OFFSET);
    as_add(envParam.offset("currentLevel"),R6,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    as_mv(envParam.offset("state"),reg_im(STATE_DECAY), AM_OFFSET,AM_REG);
    lib_ba("L_envelope_generator_s_end");

    /*
    case STATE_DECAY:
    {
      envParams[i].currentLevel += envParams[i].envelopeDiffD;
      if (envParams[i].currentLevel < envParams[i].envelopeLevelS)
      {
        envParams[i].currentLevel = envParams[i].envelopeLevelS;
        envParams[i].state = STATE_SUSTAIN;
      }
      break;
    }
    */
    label("L_envelope_generator_s_d");
    as_add(R6,envParam.offset("currentLevel"),envParam.offset("envelopeDiffD"), AM_REG,AM_OFFSET,AM_OFFSET);
    as_cgta(envParam.offset("envelopeLevelS"),R6, AM_OFFSET,AM_REG);
    as_mv(R6,envParam.offset("envelopeLevelS"), AM_REG,AM_OFFSET);
    as_add(envParam.offset("currentLevel"),R6,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    as_mv(envParam.offset("state"),reg_im(STATE_SUSTAIN), AM_OFFSET,AM_REG);
    lib_ba("L_envelope_generator_s_end");

    /*
    case STATE_RELEASE:
    {
      envParams[i].currentLevel += envParams[i].envelopeDiffR;
      if (envParams[i].currentLevel < 0)
      {
        envParams[i].currentLevel = 0;
        envParams[i].state = STATE_SLEEP;
      }
      break;
    }
    */
    label("L_envelope_generator_s_r");
    as_add(R6,envParam.offset("currentLevel"),envParam.offset("envelopeDiffR"), AM_REG,AM_OFFSET,AM_OFFSET);
    as_cgta(reg_im(0),R6, AM_REG,AM_REG);
    as_mv(R6,reg_im(0), AM_REG,AM_REG);
    as_add(envParam.offset("currentLevel"),R6,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    as_mv(envParam.offset("state"),reg_im(STATE_SLEEP), AM_OFFSET,AM_REG);
    lib_ba("L_envelope_generator_s_end");

    /*
    default: break;
    */
    label("L_envelope_generator_s_s");
    label("L_envelope_generator_s_sl");
    label("L_envelope_generator_s_end");

    // set next envParam
    as_add(R9,R9,R11, AM_REG,AM_REG,AM_REG);
    lib_wait_reg2addr();
    lib_loop_end("loop_m_envelope_generator_0");
  }

  private void m_calc_wave()
  {
    // input: none
    // modify: r6 ~ r12, d,a,b_addr
    label("m_calc_wave");

    // r8:ctrlParam address
    lib_set_im32(addr_struct("d_ctrl_param"));
    lib_simple_mv(R8, SP_REG_MVI);
    // r6 = addr_struct("d_osc_param");
    lib_set_im32(addr_struct("d_osc_param"));
    lib_simple_mv(R6, SP_REG_MVI);
    // ctrlParam.mixL,mixR,mixRevL,mixRevR = 0
    as_nop(R8,R8,R8, AM_SET,AM_SET,AM_SET);
    as_add(ctrlParam.offset("mixL"),reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    as_add(ctrlParam.offset("mixR"),reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    as_add(ctrlParam.offset("mixRevL"),reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    as_add(ctrlParam.offset("mixRevR"),reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);

    /*
      loop(OSCS)
    */
    // r7 = ctrlParam.OSCS - 1;
    as_sub(R7,ctrlParam.offset("OSCS"),reg_im(1), AM_REG,AM_OFFSET,AM_REG);
    // r12 = ctrlParam.sizeOfOscParam;
    as_add(R12,ctrlParam.offset("sizeOfOscParam"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);

    lib_loop_start("loop_m_calc_wave_0", R7, LOOP_MODE_REG);
    // reserved r6,r8,r12

    // d:oscParam a:oscParam b:ctrlParam
    as_nop(R6,R6,R8, AM_SET,AM_SET,AM_SET);

    /*
    oscParam.level = *envPatchAddr;
    oscParam.mod0 = *modPatch0Addr;
    oscParam.mod1 = *modPatch1Addr;
    */
    as_add(R9,oscParam.offset("modPatch0Addr"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    as_add(R10,oscParam.offset("modPatch1Addr"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    as_add(R11,oscParam.offset("envPatchAddr"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    // lib_wait_reg2addr();
    as_add(oscParam.offset("mod0"),R9,reg_im(0), AM_OFFSET,AM_SET,AM_REG);
    as_add(oscParam.offset("mod1"),R10,reg_im(0), AM_OFFSET,AM_SET,AM_REG);
    as_add(oscParam.offset("level"),R11,reg_im(0), AM_OFFSET,AM_SET,AM_REG);
    // lib_wait_dependency();

    /*
    waveAddr = (params[i].count +
                (params[i].mod0 * params[i].modLevel0) +
                (params[i].mod1 * params[i].modLevel1)) >>> WAVE_ADDR_SHIFT_M;
    >>> : unsigned (in Java)
    */

    // d:oscParam a:oscParam b:oscParam
    as_nop(R6,R6,R6, AM_SET,AM_SET,AM_SET);
    as_mul(R9,oscParam.offset("mod0"),oscParam.offset("modLevel0"), AM_REG,AM_OFFSET,AM_OFFSET);
    as_mul(R10,oscParam.offset("mod1"),oscParam.offset("modLevel1"), AM_REG,AM_OFFSET,AM_OFFSET);
    as_add(R9,R9,R10, AM_REG,AM_REG,AM_REG);
    as_add(R9,R9,oscParam.offset("count"), AM_REG,AM_REG,AM_OFFSET);
    // d:oscParam a:oscParam b:ctrlParam
    as_nop(R6,R6,R8, AM_SET,AM_SET,AM_SET);
    // r9:waveAddr
    as_sr(R9,R9,ctrlParam.offset("WAVE_ADDR_SHIFT_M"), AM_REG,AM_REG,AM_OFFSET);

    /*
    waveAddrF = waveAddr >>> FIXED_BITS;
    waveAddrR = (waveAddrF + 1) & WAVE_TABLE_SIZE_M1;
    waveAddrM = waveAddr & FIXED_SCALE_M1;
    oscOutF = waveData[waveAddrF];
    oscOutR = waveData[waveAddrR];
    oscOut = ((oscOutF * (FIXED_SCALE - waveAddrM)) >> FIXED_BITS) + ((oscOutR * waveAddrM) >> FIXED_BITS);
    params[i].outData = (oscOut * (params[i].level >> FIXED_BITS_ENV)) >> FIXED_BITS;
    params[i].count += params[i].pitch;
    */

    // r10:waveAddrF
    as_sr(R10,R9,ctrlParam.offset("FIXED_BITS"), AM_REG,AM_REG,AM_OFFSET);
    as_add(R11,R10,reg_im(1), AM_REG,AM_REG,AM_REG);
    // r11:waveAddrR
    as_and(R11,R11,ctrlParam.offset("WAVE_TABLE_SIZE_M1"), AM_REG,AM_REG,AM_OFFSET);
    // r10:waveAddrF + addr_abs("d_wave_table")
    lib_set_im32(addr_abs("d_wave_table"));
    as_add(R10,R10,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    // r11:waveAddrR + addr_abs("d_wave_table")
    as_add(R11,R11,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    // r9:waveAddrM
    as_and(R9,R9,ctrlParam.offset("FIXED_SCALE_M1"), AM_REG,AM_REG,AM_OFFSET);
    // r10 = oscOutF = waveData[R10]
    as_add(R10,R10,reg_im(0), AM_REG,AM_SET,AM_REG);
    // r11 = oscOutR = waveData[R11]
    as_add(R11,R11,reg_im(0), AM_REG,AM_SET,AM_REG);
    // d:oscParam a:ctrlParam b:ctrlParam
    as_nop(R6,R8,R8, AM_SET,AM_SET,AM_SET);
    // FIXED_SCALE - waveAddrM
    as_sub(R7,ctrlParam.offset("FIXED_SCALE"),R9, AM_REG,AM_OFFSET,AM_REG);
    // * oscOutF
    as_mul(R7,R7,R10, AM_REG,AM_REG,AM_REG);
    // >> FIXED_BITS
    as_sra(R7,R7,ctrlParam.offset("FIXED_BITS"), AM_REG,AM_REG,AM_OFFSET);
    // oscOutR * waveAddrM
    as_mul(R11,R11,R9, AM_REG,AM_REG,AM_REG);
    // >> FIXED_BITS
    as_sra(R11,R11,ctrlParam.offset("FIXED_BITS"), AM_REG,AM_REG,AM_OFFSET);
    // r7:oscOut
    as_add(R7,R7,R11, AM_REG,AM_REG,AM_REG);
    // d:oscParam a:oscParam b:ctrlParam
    as_nop(R6,R6,R8, AM_SET,AM_SET,AM_SET);
    // params[i].level >> FIXED_BITS_ENV
    as_sra(R11,oscParam.offset("level"),ctrlParam.offset("FIXED_BITS_ENV"), AM_REG,AM_OFFSET,AM_OFFSET);
    // * oscOut
    as_mul(R11,R11,R7, AM_REG,AM_REG,AM_REG);
    // params[i].outData
    as_sra(oscParam.offset("outData"),R11,ctrlParam.offset("FIXED_BITS"), AM_OFFSET,AM_REG,AM_OFFSET);

    // d:oscParam a:oscParam b:oscParam
    as_nop(R6,R6,R6, AM_SET,AM_SET,AM_SET);
    // params[i].count += params[i].pitch;
    as_add(oscParam.offset("count"),oscParam.offset("count"),oscParam.offset("pitch"), AM_OFFSET,AM_OFFSET,AM_OFFSET);

    // if (oscParam.mixOut == FALSE) goto m_calc_wave_L_end
    as_ceq(oscParam.offset("mixOut"),reg_im(FALSE), AM_OFFSET,AM_REG);
    lib_bc("m_calc_wave_L_end");

    /*
    tmpL = (oscParam.outData * oscParam.levelL) >> ctrlParam.FIXED_BITS;
    tmpR = (oscParam.outData * oscParam.levelR) >> ctrlParam.FIXED_BITS;
    ctrlParam.mixL += tmpL;
    ctrlParam.mixR += tmpR;
    ctrlParam.mixRevL += (tmpL * oscParam.levelRev) >> ctrlParam.FIXED_BITS;
    ctrlParam.mixRevR += (tmpR * oscParam.levelRev) >> ctrlParam.FIXED_BITS;
    */
    as_mul(R7,oscParam.offset("outData"),oscParam.offset("levelL"), AM_REG,AM_OFFSET,AM_OFFSET);
    as_mul(R9,oscParam.offset("outData"),oscParam.offset("levelR"), AM_REG,AM_OFFSET,AM_OFFSET);
    as_add(R10,oscParam.offset("levelRev"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    // d:ctrlParam a:ctrlParam b:ctrlParam
    as_nop(R8,R8,R8, AM_SET,AM_SET,AM_SET);
    as_sra(R7,R7,ctrlParam.offset("FIXED_BITS"), AM_REG,AM_REG,AM_OFFSET);
    as_sra(R9,R9,ctrlParam.offset("FIXED_BITS"), AM_REG,AM_REG,AM_OFFSET);
    as_add(ctrlParam.offset("mixL"),ctrlParam.offset("mixL"),R7, AM_OFFSET,AM_OFFSET,AM_REG);
    as_add(ctrlParam.offset("mixR"),ctrlParam.offset("mixR"),R9, AM_OFFSET,AM_OFFSET,AM_REG);
    as_mul(R7,R7,R10, AM_REG,AM_REG,AM_REG);
    as_mul(R9,R9,R10, AM_REG,AM_REG,AM_REG);
    as_sra(R7,R7,ctrlParam.offset("FIXED_BITS"), AM_REG,AM_REG,AM_OFFSET);
    as_sra(R9,R9,ctrlParam.offset("FIXED_BITS"), AM_REG,AM_REG,AM_OFFSET);
    as_add(ctrlParam.offset("mixRevL"),ctrlParam.offset("mixRevL"),R7, AM_OFFSET,AM_OFFSET,AM_REG);
    as_add(ctrlParam.offset("mixRevR"),ctrlParam.offset("mixRevR"),R9, AM_OFFSET,AM_OFFSET,AM_REG);

    label("m_calc_wave_L_end");
    // set next oscParam
    as_add(R6,R6,R12, AM_REG,AM_REG,AM_REG);
    lib_wait_reg2addr();
    lib_loop_end("loop_m_calc_wave_0");
  }

  private void m_reverb()
  {
    /*
    reverbL = (reverbBufferR[reverbAddrR] * reverbDecay) >> FIXED_BITS;
    reverbR = (reverbBufferL[reverbAddrL] * reverbDecay) >> FIXED_BITS;
    reverbL += mixRevR;
    reverbR += mixRevL;
    reverbBufferR[reverbAddrR] = reverbL;
    reverbBufferL[reverbAddrL] = reverbR;
    reverbAddrL++;
    if (reverbAddrL > reverbLSize)
    {
      reverbAddrL = 0;
    }
    reverbAddrR++;
    if (reverbAddrR > reverbRSize)
    {
      reverbAddrR = 0;
    }
    outL = mixL + reverbBufferL[reverbAddrL];
    outR = mixR + reverbBufferR[reverbAddrR];
    */
    lib_set_im32(addr_struct("d_ctrl_param"));
    lib_simple_mv(R11, SP_REG_MVI);
    lib_wait_reg2addr();
    as_nop(R11,R11,R11, AM_SET,AM_SET,AM_SET);
    // r6 = &reverbBufferR[reverbAddrR]
    as_add(R6,ctrlParam.offset("reverbBufferR"),ctrlParam.offset("reverbAddrR"), AM_REG,AM_OFFSET,AM_OFFSET);
    // r7 = &reverbBufferL[reverbAddrL]
    as_add(R7,ctrlParam.offset("reverbBufferL"),ctrlParam.offset("reverbAddrL"), AM_REG,AM_OFFSET,AM_OFFSET);
    // r8 = reverbBufferR[reverbAddrR]
    lib_wait_reg2addr();
    as_add(R8,R6,reg_im(0), AM_REG,AM_SET,AM_REG);
    // r8 = (int32)r8
    lib_int16_to_32(R8);
    // r8 = r8 * reverbDecay
    as_mul(R8,R8,ctrlParam.offset("reverbDecay"), AM_REG,AM_REG,AM_OFFSET);
    // r8 = r8 >> FIXED_BITS
    as_sra(R8,R8,ctrlParam.offset("FIXED_BITS"), AM_REG,AM_REG,AM_OFFSET);
    // r8 += mixRevR
    as_add(R8,R8,ctrlParam.offset("mixRevR"), AM_REG,AM_REG,AM_OFFSET);
    // reverbBufferR[reverbAddrR] = r8
    as_add(R7,R8,reg_im(0), AM_SET,AM_REG,AM_REG);
    
    // r8 = reverbBufferL[reverbAddrL]
    as_add(R8,R7,reg_im(0), AM_REG,AM_SET,AM_REG);
    // r8 = (int32)r8
    lib_int16_to_32(R8);
    // r8 = r8 * reverbDecay
    as_mul(R8,R8,ctrlParam.offset("reverbDecay"), AM_REG,AM_REG,AM_OFFSET);
    // r8 = r8 >> FIXED_BITS
    as_sra(R8,R8,ctrlParam.offset("FIXED_BITS"), AM_REG,AM_REG,AM_OFFSET);
    // r8 += mixRevL
    as_add(R8,R8,ctrlParam.offset("mixRevL"), AM_REG,AM_REG,AM_OFFSET);
    // reverbBufferL[reverbAddrL] = r8
    as_add(R6,R8,reg_im(0), AM_SET,AM_REG,AM_REG);

    // r8 = reverbAddrL + 1
    as_nop(R11,R11,R11, AM_SET,AM_SET,AM_SET);
    as_add(R8,ctrlParam.offset("reverbAddrL"),reg_im(1), AM_REG,AM_OFFSET,AM_REG);
    // if (r8 > reverbSizeL) r8 = 0;
    as_cgt(R8,ctrlParam.offset("reverbSizeL"), AM_REG,AM_OFFSET);
    as_mv(R8,reg_im(0), AM_REG,AM_REG);
    // reverbAddrL = r8
    as_add(ctrlParam.offset("reverbAddrL"),R8,reg_im(0), AM_OFFSET,AM_REG,AM_REG);

    // r8 = reverbAddrR + 1
    as_add(R8,ctrlParam.offset("reverbAddrR"),reg_im(1), AM_REG,AM_OFFSET,AM_REG);
    // if (r8 > reverbSizeR) r8 = 0;
    as_cgt(R8,ctrlParam.offset("reverbSizeR"), AM_REG,AM_OFFSET);
    as_mv(R8,reg_im(0), AM_REG,AM_REG);
    // reverbAddrR = r8
    as_add(ctrlParam.offset("reverbAddrR"),R8,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    lib_wait_dependency();

    // r6 = &reverbBufferL[reverbAddrL]
    as_add(R6,ctrlParam.offset("reverbBufferL"),ctrlParam.offset("reverbAddrL"), AM_REG,AM_OFFSET,AM_OFFSET);
    // r7 = &reverbBufferR[reverbAddrR]
    as_add(R7,ctrlParam.offset("reverbBufferR"),ctrlParam.offset("reverbAddrR"), AM_REG,AM_OFFSET,AM_OFFSET);
    lib_wait_reg2addr();
    // r8 = reverbBufferL[reverbAddrL]
    as_add(R8,R6,reg_im(0), AM_REG,AM_SET,AM_REG);
    // r8 = (int32)r8
    lib_int16_to_32(R8);
    // outL = mixL + reverbBufferL[reverbAddrL];
    as_add(ctrlParam.offset("outL"),R8,ctrlParam.offset("mixL"), AM_OFFSET,AM_REG,AM_OFFSET);
    // r8 = reverbBufferR[reverbAddrR]
    as_add(R8,R7,reg_im(0), AM_REG,AM_SET,AM_REG);
    // r8 = (int32)r8
    lib_int16_to_32(R8);
    // outR = mixR + reverbBufferR[reverbAddrR];
    as_add(ctrlParam.offset("outR"),R8,ctrlParam.offset("mixR"), AM_OFFSET,AM_REG,AM_OFFSET);
  }

  private void f_synth_render()
  {
    label("f_synth_render");

    lib_push(SP_REG_LINK);
    lib_push_regs(12, 14);

    label("f_synth_render_L_0");
    // if audio_full then return
    lib_set_im32(AUDIO_FULL_ADDRESS);
    lib_wait_reg2addr();
    as_ceq(SP_REG_MVI, reg_im(1), AM_SET,AM_REG);
    lib_bc("f_synth_render_L_return");

    m_sequencer();
    m_envelope_generator();
    m_calc_wave();
    m_reverb();

    lib_set_im32(addr_struct("d_ctrl_param"));
    lib_simple_mv(R9, SP_REG_MVI);
    lib_wait_reg2addr();
    as_nop(R9,R9,R9, AM_SET,AM_SET,AM_SET);
    as_add(R6,ctrlParam.offset("outL"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    as_add(R7,ctrlParam.offset("outR"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    as_add(R10,ctrlParam.offset("audioValid"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);

    // convert to uint16
    lib_set_im32(0x8000);
    as_add(R6,R6,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    as_add(R7,R7,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    lib_set_im(0xffff);
    as_and(R6,R6,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    as_and(R7,R7,SP_REG_MVI, AM_REG,AM_REG,AM_REG);

    // 16bit R + 16bit L packing
    lib_set_im(16);
    as_sl(R8,R7,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    as_or(R8,R8,R6, AM_REG,AM_REG,AM_REG);
    lib_set_im32(AUDIO_DATA_ADDRESS);
    lib_wait_reg2addr();
    as_add(SP_REG_MVI,R8,reg_im(0), AM_SET,AM_REG,AM_REG);
    lib_set_im32(AUDIO_VALID_ADDRESS);
    lib_wait_reg2addr();
    as_add(SP_REG_MVI,R10,reg_im(0), AM_SET,AM_REG,AM_REG);
    as_xor(R10,R10,reg_im(1), AM_REG,AM_REG,AM_REG);
    as_nop(R9,R9,R9, AM_SET,AM_SET,AM_SET);
    as_add(ctrlParam.offset("audioValid"),R10,reg_im(0), AM_OFFSET,AM_REG,AM_REG);

    // repeat until audio_full
    lib_ba("f_synth_render_L_0");

    label("f_synth_render_L_return");
    lib_pop_regs(14, 12);
    lib_pop(SP_REG_LINK);
    lib_return();
  }

  private void f_synth_init()
  {
    label("f_synth_init");
    lib_push(SP_REG_LINK);
    lib_push_regs(12, 14);
    // set audio divider
    lib_set_im32(AUDIO_DIVIDER);
    lib_simple_mv(R6, SP_REG_MVI);
    lib_set_im32(AUDIO_DIVIDER_ADDRESS);
    lib_wait_reg2addr();
    as_add(SP_REG_MVI,R6,reg_im(0), AM_SET,AM_REG,AM_REG);
    // clear reverbBuffer
    lib_set_im32(addr_struct("d_ctrl_param"));
    lib_wait_reg2addr();
    as_nop(0,SP_REG_MVI,0, AM_REG,AM_SET,AM_REG);
    as_add(R6,ctrlParam.offset("reverbBufferL"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    as_add(R7,ctrlParam.offset("reverbSizeL"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    lib_set(R8, 0);
    lib_call("f_memory_fill");
    lib_set_im32(addr_struct("d_ctrl_param"));
    lib_wait_reg2addr();
    as_nop(0,SP_REG_MVI,0, AM_REG,AM_SET,AM_REG);
    as_add(R6,ctrlParam.offset("reverbBufferR"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    as_add(R7,ctrlParam.offset("reverbSizeR"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    lib_set(R8, 0);
    lib_call("f_memory_fill");
    // clear seq_data
    lib_set_im32(addr_abs("d_seq_data"));
    lib_simple_mv(R6, SP_REG_MVI);
    lib_set_im32(SEQ_CH * SEQ_LENGTH * noteParam.size() - 1);
    lib_simple_mv(R7, SP_REG_MVI);
    lib_set(R8, 0);
    lib_call("f_memory_fill");
    // set random data
    // r12 = addr_struct("d_seq_param");
    lib_set_im32(addr_struct("d_seq_param"));
    lib_simple_mv(R12, SP_REG_MVI);
    lib_loop_start("loop_f_synth_init_1", SEQ_INIT_NUM, LOOP_MODE_IM);
    m_seq_set_random_data();
    lib_loop_end("loop_f_synth_init_1");

    // d,a,b addr = addr_struct("d_seq_param");
    lib_set_im32(addr_struct("d_seq_param"));
    lib_wait_reg2addr();
    as_nop(SP_REG_MVI,SP_REG_MVI,SP_REG_MVI, AM_SET,AM_SET,AM_SET);
    // seqParam.ch = SEQ_CH;
    lib_set_im(SEQ_CH);
    as_add(seqParam.offset("ch"),SP_REG_MVI,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // seqParam.length = SEQ_LENGTH;
    lib_set_im(SEQ_LENGTH);
    as_add(seqParam.offset("length"),SP_REG_MVI,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // seqParam.seqDataStart = addr_struct("d_seq_data");
    lib_set_im32(addr_struct("d_seq_data"));
    as_add(seqParam.offset("seqDataStart"),SP_REG_MVI,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // seqParam.seqDataEnd = addr_struct("d_seq_data") + seqParam.ch * seqParam.length * seqParam.sizeOfNoteParam;
    as_mul(R6,seqParam.offset("ch"),seqParam.offset("length"), AM_REG,AM_OFFSET,AM_OFFSET);
    as_mul(R6,R6,seqParam.offset("sizeOfNoteParam"), AM_REG,AM_REG,AM_OFFSET);
    lib_set_im32(addr_struct("d_seq_data"));
    as_add(seqParam.offset("seqDataEnd"),R6,SP_REG_MVI, AM_OFFSET,AM_REG,AM_REG);
    // seqParam.beat = seqParam.seqDataStart
    as_add(seqParam.offset("beat"),seqParam.offset("seqDataStart"),reg_im(0), AM_OFFSET,AM_OFFSET,AM_REG);

    // set patch address
    // d,a_addr = oscParam; b_addr = ctrlParam;
    lib_set_im32(addr_struct("d_osc_param"));
    lib_simple_mv(R7, SP_REG_MVI);
    lib_wait_reg2addr();
    as_nop(R7,R7,R7, AM_SET,AM_SET,AM_SET);
    lib_set_im32(addr_struct("d_ctrl_param"));
    lib_wait_reg2addr();
    as_nop(0,0,SP_REG_MVI, AM_REG,AM_REG,AM_SET);
    lib_loop_start("loop_f_synth_init_2", OSCS - 1, LOOP_MODE_IM);
    // oscParam.envPatchAddr = oscParam.envPatch * ctrlParam.sizeOfEnvParam + addr_struct("d_env_param") + envParam.offset("currentLevel");
    as_mul(R6,oscParam.offset("envPatch"),ctrlParam.offset("sizeOfEnvParam"), AM_REG,AM_OFFSET,AM_OFFSET);
    lib_set_im32(addr_struct("d_env_param"));
    as_add(R6,R6,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    lib_set_im32(envParam.offset("currentLevel"));
    as_add(oscParam.offset("envPatchAddr"),R6,SP_REG_MVI, AM_OFFSET,AM_REG,AM_REG);
    // oscParam.modPatch0Addr = oscParam.modPatch0 * ctrlParam.sizeOfOscParam + addr_struct("d_osc_param") + oscParam.offset("outData");
    as_mul(R6,oscParam.offset("modPatch0"),ctrlParam.offset("sizeOfOscParam"), AM_REG,AM_OFFSET,AM_OFFSET);
    lib_set_im32(addr_struct("d_osc_param"));
    as_add(R6,R6,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    lib_set_im32(oscParam.offset("outData"));
    as_add(oscParam.offset("modPatch0Addr"),R6,SP_REG_MVI, AM_OFFSET,AM_REG,AM_REG);
    // oscParam.modPatch1Addr = oscParam.modPatch1 * ctrlParam.sizeOfOscParam + addr_struct("d_osc_param") + oscParam.offset("outData");
    as_mul(R6,oscParam.offset("modPatch1"),ctrlParam.offset("sizeOfOscParam"), AM_REG,AM_OFFSET,AM_OFFSET);
    lib_set_im32(addr_struct("d_osc_param"));
    as_add(R6,R6,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    lib_set_im32(oscParam.offset("outData"));
    as_add(oscParam.offset("modPatch1Addr"),R6,SP_REG_MVI, AM_OFFSET,AM_REG,AM_REG);
    // oscParam++;
    as_add(R7,R7,ctrlParam.offset("sizeOfOscParam"), AM_REG,AM_REG,AM_OFFSET);
    lib_wait_reg2addr();
    as_nop(R7,R7,0, AM_SET,AM_SET,AM_REG);
    lib_loop_end("loop_f_synth_init_2");

    lib_pop_regs(14, 12);
    lib_pop(SP_REG_LINK);
    lib_return();
  }

  private void f_visualizer_init()
  {
    label("f_visualizer_init");
    // init vga
    lib_set_im32(SPRITE_X_ADDRESS);
    lib_wait_reg2addr();
    as_add(SP_REG_MVI,reg_im(0),reg_im(0), AM_SET,AM_REG,AM_REG);
    as_add(1,reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    as_add(2,reg_im(4),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    /// for kv260
    ///as_add(2,reg_im(3),reg_im(0), AM_OFFSET,AM_REG,AM_REG);

    // init parameter
    lib_set_im32(addr_struct("d_vis_param"));
    lib_wait_reg2addr();
    as_nop(SP_REG_MVI,0,0, AM_SET,AM_REG,AM_REG);
    lib_set_im(VIS_MAX_Y);
    as_add(visParam.offset("y"),SP_REG_MVI,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    as_add(visParam.offset("a"),reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    as_add(visParam.offset("addr"),reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    as_add(visParam.offset("vsyncState"),reg_im(FALSE),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    lib_return();
  }

  private void f_visualizer()
  {
    label("f_visualizer");
    lib_push(SP_REG_LINK);
    // quadratic function graphics
    /*
    addr:r6, x:r7, y:r8, a:r11

    if (visParam.y < VIS_MAX_Y)
    {
    }
    else
    {
      vsync = *VSYNC;
      cond = ((visParam.vsyncState == FALSE) || (vsync == TRUE));
      visParam.vsyncState = vsync;
      if (cond == TRUE)
      {
        return;
      }
      visParam.y = 0;
      visParam.a++;
      visParam.addr = SPRITE_ADDRESS;
    }
    */
    int RvisParam = R11;
    int Rvsync = R6;
    int Rcond = R7;
    int Rx1 = R6;
    int Ry1 = R7;
    int Rx = R8;
    lib_set_im32(addr_struct("d_vis_param"));
    lib_simple_mv(RvisParam, SP_REG_MVI);
    lib_wait_reg2addr();
    as_nop(RvisParam,RvisParam,RvisParam, AM_SET,AM_SET,AM_SET);
    // if (visParam.y < VIS_MAX_Y)
    as_mvi(VIS_MAX_Y);
    as_cgt(SP_REG_MVI,visParam.offset("y"), AM_REG,AM_OFFSET);
    lib_bc("f_visualizer_L_0");
    // vsync = *VSYNC;
    lib_set_im32(VSYNC_ADDRESS);
    lib_wait_reg2addr();
    as_add(Rvsync,SP_REG_MVI,reg_im(0), AM_REG,AM_SET,AM_REG);
    as_nop(RvisParam,RvisParam,RvisParam, AM_SET,AM_SET,AM_SET);
    // cond = ((visParam.vsyncState == FALSE) || (vsync == TRUE));
    as_ceq(visParam.offset("vsyncState"),reg_im(FALSE), AM_OFFSET,AM_REG);
    lib_simple_mv(Rcond, SP_REG_CP);
    as_ceq(Rvsync,reg_im(TRUE), AM_REG,AM_REG);
    as_or(Rcond,Rcond,SP_REG_CP, AM_REG,AM_REG,AM_REG);
    // visParam.vsyncState = vsync;
    as_add(visParam.offset("vsyncState"),Rvsync,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // if (cond == TRUE) return
    as_add(SP_REG_CP,Rcond,reg_im(0), AM_REG,AM_REG,AM_REG);
    lib_bc("f_visualizer_L_return");
    as_add(visParam.offset("y"),reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    as_add(visParam.offset("a"),visParam.offset("a"),reg_im(1), AM_OFFSET,AM_OFFSET,AM_REG);
    lib_set_im32(SPRITE_ADDRESS);
    as_add(visParam.offset("addr"),SP_REG_MVI,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    label("f_visualizer_L_0");

    /*
    x = 0;
    do
    {
      y1 = visParam.y - (VIS_MAX_Y / 2);
      x1 = x - (VIS_MAX_X / 2);
      x++;
      mem[visParam.addr] = (((x1 * x1 + y1 * y1) * visParam.a) >>> 10) + 1;
      visParam.addr++;
    } while (visParam.x < VIS_MAX_X);
    visParam.y++;
    visParam.addr += VIS_NEXT_LINE - VIS_MAX_X;
    */
    // x = 0;
    as_add(Rx,reg_im(0),reg_im(0), AM_REG,AM_REG,AM_REG);

    lib_loop_start("f_visualizer_loop0", VIS_MAX_X - 1, LOOP_MODE_IM);
    // y1 = visParam.y - (VIS_MAX_Y / 2);
    as_mvi(VIS_MAX_Y >>> 1);
    as_sub(Ry1,visParam.offset("y"),SP_REG_MVI, AM_REG,AM_OFFSET,AM_REG);
    // y1 * y1
    as_mul(Ry1,Ry1,Ry1, AM_REG,AM_REG,AM_REG);
    // x1 = x - (VIS_MAX_X / 2);
    as_mvi(VIS_MAX_X >>> 1);
    as_sub(Rx1,Rx,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    // x++;
    as_add(Rx,Rx,reg_im(1), AM_REG,AM_REG,AM_REG);
    // x1 * x1
    as_mul(Rx1,Rx1,Rx1, AM_REG,AM_REG,AM_REG);
    // (x1 * x1 + y1 * y1)
    as_add(Rx1,Rx1,Ry1, AM_REG,AM_REG,AM_REG);
    // visParam.addr
    as_add(Ry1,visParam.offset("addr"),reg_im(0), AM_REG,AM_OFFSET,AM_REG);    
    // ((x1 * x1 + y1 * y1) * visParam.a)
    as_mul(Rx1,visParam.offset("a"),Rx1, AM_REG,AM_OFFSET,AM_REG);
    // >>> 10
    as_sr(Rx1,Rx1,reg_im(10), AM_REG,AM_REG,AM_REG);
    // mem[visParam.addr] = (((x1 * x1 + y1 * y1) * visParam.a) >>> 10) + 1;
    as_add(Ry1,Rx1,reg_im(1), AM_SET,AM_REG,AM_REG);
    // visParam.addr++;
    as_nop(RvisParam,RvisParam,RvisParam, AM_SET,AM_SET,AM_SET);
    as_add(visParam.offset("addr"),visParam.offset("addr"),reg_im(1), AM_OFFSET,AM_OFFSET,AM_REG);
    lib_loop_end("f_visualizer_loop0");

    // visParam.y++;
    as_add(visParam.offset("y"),visParam.offset("y"),reg_im(1), AM_OFFSET,AM_OFFSET,AM_REG);
    // visParam.addr += VIS_NEXT_LINE;
    lib_set_im(VIS_NEXT_LINE - VIS_MAX_X);
    as_add(visParam.offset("addr"),visParam.offset("addr"),SP_REG_MVI, AM_OFFSET,AM_OFFSET,AM_REG);

    label("f_visualizer_L_return");
    lib_pop(SP_REG_LINK);
    lib_return();
  }

  private void synth()
  {
    lib_init_stack();
    lib_call("f_synth_init");
    lib_call("f_visualizer_init");

    label("synth_main_loop");
    lib_call("f_synth_render");
    lib_call("f_visualizer");
    lib_ba("synth_main_loop");

    // link library
    f_nrand();
    f_synth_init();
    f_synth_render();
    f_visualizer_init();
    f_visualizer();
    f_memory_fill();
    /*
    f_uart_char();
    f_uart_hex();
    f_uart_hex_word();
    f_uart_hex_word_ln();
    f_uart_memory_dump();
    */
  }

  private void synth_data()
  {
    // update sizeof params
    ctrlParam.update("sizeOfOscParam", oscParam.size());
    ctrlParam.update("sizeOfEnvParam", envParam.size());
    seqParam.update("sizeOfNoteParam", noteParam.size());

    label("d_wave_table");
    for (int i = 0; i < WAVE_TABLE_SIZE; i++)
    {
      d32((int)(Math.sin(Math.PI * 2.0 / WAVE_TABLE_SIZE * i) * FIXED_SCALE));
    }

    label("d_ctrl_param");
    store_struct(ctrlParam);

    label("d_seq_param");
    store_struct(seqParam);

    label("d_vis_param");
    store_struct(visParam);

    label("d_env_param");
    for (int i = 0; i < ENVS; i++)
    {
      store_struct(envParam);
    }

    label("d_osc_param");
    for (int i = 0; i < OSCS; i++)
    {
      oscParam.update("envPatch", i);
      oscParam.update("modPatch0", i);
      store_struct(oscParam);
    }

    label("d_switch_addr");
    d32(addr_abs("L_envelope_generator_s_a"));
    d32(addr_abs("L_envelope_generator_s_d"));
    d32(addr_abs("L_envelope_generator_s_s"));
    d32(addr_abs("L_envelope_generator_s_r"));
    d32(addr_abs("L_envelope_generator_s_sl"));

    label("d_scale_table");
    for (int i = 0; i < SCALE_TABLE_SIZE; i++)
    {
      d32((int)(Math.floor(Math.pow(2.0, i / 12.0) * 440.0 * (0x100000000L / SAMPLE_RATE / WAVE_TABLE_SIZE))));
    }

    label("d_seq_data");
    for (int i = 0; i < SEQ_LENGTH; i++)
    {
      for (int j = 0; j < SEQ_CH; j++)
      {
        store_struct(noteParam);
      }
    }

    label("d_chord_data");
    d32x4(0, 4, 7, 0); d32x4(0,0,0,0); d32x4(0,0,0,0); d32x4(0,0,0,0);
    d32x4(4, 7, 11, 0); d32x4(0,0,0,0); d32x4(0,0,0,0); d32x4(0,0,0,0);
    d32x4(0, 4, 9, 0); d32x4(0,0,0,0); d32x4(0,0,0,0); d32x4(0,0,0,0);
    d32x4(0, 5, 9, 0); d32x4(0,0,0,0); d32x4(0,0,0,0); d32x4(0,0,0,0);
    d32x4(2, 5, 9, 0); d32x4(0,0,0,0); d32x4(0,0,0,0); d32x4(0,0,0,0);
    d32x4(2, 7, 11, 0); d32x4(0,0,0,0); d32x4(0,0,0,0); d32x4(0,0,0,0);
    for (int i = 0; i < 10 * 16; i++)
    {
      d32(0);
    }

    label("d_bass_data");
    d32x4(0, 0, 2, 1); d32x4(0, 1, 0, 0); d32x4(0,0,0,0); d32x4(0,0,0,0);

    label("d_progression_data");
    d32(0); d32(6);
    d32(0); d32(6);
    d32(0); d32(6);
    d32(0); d32(6);
    d32(0); d32(6);
    d32(0); d32(3);
    for (int i = 0; i < 10 * 2; i++)
    {
      d32(0);
    }

    label("d_rand");
    d32(0xfc720c27);
  }

  @Override
  public void init()
  {
    set_rom_depth(CODE_ROM_DEPTH, DATA_ROM_DEPTH);
    set_stack_address((1 << DATA_ROM_DEPTH) - 1);
    set_filename("synth");

    envParam.clear();
    envParam.add("state", STATE_SLEEP);
    envParam.add("noteOn", FALSE);
    envParam.add("envelopeLevelA", ENV_VALUE_MAX);
    envParam.add("envelopeLevelS", ENV_VALUE_MAX >> 1);
    envParam.add("envelopeDiffA", ENV_VALUE_MAX >> 6);
    envParam.add("envelopeDiffD", (- ENV_VALUE_MAX) >> 15);
    envParam.add("envelopeDiffR", (- ENV_VALUE_MAX) >> 15);
    envParam.add("currentLevel", 0);

    oscParam.clear();
    oscParam.add("envPatch", 0);
    oscParam.add("level", 0);
    oscParam.add("pitch", 0);
    oscParam.add("count", 0);
    oscParam.add("mod0", 0);
    oscParam.add("mod1", 0);
    oscParam.add("modPatch0", 0);
    oscParam.add("modPatch1", 0);
    oscParam.add("modLevel0", FIXED_SCALE << 0);
    oscParam.add("modLevel1", 0);
    oscParam.add("outData", 0);
    oscParam.add("mixOut", TRUE);
    oscParam.add("levelL", DEFAULT_VOLUME);
    oscParam.add("levelR", DEFAULT_VOLUME);
    oscParam.add("levelRev", (int)(FIXED_SCALE * 0.7));
    oscParam.add("envPatchAddr", 0);
    oscParam.add("modPatch0Addr", 0);
    oscParam.add("modPatch1Addr", 0);

    ctrlParam.clear();
    ctrlParam.add("OSCS", OSCS);
    ctrlParam.add("ENVS", ENVS);
    ctrlParam.add("WAVE_ADDR_SHIFT_M", WAVE_ADDR_SHIFT_M);
    ctrlParam.add("FIXED_BITS", FIXED_BITS);
    ctrlParam.add("FIXED_BITS_ENV", FIXED_BITS_ENV);
    ctrlParam.add("WAVE_TABLE_SIZE_M1", WAVE_TABLE_SIZE_M1);
    ctrlParam.add("FIXED_SCALE_M1", FIXED_SCALE_M1);
    ctrlParam.add("FIXED_SCALE", FIXED_SCALE);
    ctrlParam.add("ENV_VALUE_MAX", 1 << FIXED_BITS << FIXED_BITS_ENV);
    ctrlParam.add("MOD_LEVEL_MAX", (int)(FIXED_SCALE * 0.52));
    ctrlParam.add("reverbBufferL", MEM_S_ADDRESS + 0x0000);
    ctrlParam.add("reverbBufferR", MEM_S_ADDRESS + 0x6000);
    ctrlParam.add("reverbSizeL", (int)(0x5e31 / 48000.0 * SAMPLE_RATE));
    ctrlParam.add("reverbSizeR", (int)(0x9ff0 / 48000.0 * SAMPLE_RATE));
    ctrlParam.add("reverbAddrL", 0);
    ctrlParam.add("reverbAddrR", 0);
    ctrlParam.add("reverbDecay", (int)(FIXED_SCALE * 0.7));
    ctrlParam.add("sizeOfOscParam", 0);
    ctrlParam.add("sizeOfEnvParam", 0);
    ctrlParam.add("mixL", 0);
    ctrlParam.add("mixR", 0);
    ctrlParam.add("mixRevL", 0);
    ctrlParam.add("mixRevR", 0);
    ctrlParam.add("outL", 0);
    ctrlParam.add("outR", 0);
    ctrlParam.add("audioValid", 0);

    seqParam.clear();
    seqParam.add("CHORD_LENGTH", CHORD_LENGTH);
    seqParam.add("ch", SEQ_CH);
    seqParam.add("length", SEQ_LENGTH);
    seqParam.add("sizeOfNoteParam", 0);
    seqParam.add("callbackCounter", 0);
    seqParam.add("callbackRate", (int)(8000 / 48000.0 * SAMPLE_RATE));
    seqParam.add("beat", 0);
    seqParam.add("seqDataStart", 0);
    seqParam.add("seqDataEnd", 0);
    seqParam.add("chord", 0);
    seqParam.add("barCounter", 0);
    seqParam.add("barRepeat", 4);
    seqParam.add("deleteCounter", 0);
    seqParam.add("deleteFrequency", 15);
    seqParam.add("addFrequency", 3);
    seqParam.add("octRange", 4);
    seqParam.add("octMin", 5);
    seqParam.add("toneChange", TRUE);
    seqParam.add("led", 0);
    seqParam.add("currentCh", 0);

    noteParam.clear();
    noteParam.add("note", 0);
    noteParam.add("oct", 0);

    visParam.clear();
    visParam.add("y", 0);
    visParam.add("a", 0);
    visParam.add("addr", 0);
    visParam.add("vsyncState", FALSE);
  }

  @Override
  public void program()
  {
    synth();
  }

  @Override
  public void data()
  {
    synth_data();
  }
}
