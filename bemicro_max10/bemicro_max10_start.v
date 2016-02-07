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

module bemicro_max10_start
  (
   input        SYS_CLK,
   output [7:0] USER_LED,
   input [3:0]  PB
   );

  wire          clk_pll;

  // generate reset signal (push button 1)
  wire          reset;
  reg           reset_reg1;
  reg           reset_reg2;
  assign reset = reset_reg2;

  always @(posedge clk_pll)
    begin
      reset_reg1 <= ~PB[0];
      reset_reg2 <= reset_reg1;
    end

  wire [31:0] led_n;
  assign USER_LED = ~(led_n[7:0]);

  wire [7:0]  rom_addr;
  wire [31:0] rom_data;

  simple_pll simple_pll_0
    (
     .inclk0 (SYS_CLK),
     .c0 (clk_pll)
     );

  rom rom_0
    (
     .clk (clk_pll),
     .addr (rom_addr),
     .data_out (rom_data)
     );

  sc1_cpu sc1_cpu_0
    (
     .clk (clk_pll),
     .reset (reset),
     .rom_addr (rom_addr),
     .rom_data (rom_data),
     .port_in (),
     .port_out (led_n)
     );

endmodule
