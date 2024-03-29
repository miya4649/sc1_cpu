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

public class AsmLib extends Asm
{
  public static final int DEPTH_D = 12;

  // special purpose register
  public static final int SP_REG_MVI = 0;
  public static final int SP_REG_BA = 0;
  public static final int SP_REG_CP = 1;
  public static final int SP_REG_LINK = 2;
  public static final int SP_REG_LOOP_COUNTER = 3;
  public static final int SP_REG_LOOP_END = 4;
  public static final int SP_REG_LOOP_SPAN = 5;

  public static final int SP_REG_STACK_POINTER = 15;

  // argument / return(r6[31:0],r7[63:32]) register
  public static final int R6 = 6;
  public static final int R7 = 7;
  public static final int R8 = 8;
  public static final int R9 = 9;
  // caller-save register
  public static final int R10 = 10;
  public static final int R11 = 11;
  // callee-save register
  public static final int R12 = 12;
  public static final int R13 = 13;
  public static final int R14 = 14;

  // reg_im() : -16 ~ 15 (signed 5bit)

  // addressing mode
  public static final int AM_REG = 0;
  public static final int AM_SET = 1;
  public static final int AM_INC = 2;
  public static final int AM_OFFSET = 3;

  public static final int MEM_D_ADDRESS = 0x00000000;
  public static final int IOREG_R_ADDRESS = 0x00002000;
  public static final int IOREG_W_ADDRESS = 0x00003000;
  public static final int CHR_CHR_ADDRESS = 0x00006000;
  public static final int CHR_BIT_ADDRESS = 0x00007000;
  public static final int SPRITE_ADDRESS = 0x00008000;
  public static final int MEM_S_ADDRESS = 0x00010000;

  public static final int UART_BUSY_ADDRESS = 0x00002000;
  public static final int VSYNC_ADDRESS = 0x00002001;
  public static final int VCOUNT_ADDRESS = 0x00002002;
  public static final int AUDIO_FULL_ADDRESS = 0x00002003;
  public static final int TIMER_COUNT_ADDRESS = 0x00002004;
  public static final int I2C_READ_ADDRESS = 0x00002005;
  public static final int UART_TX_ADDRESS = 0x00003000;
  public static final int SPRITE_X_ADDRESS = 0x00003001;
  public static final int SPRITE_Y_ADDRESS = 0x00003002;
  public static final int SPRITE_SCALE_ADDRESS = 0x00003003;
  public static final int LED_ADDRESS = 0x00003004;
  public static final int AUDIO_DIVIDER_ADDRESS = 0x00003005;
  public static final int AUDIO_DATA_ADDRESS = 0x00003006;
  public static final int AUDIO_VALID_ADDRESS = 0x00003007;
  public static final int CHR_X_ADDRESS = 0x00003008;
  public static final int CHR_Y_ADDRESS = 0x00003009;
  public static final int CHR_SCALE_ADDRESS = 0x0000300a;
  public static final int CHR_PALETTE0_ADDRESS = 0x0000300b;
  public static final int CHR_PALETTE1_ADDRESS = 0x0000300c;
  public static final int CHR_PALETTE2_ADDRESS = 0x0000300d;
  public static final int CHR_PALETTE3_ADDRESS = 0x0000300e;
  public static final int TIMER_RESET_ADDRESS = 0x0000300f;
  public static final int I2C_WRITE_ADDRESS = 0x00003010;
  /*
    i2c_command: *I2C_WRITE_ADDRESS[11:10]
    i2c_start: *I2C_WRITE_ADDRESS[9]
    i2c_r_ack: *I2C_WRITE_ADDRESS[8]
    i2c_data_w: *I2C_WRITE_ADDRESS[7:0]

    i2c_busy: *I2C_READ_ADDRESS[9]
    i2c_w_ack: *I2C_READ_ADDRESS[8]
    i2c_data_r: *I2C_READ_ADDRESS[7:0]
  */
  public static final int LOOP_MODE_REG = 0;
  public static final int LOOP_MODE_IM = 1;

  public int spriteLineVoffsetInBits = 6;

  public int stackAddress = ((1 << DEPTH_D) - 1);

  // set default stack address
  public void set_stack_address(int addr)
  {
    stackAddress = addr;
  }

  // lib_* --------------------------------

  // reg = ABS(reg)
  // (reg != SP_REG_MVI)
  // modify: reg, SP_REG_MVI
  public void lib_abs(int reg)
  {
    as_sub(SP_REG_MVI,reg_im(0),reg, AM_REG,AM_REG,AM_REG);
    as_cgta(reg_im(0),reg, AM_REG,AM_REG);
    as_mv(reg,SP_REG_MVI, AM_REG,AM_REG);
  }

  // allocate memory in stack
  // input r6:size
  // example:
  // lib_alloca(64);
  // lib_alloca_struct_addr(R7);
  // lib_wait_reg2addr();
  // as_nop(0,R7,0, AM_REG,AM_SET,AM_REG);
  // as_add(R6,-32~31,reg_im(0), AM_REG,AM_OFFSET,AM_REG);
  // lib_alloca_free(64);
  // AM_SET, AM_INC breaks configured address
  // to reconfigure,
  // as_mvi(33);
  // as_nop(SP_REG_STACK_POINTER,SP_REG_STACK_POINTER,SP_REG_STACK_POINTER, AM_SET,AM_SET,AM_SET);
  public void lib_alloca(int size)
  {
    if (size < 1)
    {
      print_error("lib_alloca: bad size");
    }
    if (size < 16)
    {
      as_sub(SP_REG_STACK_POINTER,SP_REG_STACK_POINTER,reg_im(size), AM_REG,AM_REG,AM_REG);
    }
    else
    {
      lib_set_im32(size);
      as_sub(SP_REG_STACK_POINTER,SP_REG_STACK_POINTER,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    }
  }

  // free stack memory
  // input r6:size
  public void lib_alloca_free(int size)
  {
    if (size < 1)
    {
      print_error("lib_alloca: bad size");
    }
    if (size < 16)
    {
      as_add(SP_REG_STACK_POINTER,SP_REG_STACK_POINTER,reg_im(size), AM_REG,AM_REG,AM_REG);
    }
    else
    {
      lib_set_im32(size);
      as_add(SP_REG_STACK_POINTER,SP_REG_STACK_POINTER,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    }
  }

  // store address to reg for StructHelper
  // available offset: -32 ~ 31
  public void lib_alloca_struct_addr(int reg)
  {
    // SP_REG_STACK_POINTER + 1 + 32
    as_mvi(33);
    as_add(reg,SP_REG_STACK_POINTER,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
  }

  // jump to label
  public void lib_ba(String name)
  {
    // modify: SP_REG_MVI
    lib_set_im32(addr_abs(name));
    as_ba();
    lib_wait_delay_slot();
  }

  // branch to label
  public void lib_bc(String name)
  {
    // modify: SP_REG_MVI
    lib_set_im32(addr_rel(name) - 2);
    as_bc();
    lib_wait_delay_slot();
  }

  // call function
  public void lib_call(String name)
  {
    lib_set_im32(addr_rel(name) - 2);
    as_bl();
    lib_wait_delay_slot();
  }

  // initialize stack
  public void lib_init_stack()
  {
    lib_set_im32(stackAddress);
    as_add(SP_REG_STACK_POINTER,SP_REG_MVI,reg_im(0), AM_REG,AM_REG,AM_REG);
  }

  // convert int8 to int32 (char to int)
  public void lib_int8_to_32(int reg)
  {
    as_mvi(24);
    as_sl(reg,reg,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    as_sra(reg,reg,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
  }

  // convert int32 to int8 (int to char)
  public void lib_int32_to_8(int reg)
  {
    as_mvi(0xff);
    as_and(reg,reg,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
  }

  // convert int16 to int32 (short to int)
  public void lib_int16_to_32(int reg)
  {
    as_mvi(16);
    as_sl(reg,reg,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    as_sra(reg,reg,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
  }

  // convert int32 to int16 (int to short)
  public void lib_int32_to_16(int reg)
  {
    as_mvi(0xffff);
    as_and(reg,reg,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
  }

  // loop helper
  // name: label name
  // count: loop count - 1
  // mode: LOOP_MODE_REG(register no.) or LOOP_MODE_IM(immidiate value) count
  public void lib_loop_start(String name, int count, int mode)
  {
    if (mode == LOOP_MODE_REG)
    {
      lib_simple_mv(SP_REG_LOOP_COUNTER, count);
    }
    else
    {
      lib_set_im32(count);
      as_add(SP_REG_LOOP_COUNTER,SP_REG_MVI,reg_im(0), AM_REG,AM_REG,AM_REG);
    }

    lib_set_im32(addr_abs(name + "_L_2") - addr_abs(name + "_L_0"));
    lib_simple_mv(SP_REG_LOOP_END, SP_REG_MVI);
    lib_set_im32(addr_abs(name + "_L_1") - addr_abs(name + "_L_2") + 1);
    lib_simple_mv(SP_REG_LOOP_SPAN, SP_REG_MVI);
    as_loop();
    label(name + "_L_0");
    lib_wait_loop();
    label(name + "_L_1");
  }

  public void lib_loop_end(String name)
  {
    label(name + "_L_2");
  }

  // simple NOP x repeat
  public void lib_nop(int repeat)
  {
    for (int i = 0; i < repeat; i++)
    {
      as_nop(0,0,0,0,0,0);
    }
  }

  // stack: push r[reg]
  // modify: d_addr
  public void lib_push(int reg)
  {
    lib_wait_reg2addr();
    as_add(SP_REG_STACK_POINTER,reg,reg_im(0), AM_SET,AM_REG,AM_REG); // mem[SP] = r[reg]
    as_sub(SP_REG_STACK_POINTER,SP_REG_STACK_POINTER,reg_im(1), AM_REG,AM_REG,AM_REG); // SP--
  }

  // stack: pop to r[reg]
  // modify: a_addr
  public void lib_pop(int reg)
  {
    as_add(SP_REG_STACK_POINTER,SP_REG_STACK_POINTER,reg_im(1), AM_REG,AM_REG,AM_REG); // SP++
    lib_wait_reg2addr();
    as_add(reg,SP_REG_STACK_POINTER,reg_im(0), AM_REG,AM_SET,AM_REG); // r[reg] = mem[SP]
  }

  // stack: push r[reg_s] ~ r[reg_e]
  // modify: d_addr
  public void lib_push_regs(int reg_s, int reg_e)
  {
    if ((reg_e - reg_s) < 1)
    {
      print_error("lib_push_regs: reg_e > reg_s");
    }
    if ((reg_e - reg_s) > 14)
    {
      print_error("lib_push_regs: reg_e - reg_s < 15");
    }
    int n = reg_e - reg_s + 1;
    lib_wait_reg2addr();
    as_add(SP_REG_STACK_POINTER,reg_s,reg_im(0), AM_SET,AM_REG,AM_REG); // mem[SP] = r[reg_s]
    for (int i = reg_s + 1; i <= reg_e; i++)
    {
      as_add(reg_im(reg_s - i),i,reg_im(0), AM_OFFSET,AM_REG,AM_REG); // mem[SP-reg_s-i] = r[i]
    }
    as_sub(SP_REG_STACK_POINTER,SP_REG_STACK_POINTER,reg_im(n), AM_REG,AM_REG,AM_REG); // SP -= n
  }

  // stack: pop to r[reg_e] ~ r[reg_s]
  // modify: a_addr
  public void lib_pop_regs(int reg_e, int reg_s)
  {
    if ((reg_e - reg_s) < 1)
    {
      print_error("lib_push_regs: reg_e > reg_s");
    }
    if ((reg_e - reg_s) > 14)
    {
      print_error("lib_push_regs: reg_e - reg_s < 15");
    }
    int n = reg_e - reg_s + 1;
    as_add(SP_REG_STACK_POINTER,SP_REG_STACK_POINTER,reg_im(n), AM_REG,AM_REG,AM_REG); // SP += n
    lib_wait_reg2addr();
    as_add(reg_s,SP_REG_STACK_POINTER,reg_im(0), AM_REG,AM_SET,AM_REG);
    for (int i = reg_s + 1; i <= reg_e; i++)
    {
      as_add(i,reg_s - i,reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    }
  }

  // return
  public void lib_return()
  {
    // SP_REG_BA = SP_REG_LINK
    as_add(SP_REG_BA,SP_REG_LINK,reg_im(0), AM_REG,AM_REG,AM_REG);
    as_ba();
    lib_wait_delay_slot();
  }

  // show register value
  public void lib_reg2led(int reg)
  {
    lib_set_im32(LED_ADDRESS);
    lib_wait_reg2addr();
    as_add(SP_REG_MVI,reg,reg_im(0), AM_SET,AM_REG,AM_REG); // mem[LED_ADDR] = reg
  }

  // set immidiate value to register
  // -32 <= value <= 30
  public void lib_set(int reg, int value)
  {
    int im0, im1;
    if ((value > 30) || (value < -32))
    {
      print_error("lib_set: -32 <= value <= 30");
    }
    if (value < -16)
    {
      im0 = -16;
      im1 = value + 16;
    }
    else if (value > 15)
    {
      im0 = 15;
      im1 = value - 15;
    }
    else
    {
      im0 = value;
      im1 = 0;
    }
    as_add(reg,reg_im(im0),reg_im(im1), AM_REG,AM_REG,AM_REG);
  }

  // set immidiate value to register (32bit value)
  public void lib_set32(int reg, long value)
  {
    lib_set_im32(value);
    lib_simple_mv(reg, SP_REG_MVI);
  }

  // set SP_REG_CP(compare register) true/false
  public void lib_set_compare_reg(boolean b)
  {
    if (b == true)
    {
      as_sub(SP_REG_CP,reg_im(0),reg_im(1), AM_REG,AM_REG,AM_REG);
    }
    else
    {
      as_add(SP_REG_CP,reg_im(0),reg_im(0), AM_REG,AM_REG,AM_REG);
    }
  }

  // reg[SP_REG_MVI] = value (16bit)
  public void lib_set_im(int value)
  {
    as_mvi((int)value);
  }

  // reg[SP_REG_MVI] = value (32bit)
  public void lib_set_im32(long value)
  {
    as_mvi((int)value);
    as_mvih((int)(value >> 16));
  }

  // reg_d = reg_a
  public void lib_simple_mv(int reg_d, int reg_a)
  {
    as_add(reg_d,reg_a,reg_im(0), AM_REG,AM_REG,AM_REG); // r[reg_d] = r[reg_a]
  }

  // stop
  public void lib_stop()
  {
    as_halt();
    lib_wait_delay_slot();
  }

  // swap register
  // modify: SP_REG_MVI
  public void lib_swap(int reg_a, int reg_b)
  {
    lib_simple_mv(SP_REG_MVI, reg_a);
    lib_simple_mv(reg_a, reg_b);
    lib_simple_mv(reg_b, SP_REG_MVI);
  }

  // wait: delay_slot
  public void lib_wait_delay_slot()
  {
    lib_nop(3);
  }

  // wait: dependency
  public void lib_wait_dependency()
  {
    lib_nop(2);
  }

  // wait: next to loop instruction
  public void lib_wait_loop()
  {
    lib_nop(3);
  }

  // wait: set address
  public void lib_wait_reg2addr()
  {
    lib_nop(2);
  }

  // f_* --------------------------------

  public void f_copy_chr_bitmap()
  {
    // copy_small_data (1,2,4,8,16bit)
    // input: none
    // output: none
    // modify: r6 ~ r9
    // depend: f_copy_small_data
    // use stack: 5
    label("f_copy_chr_bitmap");
    lib_push(SP_REG_LINK);
    lib_set_im32(addr_abs("d_ascii_chr"));
    lib_simple_mv(R6, SP_REG_MVI);
    lib_set_im32(CHR_BIT_ADDRESS);
    lib_simple_mv(R7, SP_REG_MVI);
    lib_set(R8, 0);
    lib_set_im32(128);
    lib_simple_mv(R9, SP_REG_MVI);
    lib_call("f_copy_small_data");
    lib_pop(SP_REG_LINK);
    lib_return();
  }

  public void f_copy_small_data()
  {
    // copy_small_data (1,2,4,8,16bit)
    // input: r6:srcaddr, r7:dstaddr, r8:bpp_bits_minus1, r9:size
    // output: none
    // modify: r6 ~ r9
    // depend: none
    // use stack: 5
    /*
    saddr = d_ascii_chr;
    bpp_bits_minus1 = 0; (1bit:0 2bit:1 4bit:2)
    size = 128; (word)
    bpp = 1 << bpp_bits_minus1;
    mask = (1 << bpp) - 1;
    addrend = saddr + size;
    count = (32 >> bpp_bits_minus1) - 1;
    do
    {
      data = mem[saddr];
      loop(count + 1)
      {
        bdata = data & mask;
        mem[daddr] = bdata;
        data = data >> bpp;
        daddr++;
      }
      saddr++;
    } while (saddr < addrend);

    srcaddr:r6
    dstaddr:r7
    bpp_bits_minus1:r8
    count:r8
    size:r9

    bpp:r10
    mask:r11
    addrend:r12
    data:r13
    tmp:r14,r7
    */

    label("f_copy_small_data");
    lib_push_regs(10, 14);
    // set address r7:free
    as_nop(R7,R6,R6, AM_SET,AM_SET,AM_SET);
    // r7 = 1
    as_add(R7,reg_im(1),reg_im(0), AM_REG,AM_REG,AM_REG);
    // bpp = 1 << bpp_bits_minus1;
    as_sl(R10,reg_im(1),R8, AM_REG,AM_REG,AM_REG);
    // mask = (1 << bpp) - 1;
    as_sl(R11,reg_im(1),R10, AM_REG,AM_REG,AM_REG);
    as_sub(R11,R11,reg_im(1), AM_REG,AM_REG,AM_REG);
    // addrend = saddr + size;
    as_add(R12,R6,R9, AM_REG,AM_REG,AM_REG);
    // count = (32 >> bpp_bits_minus1) - 1;
    as_mvi(32);
    as_sr(R8,SP_REG_MVI,R8, AM_REG,AM_REG,AM_REG);
    as_sub(R8,R8,reg_im(1), AM_REG,AM_REG,AM_REG);

    // loop1 start
    label("f_write_small_data_L_0");
    // data = mem[saddr];
    as_add(R13,R6,reg_im(0), AM_REG,AM_SET,AM_REG);

    // loop2 start
    lib_loop_start("f_write_small_data_loop0", R8, LOOP_MODE_REG);
    // bdata = data & mask; mem[daddr] = bdata; daddr++;
    as_and(R7,R13,R11, AM_INC,AM_REG,AM_REG);
    // data = data >> bpp;
    as_sr(R13,R13,R10, AM_REG,AM_REG,AM_REG);
    // loop2 end
    lib_loop_end("f_write_small_data_loop0");
    // saddr++;
    as_add(R6,R6,reg_im(1), AM_REG,AM_REG,AM_REG);
    // if (addrend > saddr) goto "f_write_small_data_L_0"
    as_cgt(R12,R6, AM_REG,AM_REG);
    lib_bc("f_write_small_data_L_0");
    lib_pop_regs(14, 10);
    lib_return();
  }

  public void f_i2c_command()
  {
    // input: r6: command_data
    // output: none
    // modify: r6 ~ r7, d_addr
    int R_i2c_data_in = R6;
    int R_i2c_write_addr = R7;
    label("f_i2c_command");
    lib_set_im32(I2C_WRITE_ADDRESS);
    lib_simple_mv(R_i2c_write_addr, SP_REG_MVI);
    lib_wait_reg2addr();
    // set start bit
    lib_set_im(0x200);
    as_or(R_i2c_write_addr,R_i2c_data_in,SP_REG_MVI, AM_SET,AM_REG,AM_REG);
    // reset start bit
    lib_set_im(0xfdff);
    as_and(R_i2c_write_addr,R_i2c_data_in,SP_REG_MVI, AM_SET,AM_REG,AM_REG);
    lib_return();
  }

  public void f_i2c_read()
  {
    // input: r6:dev_addr r7:reg_addr
    // output: r8:data
    // modify: r6 ~ r8, d_addr, a_addr
    int R_dev_addr = R6;
    int R_reg_addr = R7;
    int R_out_data = R8;
    label("f_i2c_read");
    lib_push(SP_REG_LINK);
    // save R6~R7
    lib_alloca(2);
    lib_wait_reg2addr();
    as_nop(SP_REG_STACK_POINTER,SP_REG_STACK_POINTER,0, AM_SET,AM_SET,AM_REG);
    as_add(1,R6,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    as_add(2,R7,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // i2c_start
    m_i2c_start();
    lib_call("f_i2c_wait_command");
    // i2c_tx(dev_addr << 1)
    as_nop(0,SP_REG_STACK_POINTER,0, AM_REG,AM_SET,AM_REG);
    as_sl(R6,1,reg_im(1), AM_REG,AM_OFFSET,AM_REG);
    m_i2c_tx();
    lib_call("f_i2c_wait_command");
    // i2c_tx(reg)
    as_nop(0,SP_REG_STACK_POINTER,0, AM_REG,AM_SET,AM_REG);
    as_add(R6,2,reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    m_i2c_tx();
    lib_call("f_i2c_wait_command");
    // i2c_start (repeated)
    m_i2c_start();
    lib_call("f_i2c_wait_command");
    // i2c_tx((dev_addr << 1) | 1)
    as_nop(0,SP_REG_STACK_POINTER,0, AM_REG,AM_SET,AM_REG);
    as_sl(R6,1,reg_im(1), AM_REG,AM_OFFSET,AM_REG);
    as_or(R6,R6,reg_im(1), AM_REG,AM_REG,AM_REG);
    m_i2c_tx();
    lib_call("f_i2c_wait_command");
    // i2c_rx(nack:1)
    as_add(R6,reg_im(1),reg_im(0), AM_REG,AM_REG,AM_REG);
    m_i2c_rx();
    lib_call("f_i2c_wait_command");
    // read rx data
    lib_call("f_i2c_status");
    lib_simple_mv(R_out_data, R6);
    lib_set_im(0xff);
    as_and(R_out_data,R_out_data,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    // i2c_stop
    m_i2c_stop();
    lib_call("f_i2c_wait_command");
    lib_alloca_free(3);
    lib_pop(SP_REG_LINK);
    lib_return();
  }

  public void f_i2c_status()
  {
    // input: none
    // output: r6: data
    // modify: r6, a_addr
    int R_i2c_data_out = R6;
    int R_i2c_read_addr = R6;
    label("f_i2c_status");
    lib_set_im32(I2C_READ_ADDRESS);
    lib_simple_mv(R_i2c_read_addr, SP_REG_MVI);
    lib_wait_reg2addr();
    as_add(R_i2c_data_out,R_i2c_read_addr,reg_im(0), AM_REG,AM_SET,AM_REG);
    lib_return();
  }

  public void f_i2c_wait_command()
  {
    // input: none
    // output: none
    // modify: r6 ~ r7, a_addr
    int R_i2c_read_data = R6;
    int R_i2c_read_addr = R7;
    label("f_i2c_wait_command");
    lib_set_im32(I2C_READ_ADDRESS);
    lib_simple_mv(R_i2c_read_addr, SP_REG_MVI);
    lib_wait_reg2addr();
    // while ((*I2C_READ_ADDRESS & 0x200) == 0x200)
    as_mvi(0x200);
    as_and(R_i2c_read_data,R_i2c_read_addr,SP_REG_MVI, AM_REG,AM_SET,AM_REG);
    as_ceq(R_i2c_read_data,SP_REG_MVI, AM_REG,AM_REG);
    as_mvi(-5);
    as_mvih(0xffff);
    as_bc();
    lib_wait_delay_slot();
    lib_return();
  }

  public void f_i2c_write()
  {
    // input: r6:dev_addr r7:reg_addr r8:data
    // output: none
    // modify: r6 ~ r8, d_addr, a_addr
    int R_dev_addr = R6;
    int R_reg_addr = R7;
    int R_data = R8;
    int R_i2c_data = R6;
    label("f_i2c_write");
    lib_push(SP_REG_LINK);
    // save R6~R8
    lib_alloca(3);
    lib_wait_reg2addr();
    as_nop(SP_REG_STACK_POINTER,SP_REG_STACK_POINTER,0, AM_SET,AM_SET,AM_REG);
    as_add(1,R6,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    as_add(2,R7,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    as_add(3,R8,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // i2c_start
    m_i2c_start();
    lib_call("f_i2c_wait_command");
    // i2c_tx(dev_addr << 1)
    as_nop(0,SP_REG_STACK_POINTER,0, AM_REG,AM_SET,AM_REG);
    as_sl(R_i2c_data,1,reg_im(1), AM_REG,AM_OFFSET,AM_REG);
    m_i2c_tx();
    lib_call("f_i2c_wait_command");
    // i2c_tx(reg)
    as_nop(0,SP_REG_STACK_POINTER,0, AM_REG,AM_SET,AM_REG);
    as_add(R_i2c_data,2,reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    m_i2c_tx();
    lib_call("f_i2c_wait_command");
    // i2c_tx(data)
    as_nop(0,SP_REG_STACK_POINTER,0, AM_REG,AM_SET,AM_REG);
    as_add(R_i2c_data,3,reg_im(0), AM_REG,AM_OFFSET,AM_REG);
    m_i2c_tx();
    lib_call("f_i2c_wait_command");
    // i2c_stop
    m_i2c_stop();
    lib_call("f_i2c_wait_command");
    lib_alloca_free(3);
    lib_pop(SP_REG_LINK);
    lib_return();
  }

  // macro of f_i2c_*
  public void m_i2c_is_busy()
  {
    // input: none
    // output: SP_REG_CP
    // modify: r6 ~ r7, a_addr
    int R_i2c_read_data = R6;
    lib_call("f_i2c_status");
    lib_set_im(0x200);
    as_and(R_i2c_read_data,R_i2c_read_data,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    as_ceq(R_i2c_read_data,SP_REG_MVI, AM_REG,AM_REG);
  }

  public void m_i2c_start()
  {
    // modify: r6 ~ r7, d_addr
    // The caller function must save the link register
    int R_i2c_command = R6;
    as_add(R_i2c_command,reg_im(0),reg_im(0), AM_REG,AM_REG,AM_REG);
    lib_call("f_i2c_command");
  }

  public void m_i2c_stop()
  {
    // modify: r6 ~ r7, d_addr
    // The caller function must save the link register
    int R_i2c_command = R6;
    lib_set_im(0x400);
    lib_simple_mv(R_i2c_command, SP_REG_MVI);
    lib_call("f_i2c_command");
  }

  public void m_i2c_tx()
  {
    // input: r6:data
    // modify: r6 ~ r7, d_addr
    // The caller function must save the link register
    int R_i2c_data = R6;
    lib_set_im(0x800);
    as_or(R_i2c_data,R_i2c_data,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    lib_call("f_i2c_command");
  }

  public void m_i2c_rx()
  {
    // input: r6: ack(0)/nack(1)
    // output: none
    // The caller function must save the link register
    // modify: r6 ~ r8, d_addr
    int R_i2c_nack = R6;
    as_and(R_i2c_nack,reg_im(1),reg_im(0), AM_REG,AM_REG,AM_REG);
    as_sl(R_i2c_nack,R_i2c_nack,reg_im(8), AM_REG,AM_REG,AM_REG);
    lib_set_im(0xc00);
    as_or(R_i2c_nack,R_i2c_nack,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    lib_call("f_i2c_command");
  }

  public void f_line()
  {
    // line(x0, y0, x1, y1, color);
    // input: r6 (data address)
    // modify: r6 ~ r10, d_addr, a_addr
    // using loop instruction
    // x0: *(R6 + 0) y0: *(R6 + 1) x1: *(R6 + 2) y1: *(R6 + 3) color: *(R6 + 4)

    label("f_line");
    /*
    if (abs(y1 - y0) > abs(x1 - x0))
    {
      swap(x0, y0);
      swap(x1, y1);
      ofx = width_in_bits;
      ofy = 0;
    }
    else
    {
      ofx = 0;
      ofy = width_in_bits;
    }
    dx = abs(x1 - x0);
    if (dx == 0) return;
    if (x1 > x0)
    {
      ix = (1 << ofx);
    }
    else
    {
      ix = -(1 << ofx);
    }
    if (y1 > y0)
    {
      iy = (1 << ofy);
    }
    else
    {
      iy = -(1 << ofy);
    }
    addr = SPRITE_ADDRESS + (x0 << ofx) + (y0 << ofy);
    dy = abs(y1 - y0);
    er = dx >> 1;
    for (i = 0; i < dx; i++)
    {
      er -= dy;
      vram[addr] = col;
      addr += ix;
      if (0 > er)
      {
        addr += iy;
        er += dx;
      }
    }
     */
    // x0:sp[0] y0:sp[1] x1:sp[2] y1:sp[3] color:sp[4]
    // ofx:sp[5] ofy:sp[6]
    // input data
    int Ix0, Iy0, Ix1, Iy1, Icolor;
    // local variable
    int Sx0, Sy0, Sx1, Sy1, Scolor, Sdx, Sdy, Sofx, Sofy, Six, Siy, Sx, Sy;
    int Rer, Raddr;
    int Rt1, Rt2, Rt3;
    Ix0 = 0;
    Iy0 = 1;
    Ix1 = 2;
    Iy1 = 3;
    Icolor = 4;
    Sx0 = 0;
    Sy0 = 1;
    Sx1 = 2;
    Sy1 = 3;
    Scolor = 4;
    Sdx = 5;
    Sdy = 6;
    Sofx = 7;
    Sofy = 8;
    Six = 9;
    Siy = 10;
    Sx = 11;
    Sy = 12;
    Rt1 = R7;
    Raddr = R7;
    Rt2 = R8;
    Rt3 = R9;
    Rer = R10;
    lib_alloca(16);
    as_nop(R6,R6,R6, AM_SET,AM_SET,AM_SET);
    // y1 - y0
    as_sub(Rt1,Iy1,Iy0, AM_REG,AM_OFFSET,AM_OFFSET);
    // abs(y1 - y0)
    lib_abs(Rt1);
    // x1 - x0
    as_sub(Rt2,Ix1,Ix0, AM_REG,AM_OFFSET,AM_OFFSET);
    // abs(x1 - x0)
    lib_abs(Rt2);
    // if (abs(y1 - y0) > abs(x1 - x0))
    as_cgta(Rt1,Rt2, AM_REG,AM_REG);
    // d_addr = SP_REG_STACK_POINTER
    as_nop(SP_REG_STACK_POINTER,0,0, AM_SET,AM_REG,AM_REG);
    // sp[4] = *(R6 + 4)
    as_add(Scolor,Icolor,reg_im(0), AM_OFFSET,AM_OFFSET,AM_REG);
    lib_bc("L_f_line_1");
    as_add(Sx0,Ix0,reg_im(0), AM_OFFSET,AM_OFFSET,AM_REG);
    as_add(Sy0,Iy0,reg_im(0), AM_OFFSET,AM_OFFSET,AM_REG);
    as_add(Sx1,Ix1,reg_im(0), AM_OFFSET,AM_OFFSET,AM_REG);
    as_add(Sy1,Iy1,reg_im(0), AM_OFFSET,AM_OFFSET,AM_REG);
    as_add(Sofx,reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    lib_set_im(spriteLineVoffsetInBits);
    as_add(Sofy,SP_REG_MVI,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    lib_ba("L_f_line_2");
    label("L_f_line_1");
    as_add(Sx0,Iy0,reg_im(0), AM_OFFSET,AM_OFFSET,AM_REG);
    as_add(Sy0,Ix0,reg_im(0), AM_OFFSET,AM_OFFSET,AM_REG);
    as_add(Sx1,Iy1,reg_im(0), AM_OFFSET,AM_OFFSET,AM_REG);
    as_add(Sy1,Ix1,reg_im(0), AM_OFFSET,AM_OFFSET,AM_REG);
    as_add(Sofy,reg_im(0),reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    lib_set_im(spriteLineVoffsetInBits);
    as_add(Sofx,SP_REG_MVI,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    label("L_f_line_2");
    // d_addr,a_addr,b_addr = SP_REG_STACK_POINTER
    as_nop(SP_REG_STACK_POINTER,SP_REG_STACK_POINTER,SP_REG_STACK_POINTER, AM_SET,AM_SET,AM_SET);
    // dx = abs(x1 - x0);
    as_sub(Rt1,Sx1,Sx0, AM_REG,AM_OFFSET,AM_OFFSET);
    lib_abs(Rt1);
    as_add(Sdx,Rt1,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // if (dx == 0) return;
    as_ceq(Rt1,reg_im(0), AM_REG,AM_REG);
    lib_bc("L_f_line_end");
    // Rt1 = 1 << ofx
    as_sl(Rt1,reg_im(1),Sofx, AM_REG,AM_REG,AM_OFFSET);
    // ix = -Rt1
    as_sub(Six,reg_im(0),Rt1, AM_OFFSET,AM_REG,AM_REG);
    // if (x1 > x0) ix = Rt1
    as_cgta(Sx1,Sx0, AM_OFFSET,AM_OFFSET);
    as_mv(Six,Rt1, AM_OFFSET,AM_REG);
    // Rt1 = 1 << ofy
    as_sl(Rt1,reg_im(1),Sofy, AM_REG,AM_REG,AM_OFFSET);
    // iy = -Rt1
    as_sub(Siy,reg_im(0),Rt1, AM_OFFSET,AM_REG,AM_REG);
    // if (y1 > y0) iy = Rt1
    as_cgta(Sy1,Sy0, AM_OFFSET,AM_OFFSET);
    as_mv(Siy,Rt1, AM_OFFSET,AM_REG);
    // addr = SPRITE_ADDRESS + (x0 << ofx) + (y0 << ofy);
    //   addr = x0 << ofx
    as_sl(Raddr,Sx0,Sofx, AM_REG,AM_OFFSET,AM_OFFSET);
    //   Rt2 = y0 << ofy
    as_sl(Rt2,Sy0,Sofy, AM_REG,AM_OFFSET,AM_OFFSET);
    //   addr += Rt2
    as_add(Raddr,Raddr,Rt2, AM_REG,AM_REG,AM_REG);
    //   addr += SPRITE_ADDRESS
    lib_set_im32(SPRITE_ADDRESS);
    as_add(Raddr,Raddr,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    // dy = abs(y1 - y0);
    as_sub(Rt2,Sy1,Sy0, AM_REG,AM_OFFSET,AM_OFFSET);
    lib_abs(Rt2);
    as_add(Sdy,Rt2,reg_im(0), AM_OFFSET,AM_REG,AM_REG);
    // er = dx >> 1;
    as_sr(Rer,Sdx,reg_im(1), AM_REG,AM_OFFSET,AM_REG);
    // Rt2 = dx - 1
    as_sub(Rt2,Sdx,reg_im(1), AM_REG,AM_OFFSET,AM_REG);
    // for (i = 0; i < dx; i++)
    lib_loop_start("Loop_f_line_1", Rt2, LOOP_MODE_REG);
    // er -= dy;
    as_sub(Rer,Rer,Sdy, AM_REG,AM_REG,AM_OFFSET);
    // vram[addr] = col;
    as_add(Raddr,Scolor,reg_im(0), AM_SET,AM_OFFSET,AM_REG);
    // addr += ix;
    as_add(Raddr,Raddr,Six, AM_REG,AM_REG,AM_OFFSET);
    // Rt2 = addr + iy
    as_add(Rt2,Raddr,Siy, AM_REG,AM_REG,AM_OFFSET);
    // Rt3 = er + dx
    as_add(Rt3,Rer,Sdx, AM_REG,AM_REG,AM_OFFSET);
    // if (0 > er) {addr = Rt2; er = Rt3}
    as_cgta(reg_im(0),Rer, AM_REG,AM_REG);
    as_mv(Raddr,Rt2, AM_REG,AM_REG);
    as_mv(Rer,Rt3, AM_REG,AM_REG);
    lib_loop_end("Loop_f_line_1");
    label("L_f_line_end");
    lib_alloca_free(16);
    lib_return();
  }

  // memory fill
  // input: r6:start_address r7:size r8:fill_data
  // output: none
  // modify: r7,r9,d_addr
  // using loop instruction
  public void f_memory_fill()
  {
    label("f_memory_fill");
    as_sub(R7,R7,reg_im(1), AM_REG,AM_REG,AM_REG);
    as_add(R9,reg_im(1),reg_im(0), AM_REG,AM_REG,AM_REG);
    as_nop(R6,0,0, AM_SET,AM_REG,AM_REG);
    lib_loop_start("loop_f_memory_fill_0", R7, LOOP_MODE_REG);
    as_add(R9,R8,reg_im(0), AM_INC,AM_REG,AM_REG);
    lib_loop_end("loop_f_memory_fill_0");
    lib_return();
  }

  // return random value r6
  public void f_rand()
  {
    // input: none
    // output: r6:random
    // modify: r6 ~ r7, d_addr, a_addr
    label("f_rand");
    m_f_rand();
    lib_return();
  }

  // return random value (0 ~ (N - 1))
  public void f_nrand()
  {
    // input: r6:max number N (16bit)
    // output: r6:random (0 ~ (N - 1))
    // modify: r6 ~ r8, d_addr, a_addr
    label("f_nrand");
    lib_simple_mv(R8, R6);
    m_f_rand();
    as_mvi(0xffff);
    as_and(R6,R6,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    as_mul(R6,R6,R8, AM_REG,AM_REG,AM_REG);
    as_mvi(16);
    as_sr(R6,R6,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    lib_return();
  }

  // macro of f_rand
  public void m_f_rand()
  {
    lib_set_im32(addr_abs("d_rand"));
    lib_wait_reg2addr();
    as_add(R6,SP_REG_MVI,reg_im(0), AM_REG,AM_SET,AM_REG); // r6 = mem[r0] + 0
    as_sl(R7,R6,reg_im(13), AM_REG,AM_REG,AM_REG); // r7 = r6 << 13
    as_xor(R6,R7,R6, AM_REG,AM_REG,AM_REG); // r6 = r7 ^ r6
    lib_set_im(17); // r0 = 17
    as_sr(R7,R6,SP_REG_MVI, AM_REG,AM_REG,AM_REG); // r7 = r6 >> r0
    as_xor(R6,R7,R6, AM_REG,AM_REG,AM_REG); // r6 = r7 ^ r6
    as_sl(R7,R6,reg_im(5), AM_REG,AM_REG,AM_REG); // r7 = r6 << 5
    as_xor(R6,R7,R6, AM_REG,AM_REG,AM_REG); // r6 = r7 ^ r6
    lib_set_im32(addr_abs("d_rand"));
    lib_wait_reg2addr();
    as_add(SP_REG_MVI,R6,reg_im(0), AM_SET,AM_REG,AM_REG); // mem[d] = r6 + 0
  }

  // reset timer and timer data
  public void f_timer_reset()
  {
    // input: none
    // modify: r6, d_addr
    int R_timer_data_addr = R6;
    int R_timer_reset_addr = R6;
    label("f_timer_reset");
    // reset timer count
    lib_set_im32(addr_abs("d_timer_data"));
    lib_simple_mv(R_timer_data_addr, SP_REG_MVI);
    lib_wait_reg2addr();
    as_add(R_timer_data_addr,reg_im(0),reg_im(0), AM_SET,AM_REG,AM_REG);
    // reset timer
    lib_set_im32(TIMER_RESET_ADDRESS);
    lib_simple_mv(R_timer_reset_addr, SP_REG_MVI);
    lib_wait_reg2addr();
    lib_set_im(1);
    as_add(R_timer_reset_addr,SP_REG_MVI,reg_im(0), AM_SET,AM_REG,AM_REG);
    lib_set_im(0);
    as_add(R_timer_reset_addr,SP_REG_MVI,reg_im(0), AM_SET,AM_REG,AM_REG);
    lib_return();
  }

  // wait R6 clock cycles (since last call) then return
  public void f_timer_wait()
  {
    // input: r6: wait clock cycle
    // modify: r6 ~ r8, d_addr, a_addr
    int R_wait_count = R6;
    int R_timer_addr = R7;
    int R_timer_data_addr = R7;
    int R_next_time = R6;
    int R_diff_time = R8;
    label("f_timer_wait");
    // R_next_time = "d_timer_data" + R_wait_count
    lib_set_im32(addr_abs("d_timer_data"));
    lib_simple_mv(R_timer_data_addr, SP_REG_MVI);
    lib_wait_reg2addr();
    as_add(R_next_time,R_timer_data_addr,R_wait_count, AM_REG,AM_SET,AM_REG);
    as_add(R_timer_data_addr,R_next_time,reg_im(0), AM_SET,AM_REG,AM_REG);
    // R_timer_addr = TIMER_COUNT_ADDRESS
    lib_set_im32(TIMER_COUNT_ADDRESS);
    lib_simple_mv(R_timer_addr, SP_REG_MVI);
    lib_wait_reg2addr();
    label("L_f_timer_wait_1");
    as_sub(R_diff_time,R_timer_addr,R_next_time, AM_REG,AM_SET,AM_REG);
    as_cgta(R_diff_time,reg_im(0), AM_REG,AM_REG);
    as_not(SP_REG_CP,SP_REG_CP, AM_REG,AM_REG);
    lib_bc("L_f_timer_wait_1");
    lib_return();
  }

  public void f_uart_char()
  {
    // uart put char
    // input: r6:char_to_send
    // output: none
    // modify: d_addr, a_addr
    label("f_uart_char");
    lib_set_im32(UART_BUSY_ADDRESS);
    lib_wait_reg2addr();
    as_nop(0,0,0, AM_REG,AM_SET,AM_REG);
    lib_set_im32(UART_TX_ADDRESS);
    lib_wait_reg2addr();
    as_nop(0,0,0, AM_SET,AM_REG,AM_REG);
    label("f_uart_char_L_0");
    as_ceq(0,reg_im(1), AM_OFFSET,AM_REG); // if (mem[a] == 1) r1 = true
    lib_bc("f_uart_char_L_0"); // wait while busy = 1
    as_add(0,R6,reg_im(0), AM_OFFSET,AM_REG,AM_REG); // uart tx <= r6
    lib_return();
  }

  public void f_uart_hex()
  {
    // uart put hex digit
    // input: r6:value(4bit)
    // output: none
    // modify: r6-r7, d_addr, a_addr
    // depend: f_uart_char
    label("f_uart_hex");
    /*
    r6 = r6 & 15;
    r7 = 48
    if (r6 > 9) {r7 = 87}
    r6 = r6 + r7
    */
    lib_push(SP_REG_LINK);
    as_and(R6,R6,reg_im(15), AM_REG,AM_REG,AM_REG);
    lib_set_im(48);
    as_add(R7,SP_REG_MVI,reg_im(0), AM_REG,AM_REG,AM_REG);
    lib_set_im(87);
    as_cgt(R6,reg_im(9), AM_REG,AM_REG);
    as_mv(R7,SP_REG_MVI, AM_REG,AM_REG);
    as_add(R6,R6,R7, AM_REG,AM_REG,AM_REG);
    lib_call("f_uart_char");
    lib_pop(SP_REG_LINK);
    lib_return();
  }

  public void f_uart_hex_word()
  {
    // uart put hex word
    // input: r6:value (32bit)
    // output: none
    // modify: r6, d_addr, a_addr
    // depend: f_uart_char, f_uart_hex
    label("f_uart_hex_word");
    /*
    push link;
    push r7;
    push r8;
    push r9;
    r8 = r6;
    r9 = 28;
    do
    {
      r6 = r8 >> r9;
      call f_uart_hex;
      r9 -= 4;
    } while (r9 > 3) (cgta)
    pop r9;
    pop r8;
    pop r7;
    pop link;
    return
     */
    lib_push(SP_REG_LINK);
    lib_push_regs(R7, R9);
    as_add(R8,R6,reg_im(0), AM_REG,AM_REG,AM_REG);
    lib_set_im(28);
    as_add(R9,SP_REG_MVI,reg_im(0), AM_REG,AM_REG,AM_REG);
    label("f_uart_hex_word_L_0");
    as_sr(R6,R8,R9, AM_REG,AM_REG,AM_REG);
    lib_call("f_uart_hex");
    as_sub(R9,R9,reg_im(4), AM_REG,AM_REG,AM_REG);
    as_cgta(R9,reg_im(-1), AM_REG,AM_REG);
    lib_bc("f_uart_hex_word_L_0");
    lib_pop_regs(R9, R7);
    lib_pop(SP_REG_LINK);
    lib_return();
  }

  public void f_uart_hex_word_ln()
  {
    // uart register monitor
    // input: r6:value (32bit)
    // output: none
    // modify: d_addr, a_addr
    // depend: f_uart_hex_word, f_uart_hex, f_uart_char
    // use stack: 2
    label("f_uart_hex_word_ln");
    lib_push(SP_REG_LINK);
    lib_call("f_uart_hex_word");
    as_add(R6,reg_im(10),reg_im(0), AM_REG,AM_REG,AM_REG);
    lib_call("f_uart_char");
    lib_pop(SP_REG_LINK);
    lib_return();
  }

  public void f_uart_memory_dump()
  {
    // uart memory dump
    // input: r6:start_address r7:dump_size(words)
    // output: none
    // modify: r6-r7, d_addr, a_addr
    // depend: f_uart_char, f_uart_hex, f_uart_hex_word
    // use stack: 3
    label("f_uart_memory_dump");
    /*
    push r8;
    push r9;
    push r10;
    r9 = r6;
    r10 = r6 + r7;
    do
    {
      r6 = r9;
      call f_uart_hex_word (put address)
      r6 = 32;
      call f_uart_char (put space)
      r6 = mem[r9];
      call f_uart_hex_word (put data)
      r6 = 10;
      call f_uart_char (put enter)
      r9++;
    } while (r10 > r9)
    pop r10;
    pop r9;
    pop r8;
     */
    lib_push(SP_REG_LINK);
    lib_push_regs(R8, R10);
    as_add(R9,R6,reg_im(0), AM_REG,AM_REG,AM_REG);
    as_add(R10,R6,R7, AM_REG,AM_REG,AM_REG);
    label("f_uart_memory_dump_L_0");
    as_add(R6,R9,reg_im(0), AM_REG,AM_REG,AM_REG);
    lib_call("f_uart_hex_word");
    lib_set_im(32);
    as_add(R6,SP_REG_MVI,reg_im(0), AM_REG,AM_REG,AM_REG);
    lib_call("f_uart_char");
    as_add(R6,R9,reg_im(0), AM_REG,AM_SET,AM_REG);
    lib_call("f_uart_hex_word");
    as_add(R6,reg_im(10),reg_im(0), AM_REG,AM_REG,AM_REG);
    lib_call("f_uart_char");
    as_add(R9,R9,reg_im(1), AM_REG,AM_REG,AM_REG);
    as_cgt(R10,R9, AM_REG,AM_REG);
    lib_bc("f_uart_memory_dump_L_0");
    lib_pop_regs(R10, R8);
    lib_pop(SP_REG_LINK);
    lib_return();
  }

  public void f_uart_print()
  {
    // uart print string
    // input: r6:text_start_address
    // output: none
    // modify: r6, d_addr, a_addr
    // depend: f_uart_char
    // use stack: 5
    label("f_uart_print");
    /*
    addr:r7 shift:r8 char:r9
    addr = r6;
    shift = 24;
    do
    {
      char = mem[addr] >> shift;
      r6 = char & 0xff;
      if (r6 == 0) break;
      call f_uart_char;
      shift -= 8;
      if (shift < 0)
      {
        shift = 24;
        addr++;
      }
    } while (1)
    */
    lib_push(SP_REG_LINK);
    lib_push_regs(R7, R10);
    lib_simple_mv(R7, R6);
    as_nop(0,R6,0, AM_REG,AM_SET,AM_REG); // a_addr = r6
    as_add(R8,reg_im(12),reg_im(12), AM_REG,AM_REG,AM_REG);
    label("f_uart_print_L_0");
    as_sr(R9,R7,R8, AM_REG,AM_SET,AM_REG);
    lib_set_im(0xff);
    as_and(R9,R9,SP_REG_MVI, AM_REG,AM_REG,AM_REG);
    lib_simple_mv(R6,R9);
    as_ceq(R6,reg_im(0), AM_REG,AM_REG);
    lib_bc("f_uart_print_L_1");
    lib_call("f_uart_char");
    as_sub(R8,R8,reg_im(8), AM_REG,AM_REG,AM_REG);
    as_add(R6,reg_im(12),reg_im(12), AM_REG,AM_REG,AM_REG);
    as_add(R10,R7,reg_im(1), AM_REG,AM_REG,AM_REG);
    as_cgta(reg_im(0),R8, AM_REG,AM_REG);
    as_mv(R8,R6, AM_REG,AM_REG);
    as_mv(R7,R10, AM_REG,AM_REG);
    lib_ba("f_uart_print_L_0");
    label("f_uart_print_L_1");
    lib_pop_regs(R10, R7);
    lib_pop(SP_REG_LINK);
    lib_return();
  }

  public void f_wait()
  {
    // simple wait (r6 x 6 + 7 cycle)
    // input: r6:count
    // use stack:
    // modify: r6
    label("f_wait");
    lib_set_im32(-2);
    as_sub(R6,R6,reg_im(1), AM_REG,AM_REG,AM_REG);
    as_cgta(R6,reg_im(0), AM_REG,AM_REG);
    as_bc();
    lib_wait_delay_slot();
    lib_return();
  }

  // wait vsync
  public void f_wait_vsync()
  {
    // input: none
    // output: none
    // modify: a_addr
    label("f_wait_vsync");
    label("f_wait_vsync_L_0");
    lib_set_im32(VSYNC_ADDRESS);
    lib_wait_reg2addr();
    as_ceq(SP_REG_MVI,reg_im(0), AM_SET,AM_REG);
    lib_bc("f_wait_vsync_L_0");
    label("f_wait_vsync_L_1");
    lib_set_im32(VSYNC_ADDRESS);
    lib_wait_reg2addr();
    as_ceq(SP_REG_MVI,reg_im(1), AM_SET,AM_REG);
    lib_bc("f_wait_vsync_L_1");
    lib_return();
  }

  // datalib --------------------------------

  public void datalib_timer_data()
  {
    label("d_timer_data");
    d32(0);
  }

  public void datalib_rand_data()
  {
    label("d_rand");
    d32(0xfc720c27);
  }

  public void datalib_ascii_chr_data()
  {
    int[] chr_data = {
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,1,0,1,0,0,0,
      0,0,1,0,1,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,1,0,0,0,1,0,0,
      1,1,1,1,1,1,1,0,
      0,1,0,0,0,1,0,0,
      0,1,0,0,0,1,0,0,
      0,1,0,0,0,1,0,0,
      1,1,1,1,1,1,1,0,
      0,1,0,0,0,1,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,1,1,1,1,1,1,0,
      1,0,0,1,0,0,0,0,
      0,1,1,1,1,1,0,0,
      0,0,0,1,0,0,1,0,
      1,1,1,1,1,1,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,0,0,0,0,0,
      1,1,1,0,0,0,1,0,
      1,0,1,0,0,1,0,0,
      1,1,1,0,1,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,1,0,1,1,1,0,
      0,1,0,0,1,0,1,0,
      1,0,0,0,1,1,1,0,
      0,0,0,0,0,0,0,0,
      0,0,1,1,1,0,0,0,
      0,1,0,0,0,1,0,0,
      0,0,1,0,1,0,0,0,
      0,0,0,1,0,0,0,0,
      0,1,1,0,1,0,1,0,
      1,0,0,0,0,1,0,0,
      0,1,1,1,1,0,1,0,
      0,0,0,0,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,1,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,1,0,0,0,0,0,
      0,0,1,0,0,0,0,0,
      0,0,1,0,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,0,1,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,1,0,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,0,1,0,0,0,
      0,0,0,0,1,0,0,0,
      0,0,0,0,1,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,1,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      1,0,0,1,0,0,1,0,
      0,1,0,1,0,1,0,0,
      0,0,1,1,1,0,0,0,
      1,1,1,1,1,1,1,0,
      0,0,1,1,1,0,0,0,
      0,1,0,1,0,1,0,0,
      1,0,0,1,0,0,1,0,
      0,0,0,0,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      1,1,1,1,1,1,1,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,1,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      1,1,1,1,1,1,1,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,1,1,0,0,0,0,
      0,0,1,1,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,1,0,
      0,0,0,0,0,1,0,0,
      0,0,0,0,1,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,1,0,0,0,0,0,
      0,1,0,0,0,0,0,0,
      1,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,1,1,1,1,1,0,0,
      1,0,0,0,0,1,1,0,
      1,0,0,0,1,0,1,0,
      1,0,0,1,0,0,1,0,
      1,0,1,0,0,0,1,0,
      1,1,0,0,0,0,1,0,
      0,1,1,1,1,1,0,0,
      0,0,0,0,0,0,0,0,
      0,0,1,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,1,1,1,0,0,0,
      0,0,0,0,0,0,0,0,
      0,1,1,1,1,1,0,0,
      1,0,0,0,0,0,1,0,
      0,0,0,0,0,0,1,0,
      0,1,1,1,1,1,0,0,
      1,0,0,0,0,0,0,0,
      1,0,0,0,0,0,0,0,
      1,1,1,1,1,1,1,0,
      0,0,0,0,0,0,0,0,
      0,1,1,1,1,1,0,0,
      1,0,0,0,0,0,1,0,
      0,0,0,0,0,0,1,0,
      0,0,1,1,1,1,0,0,
      0,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      0,1,1,1,1,1,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,1,1,1,0,0,
      0,0,1,0,0,1,0,0,
      0,1,0,0,0,1,0,0,
      1,0,0,0,0,1,0,0,
      1,1,1,1,1,1,1,0,
      0,0,0,0,0,1,0,0,
      0,0,0,0,0,1,0,0,
      0,0,0,0,0,0,0,0,
      1,0,0,0,0,0,0,0,
      1,1,1,1,1,1,1,0,
      1,0,0,0,0,0,0,0,
      1,1,1,1,1,1,0,0,
      0,0,0,0,0,0,1,0,
      0,0,0,0,0,0,1,0,
      1,1,1,1,1,1,0,0,
      0,0,0,0,0,0,0,0,
      0,1,1,1,1,1,0,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,0,0,
      1,1,1,1,1,1,0,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      0,1,1,1,1,1,0,0,
      0,0,0,0,0,0,0,0,
      1,1,1,1,1,1,1,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      0,0,0,0,0,1,0,0,
      0,0,0,0,1,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,1,1,1,1,1,0,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      0,1,1,1,1,1,0,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      0,1,1,1,1,1,0,0,
      0,0,0,0,0,0,0,0,
      0,1,1,1,1,1,0,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      0,1,1,1,1,1,1,0,
      0,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      0,1,1,1,1,1,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,1,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,1,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,1,0,0,0,0,0,
      0,1,0,0,0,0,0,0,
      0,0,1,0,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,0,1,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,1,1,1,1,1,0,0,
      0,0,0,0,0,0,0,0,
      0,1,1,1,1,1,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,1,0,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,0,1,0,0,0,
      0,0,0,0,0,1,0,0,
      0,0,0,0,1,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,1,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,1,1,1,1,1,0,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      0,0,0,0,1,1,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,1,1,1,1,1,0,0,
      1,0,0,0,0,0,1,0,
      1,0,1,1,1,0,1,0,
      1,0,1,0,1,0,1,0,
      1,0,1,1,1,1,1,0,
      1,0,0,0,0,0,0,0,
      0,1,1,1,1,1,0,0,
      0,0,0,0,0,0,0,0,
      0,0,1,1,1,0,0,0,
      0,1,0,0,0,1,0,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      1,1,1,1,1,1,1,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      0,0,0,0,0,0,0,0,
      1,1,1,1,1,1,0,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      1,1,1,1,1,1,0,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      1,1,1,1,1,1,0,0,
      0,0,0,0,0,0,0,0,
      0,1,1,1,1,1,0,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,0,0,
      1,0,0,0,0,0,0,0,
      1,0,0,0,0,0,0,0,
      1,0,0,0,0,0,1,0,
      0,1,1,1,1,1,0,0,
      0,0,0,0,0,0,0,0,
      1,1,1,1,1,0,0,0,
      1,0,0,0,0,1,0,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,1,0,0,
      1,1,1,1,1,0,0,0,
      0,0,0,0,0,0,0,0,
      1,1,1,1,1,1,1,0,
      1,0,0,0,0,0,0,0,
      1,0,0,0,0,0,0,0,
      1,1,1,1,1,1,0,0,
      1,0,0,0,0,0,0,0,
      1,0,0,0,0,0,0,0,
      1,1,1,1,1,1,1,0,
      0,0,0,0,0,0,0,0,
      1,1,1,1,1,1,1,0,
      1,0,0,0,0,0,0,0,
      1,0,0,0,0,0,0,0,
      1,1,1,1,1,1,0,0,
      1,0,0,0,0,0,0,0,
      1,0,0,0,0,0,0,0,
      1,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,1,1,1,1,1,0,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,0,0,
      1,0,0,0,1,1,1,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      0,1,1,1,1,1,0,0,
      0,0,0,0,0,0,0,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      1,1,1,1,1,1,1,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      0,0,0,0,0,0,0,0,
      0,1,1,1,1,1,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,1,1,1,1,1,0,0,
      0,0,0,0,0,0,0,0,
      0,0,1,1,1,1,1,0,
      0,0,0,0,1,0,0,0,
      0,0,0,0,1,0,0,0,
      0,0,0,0,1,0,0,0,
      0,0,0,0,1,0,0,0,
      1,0,0,0,1,0,0,0,
      0,1,1,1,0,0,0,0,
      0,0,0,0,0,0,0,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,1,0,0,
      1,0,0,0,1,0,0,0,
      1,1,1,1,0,0,0,0,
      1,0,0,0,1,0,0,0,
      1,0,0,0,0,1,0,0,
      1,0,0,0,0,0,1,0,
      0,0,0,0,0,0,0,0,
      1,0,0,0,0,0,0,0,
      1,0,0,0,0,0,0,0,
      1,0,0,0,0,0,0,0,
      1,0,0,0,0,0,0,0,
      1,0,0,0,0,0,0,0,
      1,0,0,0,0,0,0,0,
      1,1,1,1,1,1,1,0,
      0,0,0,0,0,0,0,0,
      1,0,0,0,0,0,1,0,
      1,1,0,0,0,1,1,0,
      1,0,1,0,1,0,1,0,
      1,0,0,1,0,0,1,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      0,0,0,0,0,0,0,0,
      1,0,0,0,0,0,1,0,
      1,1,0,0,0,0,1,0,
      1,0,1,0,0,0,1,0,
      1,0,0,1,0,0,1,0,
      1,0,0,0,1,0,1,0,
      1,0,0,0,0,1,1,0,
      1,0,0,0,0,0,1,0,
      0,0,0,0,0,0,0,0,
      0,1,1,1,1,1,0,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      0,1,1,1,1,1,0,0,
      0,0,0,0,0,0,0,0,
      1,1,1,1,1,1,0,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      1,1,1,1,1,1,0,0,
      1,0,0,0,0,0,0,0,
      1,0,0,0,0,0,0,0,
      1,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,1,1,1,1,1,0,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      1,0,0,1,0,0,1,0,
      1,0,0,0,1,0,1,0,
      1,0,0,0,0,1,1,0,
      0,1,1,1,1,1,1,0,
      0,0,0,0,0,0,0,0,
      1,1,1,1,1,1,0,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      1,1,1,1,1,1,0,0,
      1,0,0,0,1,0,0,0,
      1,0,0,0,0,1,0,0,
      1,0,0,0,0,0,1,0,
      0,0,0,0,0,0,0,0,
      0,1,1,1,1,1,0,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,0,0,
      0,1,1,1,1,1,0,0,
      0,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      0,1,1,1,1,1,0,0,
      0,0,0,0,0,0,0,0,
      1,1,1,1,1,1,1,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,0,0,0,0,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      0,1,1,1,1,1,0,0,
      0,0,0,0,0,0,0,0,
      1,0,0,0,0,0,1,0,
      1,0,0,0,0,0,1,0,
      0,1,0,0,0,1,0,0,
      0,1,0,0,0,1,0,0,
      0,0,1,0,1,0,0,0,
      0,0,1,0,1,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,0,0,0,0,0,
      1,0,0,1,0,0,1,0,
      1,0,0,1,0,0,1,0,
      1,0,0,1,0,0,1,0,
      1,0,1,0,1,0,1,0,
      1,0,1,0,1,0,1,0,
      1,0,1,0,1,0,1,0,
      0,1,0,0,0,1,0,0,
      0,0,0,0,0,0,0,0,
      1,0,0,0,0,0,1,0,
      0,1,0,0,0,1,0,0,
      0,0,1,0,1,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,1,0,1,0,0,0,
      0,1,0,0,0,1,0,0,
      1,0,0,0,0,0,1,0,
      0,0,0,0,0,0,0,0,
      1,0,0,0,0,0,1,0,
      0,1,0,0,0,1,0,0,
      0,0,1,0,1,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,0,0,0,0,0,
      1,1,1,1,1,1,1,0,
      0,0,0,0,0,1,0,0,
      0,0,0,0,1,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,1,0,0,0,0,0,
      0,1,0,0,0,0,0,0,
      1,1,1,1,1,1,1,0,
      0,0,0,0,0,0,0,0,
      0,0,0,1,1,1,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,1,1,0,0,
      0,0,0,0,0,0,0,0,
      1,0,0,0,0,0,0,0,
      0,1,0,0,0,0,0,0,
      0,0,1,0,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,0,1,0,0,0,
      0,0,0,0,0,1,0,0,
      0,0,0,0,0,0,1,0,
      0,0,0,0,0,0,0,0,
      0,1,1,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,1,1,1,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,1,0,0,0,0,
      0,0,1,0,1,0,0,0,
      0,1,0,0,0,1,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,
      0,1,1,1,1,1,0,0
    };
    label("d_ascii_chr");
    for (int i = 0; i < chr_data.length / 32; i++)
    {
      int data = 0;
      for (int j = 0; j < 32; j++)
      {
        data = data << 1;
        data = data | chr_data[i * 32 + 31 - j];
      }
      d32(data);
    }
  }
}
