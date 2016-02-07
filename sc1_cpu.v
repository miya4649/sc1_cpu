/*
  Copyright (c) 2015-2016, miya
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/


module sc1_cpu
  (
   input             clk,
   input             reset,
   output reg [7:0]  rom_addr,
   input [31:0]      rom_data,
   input [31:0]      port_in,
   output reg [31:0] port_out
   );

  parameter WIDTH_I = 32;
  parameter WIDTH_D = 32;
  parameter WIDTH_REG = 32;
  parameter DEPTH_I = 8;
  parameter DEPTH_D = 8;
  parameter DEPTH_REG = 4;

  parameter SP_REG_MVI = 4'd0;
  parameter SP_REG_BA = 4'd0;
  parameter SP_REG_CP = 4'd1;
  parameter SP_REG_LINK = 4'd2;
  parameter SP_REG_LOOP_COUNTER = 4'd3;
  parameter SP_REG_LOOP_END = 4'd4;
  parameter SP_REG_LOOP_SPAN = 4'd5;

  // opcode
  // special type
  parameter I_HALT = 7'h00;
  parameter I_NOP  = 7'h01;
  parameter I_MV   = 7'h02;
  parameter I_MVI  = 7'h03;
  parameter I_MVIH = 7'h04;
  parameter I_CEQ  = 7'h05;
  parameter I_CGT  = 7'h06;
  parameter I_CGTA = 7'h07;
  parameter I_BC   = 7'h08;
  parameter I_BL   = 7'h09;
  parameter I_BA   = 7'h0a;
  parameter I_LOOP = 7'h0b;
  parameter I_OUT  = 7'h0c;
  // normal type
  parameter I_ADD  = 7'h40;
  parameter I_SUB  = 7'h41;
  parameter I_AND  = 7'h42;
  parameter I_OR   = 7'h43;
  parameter I_XOR  = 7'h44;
  parameter I_NOT  = 7'h45;
  parameter I_SR   = 7'h46;
  parameter I_SL   = 7'h47;
  parameter I_SRA  = 7'h48;
  parameter I_MUL  = 7'h49;
  parameter I_IN   = 7'h4a;

  parameter TRUE = 1'b1;
  parameter FALSE = 1'b0;
  parameter ONE = 1'd1;
  parameter ZERO = 1'd0;
  parameter FFFF = {WIDTH_D{1'b1}};

  wire [WIDTH_I-1:0] mem_i_o;
  reg [DEPTH_I-1:0]  mem_i_addr_r;
  reg [DEPTH_I-1:0]  mem_i_addr_w;
  reg [WIDTH_I-1:0]  mem_i_i;
  reg                mem_i_we;

  wire [WIDTH_D-1:0] mem_d_o_a;
  wire [WIDTH_D-1:0] mem_d_o_b;
  wire               mem_d_we_sig;
  reg [WIDTH_D-1:0]  mem_d_i;
  reg [DEPTH_D-1:0]  mem_d_addr_w;
  reg [DEPTH_D-1:0]  mem_d_addr_w_d1;
  reg [DEPTH_D-1:0]  mem_d_addr_w_d2;
  reg [DEPTH_D-1:0]  mem_d_addr_r_a;
  reg [DEPTH_D-1:0]  mem_d_addr_r_b;
  reg                mem_d_we;

  reg                cpu_en;
  reg [DEPTH_I-1:0]  pc_d1;
  reg [DEPTH_I-1:0]  pc_d2;
  reg [10:0]         stage_init;
  reg [WIDTH_D-1:0]  loop_counter;
  reg [DEPTH_I-1:0]  loop_end;
  reg [DEPTH_I-1:0]  loop_span;

  wire               is_mem_d_s1;
  wire               is_mem_a_s1;
  wire               is_mem_b_s1;
  wire               add_d_s1;
  wire               add_a_s1;
  wire               add_b_s1;
  wire [DEPTH_REG-1:0] reg_d_addr_s1;
  wire [DEPTH_REG-1:0] reg_a_addr_s1;
  wire [DEPTH_REG-1:0] reg_b_addr_s1;

  reg [WIDTH_I-1:0]    mem_i_o_d1;
  wire [6:0]           op;
  wire                 is_type_normal;
  wire                 not_increment;
  wire                 is_mem_d;
  wire                 is_mem_a;
  wire                 is_mem_b;
  wire [DEPTH_REG-1:0] reg_d_addr;
  wire [DEPTH_REG-1:0] reg_a_addr;
  wire [DEPTH_REG-1:0] reg_b_addr;
  wire [15:0]          im16;
  wire signed [15:0]   ims16;

  wire [WIDTH_D-1:0]   source_a;
  wire [WIDTH_D-1:0]   source_b;

  // register file
  reg [WIDTH_REG-1:0]  reg_file[(1 << DEPTH_REG)-1:0];

  // decode(stage1)
  assign is_mem_d_s1 = mem_i_o[9];
  assign is_mem_a_s1 = mem_i_o[8];
  assign is_mem_b_s1 = mem_i_o[7];
  assign add_d_s1 = mem_i_o[12];
  assign add_a_s1 = mem_i_o[11];
  assign add_b_s1 = mem_i_o[10];
  assign reg_d_addr_s1 = mem_i_o[DEPTH_REG+26-1:26];
  assign reg_a_addr_s1 = mem_i_o[DEPTH_REG+20-1:20];
  assign reg_b_addr_s1 = mem_i_o[DEPTH_REG+14-1:14];

  // decode(stage2)
  assign op = mem_i_o_d1[6:0];
  assign is_type_normal = mem_i_o_d1[6];
  assign is_mem_d = mem_i_o_d1[9];
  assign is_mem_a = mem_i_o_d1[8];
  assign is_mem_b = mem_i_o_d1[7];
  assign reg_d_addr = mem_i_o_d1[DEPTH_REG+26-1:26];
  assign reg_a_addr = mem_i_o_d1[DEPTH_REG+20-1:20];
  assign reg_b_addr = mem_i_o_d1[DEPTH_REG+14-1:14];
  assign im16 = mem_i_o_d1[25:10];
  assign ims16 = mem_i_o_d1[25:10];

  // manual pc increment
  assign not_increment = ((op == I_HALT) || (op == I_BC) || (op == I_BL) || (op == I_BA)) ? 1'b1 : 1'b0;

  // switch source
  assign source_a = is_mem_a ? mem_d_o_a : reg_file[reg_a_addr];
  assign source_b = is_mem_b ? mem_d_o_b : reg_file[reg_b_addr];

  // switch operation
  function [WIDTH_D-1:0] result
    (
     input [6:0] op_result
     );
    begin
      case (op_result)
        I_ADD:   result = source_a + source_b;
        I_SUB:   result = source_a - source_b;
        I_AND:   result = source_a & source_b;
        I_OR:    result = source_a | source_b;
        I_XOR:   result = source_a ^ source_b;
        I_NOT:   result = ~source_a;
        I_SR:    result = source_a >> source_b;
        I_SL:    result = source_a << source_b;
        I_SRA:   result = $signed(source_a) >>> source_b;
        I_MUL:   result = $signed(source_a) * $signed(source_b);
        I_IN:    result = port_in;
        default: result = result;
      endcase
    end
  endfunction

  // mem_d_we condition
  assign mem_d_we_sig = is_mem_d & (is_type_normal | (op == I_MV));

  always @(posedge clk)
    begin
      if (reset == TRUE)
        begin
          stage_init <= ZERO;
          cpu_en <= FALSE;
          mem_i_addr_r <= ZERO;
          mem_i_addr_w <= ZERO;
          mem_i_we <= FALSE;
          port_out <= ZERO;
          loop_counter <= ZERO;
          loop_end <= ZERO;
          loop_span <= ZERO;
        end
      else if (cpu_en == FALSE)
        // init
        begin
          if (stage_init < 11'h400)
            begin
              case (stage_init[1:0])
                // load program from ROM
                2'd0:
                  begin
                    rom_addr <= stage_init[9:2];
                  end
                2'd1:
                  begin
                  end
                2'd2:
                  begin
                    mem_i_addr_w <= stage_init[9:2];
                    mem_i_i <= rom_data;
                    mem_i_we <= TRUE;
                  end
                2'd3:
                  begin
                    mem_i_we <= FALSE;
                  end
                default: ;
              endcase
              stage_init <= stage_init + ONE;
            end
          else
            begin
              cpu_en <= TRUE;
            end
        end
      else
        // cpu enable
        begin
          // increment mem_d address automatically
          if (is_mem_d_s1)
            begin
              if (add_d_s1)
                begin
                  mem_d_addr_w <= mem_d_addr_w + reg_file[reg_d_addr_s1][DEPTH_D-1:0];
                end
              else
                begin
                  mem_d_addr_w <= reg_file[reg_d_addr_s1][DEPTH_D-1:0];
                end
            end
          if (is_mem_a_s1)
            begin
              if (add_a_s1)
                begin
                  mem_d_addr_r_a <= mem_d_addr_r_a + reg_file[reg_a_addr_s1][DEPTH_D-1:0];
                end
              else
                begin
                  mem_d_addr_r_a <= reg_file[reg_a_addr_s1][DEPTH_D-1:0];
                end
            end
          if (is_mem_b_s1)
            begin
              if (add_b_s1)
                begin
                  mem_d_addr_r_b <= mem_d_addr_r_b + reg_file[reg_b_addr_s1][DEPTH_D-1:0];
                end
              else
                begin
                  mem_d_addr_r_b <= reg_file[reg_b_addr_s1][DEPTH_D-1:0];
                end
            end

          // delay
          mem_i_o_d1 <= mem_i_o;
          pc_d2 <= pc_d1;
          pc_d1 <= mem_i_addr_r;
          mem_d_we <= mem_d_we_sig;
          mem_d_addr_w_d1 <= mem_d_addr_w;
          mem_d_addr_w_d2 <= mem_d_addr_w_d1;

          // loop counter
          if (loop_end == mem_i_addr_r)
            begin
              if ((loop_counter != ZERO) && (op != I_LOOP))
                begin
                  loop_counter <= loop_counter - ONE;
                end
            end

          // increment pc (prefetch address)
          if (!not_increment)
            begin
              if (loop_end == mem_i_addr_r)
                begin
                  if (loop_counter == ZERO)
                    begin
                      mem_i_addr_r <= mem_i_addr_r + ONE;
                    end
                  else
                    begin
                      mem_i_addr_r <= mem_i_addr_r + loop_span;
                    end
                end
              else
                begin
                  mem_i_addr_r <= mem_i_addr_r + ONE;
                end
            end

          // execution
          if (is_type_normal)
            begin
              // for normal instructions
              if (is_mem_d)
                begin
                  mem_d_i <= result(op);
                end
              else
                begin
                  reg_file[reg_d_addr] <= result(op);
                end
            end
          else
            begin
              // special instructions
              case (op)
                I_HALT:
                  begin
                    mem_i_addr_r <= pc_d2;
                  end
                I_NOP:
                  begin
                  end
                I_MV:
                  begin
                    if (reg_file[SP_REG_CP] != ZERO)
                      begin
                        if (is_mem_d)
                          begin
                            mem_d_i <= source_a;
                          end
                        else
                          begin
                            reg_file[reg_d_addr] <= source_a;
                          end
                      end
                  end
                I_MVI:
                  begin
                    reg_file[SP_REG_MVI] <= im16;
                  end
                I_MVIH:
                  begin
                    reg_file[SP_REG_MVI] <= {im16, reg_file[SP_REG_MVI][15:0]};
                  end
                I_CEQ:
                  begin
                    if (source_a == source_b)
                      begin
                        reg_file[SP_REG_CP] <= FFFF;
                      end
                    else
                      begin
                        reg_file[SP_REG_CP] <= ZERO;
                      end
                  end
                I_CGT:
                  begin
                    if (source_a > source_b)
                      begin
                        reg_file[SP_REG_CP] <= FFFF;
                      end
                    else
                      begin
                        reg_file[SP_REG_CP] <= ZERO;
                      end
                  end
                I_CGTA:
                  begin
                    if ($signed(source_a) > $signed(source_b))
                      begin
                        reg_file[SP_REG_CP] <= FFFF;
                      end
                    else
                      begin
                        reg_file[SP_REG_CP] <= ZERO;
                      end
                  end
                I_BC:
                  begin
                    if (reg_file[SP_REG_CP] == ZERO)
                      begin
                        mem_i_addr_r <= mem_i_addr_r + ONE;
                      end
                    else
                      begin
                        mem_i_addr_r <= pc_d2 + ims16;
                      end
                  end
                I_BL:
                  begin
                    reg_file[SP_REG_LINK] <= pc_d2 + ONE;
                    mem_i_addr_r <= pc_d2 + ims16;
                  end
                I_BA:
                  begin
                    mem_i_addr_r <= reg_file[SP_REG_BA];
                  end
                I_LOOP:
                  begin
                    loop_counter <= reg_file[SP_REG_LOOP_COUNTER];
                    loop_end <= pc_d2 + reg_file[SP_REG_LOOP_END][DEPTH_I-1:0];
                    loop_span <= reg_file[SP_REG_LOOP_SPAN][DEPTH_I-1:0];
                  end
                I_OUT:
                  begin
                    port_out <= source_a;
                  end
                default: ;
              endcase
            end
        end
    end
  


  rw_port_ram
    #(
      .DATA_WIDTH (WIDTH_I),
      .ADDR_WIDTH (DEPTH_I)
      )
  mem_i
    (
     .clk (clk),
     .addr_r (mem_i_addr_r),
     .addr_w (mem_i_addr_w),
     .data_in (mem_i_i),
     .we (mem_i_we),
     .data_out (mem_i_o) // latency:1
     );

  rw_port_ram
    #(
      .DATA_WIDTH (WIDTH_D),
      .ADDR_WIDTH (DEPTH_D)
      )
  mem_d_a
    (
     .clk (clk),
     .addr_r (mem_d_addr_r_a),
     .addr_w (mem_d_addr_w_d2),
     .data_in (mem_d_i),
     .we (mem_d_we),
     .data_out (mem_d_o_a)
     );

  rw_port_ram
    #(
      .DATA_WIDTH (WIDTH_D),
      .ADDR_WIDTH (DEPTH_D)
      )
  mem_d_b
    (
     .clk (clk),
     .addr_r (mem_d_addr_r_b),
     .addr_w (mem_d_addr_w_d2),
     .data_in (mem_d_i),
     .we (mem_d_we),
     .data_out (mem_d_o_b)
     );

endmodule
