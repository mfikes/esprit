# Esprit

This repository contains work in progress for support for ClojureScript on the ESP32 WROVER using Espruino.

Prerequisite: Set up an ESP32 WROVER with partitions and expanded JSVar space as per [this gist](https://gist.github.com/mfikes/5ed90e461229161ba9197461af888107).

To make a ClojureScript ROM capable of running a REPL, first compile the ClojureScript via

```
clj -A:make-repl
```

and then make a ROM binary from the compiled ClojureScript using

```
clj -A:make-rom
```

You can then flash this ROM to your ESP32 via

```
esptool.py --chip esp32 --port <PORT> write_flash 0x2C0000 out/main.bin
```

To establish a REPL into the ESP32, first connect to it using USB / serial and set it up so that it joins WiFi. Once connected to WiFi, it will print a message like the following to the serial port like


```
Ready for REPL Connections
Ensure this ESP32's REPL is being advertised via MDNS. For example, on macOS:
dns-sd -P "Ambly ESP32 WROVER" _http._tcp local 53001 ambly.local 10.0.1.21
```

As indicated, manually advertise it via MDNS, and then connect to it using Ambly

```
clj -Sdeps '{:deps {org.clojure/clojurescript {:mvn/version "1.10.597"} ambly {:git/url "https://github.com/mfikes/ambly" :sha "7e84e590aeb66db09dd76e55ef701bef97bc3145"}}}' -m cljs.main -re ambly
```

To compile your own code for use on the ESP32, you can use `:optimizations` `:advanced` in your project and then make a ROM for it by executing the `esprit.make-rom` main.

If you want to instead have a ROM with your code where you can establish a REPL, instead use `:optimizations` `:simple` and somewhere in your source tree, require the `esprit.repl` namespace.
