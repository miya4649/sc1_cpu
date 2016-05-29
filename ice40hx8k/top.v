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

module top
  (
   input        CLK,
   output [7:0] LED
   );

  localparam WIDTH_D = 32;
  localparam WIDTH_REG = 32;
  localparam DEPTH_I = 8;
  localparam DEPTH_D = 8;

  localparam RESET_TIMER_BIT = 22;

  wire [DEPTH_I-1:0] rom_addr;
  wire [31:0]        rom_data;
  wire [WIDTH_REG-1:0] port_out;
  assign LED = port_out[7:0];
  
  reg                  reset = 1'b0;
  reg [31:0]           reset_counter = 32'd0;
  always @(posedge CLK)
    begin
      if (reset_counter[RESET_TIMER_BIT] == 1'b1)
        begin
          reset <= 1'b0;
        end
      else
        begin
          reset <= 1'b1;
          reset_counter <= reset_counter + 1'd1;
        end
    end

  rom rom_0
    (
     .clk (CLK),
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
     .clk (CLK),
     .reset (reset),
     .rom_addr (rom_addr),
     .rom_data (rom_data),
     .port_in ({WIDTH_REG{1'b0}}),
     .port_out (port_out)
     );

endmodule
