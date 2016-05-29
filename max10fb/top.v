/*
  Copyright (c) 2016, miya
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

module top
  (
   input        clk,
   output [2:0] led,
   input        reset_n
   );

  localparam WIDTH_D = 32;
  localparam WIDTH_REG = 32;
  localparam DEPTH_I = 8;
  localparam DEPTH_D = 8;

  wire clk_pll;
  wire [WIDTH_REG-1:0] led_n;
  assign led = ~(led_n[2:0]);

  wire [DEPTH_I-1:0]   rom_addr;
  wire [31:0]          rom_data;

  // generate reset signal
  wire           reset;
  reg            reset_reg1;
  reg            reset_reg2;
  assign reset = reset_reg2;

  always @(posedge clk_pll)
    begin
      reset_reg1 <= ~reset_n;
      reset_reg2 <= reset_reg1;
    end

  simple_pll simple_pll_0
    (
     .inclk0 (clk),
     .c0 (clk_pll)
     );

  rom rom_0
    (
     .clk (clk_pll),
     .addr (rom_addr),
     .data_out (rom_data)
     );

  sc1_cpu
    #(
      .WIDTH_D (WIDTH_D),
      .WIDTH_REG (WIDTH_REG),
      .DEPTH_I (DEPTH_I),
      .DEPTH_D (DEPTH_D)
      )
  sc1_cpu_0
    (
     .clk (clk_pll),
     .reset (reset),
     .rom_addr (rom_addr),
     .rom_data (rom_data),
     .port_in ({WIDTH_REG{1'b0}}),
     .port_out (led_n)
     );

endmodule
