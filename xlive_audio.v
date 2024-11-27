/*
  Copyright (c) 2023, miya
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

// ver.2024/11/26

module xlive_audio
  #(
    parameter AUDIO_WIDTH = 16,
    parameter BUFFER_DEPTH = 4,
    parameter AES_MODE = 0,
    parameter SAMPLE_FREQ = 48000
    )
  (
   input wire                     clk_tx,
   input wire                     clk_rx,
   input wire                     reset_tx,
   input wire                     reset_rx,
   input wire [AUDIO_WIDTH*2-1:0] data_rx,
   input wire                     en_rx,
   output wire                    full_rx,
   output reg [31:0]              data_tx,
   output reg                     valid_tx,
   output reg                     id_tx,
   input wire                     ready_tx
   );

  localparam                      ZERO = 1'd0;
  localparam                      ONE = 1'd1;
  localparam                      TRUE = 1'b1;
  localparam                      FALSE = 1'b0;

  wire                            audio_valid;
  wire                            audio_empty;
  reg [9:0]                       frame_counter;
  wire [7:0]                      cs_addr;
  wire                            cs_data;
  reg                             start;
  reg                             busy;
  wire                            completed;
  wire [8:0]                      start_d;
  wire                            fc_zero;
  wire                            fc_odd;
  reg                             audio_req;
  reg [26:0]                      audio_data1;
  reg [3:0]                       preamble;
  reg                             parity;
  wire signed [AUDIO_WIDTH*2-1:0] audio_data;
  reg [AUDIO_WIDTH*2-1:0]         audio_data0;

  assign completed = valid_tx & ready_tx;

  always @(posedge clk_tx)
  begin
    if (reset_tx == TRUE)
    begin
      start <= FALSE;
      busy <= FALSE;
    end
    else
    begin
      if (busy == FALSE)
      begin
        start <= TRUE;
        busy <= TRUE;
      end
      else
      begin
        start <= FALSE;
        if (completed == TRUE)
        begin
          busy <= FALSE;
        end
      end
    end
  end

  always @(posedge clk_tx)
  begin
    if (reset_tx == TRUE)
    begin
      frame_counter <= 0;
    end
    else
    begin
      if (completed == TRUE)
      begin
        if (frame_counter == 383)
        begin
          frame_counter <= 0;
        end
        else
        begin
          frame_counter <= frame_counter + 1;
        end
      end
    end
  end

  assign fc_zero = (frame_counter == ZERO) ? TRUE : FALSE;

  assign fc_odd = frame_counter[0];

  always @(posedge clk_tx)
  begin
    if ((start == TRUE) && (fc_odd == FALSE))
    begin
      audio_req <= TRUE;
    end
    else
    begin
      audio_req <= FALSE;
    end
  end

  always @(posedge clk_tx)
  begin
    if (reset_tx == TRUE)
    begin
      audio_data0 <= ZERO;
    end
    else
    begin
      if (audio_valid == TRUE)
      begin
        audio_data0 <= audio_data;
      end
    end
  end

  always @(posedge clk_tx)
  begin
    if (fc_odd == FALSE)
    begin
      audio_data1 <= {cs_data, 2'b0, audio_data0[AUDIO_WIDTH*2-1:AUDIO_WIDTH], {(24-AUDIO_WIDTH){1'b0}}};
  end
    else
    begin
      audio_data1 <= {cs_data, 2'b0, audio_data0[AUDIO_WIDTH-1:0], {(24-AUDIO_WIDTH){1'b0}}};
  end
  end

  always @(posedge clk_tx)
  begin
    if (fc_zero)
    begin
      preamble <= 4'b0001;
    end
    else
    begin
      if (fc_odd == FALSE)
      begin
        preamble <= 4'b0010;
      end
      else
      begin
        preamble <= 4'b0011;
      end
    end
  end

  always @(posedge clk_tx)
  begin
    if (start_d[6] == TRUE)
    begin
      data_tx <= {parity, audio_data1, preamble};
      id_tx <= fc_odd;
    end
  end

  always @(posedge clk_tx)
  begin
    if (reset_tx == TRUE)
    begin
      valid_tx <= FALSE;
    end
    else
    begin
      if (start_d[8] == TRUE)
      begin
        valid_tx <= TRUE;
      end
      else
      begin
        if (completed == TRUE)
        begin
          valid_tx <= FALSE;
        end
      end
    end
  end

  always @(posedge clk_tx)
  begin
    parity <= ^audio_data1;
  end

  assign cs_addr = frame_counter[9:1];

  cdc_fifo
    #(
      .DATA_WIDTH (AUDIO_WIDTH * 2),
      .ADDR_WIDTH (BUFFER_DEPTH),
      .MAX_ITEMS ((1 << BUFFER_DEPTH) - 8)
      )
  cdc_fifo_0
    (
     .clk_cr (clk_tx),
     .reset_cr (reset_tx),
     .data_cr (audio_data),
     .req_cr (audio_req),
     .empty_cr (audio_empty),
     .valid_cr (audio_valid),
     .clk_cw (clk_rx),
     .reset_cw (reset_rx),
     .data_cw (data_rx),
     .we_cw (en_rx),
     .almost_full_cw (full_rx)
     );

  channel_status_data
    #(
      .AES_MODE (AES_MODE),
      .SAMPLE_FREQ (SAMPLE_FREQ)
      )
  channel_status_data_0
    (
     .clk (clk_tx),
     .addr (cs_addr),
     .data (cs_data)
     );

  shift_register_factory
    #(
      .DEPTH (9)
      )
  shift_register_factory_start_d
    (
     .clk (clk_tx),
     .din (start),
     .sreg (start_d)
     );

endmodule

module channel_status_data
  #(
    parameter AES_MODE = 0,
    parameter SAMPLE_FREQ = 48000
    )
  (
   input wire       clk,
   input wire [7:0] addr,
   output reg       data
   );

  reg               ram [0:63];

  always @(posedge clk)
  begin
    if (addr[7:6] == 2'b00)
    begin
      data <= ram[addr];
    end
    else
    begin
      data <= 1'b0;
    end
  end

  generate
    initial
    begin
      if (AES_MODE == 1)
      begin
        // AES mode
        ram[8'h00] = 1'b1; // AES:1 S/PDIF:0
        ram[8'h01] = 1'b0; // PCM:0 other:1
        ram[8'h02] = 1'b1; // NO emphasis 100
        ram[8'h03] = 1'b0;
        ram[8'h04] = 1'b0;
        ram[8'h05] = 1'b0; // Source sampling frequency locked:0 unlocked:1

        // Sampling frequency auto:00 48k:01 44k:10 32k:11
        if (SAMPLE_FREQ == 48000)
        begin
          ram[8'h06] = 1'b0;
          ram[8'h07] = 1'b1;
        end
        else
        begin
          ram[8'h06] = 1'b1;
          ram[8'h07] = 1'b0;
        end

        ram[8'h08] = 1'b0; // Stereophonic mode 0100
        ram[8'h09] = 1'b1;
        ram[8'h0a] = 1'b0;
        ram[8'h0b] = 1'b0;
        ram[8'h0c] = 1'b0; // 0000: No user information 0001:192bit user bits
        ram[8'h0d] = 1'b0;
        ram[8'h0e] = 1'b0;
        ram[8'h0f] = 1'b0;

        ram[8'h10] = 1'b0; // Sample bits 16bits:000100 24bits:001101
        ram[8'h11] = 1'b0;
        ram[8'h12] = 1'b0;
        ram[8'h13] = 1'b1;
        ram[8'h14] = 1'b0;
        ram[8'h15] = 1'b0;
        ram[8'h16] = 1'b0; // Alignment level not indicated:00
        ram[8'h17] = 1'b0;

        ram[8'h18] = 1'b0;
        ram[8'h19] = 1'b0;
        ram[8'h1a] = 1'b0;
        ram[8'h1b] = 1'b0;
        ram[8'h1c] = 1'b0;
        ram[8'h1d] = 1'b0;
        ram[8'h1e] = 1'b0;
        ram[8'h1f] = 1'b0;

        ram[8'h20] = 1'b0;
        ram[8'h21] = 1'b0;
        ram[8'h22] = 1'b0;
        ram[8'h23] = 1'b0;
        ram[8'h24] = 1'b0;
        ram[8'h25] = 1'b0;
        ram[8'h26] = 1'b0;
        ram[8'h27] = 1'b0;

        ram[8'h28] = 1'b0;
        ram[8'h29] = 1'b0;
        ram[8'h2a] = 1'b0;
        ram[8'h2b] = 1'b0;
        ram[8'h2c] = 1'b0;
        ram[8'h2d] = 1'b0;
        ram[8'h2e] = 1'b0;
        ram[8'h2f] = 1'b0;

        ram[8'h30] = 1'b0;
        ram[8'h31] = 1'b0;
        ram[8'h32] = 1'b0;
        ram[8'h33] = 1'b0;
        ram[8'h34] = 1'b0;
        ram[8'h35] = 1'b0;
        ram[8'h36] = 1'b0;
        ram[8'h37] = 1'b0;

        ram[8'h38] = 1'b0;
        ram[8'h39] = 1'b0;
        ram[8'h3a] = 1'b0;
        ram[8'h3b] = 1'b0;
        ram[8'h3c] = 1'b0;
        ram[8'h3d] = 1'b0;
        ram[8'h3e] = 1'b0;
        ram[8'h3f] = 1'b0;
      end
      else
      begin
        // S/PDIF mode
        ram[8'h00] = 1'b0; // AES:1 S/PDIF:0
        ram[8'h01] = 1'b0; // PCM:0 other:1
        ram[8'h02] = 1'b1; // Copy restrict:0 permit:1
        ram[8'h03] = 1'b0; // 2ch:0 4ch:1
        ram[8'h04] = 1'b0;
        ram[8'h05] = 1'b0; // 0:no pre-emphasis 1:Pre-emphasis
        ram[8'h06] = 1'b0;
        ram[8'h07] = 1'b0;

        ram[8'h08] = 1'b0;
        ram[8'h09] = 1'b0;
        ram[8'h0a] = 1'b0;
        ram[8'h0b] = 1'b0;
        ram[8'h0c] = 1'b0;
        ram[8'h0d] = 1'b0;
        ram[8'h0e] = 1'b0;
        ram[8'h0f] = 1'b0;

        ram[8'h10] = 1'b0;
        ram[8'h11] = 1'b0;
        ram[8'h12] = 1'b0;
        ram[8'h13] = 1'b0;
        ram[8'h14] = 1'b0;
        ram[8'h15] = 1'b0;
        ram[8'h16] = 1'b0;
        ram[8'h17] = 1'b0;

        // Sampling Freq(4bits) 0000:44.1k 0100:48k 1100:32k
        if (SAMPLE_FREQ == 48000)
        begin
          ram[8'h18] = 1'b0;
          ram[8'h19] = 1'b1;
          ram[8'h1a] = 1'b0;
          ram[8'h1b] = 1'b0;
        end
        else
        begin
          ram[8'h18] = 1'b0;
          ram[8'h19] = 1'b0;
          ram[8'h1a] = 1'b0;
          ram[8'h1b] = 1'b0;
        end

        ram[8'h1c] = 1'b0; // Clock accuracy(2bits) 10:50ppm 00:1100ppm 01:variable
        ram[8'h1d] = 1'b0;
        ram[8'h1e] = 1'b0;
        ram[8'h1f] = 1'b0;

        ram[8'h20] = 1'b0; // Word length 0:20bits 1:24bits
        ram[8'h21] = 1'b0; // Sample length(3bits) 0:Undefined n:Word length-n 5:full
        ram[8'h22] = 1'b0;
        ram[8'h23] = 1'b1;
        ram[8'h24] = 1'b0;
        ram[8'h25] = 1'b0;
        ram[8'h26] = 1'b0;
        ram[8'h27] = 1'b0;

        ram[8'h28] = 1'b0;
        ram[8'h29] = 1'b0;
        ram[8'h2a] = 1'b0;
        ram[8'h2b] = 1'b0;
        ram[8'h2c] = 1'b0;
        ram[8'h2d] = 1'b0;
        ram[8'h2e] = 1'b0;
        ram[8'h2f] = 1'b0;

        ram[8'h30] = 1'b0;
        ram[8'h31] = 1'b0;
        ram[8'h32] = 1'b0;
        ram[8'h33] = 1'b0;
        ram[8'h34] = 1'b0;
        ram[8'h35] = 1'b0;
        ram[8'h36] = 1'b0;
        ram[8'h37] = 1'b0;

        ram[8'h38] = 1'b0;
        ram[8'h39] = 1'b0;
        ram[8'h3a] = 1'b0;
        ram[8'h3b] = 1'b0;
        ram[8'h3c] = 1'b0;
        ram[8'h3d] = 1'b0;
        ram[8'h3e] = 1'b0;
        ram[8'h3f] = 1'b0;
      end
    end
  endgenerate

endmodule
