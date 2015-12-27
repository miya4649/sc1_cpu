#!/bin/sh

javac Asm.java AsmTop.java
java AsmTop > ../rom.v
