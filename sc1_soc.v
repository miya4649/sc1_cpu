/*
  Copyright (c) 2015 miya
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

module sc1_soc
  #(
    parameter UART_CLK_HZ = 50000000,
    parameter UART_SCLK_HZ = 115200,
    parameter I2C_CLK_HZ = 50000000,
    parameter I2C_SCLK_HZ = 100000,
    parameter WIDTH_D = 32,
    parameter DEPTH_I = 12,
    parameter DEPTH_D = 12,
    parameter DEPTH_V = 17,
    parameter BPP = 8
    )
  (
   input wire  clk,
   input wire  reset,
`ifdef USE_UART
   input wire  uart_rxd,
   output wire uart_txd,
`endif
`ifdef USE_AUDIO
   input wire  clka,
   input wire  reseta,
   output wire audio_r,
   output wire audio_l,
`endif
`ifdef USE_MINI_AUDIO
   output wire audio_r,
   output wire audio_l,
`endif
`ifdef USE_XAUDIO
   input wire  clka,
   input wire  reseta,
   output wire [31:0] audio_data,
   output wire audio_id,
   output wire audio_valid,
   input wire  audio_ready,
`endif
`ifdef USE_VGA
   input wire  clkv,
   input wire  resetv,
   output wire vga_hs,
   output wire vga_vs,
   output wire [BPP-1:0] vga_color_out,
   output wire vga_de,
`endif
`ifdef USE_MINI_VGA
   output wire vga_hs,
   output wire vga_vs,
   output wire [`MINI_VGA_BPP-1:0] vga_color_out,
`endif
`ifdef USE_I2C
   inout wire  i2c_sda,
   inout wire  i2c_scl,
               `endif
   output reg [9:0] led
   );

  localparam TRUE = 1'b1;
  localparam FALSE = 1'b0;
  localparam ONE = 1'd1;
  localparam ZERO = 1'd0;
  localparam FFFF = {WIDTH_D{1'b1}};

  localparam WIDTH_I = 32;
  localparam WIDTH_SOC = 32;
  localparam DEPTH_REG = 4;
  localparam PAGE_BITS = 12;
  localparam DA_MSB = (DEPTH_V - 1);
  localparam DA_LSB = PAGE_BITS;
  localparam MAPPER_BITS = (DEPTH_V - PAGE_BITS);

  localparam MAP_MEM_D = 0;
  localparam MAP_NONE = 1;
  localparam MAP_REG_R = 2;
  localparam MAP_REG_W = 3;
  localparam MAP_MEM_I = 4;
  localparam MAP_SYS = 5;
  localparam MAP_CHR_CHR = 6;
  localparam MAP_CHR_BIT = 7;
  localparam MAP_MEM_S = 8;
  localparam MAP_SPRITE = 9;

  localparam DEPTH_IO_REG = 5;
  localparam DEPTH_SYS_REG = 4;

  localparam AUDIO_WIDTH = 16;
  localparam BUFFER_DEPTH = 4;

  wire [WIDTH_I-1:0]  mem_i_r;
  wire [DEPTH_I-1:0]  mem_i_addr_r;
  reg [DEPTH_I-1:0]   mem_i_addr_w;
  reg [WIDTH_I-1:0]   mem_i_w;
  reg                 mem_i_we;

  wire [WIDTH_D-1:0]  mem_d_r_a;
  wire [WIDTH_D-1:0]  mem_d_r_b;
  reg [WIDTH_D-1:0]   mem_d_w;
  reg [DEPTH_V-1:0]   mem_d_addr_w;
  reg [DEPTH_V-1:0]   mem_d_addr_r_a;
  reg [DEPTH_V-1:0]   mem_d_addr_r_b;
  reg                 mem_d_we;

  wire                cpu_stopped;
  reg [WIDTH_D-1:0]   cpu_d_r_a;
  reg [WIDTH_D-1:0]   cpu_d_r_b;
  wire [WIDTH_D-1:0]  cpu_d_w;
  wire [DEPTH_V-1:0]  cpu_d_addr_w;
  wire [DEPTH_V-1:0]  cpu_d_addr_r_a;
  wire [DEPTH_V-1:0]  cpu_d_addr_r_b;
  reg [DEPTH_V-1:0]   cpu_d_addr_w_d1;
  wire                cpu_d_we;

  reg [DEPTH_V-1:0]   soc_addr_r;
  reg [DEPTH_V-1:0]   soc_addr_w;
  reg [WIDTH_SOC-1:0] soc_data_r;
  reg [WIDTH_SOC-1:0] soc_data_w;
  reg                 soc_we;
  reg                 soc_we_p1;

  // I/O register
  localparam IO_REG_W_UART_TX = 0;
  localparam IO_REG_W_SPRITE_X = 1;
  localparam IO_REG_W_SPRITE_Y = 2;
  localparam IO_REG_W_SPRITE_SCALE = 3;
  localparam IO_REG_W_LED = 4;
  localparam IO_REG_W_AUDIO_DIVIDER = 5;
  localparam IO_REG_W_AUDIO_DATA = 6;
  localparam IO_REG_W_AUDIO_VALID = 7;
  localparam IO_REG_W_CHR_X = 8;
  localparam IO_REG_W_CHR_Y = 9;
  localparam IO_REG_W_CHR_SCALE = 10;
  localparam IO_REG_W_CHR_PALETTE0 = 11;
  localparam IO_REG_W_CHR_PALETTE1 = 12;
  localparam IO_REG_W_CHR_PALETTE2 = 13;
  localparam IO_REG_W_CHR_PALETTE3 = 14;
  localparam IO_REG_W_TIMER_RESET = 15;
  localparam IO_REG_W_I2C = 16;
  localparam IO_REG_R_UART_BUSY = 0;
  localparam IO_REG_R_VGA_VSYNC = 1;
  localparam IO_REG_R_VGA_VCOUNT = 2;
  localparam IO_REG_R_AUDIO_FULL = 3;
  localparam IO_REG_R_TIMER_COUNT = 4;
  localparam IO_REG_R_I2C = 5;
  reg [WIDTH_D-1:0]      io_reg_w[0:((1 << DEPTH_IO_REG) - 1)];
  reg [WIDTH_D-1:0]      io_reg_r[0:((1 << DEPTH_IO_REG) - 1)];
  reg                    io_reg_we;
  reg [DEPTH_IO_REG-1:0] io_reg_addr_r;
  reg [WIDTH_D-1:0]      io_reg_r_copy;

  // System register
  localparam SYS_REG_CPU_RESET = 0;
  localparam SYS_REG_CPU_RESUME = 1;
  localparam SYS_REG_MASTER_OF_MEM_D = 2;
  reg [WIDTH_D-1:0] sys_reg_w[0:((1 << DEPTH_SYS_REG) - 1)];
  reg               sys_reg_we;

  // memory mapper
  reg [MAPPER_BITS-1:0] map_cpu_d_r_a;
  reg [MAPPER_BITS-1:0] map_cpu_d_r_b;
  reg [MAPPER_BITS-1:0] map_cpu_d_r_a_d1;
  reg [MAPPER_BITS-1:0] map_cpu_d_r_b_d1;
  reg [MAPPER_BITS-1:0] map_cpu_d_w;
  reg [MAPPER_BITS-1:0] map_soc_r;
  reg [MAPPER_BITS-1:0] map_soc_r_d1;
  reg [MAPPER_BITS-1:0] map_soc_w;

  integer i;

  // master
  localparam MASTER_BITS = 1;
  localparam MASTER_IS_SOC = 0;
  localparam MASTER_IS_CPU = 1;

  // virtual address
  localparam VADDR_MEM_D   = 32'h00000000;
  localparam VADDR_MEM_I   = 32'h00004000;
  localparam VADDR_SYS_REG = 32'h00005000;

`ifdef USE_UART
  // uart
  localparam            UART_WIDTH = 8;
  reg                   uart_start;
  reg [UART_WIDTH-1:0]  uart_data_tx;
  wire                  uart_busy;
  wire                  uart_re;

  localparam            SOC_UART_IF_START_BYTE = 8'b10101010;
  localparam            SOC_UART_IF_END_BYTE = 8'b01010101;
  wire [UART_WIDTH-1:0] uart_data_rx;
  reg [7:0]             soc_reg [0:9];
  reg [3:0]             soc_reg_state;
  reg                   uart_re_d1;
  wire                  uart_re_posedge;
`endif
`ifdef USE_VGA
  // chr_bg
  localparam           CHR_SIZE_BITS = 6;
  localparam           CHR_WIDTH = 8;
  localparam           CHR_BPP = 2;
  reg [DEPTH_V-1:0]    chr_chr_d_addr_w;
  reg [CHR_WIDTH-1:0]  chr_chr_d_w;
  reg                  chr_chr_d_we;
  reg [DEPTH_V-1:0]    chr_bit_d_addr_w;
  reg [CHR_BPP-1:0]    chr_bit_d_w;
  reg                  chr_bit_d_we;
  // sprite
  localparam           SPRITE_BPP = 8;
  reg [DEPTH_V-1:0]    sprite_d_addr_w;
  reg [SPRITE_BPP-1:0] sprite_d_w;
  reg                  sprite_d_we;
  // vga
  localparam           PIXEL_DELAY = 9;
  wire                 vga_vsync;
  wire [WIDTH_D-1:0]   vga_vcount;
  wire [32-1:0]        ext_vga_count_h;
  wire [32-1:0]        ext_vga_count_v;
`endif
`ifdef USE_MINI_VGA
  // sprite
  reg [DEPTH_V-1:0]        sprite_d_addr_w;
  reg [`MINI_VGA_BPP-1:0]  sprite_d_w;
  reg                      sprite_d_we;
  // vga
  wire                     vga_vsync;
  wire [WIDTH_D-1:0]       vga_vcount;
  wire [32-1:0]            ext_vga_count_h;
  wire [32-1:0]            ext_vga_count_v;
  wire [`MINI_VGA_BPP-1:0] vga_color_in;
`endif
`ifdef USE_AUDIO
  // audio
  wire audio_full;
`endif
`ifdef USE_MINI_AUDIO
  // mini audio
  wire audio_full;
`endif
`ifdef USE_XAUDIO
  // xaudio
  wire audio_full;
`endif
`ifdef USE_TIMER
  // timer
  wire [WIDTH_D-1:0] timer_count;
`endif
`ifdef USE_I2C
  wire [1:0] i2c_command;
  wire       i2c_start;
  wire [7:0] i2c_data_w;
  wire       i2c_r_ack;
  wire       i2c_w_ack;
  wire [7:0] i2c_data_r;
  wire       i2c_busy;
  assign i2c_command = io_reg_w[IO_REG_W_I2C][11:10];
  assign i2c_start = io_reg_w[IO_REG_W_I2C][9];
  assign i2c_r_ack = io_reg_w[IO_REG_W_I2C][8];
  assign i2c_data_w = io_reg_w[IO_REG_W_I2C][7:0];
`endif
`ifdef USE_MEM_S
  localparam         WIDTH_S = 16;
  localparam         DEPTH_S = 16;
  wire [WIDTH_S-1:0] mem_s_r;
  reg [WIDTH_S-1:0]  mem_s_w;
  reg [DEPTH_S-1:0]  mem_s_addr_w;
  reg [DEPTH_S-1:0]  mem_s_addr_r;
  reg                mem_s_we;
`endif

  // address decode
  always @*
    begin
      case (cpu_d_addr_r_a[DA_MSB:DA_LSB])
        0:       map_cpu_d_r_a = MAP_MEM_D;
        1:       map_cpu_d_r_a = MAP_NONE;
        2:       map_cpu_d_r_a = MAP_REG_R;
        3:       map_cpu_d_r_a = MAP_REG_W;
        4:       map_cpu_d_r_a = MAP_MEM_I;
        5:       map_cpu_d_r_a = MAP_SYS;
        6:       map_cpu_d_r_a = MAP_CHR_CHR;
        7:       map_cpu_d_r_a = MAP_CHR_BIT;
        8,9,10,11: map_cpu_d_r_a = MAP_SPRITE;
        default: map_cpu_d_r_a = MAP_MEM_S;
      endcase
    end

  always @*
    begin
      case (cpu_d_addr_r_b[DA_MSB:DA_LSB])
        0:       map_cpu_d_r_b = MAP_MEM_D;
        1:       map_cpu_d_r_b = MAP_NONE;
        2:       map_cpu_d_r_b = MAP_REG_R;
        3:       map_cpu_d_r_b = MAP_REG_W;
        4:       map_cpu_d_r_b = MAP_MEM_I;
        5:       map_cpu_d_r_b = MAP_SYS;
        6:       map_cpu_d_r_b = MAP_CHR_CHR;
        7:       map_cpu_d_r_b = MAP_CHR_BIT;
        8,9,10,11: map_cpu_d_r_b = MAP_SPRITE;
        default: map_cpu_d_r_b = MAP_MEM_S;
      endcase
    end

  always @*
    begin
      case (cpu_d_addr_w[DA_MSB:DA_LSB])
        0:       map_cpu_d_w = MAP_MEM_D;
        1:       map_cpu_d_w = MAP_NONE;
        2:       map_cpu_d_w = MAP_REG_R;
        3:       map_cpu_d_w = MAP_REG_W;
        4:       map_cpu_d_w = MAP_MEM_I;
        5:       map_cpu_d_w = MAP_SYS;
        6:       map_cpu_d_w = MAP_CHR_CHR;
        7:       map_cpu_d_w = MAP_CHR_BIT;
        8,9,10,11: map_cpu_d_w = MAP_SPRITE;
        default: map_cpu_d_w = MAP_MEM_S;
      endcase
    end

  always @*
    begin
      case (soc_addr_r[DA_MSB:DA_LSB])
        0:       map_soc_r = MAP_MEM_D;
        1:       map_soc_r = MAP_NONE;
        2:       map_soc_r = MAP_REG_R;
        3:       map_soc_r = MAP_REG_W;
        4:       map_soc_r = MAP_MEM_I;
        5:       map_soc_r = MAP_SYS;
        6:       map_soc_r = MAP_CHR_CHR;
        7:       map_soc_r = MAP_CHR_BIT;
        8,9,10,11: map_soc_r = MAP_SPRITE;
        default: map_soc_r = MAP_MEM_S;
      endcase
    end

  always @*
    begin
      case (soc_addr_w[DA_MSB:DA_LSB])
        0:       map_soc_w = MAP_MEM_D;
        1:       map_soc_w = MAP_NONE;
        2:       map_soc_w = MAP_REG_R;
        3:       map_soc_w = MAP_REG_W;
        4:       map_soc_w = MAP_MEM_I;
        5:       map_soc_w = MAP_SYS;
        6:       map_soc_w = MAP_CHR_CHR;
        7:       map_soc_w = MAP_CHR_BIT;
        8,9,10,11: map_soc_w = MAP_SPRITE;
        default: map_soc_w = MAP_MEM_S;
      endcase
    end

  // switching address bus
  // mem_d_addr_r_a
  always @*
    begin
      if ((sys_reg_w[SYS_REG_MASTER_OF_MEM_D] == MASTER_IS_SOC) && (map_soc_r == MAP_MEM_D))
        begin
          mem_d_addr_r_a = soc_addr_r;
        end
      else if ((sys_reg_w[SYS_REG_MASTER_OF_MEM_D] == MASTER_IS_CPU) && (map_cpu_d_r_a == MAP_MEM_D))
        begin
          mem_d_addr_r_a = cpu_d_addr_r_a;
        end
      else
        begin
          mem_d_addr_r_a = ZERO;
        end
    end

  // mem_d_addr_r_b
  always @*
    begin
      mem_d_addr_r_b = cpu_d_addr_r_b;
    end

  // mem_d_addr_w
  always @*
    begin
      if ((sys_reg_w[SYS_REG_MASTER_OF_MEM_D] == MASTER_IS_SOC) && (map_soc_w == MAP_MEM_D))
        begin
          mem_d_addr_w = soc_addr_w;
        end
      else if ((sys_reg_w[SYS_REG_MASTER_OF_MEM_D] == MASTER_IS_CPU) && (map_cpu_d_w == MAP_MEM_D))
        begin
          mem_d_addr_w = cpu_d_addr_w;
        end
      else
        begin
          mem_d_addr_w = ZERO;
        end
    end

  // mem_i_addr_w
  // mem_i master: soc
  always @*
    begin
      mem_i_addr_w = soc_addr_w;
    end

  // io_reg_addr_r
  always @*
    begin
      io_reg_addr_r = cpu_d_addr_r_a;
    end

  // delay
  always @(posedge clk)
    begin
      map_cpu_d_r_a_d1 <= map_cpu_d_r_a;
      map_cpu_d_r_b_d1 <= map_cpu_d_r_b;
      map_soc_r_d1 <= map_soc_r;
    end

  // switching data read bus
  // data read clock phase = address clock phase + 1
  // soc_data_r
  always @*
    begin
      soc_data_r = mem_d_r_a;
    end

  // cpu_d_r_a
  always @*
    begin
      if ((sys_reg_w[SYS_REG_MASTER_OF_MEM_D] == MASTER_IS_CPU) && (map_cpu_d_r_a_d1 == MAP_MEM_D))
        begin
          cpu_d_r_a = mem_d_r_a;
        end
      else if (map_cpu_d_r_a_d1 == MAP_REG_R)
        begin
          cpu_d_r_a = io_reg_r_copy;
        end
`ifdef USE_MEM_S
      else if (map_cpu_d_r_a_d1 == MAP_MEM_S)
        begin
          cpu_d_r_a = mem_s_r;
        end
`endif
      else
        begin
          cpu_d_r_a = ZERO;
        end
    end

  // cpu_d_r_b
  always @*
    begin
      cpu_d_r_b = mem_d_r_b;
    end

  // switching data write bus
  // mem_d_w
  always @*
    begin
      if ((sys_reg_w[SYS_REG_MASTER_OF_MEM_D] == MASTER_IS_SOC) && (map_soc_w == MAP_MEM_D))
        begin
          mem_d_w = soc_data_w;
        end
      else if ((sys_reg_w[SYS_REG_MASTER_OF_MEM_D] == MASTER_IS_CPU) && (map_cpu_d_w == MAP_MEM_D))
        begin
          mem_d_w = cpu_d_w;
        end
      else
        begin
          mem_d_w = ZERO;
        end
    end

  // mem_i_w
  always @*
    begin
      mem_i_w = soc_data_w;
    end

  // mem_d_we
  always @*
    begin
      if ((sys_reg_w[SYS_REG_MASTER_OF_MEM_D] == MASTER_IS_SOC) && (map_soc_w == MAP_MEM_D))
        begin
          mem_d_we = soc_we;
        end
      else if ((sys_reg_w[SYS_REG_MASTER_OF_MEM_D] == MASTER_IS_CPU) && (map_cpu_d_w == MAP_MEM_D))
        begin
          mem_d_we = cpu_d_we;
        end
      else
        begin
          mem_d_we = FALSE;
        end
    end

  // mem_i_we
  always @*
    begin
      if (map_soc_w == MAP_MEM_I)
        begin
          mem_i_we = soc_we;
        end
      else
        begin
          mem_i_we = FALSE;
        end
    end

  // io_reg_we
  always @*
    begin
      if (map_cpu_d_w == MAP_REG_W)
        begin
          io_reg_we = cpu_d_we;
        end
      else
        begin
          io_reg_we = FALSE;
        end
    end

  // sys_reg_we
  always @*
    begin
      if (map_soc_w == MAP_SYS)
        begin
          sys_reg_we = soc_we;
        end
      else
        begin
          sys_reg_we = FALSE;
        end
    end

  // write io_reg_w
  always @(posedge clk)
    begin
      if (io_reg_we == TRUE)
        begin
          io_reg_w[cpu_d_addr_w[DEPTH_IO_REG-1:0]] <= cpu_d_w;
        end
    end

  // write io_reg_r
  always @(posedge clk)
    begin
      io_reg_r_copy <= io_reg_r[io_reg_addr_r];
`ifdef USE_UART
      io_reg_r[IO_REG_R_UART_BUSY] <= uart_busy;
`endif
`ifdef USE_VGA
      io_reg_r[IO_REG_R_VGA_VSYNC] <= vga_vsync;
      io_reg_r[IO_REG_R_VGA_VCOUNT] <= vga_vcount;
`endif
`ifdef USE_MINI_VGA
      io_reg_r[IO_REG_R_VGA_VSYNC] <= vga_vsync;
      io_reg_r[IO_REG_R_VGA_VCOUNT] <= vga_vcount;
`endif
`ifdef USE_AUDIO
      io_reg_r[IO_REG_R_AUDIO_FULL] <= audio_full;
`endif
`ifdef USE_MINI_AUDIO
      io_reg_r[IO_REG_R_AUDIO_FULL] <= audio_full;
`endif
`ifdef USE_XAUDIO
      io_reg_r[IO_REG_R_AUDIO_FULL] <= audio_full;
`endif
`ifdef USE_TIMER
      io_reg_r[IO_REG_R_TIMER_COUNT] <= timer_count;
`endif
`ifdef USE_I2C
      io_reg_r[IO_REG_R_I2C] <= {i2c_busy, i2c_w_ack, i2c_data_r};
`endif
    end

  // LED
  always @(posedge clk)
    begin
      led <= io_reg_w[IO_REG_W_LED];
    end

  // write sys_reg_w
  always @(posedge clk)
    begin
      if (reset == TRUE)
        begin
          for (i = 0; i < ((1 << DEPTH_SYS_REG)-1); i = i + 1)
            begin
              sys_reg_w[i] <= ZERO;
            end
        end
      else
        begin
          if (sys_reg_we == TRUE)
            begin
              sys_reg_w[soc_addr_w[DEPTH_SYS_REG-1:0]] <= soc_data_w;
            end
        end
    end


  sc1_cpu
    #(
      .WIDTH_I (WIDTH_I),
      .WIDTH_D (WIDTH_D),
      .DEPTH_I (DEPTH_I),
      .DEPTH_D (DEPTH_V),
      .DEPTH_REG (DEPTH_REG)
      )
  sc1_cpu_0
    (
     .clk (clk),
     .reset (sys_reg_w[SYS_REG_CPU_RESET]),
     .resume (sys_reg_w[SYS_REG_CPU_RESUME]),
     .mem_i_r (mem_i_r),
     .mem_i_addr_r (mem_i_addr_r),
     .mem_d_r_a (cpu_d_r_a),
     .mem_d_r_b (cpu_d_r_b),
     .mem_d_w (cpu_d_w),
     .mem_d_addr_w (cpu_d_addr_w),
     .mem_d_addr_r_a (cpu_d_addr_r_a),
     .mem_d_addr_r_b (cpu_d_addr_r_b),
     .mem_d_we (cpu_d_we),
     .stopped (cpu_stopped)
     );

`ifdef USE_INIT_RAM
  default_code_mem
    #(
      .DATA_WIDTH (WIDTH_I),
      .ADDR_WIDTH (DEPTH_I)
      )
  mem_i
    (
     .clk (clk),
     .addr_r (mem_i_addr_r),
     .addr_w (mem_i_addr_w),
     .data_in (mem_i_w),
     .we (mem_i_we),
     .data_out (mem_i_r)
     );

  default_data_mem_pair
    #(
      .DATA_WIDTH (WIDTH_D),
      .ADDR_WIDTH (DEPTH_D)
      )
  mem_d
    (
     .clk (clk),
     .addr_r_a (mem_d_addr_r_a),
     .addr_r_b (mem_d_addr_r_b),
     .addr_w (mem_d_addr_w),
     .data_in (mem_d_w),
     .we (mem_d_we),
     .data_out_a (mem_d_r_a),
     .data_out_b (mem_d_r_b)
     );

  reg [3:0] state_loader;
  // reset and resume
  always @(posedge clk)
    begin
      if (reset == TRUE)
        begin
          state_loader <= ZERO;
          soc_addr_w <= ZERO;
          soc_data_w <= ZERO;
          soc_we <= FALSE;
        end
      else
        // init
        begin
          case (state_loader)
            0:
              begin
                // cpu reset on
                soc_addr_w <= VADDR_SYS_REG + SYS_REG_CPU_RESET;
                soc_data_w <= 32'h00000001;
                soc_we <= TRUE;
                state_loader <= 1;
              end
            1:
              begin
                // master of mem_d: cpu
                soc_addr_w <= VADDR_SYS_REG + SYS_REG_MASTER_OF_MEM_D;
                soc_data_w <= 32'h00000001;
                soc_we <= TRUE;
                state_loader <= 2;
              end
            2:
              begin
                // cpu reset off
                soc_addr_w <= VADDR_SYS_REG + SYS_REG_CPU_RESET;
                soc_data_w <= 32'h00000000;
                soc_we <= TRUE;
                state_loader <= 3;
              end
            3:
              begin
                // resume on
                soc_addr_w <= VADDR_SYS_REG + SYS_REG_CPU_RESUME;
                soc_data_w <= 32'h00000001;
                soc_we <= TRUE;
                state_loader <= 4;
              end
            4:
              begin
                // resume off
                soc_addr_w <= VADDR_SYS_REG + SYS_REG_CPU_RESUME;
                soc_data_w <= 32'h00000000;
                soc_we <= TRUE;
                state_loader <= 4;
              end
            default:
              begin
                soc_addr_w <= ZERO;
                soc_data_w <= ZERO;
                soc_we <= FALSE;
                state_loader <= 0;
              end
          endcase
        end
    end
`else
  rw_port_ram
    #(
      .DATA_WIDTH (WIDTH_I),
      .ADDR_WIDTH (DEPTH_I)
      )
  mem_i
    (
     .clk (clk),
     .addr_r (mem_i_addr_r),
     .addr_w (mem_i_addr_w),
     .data_in (mem_i_w),
     .we (mem_i_we),
     .data_out (mem_i_r)
     );

  r2w1_port_ram
    #(
      .DATA_WIDTH (WIDTH_D),
      .ADDR_WIDTH (DEPTH_D)
      )
  mem_d
    (
     .clk (clk),
     .addr_r_a (mem_d_addr_r_a),
     .addr_r_b (mem_d_addr_r_b),
     .addr_w (mem_d_addr_w),
     .data_in (mem_d_w),
     .we (mem_d_we),
     .data_out_a (mem_d_r_a),
     .data_out_b (mem_d_r_b)
     );
`endif

  // 16bit scratch memory
`ifdef USE_MEM_S
  // mem_s_addr_r
  always @*
    begin
      mem_s_addr_r = cpu_d_addr_r_a;
    end

  // mem_s_addr_w
  always @*
    begin
      mem_s_addr_w = cpu_d_addr_w;
    end

  // mem_s_w
  always @*
    begin
      mem_s_w = cpu_d_w;
    end

  // mem_s_we
  always @*
    begin
      if (map_cpu_d_w == MAP_MEM_S)
        begin
          mem_s_we = cpu_d_we;
        end
      else
        begin
          mem_s_we = FALSE;
        end
    end

  rw_port_ram
    #(
      .DATA_WIDTH (WIDTH_S),
      .ADDR_WIDTH (DEPTH_S)
      )
  mem_s
    (
     .clk (clk),
     .addr_r (mem_s_addr_r),
     .addr_w (mem_s_addr_w),
     .data_in (mem_s_w),
     .we (mem_s_we),
     .data_out (mem_s_r)
     );
`endif

`ifdef USE_UART

  // ---- SoC UART Interface Start ----
  /* uart memo
   start byte [0]: 10101010
   addr [4][3][2][1]
   data [8][7][6][5]
   end byte [9]: 01010101
   */

  // uart read packet
  always @(posedge clk)
    begin
      uart_re_d1 <= uart_re;
    end
  assign uart_re_posedge = ((uart_re == TRUE) && (uart_re_d1 == FALSE)) ? TRUE : FALSE;

  always @(posedge clk)
    begin
      if (reset == TRUE)
        begin
          soc_reg_state <= 4'd0;
          soc_we_p1 <= FALSE;
        end
      else
        begin
          if (uart_re_posedge == TRUE)
            begin
              soc_reg[soc_reg_state] <= uart_data_rx;
              case (soc_reg_state)
                4'd0:
                  begin
                    // check start byte
                    if (uart_data_rx == SOC_UART_IF_START_BYTE)
                      begin
                        soc_reg_state <= soc_reg_state + ONE;
                        soc_we_p1 <= FALSE;
                      end
                    else
                      begin
                        soc_reg_state <= 4'd0;
                        soc_we_p1 <= FALSE;
                      end
                  end
                4'd9:
                  begin
                    // check end byte
                    soc_reg_state <= 4'd0;
                    if (uart_data_rx == SOC_UART_IF_END_BYTE)
                      begin
                        soc_we_p1 <= TRUE;
                      end
                    else
                      begin
                        soc_we_p1 <= FALSE;
                      end
                  end
                default:
                  begin
                    soc_reg_state <= soc_reg_state + ONE;
                    soc_we_p1 <= FALSE;
                  end
              endcase
            end
          else
            begin
              soc_we_p1 <= FALSE;
            end
        end
    end

  always @(posedge clk)
    begin
      if (reset == TRUE)
        begin
          soc_we <= FALSE;
        end
      else
        begin
          soc_we <= soc_we_p1;
        end
    end

  always @(posedge clk)
    begin
      if (reset == TRUE)
        begin
          soc_addr_w <= ZERO;
          soc_data_w <= ZERO;
        end
      else
        begin
          if (soc_we_p1 == TRUE)
            begin
              soc_addr_w <= {soc_reg[4], soc_reg[3], soc_reg[2], soc_reg[1]};
              soc_data_w <= {soc_reg[8], soc_reg[7], soc_reg[6], soc_reg[5]};
            end
        end
    end

  // uart send byte
  always @(posedge clk)
    begin
      if (reset == TRUE)
        begin
          uart_data_tx <= ZERO;
          uart_start <= FALSE;
        end
      else
        begin
          if ((io_reg_we == TRUE) && (cpu_d_addr_w[DEPTH_IO_REG-1:0] == IO_REG_W_UART_TX) && (uart_busy == FALSE))
            begin
              uart_data_tx <= cpu_d_w;
              uart_start <= TRUE;
            end
          else
            begin
              uart_data_tx <= ZERO;
              uart_start <= FALSE;
            end
        end
    end
  // ---- SoC UART Interface End ----

  uart
    #(
      .CLK_HZ (UART_CLK_HZ),
      .SCLK_HZ (UART_SCLK_HZ),
      .WIDTH (UART_WIDTH)
      )
  uart_0
    (
     .clk (clk),
     .reset (reset),
     .rxd (uart_rxd),
     .start (uart_start),
     .data_tx (uart_data_tx),
     .txd (uart_txd),
     .busy (uart_busy),
     .re (uart_re),
     .data_rx (uart_data_rx)
     );

`endif

`ifdef USE_VGA

  // layer
  localparam PLANES = 2;
  wire [BPP-1:0] color_all;
  wire [BPP-1:0] color [0:PLANES-1];
  reg  [BPP-1:0] layer [0:PLANES-1];
  always @*
    begin
      layer[0] = (color[0] == 0) ? 0 : color[0];
      for (i = 0; i < PLANES-1; i = i + 1)
        begin
          layer[i+1] = (color[i+1] == 0) ? layer[i] : color[i+1];
        end
    end
  assign color_all = layer[PLANES-1]; // color_all: delay 8

  // chr_chr_d_addr_w
  always @*
    begin
      chr_chr_d_addr_w = cpu_d_addr_w;
    end

  // chr_chr_d_w
  always @*
    begin
      chr_chr_d_w = cpu_d_w;
    end

  // chr_chr_d_we
  always @*
    begin
      if (map_cpu_d_w == MAP_CHR_CHR)
        begin
          chr_chr_d_we = cpu_d_we;
        end
      else
        begin
          chr_chr_d_we = FALSE;
        end
    end

  // chr_bit_d_addr_w
  always @*
    begin
      chr_bit_d_addr_w = cpu_d_addr_w;
    end

  // chr_bit_d_w
  always @*
    begin
      chr_bit_d_w = cpu_d_w;
    end

  // chr_bit_d_we
  always @*
    begin
      if (map_cpu_d_w == MAP_CHR_BIT)
        begin
          chr_bit_d_we = cpu_d_we;
        end
      else
        begin
          chr_bit_d_we = FALSE;
        end
    end

  chr_bg
    #(
      .CHR_SIZE_BITS (6),
      .BITMAP_BITS (2)
      )
  chr_bg_0
    (
     .clk (clk),
     .reset (reset),
     .chr_length (),
     .chr_address (chr_chr_d_addr_w),
     .chr_din (chr_chr_d_w),
     .chr_dout (),
     .chr_we (chr_chr_d_we),
     .chr_oe (FALSE),
     .bitmap_length (),
     .bitmap_address (chr_bit_d_addr_w),
     .bitmap_din (chr_bit_d_w),
     .bitmap_dout (),
     .bitmap_we (chr_bit_d_we),
     .bitmap_oe (FALSE),
     .x (io_reg_w[IO_REG_W_CHR_X]),
     .y (io_reg_w[IO_REG_W_CHR_Y]),
     .scale (io_reg_w[IO_REG_W_CHR_SCALE]),
     .palette0 (io_reg_w[IO_REG_W_CHR_PALETTE0]),
     .palette1 (io_reg_w[IO_REG_W_CHR_PALETTE1]),
     .palette2 (io_reg_w[IO_REG_W_CHR_PALETTE2]),
     .palette3 (io_reg_w[IO_REG_W_CHR_PALETTE3]),
     .ext_clkv (clkv),
     .ext_resetv (resetv),
     .ext_color (color[1]),
     .ext_count_h (ext_vga_count_h),
     .ext_count_v (ext_vga_count_v)
     );

  // sprite_d_addr_w
  always @*
    begin
      sprite_d_addr_w = cpu_d_addr_w;
    end

  // sprite_d_w
  always @*
    begin
      sprite_d_w = cpu_d_w;
    end

  // sprite_d_we
  always @*
    begin
      if (map_cpu_d_w == MAP_SPRITE)
        begin
          sprite_d_we = cpu_d_we;
        end
      else
        begin
          sprite_d_we = FALSE;
        end
    end

  sprite sprite_0
    (
     .clk (clk),
     .reset (reset),

     .bitmap_length (),
     .bitmap_address (sprite_d_addr_w),
     .bitmap_din (sprite_d_w),
     .bitmap_dout (),
     .bitmap_we (sprite_d_we),
     .bitmap_oe (FALSE),

     .x (io_reg_w[IO_REG_W_SPRITE_X]),
     .y (io_reg_w[IO_REG_W_SPRITE_Y]),
     .scale (io_reg_w[IO_REG_W_SPRITE_SCALE]),

     .ext_clkv (clkv),
     .ext_resetv (resetv),
     .ext_color (color[0]),
     .ext_count_h (ext_vga_count_h),
     .ext_count_v (ext_vga_count_v)
     );

  vga_iface
    #(
      .PIXEL_DELAY (PIXEL_DELAY),
      .BPP (BPP),
`ifdef VGA_720P
      .VGA_MAX_H (1650-1),
      .VGA_MAX_V (750-1),
      .VGA_WIDTH (1280),
      .VGA_HEIGHT (720),
      .VGA_SYNC_H_START (1390),
      .VGA_SYNC_V_START (725),
      .VGA_SYNC_H_END (1430),
      .VGA_SYNC_V_END (730)
`else
      .VGA_MAX_H (800-1),
      .VGA_MAX_V (525-1),
      .VGA_WIDTH (640),
      .VGA_HEIGHT (480),
      .VGA_SYNC_H_START (656),
      .VGA_SYNC_V_START (490),
      .VGA_SYNC_H_END (752),
      .VGA_SYNC_V_END (492)
`endif
      )
  vga_iface_0
    (
     .clk (clk),
     .reset (reset),
     .vsync (vga_vsync),
     .vcount (vga_vcount),
     .ext_clkv (clkv),
     .ext_resetv (resetv),
     .ext_color_in (color_all),
     .ext_vga_hs (vga_hs),
     .ext_vga_vs (vga_vs),
     .ext_vga_de (vga_de),
     .ext_vga_color_out (vga_color_out),
     .ext_count_h (ext_vga_count_h),
     .ext_count_v (ext_vga_count_v)
     );

`endif

`ifdef USE_MINI_VGA

  // sprite_d_addr_w
  always @*
    begin
      sprite_d_addr_w = cpu_d_addr_w;
    end

  // sprite_d_w
  always @*
    begin
      sprite_d_w = cpu_d_w;
    end

  // sprite_d_we
  always @*
    begin
      if (map_cpu_d_w == MAP_SPRITE)
        begin
          sprite_d_we = cpu_d_we;
        end
      else
        begin
          sprite_d_we = FALSE;
        end
    end

  mini_sprite
    #(
      .SPRITE_WIDTH_BITS (`MINI_VGA_SPRITE_WIDTH_BITS),
      .SPRITE_HEIGHT_BITS (`MINI_VGA_SPRITE_HEIGHT_BITS),
      .BPP (`MINI_VGA_BPP)
      )
  mini_sprite_0
    (
     .clk (clk),
     .reset (reset),

     .bitmap_length (),
     .bitmap_address (sprite_d_addr_w),
     .bitmap_din (sprite_d_w),
     .bitmap_dout (),
     .bitmap_we (sprite_d_we),
     .bitmap_oe (FALSE),

     .x (io_reg_w[IO_REG_W_SPRITE_X]),
     .y (io_reg_w[IO_REG_W_SPRITE_Y]),
     .scale (io_reg_w[IO_REG_W_SPRITE_SCALE]),

     .ext_color (vga_color_in),
     .ext_count_h (ext_vga_count_h),
     .ext_count_v (ext_vga_count_v)
     );

  mini_vga
    #(
      .VGA_MAX_H (`MINI_VGA_MAX_H),
      .VGA_MAX_V (`MINI_VGA_MAX_V),
      .VGA_WIDTH (`MINI_VGA_WIDTH),
      .VGA_HEIGHT (`MINI_VGA_HEIGHT),
      .VGA_SYNC_H_START (`MINI_VGA_SYNC_H_START),
      .VGA_SYNC_V_START (`MINI_VGA_SYNC_V_START),
      .VGA_SYNC_H_END (`MINI_VGA_SYNC_H_END),
      .VGA_SYNC_V_END (`MINI_VGA_SYNC_V_END),
      .LINE_BITS (`MINI_VGA_LINE_BITS),
      .COUNTER_BITS (`MINI_VGA_COUNTER_BITS),
      .BPP (`MINI_VGA_BPP),
      .ENABLE_FRAC_CLK (`MINI_VGA_ENABLE_FRAC_CLK)
      )
  mini_vga_0
    (
     .clk (clk),
     .reset (reset),

     .ext_factor_mul (`MINI_VGA_FACTOR_MUL),
     .ext_factor_div (`MINI_VGA_FACTOR_DIV),
     .scale_h (`MINI_VGA_SCALE_H),
     .vsync (vga_vsync),
     .vcount (vga_vcount),
     .ext_color (vga_color_in),
     .ext_vga_hs (vga_hs),
     .ext_vga_vs (vga_vs),
     .ext_vga_de (),
     .ext_vga_clk (),
     .ext_vga_color (vga_color_out),
     .ext_count_h (ext_vga_count_h),
     .ext_count_v (ext_vga_count_v)
     );

`endif

`ifdef USE_AUDIO

  audio_output audio_output_0
    (
     .clk (clk),
     .reset (reset),
     .data (io_reg_w[IO_REG_W_AUDIO_DATA]),
     .clock_divider (io_reg_w[IO_REG_W_AUDIO_DIVIDER]),
     .valid_toggle (io_reg_w[IO_REG_W_AUDIO_VALID]),
     .full (audio_full),
     .ext_audio_clk (clka),
     .ext_audio_reset (reseta),
     .ext_audio_r (audio_r),
     .ext_audio_l (audio_l)
     );

`endif

`ifdef USE_MINI_AUDIO

  mini_audio
    #(
      .COUNTER_BITS (32)
      )
  mini_audio_0
    (
     .clk (clk),
     .reset (reset),
     .data (io_reg_w[IO_REG_W_AUDIO_DATA]),
     .valid_toggle (io_reg_w[IO_REG_W_AUDIO_VALID]),
     .ext_factor_mul (`MINI_AUDIO_FACTOR_MUL),
     .ext_factor_div (`MINI_AUDIO_FACTOR_DIV),
     .full (audio_full),
     .ext_audio_r (audio_r),
     .ext_audio_l (audio_l)
     );

`endif

`ifdef USE_XAUDIO

  wire signed [16:0] sample_l0;
  wire signed [16:0] sample_r0;
  reg signed [15:0]  sample_l1;
  reg signed [15:0]  sample_r1;
  wire [31:0]        sample_data;
  reg                sample_en;
  reg                audio_toggle_pr;

  assign sample_l0 = io_reg_w[IO_REG_W_AUDIO_DATA][31:16];
  assign sample_r0 = io_reg_w[IO_REG_W_AUDIO_DATA][15:0];

  always @(posedge clk)
    begin
      sample_l1 <= sample_l0 - 16'h8000;
      sample_r1 <= sample_r0 - 16'h8000;
    end

  assign sample_data = {sample_l1, sample_r1};

  always @(posedge clk)
    begin
      audio_toggle_pr <= io_reg_w[IO_REG_W_AUDIO_VALID];
    end

  always @(posedge clk)
    begin
      if (audio_toggle_pr != io_reg_w[IO_REG_W_AUDIO_VALID])
        begin
          sample_en <= TRUE;
        end
      else
        begin
          sample_en <= FALSE;
        end
    end

  xlive_audio
    #(
      .AUDIO_WIDTH (AUDIO_WIDTH),
      .BUFFER_DEPTH (BUFFER_DEPTH)
      )
  xlive_audio_0
    (
     .clk_tx (clka),
     .clk_rx (clk),
     .reset_tx (reseta),
     .reset_rx (reset),
     .data_rx (sample_data),
     .en_rx (sample_en),
     .full_rx (audio_full),
     .data_tx (audio_data),
     .valid_tx (audio_valid),
     .id_tx (audio_id),
     .ready_tx (audio_ready)
     );

`endif

`ifdef USE_ROM_LOADER

  reg [DEPTH_I:0] rom_i_addr;
  wire [WIDTH_I-1:0] rom_i_data;
  reg [DEPTH_D:0]    rom_d_addr;
  wire [WIDTH_D-1:0] rom_d_data;
  reg [3:0]          state_loader;
  // load program
  always @(posedge clk)
    begin
      if (reset == TRUE)
        begin
          state_loader <= ZERO;
          soc_addr_w <= ZERO;
          soc_data_w <= ZERO;
          soc_we <= FALSE;
          rom_i_addr <= ZERO;
          rom_d_addr <= ZERO;
        end
      else
        // init
        begin
          case (state_loader)
            0:
              begin
                // cpu reset on
                soc_addr_w <= VADDR_SYS_REG + SYS_REG_CPU_RESET;
                soc_data_w <= 32'h00000001;
                rom_i_addr <= ZERO;
                rom_d_addr <= ZERO;
                soc_we <= TRUE;
                state_loader <= 1;
              end
            1:
              begin
                // master of mem_d: soc
                soc_addr_w <= VADDR_SYS_REG + SYS_REG_MASTER_OF_MEM_D;
                soc_data_w <= 32'h00000000;
                rom_i_addr <= ZERO;
                rom_d_addr <= ZERO;
                soc_we <= TRUE;
                state_loader <= 2;
              end
            2:
              begin
                // start loading code
                soc_addr_w <= VADDR_MEM_I;
                soc_data_w <= ZERO;
                rom_i_addr <= ZERO;
                rom_d_addr <= ZERO;
                soc_we <= FALSE;
                state_loader <= 3;
              end
            3:
              begin
                // rom load wait
                if (rom_i_addr[DEPTH_I] == ONE)
                  begin
                    state_loader <= 6;
                  end
                else
                  begin
                    state_loader <= 4;
                  end
                soc_addr_w <= soc_addr_w;
                soc_data_w <= ZERO;
                rom_i_addr <= rom_i_addr;
                rom_d_addr <= ZERO;
                soc_we <= FALSE;
              end
            4:
              begin
                // write
                soc_addr_w <= soc_addr_w;
                soc_data_w <= rom_i_data;
                rom_i_addr <= rom_i_addr;
                rom_d_addr <= ZERO;
                soc_we <= TRUE;
                state_loader <= 5;
              end
            5:
              begin
                // next
                soc_addr_w <= soc_addr_w + ONE;
                soc_data_w <= ZERO;
                rom_i_addr <= rom_i_addr + ONE;
                rom_d_addr <= ZERO;
                soc_we <= FALSE;
                state_loader <= 3;
              end
            6:
              begin
                // start loading data
                soc_addr_w <= VADDR_MEM_D;
                soc_data_w <= ZERO;
                rom_i_addr <= ZERO;
                rom_d_addr <= ZERO;
                soc_we <= FALSE;
                state_loader <= 7;
              end
            7:
              begin
                // rom load wait
                if (rom_d_addr[DEPTH_D] == ONE)
                  begin
                    state_loader <= 10;
                  end
                else
                  begin
                    state_loader <= 8;
                  end

                soc_addr_w <= soc_addr_w;
                soc_data_w <= ZERO;
                rom_i_addr <= ZERO;
                rom_d_addr <= rom_d_addr;
                soc_we <= FALSE;
              end
            8:
              begin
                // write data
                soc_addr_w <= soc_addr_w;
                soc_data_w <= rom_d_data;
                rom_i_addr <= ZERO;
                rom_d_addr <= rom_d_addr;
                soc_we <= TRUE;
                state_loader <= 9;
              end
            9:
              begin
                soc_addr_w <= soc_addr_w + ONE;
                soc_data_w <= ZERO;
                rom_i_addr <= ZERO;
                rom_d_addr <= rom_d_addr + ONE;
                soc_we <= FALSE;
                state_loader <= 7;
              end
            10:
              begin
                // master of mem_d: cpu
                soc_addr_w <= VADDR_SYS_REG + SYS_REG_MASTER_OF_MEM_D;
                soc_data_w <= 32'h00000001;
                rom_i_addr <= ZERO;
                rom_d_addr <= ZERO;
                soc_we <= TRUE;
                state_loader <= 11;
              end
            11:
              begin
                // cpu reset off
                soc_addr_w <= VADDR_SYS_REG + SYS_REG_CPU_RESET;
                soc_data_w <= 32'h00000000;
                rom_i_addr <= ZERO;
                rom_d_addr <= ZERO;
                soc_we <= TRUE;
                state_loader <= 12;
              end
            12:
              begin
                // resume on
                soc_addr_w <= VADDR_SYS_REG + SYS_REG_CPU_RESUME;
                soc_data_w <= 32'h00000001;
                rom_i_addr <= ZERO;
                rom_d_addr <= ZERO;
                soc_we <= TRUE;
                state_loader <= 13;
              end
            13:
              begin
                // resume off
                soc_addr_w <= VADDR_SYS_REG + SYS_REG_CPU_RESUME;
                soc_data_w <= 32'h00000000;
                rom_i_addr <= ZERO;
                rom_d_addr <= ZERO;
                soc_we <= TRUE;
                state_loader <= 14;
              end
            default:
              begin
                soc_addr_w <= ZERO;
                soc_data_w <= ZERO;
                rom_i_addr <= ZERO;
                rom_d_addr <= ZERO;
                soc_we <= FALSE;
                state_loader <= 14;
              end
          endcase
        end
    end

  default_code_rom default_code_rom_0
    (
     .clk (clk),
     .addr (rom_i_addr),
     .data_out (rom_i_data)
     );

  default_data_rom default_data_rom_0
    (
     .clk (clk),
     .addr (rom_d_addr),
     .data_out (rom_d_data)
     );

`endif

`ifdef USE_TIMER

  free_run_counter
    #(
      .WIDTH (WIDTH_D)
      )
  free_run_counter_0
    (
     .clk (clk),
     .reset (io_reg_w[IO_REG_W_TIMER_RESET]),
     .count (timer_count)
     );

`endif

`ifdef USE_I2C

  i2c_master_single
    #(
      .CLK_HZ (I2C_CLK_HZ),
      .SCLK_HZ (I2C_SCLK_HZ)
      )
  i2c_master_single_0
    (
     .clk (clk),
     .reset (reset),
     .command (i2c_command),
     .start (i2c_start),
     .data_w (i2c_data_w),
     .r_ack (i2c_r_ack),
     .w_ack (i2c_w_ack),
     .data_r (i2c_data_r),
     .busy (i2c_busy),
     .scl (i2c_scl),
     .sda (i2c_sda)
     );

`endif

endmodule
