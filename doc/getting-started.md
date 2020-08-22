# Getting Started

Welcome to the Esprit project - ClojureScript for Microcontrollers (well, just the ESP32 for now). 

## Forward
Esprit attempts to provide a functional ClojureScript environment running atop the [Espruino](https://www.espruino.com/) JavaScript interpreter. As Espruino isn't quite feature-complete, some modifications to ClojureScript are necessary. This project attempts to automate that process and provide some functionality for Clojure-friendly abstractions to hardware-level configurations.

If you haven't already, check out the [Clojure/north talk](https://youtu.be/u1jr4v7dhoo) and the [Apropos episode](https://youtu.be/J0wF92Zvq2c). For help, check out the #esprit channel on the [Clojurians Slack](http://clojurians.net/).

## Prerequisites
To run Esprit, you will need an ESP32. However, not every ESP32 is created equal. Specifically, we require modules with at least 8MB of flash. The ESP32 WROVER module with 8MB of PSIRAM has been shown to work great. Additionally, you can use the purpose-built [Esprit board](https://github.com/mfikes/esprit-board), which comes pre-configured with battery management and blikenlights galore.

All of the interaction with the board itself is done with Espressif's [esptool.py](https://github.com/espressif/esptool). Make sure this is installed, and on your path before proceeding.

We are assuming that the device is plugged in to your machine via USB.

> Note:  If you are using the Esprit board, you will need the Silicon Labs CP2102N USB to UART chip [drivers](https://www.silabs.com/products/development-tools/software/usb-to-uart-bridge-vcp-drivers) if they are not already installed on your computer.

## I haz board, how REPL?
Alright hot shot, wanna jump in head first?
Create a `deps.edn` file with ClojureScript and Esprit
```clojure
{:deps {org.clojure/clojurescript {:mvn/version "1.10.764"}
        esprit {:mvn/version "0.9.0"}}}
```
Build a js file, containing the Esprit REPL, baking WiFi credentials into it (change `MySSID` and `MyWiFiPassword`):
```sh
clj -m cljs.main -co '{:closure-defines {esprit.repl/wifi-ssid "MySSID" esprit.repl/wifi-password "MyWiFiPassword"} :optimizations :simple :target :none :browser-repl false :process-shim false}' -c esprit.repl
```
> Normally we'd just have the Espruino persist the WiFi info via its existing capability to do so, but this is currently not reliable with this particular modified build, while baking it in as illustrated above works every time.

Then make a ROM binary from the compiled ClojureScript using
```sh
clj -m esprit.make-rom
```
Now that your binary is ready, we now have to prepare the board for upload.

> Note: These steps require `esptool.py` on your `PATH`. By default, this tool will search for the USB port where the ESP32 is connected, and this can be a bit slow. The search can be skipped by creating a `config.edn` file specifying the port. For example, if you are using the Esprit board, its contents would be: `{:serial-port "/dev/cu.SLAB_USBtoUART"}`.

First, erase whatever is on your board
```sh
clj -m esprit.flash --erase
```
Now bootstrap the Espruino runtime
```sh
clj -m esprit.flash --bootstrap
```
Finally, flash your binary
```sh
clj -m esprit.flash --flash out/main.bin
```
Open up the ESP32's com port, say with `screen` at the default baudrate of 115200. It wouldn't be a bad idea to hit reset at this point.
```sh
screen <your port> 115200
```
It can take about 15 seconds to load the ClojureScript runtime. (The Esprit board will flash its EVAL LED while this is occcuring.)

Then the code will attempt to join the WiFi. (The Esprit board will dimly light the CONN LED while this is occuring, and once connected to WiFi it will switch to doing short pulses until a REPL connection is established.)

Once the device is connected to WiFi, it will print a message like the following to the serial port like:
```
Ready for REPL Connections
Establish an Esprit REPL by executing
clj -m cljs.main -re esprit -ro '{:endpoint-address "10.0.0.1"}' -r
```

Copy this command, and then exit your terminal session (in screen this is done via Ctrl-a, k, y), and then issue the copied command to start the REPL.
