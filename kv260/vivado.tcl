set board_type kv260
set rtl_top_name rtl_top
set rtl_files {rtl_top.v topinclude.v ../audio_output.v ../cdc_fifo.v ../cdc_synchronizer.v ../chr_bg.v ../default_code_mem.v ../default_code_rom.v ../default_data_mem.v ../default_data_mem_pair.v ../default_data_rom.v ../dual_clk_ram.v ../fifo.v ../frac_clk.v ../free_run_counter.v ../i2c_master_single.v ../one_shot.v ../r2w1_port_ram.v ../rw_port_ram.v ../sc1_cpu.v ../sc1_soc.v ../shift_register.v ../shift_register_vector.v ../shift_register_factory.v ../sprite.v ../uart.v ../vga_iface.v ../xlive_audio.v}
set pin_xdc_file {pins.xdc}
set timing_xdc_file {timings.xdc}
set project_name project_1
set project_dir project_1
set design_name design_1
set ps_ip xilinx.com:ip:zynq_ultra_ps_e
set ps_name zynq_ultra_ps_e_0
set init_rule xilinx.com:bd_rule:zynq_ultra_ps_e
set rtl_top_instance ${rtl_top_name}_0

if { $board_type eq "kr260" } {
    set board_parts [get_board_parts "*:kr260_som:*" -latest_file_version]
    set som_connection {som240_1_connector xilinx.com:kr260_carrier:som240_1_connector:1.0 som240_2_connector xilinx.com:kr260_carrier:som240_2_connector:1.0}
} else {
    set board_parts [get_board_parts "*:kv260_som:*" -latest_file_version]
    set som_connection {som240_1_connector xilinx.com:kv260_carrier:som240_1_connector:1.3}
}

# create project
create_project -name $project_name -force -dir $project_dir -part [get_property PART_NAME $board_parts]

set_property board_part $board_parts [current_project]

add_files -fileset constrs_1 -norecurse $pin_xdc_file

add_files -fileset constrs_1 -norecurse $timing_xdc_file
set_property used_in_synthesis false [get_files $timing_xdc_file]

add_files -fileset sources_1 -norecurse $rtl_files

set_property is_global_include true [get_files topinclude.v]

set_property board_connections $som_connection [current_project]

create_bd_design $design_name

current_bd_design $design_name

set top_instance [get_bd_cells /]

current_bd_instance $top_instance

# config bd
create_bd_cell -type ip -vlnv $ps_ip $ps_name

apply_bd_automation -rule $init_rule -config {apply_board_preset "1"} [get_bd_cells $ps_name]

set_property -dict [list \
CONFIG.PSU__USE__M_AXI_GP0  {0} \
CONFIG.PSU__USE__M_AXI_GP1  {0} \
CONFIG.PSU__USE__IRQ0 {0} \
CONFIG.PSU__FPGA_PL1_ENABLE {0} \
CONFIG.PSU__USE__AUDIO {1} \
CONFIG.PSU__USE__VIDEO {1} \
] [get_bd_cells $ps_name] 

create_bd_cell -type ip -vlnv xilinx.com:ip:clk_wiz:6.0 clk_wiz_0

set_property -dict [list \
CONFIG.PRIM_SOURCE {Global_buffer} \
CONFIG.RESET_TYPE {ACTIVE_LOW} \
] [get_bd_cells clk_wiz_0]

create_bd_cell -type ip -vlnv xilinx.com:ip:clk_wiz:6.0 clk_wiz_1

set_property -dict [list \
CONFIG.CLKOUT1_REQUESTED_OUT_FREQ {22.5792} \
CONFIG.PRIM_SOURCE {Global_buffer} \
CONFIG.RESET_TYPE {ACTIVE_LOW} \
CONFIG.USE_LOCKED {false} \
] [get_bd_cells clk_wiz_1]

create_bd_cell -type module -reference $rtl_top_name $rtl_top_instance


# port connection
connect_bd_net [get_bd_pins ${ps_name}/pl_clk0] [get_bd_pins clk_wiz_0/clk_in1] [get_bd_pins clk_wiz_1/clk_in1]
connect_bd_net [get_bd_pins ${ps_name}/pl_resetn0] [get_bd_pins clk_wiz_0/resetn] [get_bd_pins clk_wiz_1/resetn]

connect_bd_net [get_bd_pins ${rtl_top_instance}/clk] [get_bd_pins clk_wiz_0/clk_out1]

connect_bd_net [get_bd_pins ${rtl_top_instance}/resetn] [get_bd_pins clk_wiz_0/locked]

connect_bd_net [get_bd_pins ${rtl_top_instance}/clkv] [get_bd_pins ${ps_name}/dp_video_in_clk] [get_bd_pins ${ps_name}/dp_video_ref_clk]

connect_bd_net [get_bd_pins ${rtl_top_instance}/audio_data] [get_bd_pins ${ps_name}/dp_s_axis_audio_tdata]

connect_bd_net [get_bd_pins ${rtl_top_instance}/audio_id] [get_bd_pins ${ps_name}/dp_s_axis_audio_tid]

connect_bd_net [get_bd_pins ${rtl_top_instance}/audio_valid] [get_bd_pins ${ps_name}/dp_s_axis_audio_tvalid]

connect_bd_net [get_bd_pins ${rtl_top_instance}/video_color] [get_bd_pins ${ps_name}/dp_live_video_in_pixel1]

connect_bd_net [get_bd_pins ${rtl_top_instance}/video_de] [get_bd_pins ${ps_name}/dp_live_video_in_de]

connect_bd_net [get_bd_pins ${rtl_top_instance}/video_hsyncn] [get_bd_pins ${ps_name}/dp_live_video_in_hsync]

connect_bd_net [get_bd_pins ${rtl_top_instance}/video_vsyncn] [get_bd_pins ${ps_name}/dp_live_video_in_vsync]

connect_bd_net [get_bd_pins ${rtl_top_instance}/audio_ready] [get_bd_pins ${ps_name}/dp_s_axis_audio_tready]

connect_bd_net [get_bd_pins clk_wiz_1/clk_out1] [get_bd_pins ${rtl_top_instance}/clka] [get_bd_pins ${ps_name}/dp_s_axis_audio_clk]

make_bd_pins_external [get_bd_pins ${rtl_top_instance}/led]

make_bd_pins_external [get_bd_pins ${rtl_top_instance}/uart_txd]

make_bd_pins_external [get_bd_pins ${rtl_top_instance}/uart_rxd]


# make wrapper
current_bd_instance $top_instance

make_wrapper -files [get_files $project_dir/${project_name}.srcs/sources_1/bd/$design_name/${design_name}.bd] -top

add_files -norecurse $project_dir/${project_name}.gen/sources_1/bd/$design_name/hdl/${design_name}_wrapper.v

set_property top ${design_name}_wrapper [current_fileset]

update_compile_order -fileset sources_1

validate_bd_design

regenerate_bd_layout

save_bd_design
