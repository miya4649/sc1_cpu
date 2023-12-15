/*
  Copyright (c) 2018 miya
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

`default_nettype none
`define USE_INIT_RAM

module top
  (
   input        CLK12M,
   input        USER_BTN,
   output [7:0] LED
   );

  localparam WIDTH_D = 32;
  localparam DEPTH_I = 12;
  localparam DEPTH_D = 12;

  // reset
  reg           reset;
  reg           reset1;
  reg           resetpll;
  reg           resetpll1;

  always @(posedge CLK12M)
    begin
      resetpll1 <= ~USER_BTN;
      resetpll <= resetpll1;
    end

  always @(posedge clk)
    begin
      reset1 <= ~pll_locked;
      reset <= reset1;
    end

  wire [WIDTH_D-1:0] led_data;
  assign LED = led_data[7:0];

  wire               clk;
  wire               pll_locked;

  simple_pll simple_pll_0
    (
     .areset (resetpll),
     .inclk0 (CLK12M),
     .c0 (clk),
     .locked (pll_locked)
     );

  sc1_soc
    #(
      .WIDTH_D (WIDTH_D),
      .DEPTH_I (DEPTH_I),
      .DEPTH_D (DEPTH_D)
      )
  sc1_soc_0
    (
     .clk (clk),
     .reset (reset),
     .led (led_data)
   );

endmodule
