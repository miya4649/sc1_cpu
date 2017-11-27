/*
  Copyright (c) 2015-2017, miya
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

/*
 data[31:16] : audio R unsigned pcm
 data[15:0]  : audio L unsigned pcm
 */

module audio_output
  (
   input        clk,
   input        reset,
   input [31:0] data,
   // clock_divider = (audio_clk / sampling_rate) - 1
   // ex: (18MHz / 48000Hz) - 1 = 374
   input [31:0] clock_divider,
   input        valid_toggle,
   output       full,
   input        ext_audio_clk,
   input        ext_audio_reset,
   output       ext_audio_r,
   output       ext_audio_l
   );

  parameter FIFO_DEPTH_IN_BITS = 4;

  // data write
  wire          we_cw;
  reg           toggle;
  reg           toggle_prev;
  reg [31:0]    data_in;

  assign we_cw = ((toggle_prev != toggle) && (full == 1'b0)) ? 1'b1 : 1'b0;

  always @(posedge clk)
    begin
      if (reset == 1'b1)
        begin
          toggle <= 1'b0;
          data_in <= 1'd0;
          toggle_prev <= 1'b0;
        end
      else
        begin
          toggle <= valid_toggle;
          data_in <= data;
          toggle_prev <= toggle;
        end
    end

  // data read
  wire          req_ca;
  wire          empty_ca;
  wire          valid_ca;
  reg [15+1:0]  sample_sum_r_ca;
  reg [15+1:0]  sample_sum_l_ca;
  wire [31:0]   data_out_ca;
  reg [31:0]    data_out_reg_ca;
  reg [15:0]    read_freq_counter;

  assign req_ca = (read_freq_counter == 1'd0) ? 1'b1 : 1'b0;
  assign ext_audio_r = sample_sum_r_ca[15+1];
  assign ext_audio_l = sample_sum_l_ca[15+1];

  always @(posedge ext_audio_clk)
    begin
      if (ext_audio_reset == 1'b1)
        begin
          sample_sum_r_ca <= 1'd0;
          sample_sum_l_ca <= 1'd0;
          data_out_reg_ca <=  1'd0;
          read_freq_counter <= 1'd0;
        end
      else
        begin
          // read data delays 1 cycle
          if (valid_ca)
            begin
              data_out_reg_ca <= data_out_ca;
            end
          // delta sigma
          sample_sum_r_ca <= sample_sum_r_ca[15:0] + data_out_reg_ca[31:16];
          sample_sum_l_ca <= sample_sum_l_ca[15:0] + data_out_reg_ca[15:0];
          // read data once per READ_FREQ+1 cycles
          if (read_freq_counter == 1'd0)
            begin
              read_freq_counter <= clock_divider;
            end
          else
            begin
              read_freq_counter <= read_freq_counter - 1'd1;
            end
        end
    end

  cdc_fifo
    #(
      .DATA_WIDTH (32),
      .ADDR_WIDTH (FIFO_DEPTH_IN_BITS),
      .MAX_ITEMS ((1 << FIFO_DEPTH_IN_BITS) - 3)
      )
  cdc_fifo_0
    (
     .clk_cr (ext_audio_clk),
     .reset_cr (ext_audio_reset),
     .data_cr (data_out_ca),
     .req_cr (req_ca),
     .empty_cr (empty_ca),
     .valid_cr (valid_ca),
     .clk_cw (clk),
     .reset_cw (reset),
     .data_cw (data_in),
     .we_cw (we_cw),
     .full_cw (),
     .almost_full_cw (full)
     );

endmodule
