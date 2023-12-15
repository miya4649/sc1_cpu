/*
  Copyright (c) 2017 miya
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

/*
 Usage:

 Tx: Wait for 'busy' == 0, set 'data_tx' and set 1 to the 'start' signal.

 Rx: Read 'data_rx' at the rising edge of the 're' signal
 */

module uart
  #(
    parameter CLK_HZ = 50000000,
    parameter SCLK_HZ = 115200,
    parameter WIDTH = 8
    )
  (
   input wire              clk,
   input wire              reset,
   input wire              rxd,
   input wire              start,
   input wire [WIDTH-1:0]  data_tx,
   output wire             txd,
   output reg              busy,
   output reg              re,
   output wire [WIDTH-1:0] data_rx
   );

  localparam TRUE = 1'b1;
  localparam FALSE = 1'b0;
  localparam SHIFT_REG_DEPTH = 3;
  localparam WAIT = (CLK_HZ / SCLK_HZ - 1);
  localparam WAIT_HALF = ((CLK_HZ / SCLK_HZ) / 2 - 1);
  localparam COUNTER_WIDTH = $clog2(WAIT + 2);
  localparam START_BIT = 1'b0;
  localparam STOP_BIT = 1'b1;

  reg [WIDTH+1:0]         data_tx_buf;
  reg [WIDTH+1:0]         data_rx_buf;
  reg [SHIFT_REG_DEPTH:0] rxd_shift_reg;
  wire                    rxd_sync;
  reg                     tx_state;
  reg                     rx_state;
  reg [COUNTER_WIDTH-1:0] tx_counter;
  reg [COUNTER_WIDTH-1:0] rx_counter;
  reg [WIDTH:0]           txd_counter;
  reg [WIDTH:0]           rxd_counter;

  // tx
  assign txd = data_tx_buf[txd_counter];

  always @(posedge clk)
    begin
      if (busy == FALSE)
        begin
          data_tx_buf <= {STOP_BIT, data_tx, START_BIT};
        end
      else
        begin
          data_tx_buf <= data_tx_buf;
        end
    end

  always @(posedge clk)
    begin
      if ((busy == TRUE) && (tx_counter != 0))
        begin
          tx_counter <= tx_counter - 1;
        end
      else
        begin
          tx_counter <= WAIT;
        end
    end

  always @(posedge clk)
    begin
      if (reset == TRUE)
        begin
          txd_counter <= (WIDTH + 1);
          busy <= FALSE;
          tx_state <= 0;
        end
      else
        begin
          case (tx_state)
            0:
              begin
                if ((start == TRUE) && (busy == FALSE))
                  begin
                    txd_counter <= 0;
                    busy <= TRUE;
                    tx_state <= 1;
                  end
                else
                  begin
                    txd_counter <= (WIDTH + 1);
                    busy <= FALSE;
                    tx_state <= 0;
                  end
              end
            1:
              begin
                if (tx_counter == 0)
                  begin
                    if (txd_counter == (WIDTH + 1))
                      begin
                        txd_counter <= (WIDTH + 1);
                        busy <= FALSE;
                        tx_state <= 0;
                      end
                    else
                      begin
                        txd_counter <= txd_counter + 1;
                        busy <= TRUE;
                        tx_state <= 1;
                      end
                  end
                else
                  begin
                    txd_counter <= txd_counter;
                    busy <= TRUE;
                    tx_state <= 1;
                  end
              end
          endcase
        end
    end

  // sync rxd
  always @(posedge clk)
    begin
      if (reset == TRUE)
        begin
          rxd_shift_reg <= 0;
        end
      else
        begin
          rxd_shift_reg <= {rxd_shift_reg, ~rxd};
        end
    end

  assign rxd_sync = ~rxd_shift_reg[SHIFT_REG_DEPTH];

  // rx
  always @(posedge clk)
    begin
      if (reset == TRUE)
        begin
          rx_counter <= 0;
        end
      else
        begin
          if ((rx_state != 0) && (rx_counter != 0))
            begin
              rx_counter <= rx_counter - 1;
            end
          else
            begin
              if (rx_state == 0)
                begin
                  rx_counter <= WAIT_HALF;
                end
              else
                begin
                  rx_counter <= WAIT;
                end
            end
        end
    end

  always @(posedge clk)
    begin
      if (rx_state == 0)
        begin
          rxd_counter <= 0;
        end
      else
        begin
          if ((rx_counter == 0) && (rxd_counter != (WIDTH + 1)))
            begin
              rxd_counter <= rxd_counter + 1;
            end
          else
            begin
              rxd_counter <= rxd_counter;
            end
        end
    end

  assign data_rx = data_rx_buf[WIDTH:1];
  always @(posedge clk)
    begin
      if (reset == TRUE)
        begin
          rx_state <= 0;
          data_rx_buf <= 0;
          re <= FALSE;
        end
      else
        begin
          case (rx_state)
            0:
              begin
                if (rxd_sync == START_BIT)
                  begin
                    re <= FALSE;
                    rx_state <= 1;
                  end
                else
                  begin
                    re <= re;
                    rx_state <= 0;
                  end
              end
            1:
              begin
                if (rx_counter == 0)
                  begin
                    if (rxd_counter == (WIDTH + 1))
                      begin
                        re <= TRUE;
                        rx_state <= 0;
                      end
                    else
                      begin
                        data_rx_buf[rxd_counter] <= rxd_sync;
                        re <= FALSE;
                        rx_state <= 1;
                      end
                  end
                else
                  begin
                    re <= FALSE;
                    rx_state <= 1;
                  end
              end
          endcase
        end
    end

endmodule
