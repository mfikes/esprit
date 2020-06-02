# Esprit
This repository contains support for ClojureScript on the ESP32 WROVER using Espruino.

## Prerequisite
Set up an ESP32 WROVER with partitions and expanded JSVar space (below modeled per [this gist][1]).
We are assuming that the device is connected on port `/dev/cu.SLAB_USBtoUART`.

	esptool.py --port /dev/cu.SLAB_USBtoUART erase_flash

Download bootloader, partitions, and Espruino engine:

- [`bootloader.bin`][2]
- [`partitions_espruino.bin`][3]
- [`espruino_esp32.bin`][4]	

Then flash via

	esptool.py --port /dev/cu.SLAB\_USBtoUART --baud 2000000 write\_flash 0x1000 bootloader.bin 0x8000 partitions\_espruino.bin 0x10000 espruino\_esp32.bin
## REPL

Make a REPL, baking the WiFi info into it:

        clj -m cljs.main -co co-repl.edn -co '{:closure-defines {esprit.repl/wifi-ssid "MySSID" esprit.repl/wifi-password "MyWiFiPassword"}}' -c esprit.repl

(Normally we'd just have the Espruino persist the WiFi info via its existing capability to do so, but this is currently not reliable with this particular modified build, while baking it in as illustrated above works every time.)

Then make a ROM binary from the compiled ClojureScript using

	clj -A:make-rom

You can then flash this ROM to your ESP32 via

	esptool.py --port /dev/cu.SLAB_USBtoUART --baud 2000000 write_flash 0x2C0000 out/main.bin

To establish a REPL into the ESP32, we need to first learn its IP address. We can do this by connecting to it via the USB serial port:

	screen /dev/cu.SLAB_USBtoUART 115200

Hit return to get a prompt and press the reset button on the device. It can take about 15 seconds to load the ClojureScript runtime) and configure the device to your WiFi network:
  
Once the device is connected to WiFi, it will print a message like the following to the serial port like:

	Ready for REPL Connections
	Establish an Esprit REPL by executing
	clj -m cljs.main -re esprit -ro '{:endpoint-address "10.0.0.1"}' -r

Copy this command, and then exit your terminal session (in screen this is done via `Ctrl-a`, `k`, `y`), and then issue the copied command to start the REPL.

### Other Stuff

To compile your own code for use on the ESP32, you can use `:optimizations` `:advanced` in your project and then make a ROM for it by executing the `esprit.make-rom` main.

If you want to instead have a ROM with your code where you can establish a REPL, instead use `:optimizations` `:simple` and somewhere in your source tree, require the `esprit.repl` namespace.

[1]:	https://gist.github.com/mfikes/5ed90e461229161ba9197461af888107
[2]:	http://planck-repl.org/releases/ESP32-REPL-2/bootloader.bin
[3]:	http://planck-repl.org/releases/ESP32-REPL-2/partitions_espruino.bin
[4]:	http://planck-repl.org/releases/ESP32-REPL-2/espruino_esp32.bin
