# Variables from brltty's Drivers/Braille/EuroBraille/Makefile.in. That file
# also defines clean:: and the eutp transfer tool, which ndk-build rejects.

DRIVER_CODE = eu
DRIVER_NAME = EuroBraille
DRIVER_VERSION = 2.0
DRIVER_DEVELOPERS = Yannick PLASSIARD <yan@mistigri.org>, Olivier BERT <obert01@mistigri.org>, Nicolas PITRE <nico@fluxnic.net>

SRC_FILES = eu_braille.c eu_clio.c eu_esysiris.c
