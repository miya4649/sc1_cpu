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

// factor_mul < factor_div
// COUNTER_BITS > factor_mul,div bits + 1
// en, clk_out frequency: clk * factor_mul / factor_div / 2 Hz

module frac_clk
  #(
    parameter COUNTER_BITS = 4
    )
  (
   input wire                           clk,
   input wire                           reset,
   input wire signed [COUNTER_BITS-1:0] factor_mul,
   input wire signed [COUNTER_BITS-1:0] factor_div,
   output wire                          clk_out,
   output reg                           en
   );

  localparam TRUE = 1'b1;
  localparam FALSE = 1'b0;
  localparam ONE = 1'd1;
  localparam ZERO = 1'd0;
  localparam SZERO = 1'sd0;

  // mul counter
  reg signed [COUNTER_BITS-1:0] counter_mul;
  always @(posedge clk)
    begin
      if (reset == TRUE)
        begin
          counter_mul <= ZERO;
        end
      else
        begin
          counter_mul <= counter_mul + factor_mul;
        end
    end

  // div counter, generate clk_out
  reg signed [COUNTER_BITS-1:0] counter_div;
  reg clk_out1;
  always @(posedge clk)
    begin
      if (reset == TRUE)
        begin
          counter_div <= ZERO;
          clk_out1 <= FALSE;
        end
      else
        begin
          if (counter_mul - counter_div > SZERO)
            begin
              counter_div <= counter_div + factor_div;
              clk_out1 <= ~clk_out1;
            end
        end
    end

  reg clk_out2;
  always @(posedge clk)
    begin
      if (reset == TRUE)
        begin
          clk_out2 <= FALSE;
        end
      else
        begin
          clk_out2 <= clk_out1;
        end
    end
  assign clk_out = start ? clk_out2 : FALSE;

  // generate en signal
  always @(posedge clk)
    begin
      if (reset == TRUE)
        begin
          en <= FALSE;
        end
      else
        begin
          if ((clk_out1 == FALSE) && (clk_out2 == TRUE))
            begin
              en <= TRUE;
            end
          else
            begin
              en <= FALSE;
            end
        end
    end

  reg start;
  always @(posedge clk)
    begin
      if (reset == TRUE)
        begin
          start <= FALSE;
        end
      else
        begin
          if (en == TRUE)
            begin
              start <= TRUE;
            end
        end
    end

endmodule
