/*
  Copyright (c) 2017, miya
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
  public static final int SPRITE_ADDRESS = 0x00001000;
  public static final int IOREG_R_ADDRESS = 0x00002000;
  public static final int IOREG_W_ADDRESS = 0x00003000;
  public static final int CHR_CHR_ADDRESS = 0x00006000;
  public static final int CHR_BIT_ADDRESS = 0x00007000;
  public static final int MEM_S_ADDRESS = 0x00010000;

  public static final int UART_BUSY_ADDRESS = 0x00002000;
  public static final int VSYNC_ADDRESS = 0x00002001;
  public static final int VCOUNT_ADDRESS = 0x00002002;
  public static final int AUDIO_FULL_ADDRESS = 0x00002003;
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

  public static final int LOOP_MODE_REG = 0;
  public static final int LOOP_MODE_IM = 1;

  public int stackAddress = ((1 << DEPTH_D) - 1);

  // set default stack address
  public void set_stack_address(int addr)
  {
    stackAddress = addr;
  }

  // lib_* --------------------------------

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

  // convert int16 to int32
  public void lib_int16_to_32(int reg)
  {
    // if (reg > 0x00007fff) reg |= 0xffff0000;
    as_mvi(0x7fff);
    as_cgt(reg,SP_REG_MVI, AM_REG,AM_REG);
    as_add(SP_REG_MVI,reg,reg_im(0), AM_REG,AM_REG,AM_REG);
    as_mvih(0xffff);
    as_mv(reg,SP_REG_MVI, AM_REG,AM_REG);
  }

  // convert int32 to int16
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

  // r[reg] = value (16bit)
  public void lib_set_im(int value)
  {
    as_mvi((int)value);
  }

  // r[reg] = value (32bit)
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

  // show register and stop
  public void lib_stop(int reg)
  {
    lib_reg2led(reg);
    as_halt();
    lib_wait_delay_slot();
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

  // memory fill
  // input: r6:start_address r7:size r8:fill_data
  // output: none
  // modify: r7,r9,d_addr
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

  // return random value (0 ~ N)
  public void f_nrand()
  {
    // input: r6:max number N (16bit)
    // output: r6:random (0 ~ N)
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
    lib_set_im32(addr_abs("d_rand"));
    as_sl(R7,R6,reg_im(5), AM_REG,AM_REG,AM_REG); // r7 = r6 << 5
    as_xor(R6,R7,R6, AM_REG,AM_REG,AM_REG); // r6 = r7 ^ r6
    as_add(SP_REG_MVI,R6,reg_im(0), AM_SET,AM_REG,AM_REG); // mem[d] = r6 + 0
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
