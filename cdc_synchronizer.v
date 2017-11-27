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

// latency: 2 clk_in cycles + 3 clk_out cycles
// data_in value must be held for 4 clk_out cycles
module cdc_synchronizer
  #(
    parameter DATA_WIDTH=8
    )
  (
   input                     clk_in,
   input                     clk_out,
   input [(DATA_WIDTH-1):0]  data_in,
   output [(DATA_WIDTH-1):0] data_out,
   input                     reset_in
   );

  reg [(DATA_WIDTH-1):0]     data_in_reg;
  reg [(DATA_WIDTH-1):0]     data_out_reg[2:0];
  reg                        change_flag_in;
  reg [2:0]                  change_flag_out;

  always @(posedge clk_in)
    begin
      if (reset_in == 1'b1)
        begin
          change_flag_in <= 1'b0;
        end
      else if (data_in_reg != data_in)
        begin
          change_flag_in <= ~change_flag_in;
        end
      data_in_reg <= data_in;
    end

  always @(posedge clk_out)
    begin
      if (change_flag_out[2] == change_flag_out[1])
        begin
          data_out_reg[2] <= data_out_reg[1];
        end
    end

  always @(posedge clk_out)
    begin
      change_flag_out <= {change_flag_out[1:0], change_flag_in};
      data_out_reg[1] <= data_out_reg[0];
      data_out_reg[0] <= data_in_reg;
    end

  assign data_out = data_out_reg[2];
endmodule
