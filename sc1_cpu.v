/*
  Copyright (c) 2015-2017, miya
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/


module sc1_cpu
  #(
    parameter WIDTH_I = 32,
    parameter WIDTH_D = 32,
    parameter DEPTH_I = 8,
    parameter DEPTH_D = 8,
    parameter DEPTH_REG = 4
    )
  (
   input                    clk,
   input                    reset,
   input                    resume,
   input [WIDTH_I-1:0]      mem_i_r,
   output reg [DEPTH_I-1:0] mem_i_addr_r,
   input [WIDTH_D-1:0]      mem_d_r_a,
   input [WIDTH_D-1:0]      mem_d_r_b,
   output reg [WIDTH_D-1:0] mem_d_w,
   output reg [DEPTH_D-1:0] mem_d_addr_w,
   output reg [DEPTH_D-1:0] mem_d_addr_r_a,
   output reg [DEPTH_D-1:0] mem_d_addr_r_b,
   output reg               mem_d_we,
   output reg               stopped
   );

  localparam SP_REG_MVI = 4'd0;
  localparam SP_REG_BA = 4'd0;
  localparam SP_REG_CP = 4'd1;
  localparam SP_REG_LINK = 4'd2;
  localparam SP_REG_LOOP_COUNTER = 4'd3;
  localparam SP_REG_LOOP_END = 4'd4;
  localparam SP_REG_LOOP_SPAN = 4'd5;

  // opcode
  // special type
  localparam I_HALT = 7'h00;
  localparam I_NOP  = 7'h01;
  localparam I_MV   = 7'h02;
  localparam I_MVI  = 7'h03;
  localparam I_MVIH = 7'h04;
  localparam I_CEQ  = 7'h05;
  localparam I_CGT  = 7'h06;
  localparam I_CGTA = 7'h07;
  localparam I_BC   = 7'h08;
  localparam I_BL   = 7'h09;
  localparam I_BA   = 7'h0a;
  localparam I_LOOP = 7'h0b;
  // normal type
  localparam I_ADD  = 7'h40;
  localparam I_SUB  = 7'h41;
  localparam I_AND  = 7'h42;
  localparam I_OR   = 7'h43;
  localparam I_XOR  = 7'h44;
  localparam I_NOT  = 7'h45;
  localparam I_SR   = 7'h46;
  localparam I_SL   = 7'h47;
  localparam I_SRA  = 7'h48;
  localparam I_MUL  = 7'h49;

  localparam TRUE = 1'b1;
  localparam FALSE = 1'b0;
  localparam ONE = 1'd1;
  localparam ZERO = 1'd0;
  localparam FFFF = {WIDTH_D{1'b1}};
  localparam ADDR_MODE_0 = 2'd0;
  localparam ADDR_MODE_1 = 2'd1;
  localparam ADDR_MODE_2 = 2'd2;
  localparam ADDR_MODE_3 = 2'd3;
  localparam DELAY_SLOT = 2'd3;
  localparam DELAY_SLOT_P1 = 3'd4;
  localparam OPERAND_BITS = 6;
  localparam REG_IM_BITS = 5;
  localparam NOP_OP = ONE;

  reg                       resume_d1;
  wire                      resume_pulse;
  wire                      mem_d_we_sig;
  reg [DEPTH_D-1:0]         mem_d_addr_w_d0;
  reg [DEPTH_D-1:0]         mem_d_addr_w_d1;
  reg [DEPTH_D-1:0]         mem_d_addr_w_s_d0;
  reg [DEPTH_D-1:0]         mem_d_addr_r_a_s;
  reg [DEPTH_D-1:0]         mem_d_addr_r_b_s;

  reg [DEPTH_I-1:0]         pc_d1;
  reg [DEPTH_I-1:0]         pc_d2;
  reg [DEPTH_I-1:0]         pc_d3;
  reg [WIDTH_D-1:0]         loop_counter;
  reg [DEPTH_I-1:0]         loop_end;
  reg [DEPTH_I-1:0]         loop_span;

  wire [1:0]                addr_mode_d_s1;
  wire [1:0]                addr_mode_a_s1;
  wire [1:0]                addr_mode_b_s1;
  wire [DEPTH_REG-1:0]      reg_d_addr_s1;
  wire [DEPTH_REG-1:0]      reg_a_addr_s1;
  wire [DEPTH_REG-1:0]      reg_b_addr_s1;
  wire signed [OPERAND_BITS-1:0] ims_d_s1;
  wire signed [OPERAND_BITS-1:0] ims_a_s1;
  wire signed [OPERAND_BITS-1:0] ims_b_s1;

  wire [WIDTH_I-1:0]             inst;
  reg [WIDTH_I-1:0]              inst_d1;
  reg [WIDTH_I-1:0]              inst_d2;
  wire [1:0]                     addr_mode_d;
  wire [1:0]                     addr_mode_a;
  wire [1:0]                     addr_mode_b;
  wire                           reg_mode_a;
  wire                           reg_mode_b;
  wire [6:0]                     op;
  wire                           is_type_normal;
  wire                           not_increment;
  wire [DEPTH_REG-1:0]           reg_d_addr;
  wire [DEPTH_REG-1:0]           reg_a_addr;
  wire [DEPTH_REG-1:0]           reg_b_addr;
  wire [15:0]                    im16;
  wire signed [REG_IM_BITS-1:0]  ims_a;
  wire signed [REG_IM_BITS-1:0]  ims_b;
  reg                            req_stop;
  reg                            req_stop_d1;

  reg [WIDTH_D-1:0]              source_a;
  reg [WIDTH_D-1:0]              source_b;

  // register file
  reg [WIDTH_D-1:0]              reg_file [0:(1 << DEPTH_REG)-1];

  integer                        i;

  // debug
  wire [6:0]                     debug_op;
  assign debug_op = inst[6:0];

  // decode(stage1)
  assign inst = ((stopped == TRUE) || (req_stop == TRUE)) ? NOP_OP : mem_i_r;
  assign addr_mode_d_s1 = inst[12:11];
  assign addr_mode_a_s1 = inst[10:9];
  assign addr_mode_b_s1 = inst[8:7];
  assign reg_d_addr_s1 = inst[DEPTH_REG+26-1:26];
  assign reg_a_addr_s1 = inst[DEPTH_REG+20-1:20];
  assign reg_b_addr_s1 = inst[DEPTH_REG+14-1:14];
  assign ims_d_s1 = inst[OPERAND_BITS+26-1:26];
  assign ims_a_s1 = inst[OPERAND_BITS+20-1:20];
  assign ims_b_s1 = inst[OPERAND_BITS+14-1:14];

  // decode(stage2)
  assign op = inst_d2[6:0];
  assign is_type_normal = inst_d2[6];
  assign addr_mode_d = inst_d2[12:11];
  assign addr_mode_a = inst_d2[10:9];
  assign addr_mode_b = inst_d2[8:7];
  assign reg_d_addr = inst_d2[DEPTH_REG+26-1:26];
  assign reg_a_addr = inst_d2[DEPTH_REG+20-1:20];
  assign reg_b_addr = inst_d2[DEPTH_REG+14-1:14];
  assign im16 = inst_d2[31:16];
  assign reg_mode_a = inst_d2[OPERAND_BITS+20-1];
  assign reg_mode_b = inst_d2[OPERAND_BITS+14-1];
  assign ims_a = inst_d2[REG_IM_BITS+20-1:20];
  assign ims_b = inst_d2[REG_IM_BITS+14-1:14];

  // manual pc increment
  assign not_increment = ((stopped == TRUE) || (req_stop == TRUE) || (op == I_HALT) || (op == I_BC) || (op == I_BL) || (op == I_BA)) ? 1'b1 : 1'b0;

  // switch source
  // source_a
  always @*
    begin
      if (addr_mode_a != ADDR_MODE_0)
        begin
          source_a = mem_d_r_a;
        end
      else
        begin
          if (reg_mode_a == TRUE)
            begin
              source_a = ims_a;
            end
          else
            begin
              source_a = reg_file[reg_a_addr];
            end
        end
    end
  // source_b
  always @*
    begin
      if (addr_mode_b != ADDR_MODE_0)
        begin
          source_b = mem_d_r_b;
        end
      else
        begin
          if (reg_mode_b == TRUE)
            begin
              source_b = ims_b;
            end
          else
            begin
              source_b = reg_file[reg_b_addr];
            end
        end
    end

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
        default: result = ZERO;
      endcase
    end
  endfunction

  // mem_d_we condition
  assign mem_d_we_sig = (addr_mode_d != ADDR_MODE_0) & (is_type_normal | ((op == I_MV) & ((reg_file[SP_REG_CP] != ZERO))));

  always @(posedge clk)
    begin
      if (reset == TRUE)
        begin
          loop_counter <= ZERO;
          loop_end <= ZERO;
          loop_span <= ZERO;
          mem_d_addr_w_d0 <= ZERO;
          mem_d_addr_w_s_d0 <= ZERO;
          mem_d_addr_r_a <= ZERO;
          mem_d_addr_r_a_s <= ZERO;
          mem_d_addr_r_b <= ZERO;
          mem_d_addr_r_b_s <= ZERO;
          mem_d_addr_w_d1 <= ZERO;
          mem_d_addr_w <= ZERO;
          mem_d_w <= ZERO;
          mem_d_we <= ZERO;
          mem_i_addr_r <= ZERO;
          inst_d1 <= ZERO;
          inst_d2 <= ZERO;
          pc_d1 <= ZERO;
          pc_d2 <= ZERO;
          pc_d3 <= ZERO;
          stopped <= TRUE;
          req_stop <= FALSE;
          req_stop_d1 <= FALSE;
          /*
          for (i = 0; i < (1 << DEPTH_REG); i = i + ONE)
            begin
              reg_file[i] <= ZERO;
            end
           */
        end
      else
        begin
          // delay
          inst_d1 <= inst;
          inst_d2 <= inst_d1;
          pc_d3 <= pc_d2;
          pc_d2 <= pc_d1;
          pc_d1 <= mem_i_addr_r;
          mem_d_we <= mem_d_we_sig;
          mem_d_addr_w_d1 <= mem_d_addr_w_d0;
          // mem_d_addr_w: d2
          mem_d_addr_w <= mem_d_addr_w_d1;

          req_stop_d1 <= req_stop;
          if (req_stop_d1 == TRUE)
            begin
              stopped <= TRUE;
              req_stop <= FALSE;
            end

          if (stopped == FALSE)
            begin
              // addressing
              case (addr_mode_d_s1)
                ADDR_MODE_0:
                  begin
                    mem_d_addr_w_d0 <= mem_d_addr_w_d0;
                    mem_d_addr_w_s_d0 <= mem_d_addr_w_s_d0;
                  end
                ADDR_MODE_1:
                  begin
                    mem_d_addr_w_d0 <= reg_file[reg_d_addr_s1][DEPTH_D-1:0];
                    mem_d_addr_w_s_d0 <= reg_file[reg_d_addr_s1][DEPTH_D-1:0];
                  end
                ADDR_MODE_2:
                  begin
                    mem_d_addr_w_d0 <= mem_d_addr_w_s_d0;
                    mem_d_addr_w_s_d0 <= mem_d_addr_w_s_d0 + reg_file[reg_d_addr_s1][DEPTH_D-1:0];
                  end
                ADDR_MODE_3:
                  begin
                    mem_d_addr_w_d0 <= $signed(mem_d_addr_w_s_d0) + ims_d_s1;
                    mem_d_addr_w_s_d0 <= mem_d_addr_w_s_d0;
                  end
              endcase

              case (addr_mode_a_s1)
                ADDR_MODE_0:
                  begin
                    mem_d_addr_r_a <= mem_d_addr_r_a;
                    mem_d_addr_r_a_s <= mem_d_addr_r_a_s;
                  end
                ADDR_MODE_1:
                  begin
                    mem_d_addr_r_a <= reg_file[reg_a_addr_s1][DEPTH_D-1:0];
                    mem_d_addr_r_a_s <= reg_file[reg_a_addr_s1][DEPTH_D-1:0];
                  end
                ADDR_MODE_2:
                  begin
                    mem_d_addr_r_a <= mem_d_addr_r_a_s;
                    mem_d_addr_r_a_s <= mem_d_addr_r_a_s + reg_file[reg_a_addr_s1][DEPTH_D-1:0];
                  end
                ADDR_MODE_3:
                  begin
                    mem_d_addr_r_a <= $signed(mem_d_addr_r_a_s) + ims_a_s1;
                    mem_d_addr_r_a_s <= mem_d_addr_r_a_s;
                  end
              endcase

              case (addr_mode_b_s1)
                ADDR_MODE_0:
                  begin
                    mem_d_addr_r_b <= mem_d_addr_r_b;
                    mem_d_addr_r_b_s <= mem_d_addr_r_b_s;
                  end
                ADDR_MODE_1:
                  begin
                    mem_d_addr_r_b <= reg_file[reg_b_addr_s1][DEPTH_D-1:0];
                    mem_d_addr_r_b_s <= reg_file[reg_b_addr_s1][DEPTH_D-1:0];
                  end
                ADDR_MODE_2:
                  begin
                    mem_d_addr_r_b <= mem_d_addr_r_b_s;
                    mem_d_addr_r_b_s <= mem_d_addr_r_b_s + reg_file[reg_b_addr_s1][DEPTH_D-1:0];
                  end
                ADDR_MODE_3:
                  begin
                    mem_d_addr_r_b <= $signed(mem_d_addr_r_b_s) + ims_b_s1;
                    mem_d_addr_r_b_s <= mem_d_addr_r_b_s;
                  end
              endcase

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
                  case (addr_mode_d)
                    ADDR_MODE_0:
                      begin
                        reg_file[reg_d_addr] <= result(op);
                      end
                    ADDR_MODE_1, ADDR_MODE_2, ADDR_MODE_3:
                      begin
                        mem_d_w <= result(op);
                      end
                  endcase
                end
              else
                begin
                  // special instructions
                  case (op)
                    I_HALT:
                      begin
                        req_stop <= TRUE;
                      end
                    I_NOP:
                      begin
                      end
                    I_MV:
                      begin
                        if (reg_file[SP_REG_CP] != ZERO)
                          begin
                            case (addr_mode_d)
                              ADDR_MODE_0:
                                begin
                                  reg_file[reg_d_addr] <= source_a;
                                end
                              ADDR_MODE_1, ADDR_MODE_2, ADDR_MODE_3:
                                begin
                                  mem_d_w <= source_a;
                                end
                            endcase
                          end
                      end
                    I_MVI:
                      begin
                        reg_file[SP_REG_MVI] <= im16;
                      end
                    I_MVIH:
                      begin
                        if (WIDTH_D >= 16)
                          begin
                            reg_file[SP_REG_MVI] <= {im16, reg_file[SP_REG_MVI][15:0]};
                          end
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
                            mem_i_addr_r <= pc_d3 + reg_file[SP_REG_BA];
                          end
                      end
                    I_BL:
                      begin
                        reg_file[SP_REG_LINK] <= pc_d3 + DELAY_SLOT_P1;
                        mem_i_addr_r <= pc_d3 + reg_file[SP_REG_BA];
                      end
                    I_BA:
                      begin
                        mem_i_addr_r <= reg_file[SP_REG_BA];
                      end
                    I_LOOP:
                      begin
                        loop_counter <= reg_file[SP_REG_LOOP_COUNTER];
                        loop_end <= pc_d3 + reg_file[SP_REG_LOOP_END][DEPTH_I-1:0];
                        loop_span <= reg_file[SP_REG_LOOP_SPAN][DEPTH_I-1:0];
                      end
                    default: ;
                  endcase
                end
            end
          else
            begin
              // if stopped == TRUE
              if (resume_pulse == TRUE)
                begin
                  stopped <= FALSE;
                  mem_i_addr_r <= mem_i_addr_r + ONE;
                end
            end
        end
    end

  // create resume_pulse
  always @(posedge clk)
    begin
      resume_d1 <= resume;
    end

  assign resume_pulse = ((resume == TRUE) && (resume_d1 == FALSE)) ? TRUE : FALSE;

endmodule
