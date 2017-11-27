PROJECT = top
PCF = hx8kboard.pcf
DEVICE_SHORT = 8k
DEVICE = hx$(DEVICE_SHORT)
SOURCE = top.v ../r2w1_port_ram.v ../rw_port_ram.v ../sc1_cpu.v ../sc1_soc.v ../uart.v

all: $(PROJECT).rpt $(PROJECT).bin

$(PROJECT).blif: $(SOURCE)
	yosys -p 'synth_ice40 -top top -blif $(PROJECT).blif' $(SOURCE)

$(PROJECT).asc: $(PCF) $(PROJECT).blif
	arachne-pnr -r -d $(DEVICE_SHORT) -o $(PROJECT).asc -p $(PCF) $(PROJECT).blif

$(PROJECT).bin: $(PROJECT).asc
	icepack $(PROJECT).asc $(PROJECT).bin

$(PROJECT).rpt: $(PROJECT).asc
	icetime -d $(DEVICE) -mtr $(PROJECT).rpt $(PROJECT).asc

prog: $(PROJECT).bin
	iceprog $(PROJECT).bin

prog-ram: $(PROJECT).bin
	iceprog -S $(PROJECT).bin

clean:
	rm -f $(PROJECT).blif $(PROJECT).asc $(PROJECT).rpt $(PROJECT).bin
