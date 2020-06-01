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
To make a ClojureScript ROM capable of running a REPL, first compile the ClojureScript via

	clj -A:make-repl

and then make a ROM binary from the compiled ClojureScript using

	clj -A:make-rom

You can then flash this ROM to your ESP32 via

	esptool.py --port /dev/cu.SLAB_USBtoUART --baud 2000000 write_flash 0x2C0000 out/main.bin

To establish a REPL into the ESP32, first connect to it using USB / serial and set it up so that it joins WiFi.

	screen /dev/cu.SLAB_USBtoUART 115200

Hit return to get a prompt (it can take about 15 seconds to load the ClojureScript runtime) and then configure the device to your WiFi network:

	var ssid = 'YOUR_SSID';
	var password = 'YOUR_SSID_PASSWORD';
	
	var wifi = require('Wifi');
	wifi.connect(ssid, {password: password}, function() {
	    console.log('Connected to Wifi.  IP address is:', wifi.getIP().ip);
	    wifi.save(); // Next reboot will auto-connect
	});
  
Once connected to WiFi, it will print a message like the following to the serial port like:

	Ready for REPL Connections
	Ensure this ESP32's REPL is being advertised via MDNS. For example, on macOS:
	dns-sd -P "Esprit ESP32 WROVER" _http._tcp local 53001 esprit.local 10.0.1.21

As indicated, manually advertise it via MDNS, and then connect to it using Esprit

	clj -m cljs.main -re esprit

To compile your own code for use on the ESP32, you can use `:optimizations` `:advanced` in your project and then make a ROM for it by executing the `esprit.make-rom` main.

If you want to instead have a ROM with your code where you can establish a REPL, instead use `:optimizations` `:simple` and somewhere in your source tree, require the `esprit.repl` namespace.

[1]:	https://gist.github.com/mfikes/5ed90e461229161ba9197461af888107
[2]:	http://planck-repl.org/releases/ESP32-REPL-2/bootloader.bin
[3]:	http://planck-repl.org/releases/ESP32-REPL-2/partitions_espruino.bin
[4]:	http://planck-repl.org/releases/ESP32-REPL-2/espruino_esp32.bin
