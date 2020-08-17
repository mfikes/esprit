# Building and Board Flashing

To flash binaries to your board, you will need a copy of [esptool](https://github.com/espressif/esptool)
Additionally, as the ESP32 does not have native USB, you may need to install drivers for the USB to UART adapter that is on your board. For the Esprit board, you will need the Silicon Labs CP2102N [drivers](https://www.silabs.com/products/development-tools/software/usb-to-uart-bridge-vcp-drivers).

## esprit.flash
Esprit provides a wrapper for esptool to give some convenient utilities to flashing esprit-specific binaries. The flags are given in each section below, and can also be shown via the command-line `-h` or `--help` argument to `clj -m esprit.flash`. All of the `esprit.flash` commands take an optional `-p` or `--port` option to specify the upload port, but esptool can usually find the correct port without it. At the project level, the port can be defined with the `:serial-port` config option in `config.edn`.

## Erasing
Given an ESP32 in an unknown state, it may be a good idea to completely erase the flash to start from a clean slate, this can be done with:
```bash
esptool.py erase_flash
```
Or with the provided functionality
```bash
clj -m esprit.flash --erase
```

## Bootstraping
The Esprit project is built atop the Espruino JS interpreter, and as such, Espruino must be uploaded to the ESP32 first. This phase of flashing "bootstraping" is only needed once, unless of course the underlying Espruino version gets updated. The bootstrap payload consists of `bootloader.bin`, `partitions_espruino.bin`, and `espruino_esp32.bin`.
To learn more how these were built, read [this-gist](https://gist.github.com/kiranshila/9f7ff8a538f6098e642d108b62a5ede5).

```bash
esptool.py --baud 2000000 write_flash /
0x1000 bootloader.bin /
0x8000 partitions_espruino.bin /
0x10000 espruino_esp32.bin
```
To bootstrap using the included binaries, run
```bash
clj -m esprit.flash --bootstrap
```

## Building
A few compiler options need to be set to get a working build of Esprit:
```clojure
{:optimizations :simple 
 :target :none 
 :browser-repl false 
 :process-shim false}
```
If you are using the Esprit REPL, you can bake in WiFi credentials by setting the additional compiler options:
```clojure
{:closure-defines {esprit.repl/wifi-ssid "MySSID"
                   esprit.repl/wifi-password "MyWiFiPassword"}
```
Using the CLI tools, this can be done by directly calling the options with
```bash
clj -m cljs.main -co '{:closure-defines {esprit.repl/wifi-ssid "MySSID" esprit.repl/wifi-password "MyWiFiPassword"} :optimizations :simple :target :none :browser-repl false :process-shim false}' -c <your compile target>
```
Or with an EDN file on the classpath and
```bash
clj -m cljs.main -co @compile-options.edn -c <your compile target>
```

## Flashing
Assuming some ClojureScript project has been built to `out/main.js`, a binary can be created with `clj -m esprit.make-rom`. This command will create a binary suitable for writing to the `js_code` partition in `out/main.bin`. This can be written with
```bash
esptool.py --baud 2000000 write_flash 0x320000 out/main.bin
```
To flash with the built in functionality, run
```bash
clj -m esprit.flash --flash out/main.bin
```
