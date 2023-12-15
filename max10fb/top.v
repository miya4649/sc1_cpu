/*
  Copyright (c) 2016 miya
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

`default_nettype none
`define USE_UART

module top
  (
   input        clk,
   output [2:0] led_n,
`ifdef USE_UART
   output       cn2_8,
   input        cn2_9,
`endif
   input        reset_n
   );

  localparam UART_CLK_HZ = 40000000;
  localparam UART_SCLK_HZ = 115200;
  localparam WIDTH_D = 32;
  // for small FPGAs
  localparam DEPTH_I = 10;
  localparam DEPTH_D = 9;

  wire clk_pll;
  wire [WIDTH_D-1:0] led;
  assign led_n = ~(led[2:0]);

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

  sc1_soc
    #(
      .UART_CLK_HZ (UART_CLK_HZ),
      .UART_SCLK_HZ (UART_SCLK_HZ),
      .WIDTH_D (WIDTH_D),
      .DEPTH_I (DEPTH_I),
      .DEPTH_D (DEPTH_D)
      )
  sc1_soc_0
    (
`ifdef USE_UART
     .uart_rxd (uart_rxd),
     .uart_txd (uart_txd),
`endif
     .clk (clk_pll),
     .reset (reset),
     .led (led)
   );

`ifdef USE_UART
  // uart
  wire          uart_txd;
  wire          uart_rxd;
  assign cn2_8 = uart_txd;
  assign uart_rxd = cn2_9;
`endif

endmodule
