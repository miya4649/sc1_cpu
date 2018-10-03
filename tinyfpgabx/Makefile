PROJECT=top
PCF=tinyfpgabx.pcf
DEVICE_SHORT=8k
DEVICE=lp$(DEVICE_SHORT)
PACKAGE=cm81
SOURCE_LIB=uart.v mini_audio.v mini_vga.v mini_sprite.v fifo.v frac_clk.v r2w1_port_ram.v rw_port_ram.v shift_register_vector.v sc1_cpu.v sc1_soc.v default_code_mem.v default_data_mem.v default_data_mem_pair.v free_run_counter.v i2c_master_single.v
SOURCE=top.v
TMPFILE=$(PROJECT).blif $(PROJECT).asc $(PROJECT).rpt $(PROJECT).bin .sconsign.dblite $(SOURCE_LIB)
CHIPDB_PATH=$(HOME)/.apio/packages/toolchain-icestorm/share/icebox

all: $(PROJECT).bin

$(SOURCE_LIB):
	cp ../$@ ./

$(PROJECT).blif: $(SOURCE) $(SOURCE_LIB)
	yosys -p 'synth_ice40 -top $(PROJECT) -blif $(PROJECT).blif' $(SOURCE) $(SOURCE_LIB)

$(PROJECT).asc: $(PCF) $(PROJECT).blif
	arachne-pnr -r -d $(DEVICE_SHORT) -P $(PACKAGE) -o $(PROJECT).asc -p $(PCF) $(PROJECT).blif

$(PROJECT).bin: $(PROJECT).asc
	icepack $(PROJECT).asc $(PROJECT).bin

timing: $(PROJECT).asc
	icetime -C $(CHIPDB_PATH)/chipdb-$(DEVICE_SHORT).txt -d $(DEVICE) -mtr $(PROJECT).rpt $(PROJECT).asc

prog: $(PROJECT).bin
	tinyprog -p $(PROJECT).bin

clean:
	rm -f $(TMPFILE)
