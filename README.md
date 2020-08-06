# Esprit
This repository contains support for ClojureScript on the ESP32 WROVER (or [Esprit board](https://github.com/mfikes/esprit-board)) using Espruino.

If you have any questions about this stuff, be sure to check out the `#esprit` [Clojurians Slack][1] channel.

Check out the [Clojure/north Talk](https://youtu.be/u1jr4v7dhoo) and [Apropos Episode](https://youtu.be/J0wF92Zvq2c).

## Prerequisites

> Schematics and other artifacts for a customized ESP32 WROVER board are available in the [esprit-board][2] repository, but any ESP32 WROVER with 8 MiB of PSIRAM will work.

Ensure you have Espressif's `esptool.py` tool available (for flashing ESP32s). This can be obtained at https://github.com/espressif/esptool

In this section, we will set up an ESP32 WROVER with partitions and expanded JSVar space (the pre-built files below are modeled per [this gist][3]).

We are assuming that the device is plugged in to your machine via USB and connected on port `/dev/cu.SLAB_USBtoUART`. (_This will be the case if you are using the Esprit board, and in that case you will need the Silicon Labs CP2102N USB to UART chip [drivers][4] if they are not already installed on your computer. On Linux devices, it may mount on `/dev/ttyUSB0`. You can have esptool search for the port by running: `esptool.py chip_id`_)

	esptool.py --port /dev/cu.SLAB_USBtoUART erase_flash

Download bootloader, partitions, and Espruino engine:

- [`bootloader.bin`][5]
- [`partitions_espruino.bin`][6]
- [`espruino_esp32.bin`][7]	

Then flash via

	esptool.py --port /dev/cu.SLAB_USBtoUART --baud 2000000 write_flash 0x1000 bootloader.bin 0x8000 partitions_espruino.bin 0x10000 espruino_esp32.bin

## REPL

`deps.edn`:

	{:deps {org.clojure/clojurescript {:mvn/version "1.10.597"}
	        esprit {:mvn/version "0.6.0"}}}

Make a REPL, baking WiFi credentials into it (change `MySSID` and `MyWiFiPassword`):

	clj -m cljs.main -co '{:closure-defines {esprit.repl/wifi-ssid "MySSID" esprit.repl/wifi-password "MyWiFiPassword"} :optimizations :simple :target :none :browser-repl false :process-shim false}' -c esprit.repl

> Normally we'd just have the Espruino persist the WiFi info via its existing capability to do so, but this is currently not reliable with this particular modified build, while baking it in as illustrated above works every time.

Then make a ROM binary from the compiled ClojureScript using

	clj -m esprit.make-rom

You can then flash this ROM to your ESP32 via

	esptool.py --port /dev/cu.SLAB_USBtoUART --baud 2000000 write_flash 0x2C0000 out/main.bin

To establish a REPL into the ESP32, we need to first learn its IP address. We can do this by connecting to it via the USB serial port:

	screen /dev/cu.SLAB_USBtoUART 115200

Hit return to get a prompt and press the reset button on the device. 

It can take about 15 seconds to load the ClojureScript runtime. (_The Esprit board will flash its EVAL LED while this is occcuring._) 

Then the code will attempt to join the WiFi. (_The Esprit board will dimly light the CONN LED while this is occuring, and once connected to WiFi it will switch to doing short pulses until a REPL connection is established._)

Once the device is connected to WiFi, it will print a message like the following to the serial port like:

	Ready for REPL Connections
	Establish an Esprit REPL by executing
	clj -m cljs.main -re esprit -ro '{:endpoint-address "10.0.0.1"}' -r

Copy this command, and then exit your terminal session (in `screen` this is done via `Ctrl-a`, `k`, `y`), and then issue the copied command to start the REPL.

### Known issues

See https://github.com/mfikes/esprit/issues for known issues.

### Other Stuff

To compile your own code for use on the ESP32, you can use `:optimizations` `:advanced` in your project and then make a ROM for it by executing the `esprit.make-rom` main.

If you want to instead have a ROM with your code where you can establish a REPL, instead use `:optimizations` `:simple` and somewhere in your source tree, require the `esprit.repl` namespace.

[1]:	http://clojurians.net
[2]:	https://github.com/mfikes/esprit-board
[3]:	https://gist.github.com/mfikes/5ed90e461229161ba9197461af888107
[4]:	https://www.silabs.com/products/development-tools/software/usb-to-uart-bridge-vcp-drivers
[5]:	http://planck-repl.org/releases/ESP32-REPL-2/bootloader.bin
[6]:	http://planck-repl.org/releases/ESP32-REPL-2/partitions_espruino.bin
[7]:	http://planck-repl.org/releases/ESP32-REPL-2/espruino_esp32.bin
