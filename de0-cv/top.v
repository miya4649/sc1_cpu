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
   input        CLOCK_50,
   output [6:0] HEX0,
   output [6:0] HEX1,
   output [6:0] HEX2,
   output [6:0] HEX3,
   output [6:0] HEX4,
   output [6:0] HEX5,
   output [9:0] LEDR,
   input        RESET_N
   );

  localparam WIDTH_D = 32;
  localparam WIDTH_REG = 32;
  localparam DEPTH_I = 8;
  localparam DEPTH_D = 8;

  // generate reset signal (push button 1)
  reg           reset;
  reg           reset1;

  always @(posedge CLOCK_50)
    begin
      reset1 <= ~RESET_N;
      reset <= reset1;
    end

  wire [WIDTH_REG-1:0] out_data;
  assign LEDR = out_data[9:0];

  wire [DEPTH_I-1:0]   rom_addr;
  wire [31:0]          rom_data;

  rom rom_0
    (
     .clk (CLOCK_50),
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
     .clk (CLOCK_50),
     .reset (reset),
     .rom_addr (rom_addr),
     .rom_data (rom_data),
     .port_in ({WIDTH_REG{1'b0}}),
     .port_out (out_data)
     );

  // turn off hex leds
  assign HEX0 = 7'b1111111;
  assign HEX1 = 7'b1111111;
  assign HEX2 = 7'b1111111;
  assign HEX3 = 7'b1111111;
  assign HEX4 = 7'b1111111;
  assign HEX5 = 7'b1111111;

endmodule
