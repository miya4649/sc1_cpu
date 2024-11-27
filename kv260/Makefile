PROJECT = project_1
VITIS_WORKSPACE = vitis_workspace
VIVADO = $(XILINX_VIVADO)/bin/vivado
VITIS = $(XILINX_VITIS)/bin/vitis
RM = rm -rf
ROMS=$(wildcard ../*_code.bin ../*_data.bin ../*_code_rom.v ../*_data_rom.v ../*_code_mem.v ../*_data_mem.v ../*_code_tb.txt ../*_data_tb.txt)

all: asm tool vivado vivado-run vitis

run: tool
	make -C ../tools run

asm:
	make -C ../asm WIDEVGA=y VGA720P=y

tool:
	make -C ../tools

vivado:
	$(VIVADO) -mode batch -notrace -source vivado.tcl

vivado-run:
	$(VIVADO) -mode batch -notrace -source vivado-run.tcl

vitis:
	$(VITIS) -source vitisnew.py

clean:
	make -C ../tools clean
	make -C ../asm clean
	$(RM) $(PROJECT) $(VITIS_WORKSPACE) $(ROMS) vivado.log vivado*.jou .Xil *dynamic* *.log *.xpe .gitignore .lock .peers.ini
