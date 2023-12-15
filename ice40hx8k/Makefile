SRC=top.v ../r2w1_port_ram.v ../rw_port_ram.v ../sc1_cpu.v ../sc1_soc.v ../uart.v ../free_run_counter.v ../i2c_master_single.v ../default_code_rom.v ../default_data_rom.v ../default_code_mem.v ../default_data_mem.v ../default_data_mem_pair.v
PNROP=--hx8k
PCF=hx8kboard.pcf

all: pack.bin

prog: pack.bin
	iceprog $<

prog-ram: pack.bin
	iceprog -S $<

pack.bin: pnr.asc
	icepack $< $@

pnr.asc: synth.json
	nextpnr-ice40 $(PNROP) --json $< --pcf $(PCF) --asc $@

synth.json: $(SRC)
	yosys -p "read_verilog $^; synth_ice40 -json $@"

clean:
	rm -f *.json *.asc *.bin
