/*
  Copyright (c) 2015 miya
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

import java.io.*;
import java.util.*;

public class Asm
{
  private static final int DATA_WIDTH = 32;
  private static final int OPERAND_BITS = 6;
  private static final int REG_IM_BITS = 5;
  private static final int W_CODE = 0;
  private static final int W_DATA = 1;

  private int romDepth = 8;
  private int codeROMDepth = 8;
  private int dataROMDepth = 8;
  private int pAddress;
  private int pass;
  private String fileName = "default";
  private final ArrayList<Integer> data = new ArrayList<Integer>();
  private final HashMap<String, Integer> labelValue = new HashMap<String, Integer>();

  // opcode
  // special type
  private static final int I_HALT = 0x00;
  private static final int I_NOP  = 0x01;
  private static final int I_MV   = 0x02;
  private static final int I_MVI  = 0x03;
  private static final int I_MVIH = 0x04;
  private static final int I_CEQ  = 0x05;
  private static final int I_CGT  = 0x06;
  private static final int I_CGTA = 0x07;
  private static final int I_BC   = 0x08;
  private static final int I_BL   = 0x09;
  private static final int I_BA   = 0x0a;
  private static final int I_LOOP = 0x0b;
  // normal type
  private static final int I_ADD  = 0x40;
  private static final int I_SUB  = 0x41;
  private static final int I_AND  = 0x42;
  private static final int I_OR   = 0x43;
  private static final int I_XOR  = 0x44;
  private static final int I_NOT  = 0x45;
  private static final int I_SR   = 0x46;
  private static final int I_SL   = 0x47;
  private static final int I_SRA  = 0x48;
  private static final int I_MUL  = 0x49;

  public void write_verilog(int mode)
  {
    try
    {
      String name;
      if (mode == W_CODE)
      {
        name = "code";
        romDepth = codeROMDepth;
      }
      else
      {
        name = "data";
        romDepth = dataROMDepth;
      }
      String hdlName = fileName + "_" + name + "_rom";
      File file = new File("../" + hdlName + ".v");
      file.createNewFile();
      PrintWriter writer = new PrintWriter(file);
      writer.printf(
        "module %s\n", hdlName);
      writer.printf(
        "  (\n" +
        "    input wire clk,\n");
      writer.printf(
        "    input wire [%d:0] addr,\n", (romDepth - 1));
      writer.printf(
        "    output reg [%d:0] data_out\n", (DATA_WIDTH - 1));
      writer.printf(
        "  );\n" +
        "\n" +
        "  always @(posedge clk)\n" +
        "    begin\n" +
        "      case (addr)\n");
      for (int i = 0; i < (1 << romDepth); i++)
      {
        int d;
        if (i < data.size())
        {
          d = data.get(i);
        }
        else
        {
          d = 0;
        }
        writer.printf("        32'h%08x: data_out <= 32'h%08x;\n", i, d);
      }
      writer.printf(
        "      endcase\n" +
        "    end\n" +
        "endmodule\n");
      writer.close();
    }
    catch (Exception e)
    {
    }
  }

  public void write_mem(int mode)
  {
    try
    {
      String name;
      if (mode == W_CODE)
      {
        name = "code";
        romDepth = codeROMDepth;
      }
      else
      {
        name = "data";
        romDepth = dataROMDepth;
      }
      String hdlName = fileName + "_" + name + "_mem";
      File file = new File("../" + hdlName + ".v");
      file.createNewFile();
      PrintWriter writer = new PrintWriter(file);
      writer.printf(
        "module %s\n", hdlName);
      writer.printf(
        "  #(\n" +
        "    parameter DATA_WIDTH=32,\n");
      writer.printf(
        "    parameter ADDR_WIDTH=%d\n", romDepth);
      writer.printf(
        "    )\n" +
        "  (\n" +
        "   input wire                    clk,\n" +
        "   input wire [(ADDR_WIDTH-1):0] addr_r,\n" +
        "   input wire [(ADDR_WIDTH-1):0] addr_w,\n" +
        "   input wire [(DATA_WIDTH-1):0] data_in,\n" +
        "   input wire                    we,\n" +
        "   output reg [(DATA_WIDTH-1):0] data_out\n" +
        "   );\n" +
        "\n" +
        "  reg [DATA_WIDTH-1:0]           ram [0:(1 << ADDR_WIDTH)-1];\n" +
        "\n" +
        "  always @(posedge clk)\n" +
        "    begin\n" +
        "      data_out <= ram[addr_r];\n" +
        "      if (we)\n" +
        "        begin\n" +
        "          ram[addr_w] <= data_in;\n" +
        "        end\n" +
        "    end\n" +
        "\n" +
        "  initial\n" +
        "    begin\n");
      for (int i = 0; i < (1 << romDepth); i++)
      {
        int d;
        if (i < data.size())
        {
          d = data.get(i);
        }
        else
        {
          d = 0;
        }
        writer.printf("      ram[32'h%08x] = 32'h%08x;\n", i, d);
      }
      writer.printf(
        "    end\n" +
        "\n" +
        "endmodule\n");
      writer.close();
    }
    catch (Exception e)
    {
    }
  }

  public void write_testbench(int mode)
  {
    try
    {
      String name;
      int offset;
      if (mode == W_CODE)
      {
        name = "code";
        offset = 0x4000;
      }
      else
      {
        name = "data";
        offset = 0x0000;
      }
      String hdlName = fileName + "_" + name + "_tb";
      File file = new File("../" + hdlName + ".txt");
      file.createNewFile();
      PrintWriter writer = new PrintWriter(file);
      for (int i = 0; i < data.size(); i++)
      {
        writer.printf("      uart_word(32'h%08x, 32'h%08x);\n", i + offset, data.get(i));
      }
      writer.close();
    }
    catch (Exception e)
    {
    }
  }

  public void write_binary(int mode)
  {
    try
    {
      String name;
      if (mode == W_CODE)
      {
        name = "code";
      }
      else
      {
        name = "data";
      }
      String binName = fileName + "_" + name;
      File file = new File("../" + binName + ".bin");
      file.createNewFile();
      FileOutputStream fos = new FileOutputStream(file);
      DataOutputStream dos = new DataOutputStream(fos);
      for (int i = 0; i < data.size(); i++)
      {
        dos.writeInt(data.get(i));
      }
      dos.flush();
      dos.close();
    }
    catch (Exception e)
    {
    }
  }

  public void init()
  {
    // init: must be implemented in sub-classes
  }

  public void program()
  {
    // program: must be implemented in sub-classes
  }

  public void data()
  {
    // data: must be implemented in sub-classes
  }

  // Print Error
  public void print_error(String err)
  {
    System.out.printf("Error: %s Address: %d\n", err, pAddress);
    System.exit(1);
  }

  // Print Info
  public void print_info(String info)
  {
    if (pass == 1)
    {
      System.out.println(info);
    }
  }

  // set rom depth
  public void set_rom_depth(int depth)
  {
    romDepth = depth;
    codeROMDepth = depth;
    dataROMDepth = depth;
  }

  // set rom depth
  public void set_rom_depth(int code_depth, int data_depth)
  {
    romDepth = code_depth;
    codeROMDepth = code_depth;
    dataROMDepth = data_depth;
  }

  // set filename
  public void set_filename(String name)
  {
    fileName = name;
  }

  // calculate REG_IM value
  public int reg_im(int value)
  {
    if ((value > 15) || (value < -16))
    {
      print_error("reg_im: -16 <= value <= 15");
    }
    return (value & ((1 << REG_IM_BITS) - 1)) | 0x20;
  }

  // label (hold the current program counter)
  public void label(String key)
  {
    if (pass == 0)
    {
      labelValue.put(key, pAddress);
    }
    print_info(String.format("Label: %s: %x", key, pAddress));
  }

  // return the absolute address of the label
  public int addr_abs(String key)
  {
    if (pass == 0)
    {
      return 0;
    }
    if (labelValue.get(key) == null)
    {
      print_error("addr_abs: key: " + key);
      return 0;
    }
    return labelValue.get(key);
  }

  // return the relative address between the current line and the label
  public int addr_rel(String key)
  {
    if (pass == 0)
    {
      return 0;
    }
    if (labelValue.get(key) == null)
    {
      print_error("addr_rel: key: " + key);
      return 0;
    }
    return labelValue.get(key) - pAddress;
  }

  // calculate struct address
  public int addr_struct(String key)
  {
    if (pass == 0)
    {
      return 0;
    }
    if (labelValue.get(key) == null)
    {
      print_error("addr_struct: key: " + key);
      return 0;
    }
    return (labelValue.get(key) + (1 << (OPERAND_BITS - 1)));
  }

  public void do_asm()
  {
    init();

    labelValue.clear();
    data.clear();

    pass = 0; // pass 1
    pAddress = 0;
    data.clear();
    program();
    pAddress = 0;
    data.clear();
    data();

    pass++; // pass 2
    pAddress = 0;
    data.clear();
    program();
    write_verilog(W_CODE);
    write_binary(W_CODE);
    write_testbench(W_CODE);
    write_mem(W_CODE);
    pAddress = 0;
    data.clear();
    data();
    write_verilog(W_DATA);
    write_binary(W_DATA);
    write_testbench(W_DATA);
    write_mem(W_DATA);
  }

  private void store_inst(int inst)
  {
    if (pass == 1)
    {
      if (pAddress > (1 << romDepth))
      {
        print_error("memory full");
      }
      data.add(pAddress, inst);
    }
    pAddress++;
  }

  private int cut_bits(int bits, int value)
  {
    return (value & ((1 << bits) - 1));
  }

  private int set_field(int shift, int bits, int value)
  {
    return (cut_bits(bits, value) << shift);
  }

  private void set_inst_normal(int reg_d, int reg_a, int reg_b, int addr_mode_d, int addr_mode_a, int addr_mode_b, int op)
  {
    int inst = 0;
    inst |= set_field(26, 6, reg_d);
    inst |= set_field(20, 6, reg_a);
    inst |= set_field(14, 6, reg_b);
    inst |= set_field(11, 2, addr_mode_d);
    inst |= set_field(9, 2, addr_mode_a);
    inst |= set_field(7, 2, addr_mode_b);
    inst |= set_field(0, 7, op);
    store_inst(inst);
  }

  private void set_inst_im(int im, int op)
  {
    int inst = 0;
    inst |= set_field(16, 16, im);
    inst |= set_field(0, 7, op);
    store_inst(inst);
  }

  // data store
  public void d32(int value0)
  {
    store_inst(value0);
  }

  // data store x4
  public void d32x4(int value0, int value1, int value2, int value3)
  {
    store_inst(value0);
    store_inst(value1);
    store_inst(value2);
    store_inst(value3);
  }

  // structHelper data store
  public void store_struct(StructHelper sh)
  {
    for (String key : sh.keySet())
    {
      d32(sh.value(key));
    }
  }

  // store byte to int
  public int byte_store(int value_int, int value_byte, int position)
  {
    int mask = ~(0xff << (position * 8));
    int out = (value_int & mask) | (value_byte << (position * 8));
    return out;
  }

  // string data store
  public void string_data(String s)
  {
    int length = (s.length() + 4) & (~3);
    int byte_count = 3;
    int value_int = 0;
    for (int i = 0; i < length; i++)
    {
      int value_byte = 0;
      if (i < s.length())
      {
        value_byte = s.charAt(i);
      }
      value_int = byte_store(value_int, value_byte, byte_count);
      if (byte_count == 0)
      {
        store_inst(value_int);
        byte_count = 3;
      }
      else
      {
        byte_count--;
      }
    }
  }

  // assembly

  public void as_halt()
  {
    set_inst_normal(0, 0, 0, 0, 0, 0, I_HALT);
  }

  public void as_nop(int reg_d, int reg_a, int reg_b, int addr_mode_d, int addr_mode_a, int addr_mode_b)
  {
    set_inst_normal(reg_d, reg_a, reg_b, addr_mode_d, addr_mode_a, addr_mode_b, I_NOP);
  }

  public void as_mv(int reg_d, int reg_a, int addr_mode_d, int addr_mode_a)
  {
    set_inst_normal(reg_d, reg_a, 0, addr_mode_d, addr_mode_a, 0, I_MV);
  }

  public void as_mvi(int value)
  {
    set_inst_im(value, I_MVI);
  }

  public void as_mvih(int value)
  {
    set_inst_im(value, I_MVIH);
  }

  public void as_ceq(int reg_a, int reg_b, int addr_mode_a, int addr_mode_b)
  {
    set_inst_normal(0, reg_a, reg_b, 0, addr_mode_a, addr_mode_b, I_CEQ);
  }

  public void as_cgt(int reg_a, int reg_b, int addr_mode_a, int addr_mode_b)
  {
    set_inst_normal(0, reg_a, reg_b, 0, addr_mode_a, addr_mode_b, I_CGT);
  }

  public void as_cgta(int reg_a, int reg_b, int addr_mode_a, int addr_mode_b)
  {
    set_inst_normal(0, reg_a, reg_b, 0, addr_mode_a, addr_mode_b, I_CGTA);
  }

  public void as_bc()
  {
    set_inst_normal(0, 0, 0, 0, 0, 0, I_BC);
  }

  public void as_bl()
  {
    set_inst_normal(0, 0, 0, 0, 0, 0, I_BL);
  }

  public void as_ba()
  {
    set_inst_normal(0, 0, 0, 0, 0, 0, I_BA);
  }

  public void as_loop()
  {
    set_inst_normal(0, 0, 0, 0, 0, 0, I_LOOP);
  }

  public void as_add(int reg_d, int reg_a, int reg_b, int addr_mode_d, int addr_mode_a, int addr_mode_b)
  {
    set_inst_normal(reg_d, reg_a, reg_b, addr_mode_d, addr_mode_a, addr_mode_b, I_ADD);
  }

  public void as_sub(int reg_d, int reg_a, int reg_b, int addr_mode_d, int addr_mode_a, int addr_mode_b)
  {
    set_inst_normal(reg_d, reg_a, reg_b, addr_mode_d, addr_mode_a, addr_mode_b, I_SUB);
  }

  public void as_and(int reg_d, int reg_a, int reg_b, int addr_mode_d, int addr_mode_a, int addr_mode_b)
  {
    set_inst_normal(reg_d, reg_a, reg_b, addr_mode_d, addr_mode_a, addr_mode_b, I_AND);
  }

  public void as_or(int reg_d, int reg_a, int reg_b, int addr_mode_d, int addr_mode_a, int addr_mode_b)
  {
    set_inst_normal(reg_d, reg_a, reg_b, addr_mode_d, addr_mode_a, addr_mode_b, I_OR);
  }

  public void as_xor(int reg_d, int reg_a, int reg_b, int addr_mode_d, int addr_mode_a, int addr_mode_b)
  {
    set_inst_normal(reg_d, reg_a, reg_b, addr_mode_d, addr_mode_a, addr_mode_b, I_XOR);
  }

  public void as_not(int reg_d, int reg_a, int addr_mode_d, int addr_mode_a)
  {
    set_inst_normal(reg_d, reg_a, 0, addr_mode_d, addr_mode_a, 0, I_NOT);
  }

  public void as_sr(int reg_d, int reg_a, int reg_b, int addr_mode_d, int addr_mode_a, int addr_mode_b)
  {
    set_inst_normal(reg_d, reg_a, reg_b, addr_mode_d, addr_mode_a, addr_mode_b, I_SR);
  }

  public void as_sl(int reg_d, int reg_a, int reg_b, int addr_mode_d, int addr_mode_a, int addr_mode_b)
  {
    set_inst_normal(reg_d, reg_a, reg_b, addr_mode_d, addr_mode_a, addr_mode_b, I_SL);
  }

  public void as_sra(int reg_d, int reg_a, int reg_b, int addr_mode_d, int addr_mode_a, int addr_mode_b)
  {
    set_inst_normal(reg_d, reg_a, reg_b, addr_mode_d, addr_mode_a, addr_mode_b, I_SRA);
  }

  public void as_mul(int reg_d, int reg_a, int reg_b, int addr_mode_d, int addr_mode_a, int addr_mode_b)
  {
    set_inst_normal(reg_d, reg_a, reg_b, addr_mode_d, addr_mode_a, addr_mode_b, I_MUL);
  }
}


class StructHelperItem
{
  public int address;
  public int value;
}


class StructHelper
{
  private static final int OPERAND_BITS = 6;
  private LinkedHashMap<String, StructHelperItem> map = new LinkedHashMap<String, StructHelperItem>();
  private int address = 0;

  public void clear()
  {
    map.clear();
  }

  public void add(String s, int value)
  {
    map.put(s, new StructHelperItem());
    map.get(s).address = address;
    map.get(s).value = value;
    address++;
    if (address > 64)
    {
      print_error("StructHelper Items <= 64");
    }
  }

  public void update(String s, int value)
  {
    map.get(s).value = value;
  }

  public StructHelperItem get(String s)
  {
    return map.get(s);
  }

  public Set<String> keySet()
  {
    return map.keySet();
  }

  public int offset(String s)
  {
    StructHelperItem item = map.get(s);
    if (item == null)
    {
      print_error("StructHelper: offset: key: " + s);
    }
    return (map.get(s).address - (1 << (OPERAND_BITS - 1)));
  }

  public int value(String s)
  {
    return map.get(s).value;
  }

  public int size()
  {
    return map.size();
  }

  public void print_error(String err)
  {
    System.out.printf("Error: %s\n", err);
    System.exit(1);
  }
}
