/*
  Copyright (c) 2018, miya
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

module i2c_master_single
  #(
    parameter CLK_HZ = 50000000,
    parameter SCLK_HZ = 100000
    )
  (
   input        clk,
   input        reset,
   input [1:0]  command,
   input        start,
   input [7:0]  data_w,
   input        r_ack,
   output       w_ack,
   output [7:0] data_r,
   output reg   busy,
   inout        scl,
   inout        sda
   );

  localparam TRUE = 1'b1;
  localparam FALSE = 1'b0;
  localparam ONE = 1'd1;
  localparam ZERO = 1'd0;
  localparam CMD_START = 0;
  localparam CMD_STOP = 1;
  localparam CMD_TX = 2;
  localparam CMD_RX = 3;
  localparam CLK_DIVIDER = (CLK_HZ / SCLK_HZ / 4);
  localparam I2C_COUNTER_WIDTH = $clog2(CLK_DIVIDER + 1);

  // i2c clock counter
  wire          i2c_counter_end;
  reg [I2C_COUNTER_WIDTH-1:0] i2c_counter;
  assign i2c_counter_end = (i2c_counter == (CLK_DIVIDER - 1)) ? TRUE : FALSE;

  always @(posedge clk)
    begin
      if (busy == FALSE)
        begin
          i2c_counter <= ZERO;
        end
      else
        begin
          if (i2c_counter_end == TRUE)
            begin
              i2c_counter <= ZERO;
            end
          else
            begin
              i2c_counter <= i2c_counter + ONE;
            end
        end
    end

  wire              scl_i;
  reg               scl_o_n;
  wire              sda_i;
  reg               sda_o_n;
  assign scl = scl_o_n ? 1'b0 : 1'bz;
  assign sda = sda_o_n ? 1'b0 : 1'bz;

  reg [3:0]         bit_counter;
  reg [1:0]         event_counter;
  reg [1:0]         command_reg;
  reg [8:0]         data_w_reg;
  reg [8:0]         data_r_reg;
  assign data_r = data_r_reg[8:1];
  assign w_ack = data_r_reg[0];

  always @(posedge clk)
    begin
      if (reset == TRUE)
        begin
          bit_counter <= ZERO;
          event_counter <= ZERO;
          busy <= FALSE;
          command_reg <= ZERO;
          data_w_reg <= ZERO;
        end
      else
        begin
          if (busy == TRUE)
            begin
              if (i2c_counter_end == TRUE)
                begin
                  if (event_counter != 3)
                    begin
                      event_counter <= event_counter + ONE;
                    end
                  else
                    begin
                      event_counter <= ZERO;
                      if (bit_counter == ZERO)
                        begin
                          busy <= FALSE;
                        end
                      else
                        begin
                          bit_counter <= bit_counter - ONE;
                        end
                    end
                end
            end
          else
            begin
              if (start == TRUE)
                begin
                  busy <= TRUE;
                  event_counter <= ZERO;
                  command_reg <= command;

                  case (command)
                    CMD_START:
                      begin
                        bit_counter <= 0;
                        data_w_reg <= ZERO;
                      end
                    CMD_STOP:
                      begin
                        bit_counter <= 0;
                        data_w_reg <= ZERO;
                      end
                    CMD_TX:
                      begin
                        bit_counter <= 8;
                        data_w_reg <= {data_w, 1'b1};
                      end
                    CMD_RX:
                      begin
                        bit_counter <= 8;
                        data_w_reg <= {8'hff, r_ack};
                      end
                  endcase
                end
            end
        end
    end

  always @(posedge clk)
    begin
      if (reset == TRUE)
        begin
          scl_o_n <= FALSE;
          sda_o_n <= FALSE;
          data_r_reg <= ZERO;
        end
      else
        begin
          if ((i2c_counter == ZERO) && (busy == TRUE))
            begin
              case (command_reg)
                CMD_START:
                  begin
                    case (event_counter)
                      0:
                        begin
                          scl_o_n <= FALSE;
                          sda_o_n <= FALSE;
                        end
                      1:
                        begin
                          scl_o_n <= FALSE;
                          sda_o_n <= FALSE;
                        end
                      2:
                        begin
                          scl_o_n <= FALSE;
                          sda_o_n <= TRUE;
                        end
                      3:
                        begin
                          scl_o_n <= TRUE;
                          sda_o_n <= TRUE;
                        end
                    endcase
                  end
                CMD_STOP:
                  begin
                    case (event_counter)
                      0:
                        begin
                          scl_o_n <= FALSE;
                          sda_o_n <= TRUE;
                        end
                      1:
                        begin
                          scl_o_n <= FALSE;
                          sda_o_n <= TRUE;
                        end
                      2:
                        begin
                          scl_o_n <= FALSE;
                          sda_o_n <= FALSE;
                        end
                      3:
                        begin
                          scl_o_n <= FALSE;
                          sda_o_n <= FALSE;
                        end
                    endcase
                  end
                default:
                  begin
                    sda_o_n <= ~data_w_reg[bit_counter];

                    case (event_counter)
                      0:
                        begin
                          scl_o_n <= TRUE;
                        end
                      1:
                        begin
                          scl_o_n <= FALSE;
                        end
                      2:
                        begin
                          scl_o_n <= FALSE;
                          data_r_reg[bit_counter] <= sda_i;
                        end
                      3:
                        begin
                          scl_o_n <= TRUE;
                        end
                    endcase
                  end
              endcase
            end
        end
    end

  // synchronize scl input signal
  shift_register_vector
  #(
    .WIDTH (1),
    .DEPTH (3)
    )
  shift_register_vector_0
  (
   .clk (clk),
   .data_in (scl),
   .data_out (scl_i)
   );

  // synchronize sda input signal
  shift_register_vector
  #(
    .WIDTH (1),
    .DEPTH (3)
    )
  shift_register_vector_1
  (
   .clk (clk),
   .data_in (sda),
   .data_out (sda_i)
   );

endmodule
